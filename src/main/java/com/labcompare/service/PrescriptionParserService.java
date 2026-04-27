package com.labcompare.service;

import com.labcompare.model.Test;
import com.labcompare.repository.TestRepository;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Reads an uploaded prescription file (PDF / image / text) and identifies
 * which catalog tests are mentioned.
 *
 * KEY FIXES:
 * 1. Word-order independent matching — "MRI BRAIN" matches "Brain MRI"
 * 2. Partial significant word matching — "BONE MARROW EXAMINATION" matches "Bone Marrow Biopsy"
 * 3. containsPhrase() fix — \b regex broke for multi-word names
 * 4. Acronym min length 3 — avoids false positives like "bm"
 * 5. Description keyword boost — helps admin-added tests
 */
@Service
public class PrescriptionParserService {

    private static final Logger log = LoggerFactory.getLogger(PrescriptionParserService.class);

    private final TestRepository testRepository;

    @Value("${labcompare.prescription.tesseract-path:tesseract}")
    private String tesseractPath;

    @Value("${labcompare.prescription.ocr-enabled:true}")
    private boolean ocrEnabled;

    public PrescriptionParserService(TestRepository testRepository) {
        this.testRepository = testRepository;
    }

    // -------------------------------------------------------------------------
    // Text extraction
    // -------------------------------------------------------------------------

    public String extractText(MultipartFile file) {
        if (file == null || file.isEmpty()) return "";
        String name = Optional.ofNullable(file.getOriginalFilename()).orElse("").toLowerCase();
        try {
            if (name.endsWith(".pdf"))  return extractPdf(file);
            if (isImage(name))          return extractImageViaTesseract(file);
            if (name.endsWith(".txt") || name.endsWith(".csv"))
                return new String(file.getBytes(), StandardCharsets.UTF_8);
            try { return new String(file.getBytes(), StandardCharsets.UTF_8); }
            catch (Exception ignored) { return ""; }
        } catch (Throwable t) {
            log.warn("[Prescription] Could not extract text from '{}': {}", name, t.getMessage());
            return "";
        }
    }

    private boolean isImage(String name) {
        return name.endsWith(".png") || name.endsWith(".jpg") || name.endsWith(".jpeg")
                || name.endsWith(".tif") || name.endsWith(".tiff")
                || name.endsWith(".bmp") || name.endsWith(".gif") || name.endsWith(".webp");
    }

    private String extractPdf(MultipartFile file) {
        try (PDDocument doc = Loader.loadPDF(file.getBytes())) {
            PDFTextStripper stripper = new PDFTextStripper();
            String text = stripper.getText(doc);
            return text == null ? "" : text;
        } catch (Throwable t) {
            log.warn("[Prescription] PDF parsing failed: {}", t.getMessage());
            return "";
        }
    }

    private String extractImageViaTesseract(MultipartFile file) {
        if (!ocrEnabled) { log.info("[Prescription] OCR disabled."); return ""; }
        File tmpInput = null;
        File tmpOutput = null;
        try {
            String ext = getExtension(Optional.ofNullable(file.getOriginalFilename()).orElse("img.jpg"));
            tmpInput = File.createTempFile("rx_in_", "." + ext);
            file.transferTo(tmpInput);
            tmpOutput = File.createTempFile("rx_out_", "");
            String outputBase = tmpOutput.getAbsolutePath();

            ProcessBuilder pb = new ProcessBuilder(tesseractPath,
                    tmpInput.getAbsolutePath(), outputBase, "-l", "eng");
            pb.redirectErrorStream(true);
            Process process = pb.start();
            String out = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            int exitCode = process.waitFor();
            if (exitCode != 0) {
                log.warn("[Prescription] Tesseract error ({}): {}", exitCode, out);
                return "";
            }
            File resultFile = new File(outputBase + ".txt");
            if (!resultFile.exists()) return "";
            String result = new String(java.nio.file.Files.readAllBytes(resultFile.toPath()), StandardCharsets.UTF_8);
            resultFile.delete();
            log.info("[Prescription] Tesseract extracted: {}", result.trim());
            return result;
        } catch (Throwable t) {
            log.warn("[Prescription] Tesseract failed: {}", t.getMessage());
            return "";
        } finally {
            if (tmpInput  != null) tmpInput.delete();
            if (tmpOutput != null) tmpOutput.delete();
        }
    }

    private String getExtension(String filename) {
        int dot = filename.lastIndexOf('.');
        return dot >= 0 ? filename.substring(dot + 1) : "jpg";
    }

    // -------------------------------------------------------------------------
    // Matching
    // -------------------------------------------------------------------------

    public List<Test> matchTests(String text) {
        if (text == null || text.isBlank()) return Collections.emptyList();
        String norm = normalize(text);
        List<Test> all;
        try { all = testRepository.findAll(); }
        catch (Throwable t) {
            log.warn("[Prescription] Could not load test catalog: {}", t.getMessage());
            return Collections.emptyList();
        }

        Map<Long, Integer> scores = new HashMap<>();
        for (Test t : all) {
            int s = scoreMatch(norm, t);
            if (s > 0) {
                scores.put(t.getId(), s);
                log.info("[Prescription] Matched '{}' with score {}", t.getName(), s);
            }
        }
        return all.stream()
                .filter(t -> scores.containsKey(t.getId()))
                .sorted((a, b) -> Integer.compare(scores.get(b.getId()), scores.get(a.getId())))
                .collect(Collectors.toList());
    }

    private String normalize(String s) {
        return s.toLowerCase()
                .replaceAll("[^a-z0-9 ]+", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }

    /**
     * Scoring strategy:
     *
     * 100 — exact phrase match           "brain mri" found as-is
     *  95 — all words found (any order)  "mri brain" matches "brain mri"  ← KEY FIX
     *  90 — bracketed abbr found         "cbc" from "Complete Blood Count (CBC)"
     *  85 — acronym found (≥3 chars)     "lft" from "Liver Function Test"
     *  70 — majority words match         "bone marrow" matches "Bone Marrow Biopsy" ← KEY FIX
     *  50 — partial match with desc      description keywords boost score
     */
    private int scoreMatch(String text, Test t) {
        if (t.getName() == null) return 0;

        String fullName = normalize(t.getName());
        if (fullName.isEmpty()) return 0;

        // Strip bracketed part for word matching: "Complete Blood Count (CBC)" → "complete blood count"
        String nameClean = fullName.replaceAll("\\(.*?\\)", "").trim();
        String[] nameWords = nameClean.split(" ");

        // Collect significant words (≥3 chars) from the test name
        List<String> sigWords = Arrays.stream(nameWords)
                .filter(w -> w.length() >= 3)
                .collect(Collectors.toList());

        // ---------------------------------------------------------------
        // 1. Exact phrase match (e.g. "brain mri" found exactly in text)
        // ---------------------------------------------------------------
        if (containsPhrase(text, nameClean)) return 100;

        // ---------------------------------------------------------------
        // 2. ALL significant words found in ANY order — KEY FIX
        //    "MRI BRAIN" in prescription matches "Brain MRI" in DB
        //    "BONE MARROW EXAMINATION" matches "Bone Marrow Biopsy"
        //    because "bone" + "marrow" are both found regardless of order
        // ---------------------------------------------------------------
        if (!sigWords.isEmpty()) {
            long hits = sigWords.stream().filter(w -> containsPhrase(text, w)).count();

            // All words found → very high confidence even if order differs
            if (hits == sigWords.size() && sigWords.size() >= 1) return 95;

            // Most words found (all-but-one) → good match
            // e.g. "bone marrow" (2/3 words of "Bone Marrow Biopsy") → still matches
            if (sigWords.size() >= 2 && hits >= sigWords.size() - 1) return 70;

            // At least half the words found → possible match
            if (sigWords.size() >= 3 && hits >= (sigWords.size() / 2) + 1) return 50;
        }

        // ---------------------------------------------------------------
        // 3. Bracketed abbreviation — "Complete Blood Count (CBC)" → try "cbc"
        // ---------------------------------------------------------------
        String bracketAbbr = extractBracketedAbbr(t.getName());
        if (bracketAbbr != null && !bracketAbbr.isBlank()) {
            String normAbbr = normalize(bracketAbbr);
            if (!normAbbr.isBlank() && containsPhrase(text, normAbbr)) return 90;
        }

        // ---------------------------------------------------------------
        // 4. Auto acronym (min 3 chars to avoid noise like "bm")
        // ---------------------------------------------------------------
        if (nameWords.length >= 2) {
            StringBuilder ab = new StringBuilder();
            for (String w : nameWords) if (!w.isEmpty()) ab.append(w.charAt(0));
            String acr = ab.toString();
            if (acr.length() >= 3 && containsPhrase(text, acr)) return 85;
        }

        // ---------------------------------------------------------------
        // 5. Description keyword boost for admin-added tests
        //    e.g. admin added "Brain MRI" with description "magnetic resonance imaging brain"
        // ---------------------------------------------------------------
        if (t.getDescription() != null && !t.getDescription().isBlank()) {
            String desc = normalize(t.getDescription());
            List<String> descWords = Arrays.stream(desc.split(" "))
                    .filter(w -> w.length() >= 5)
                    .collect(Collectors.toList());
            long descHits = descWords.stream().filter(w -> containsPhrase(text, w)).count();
            if (!descWords.isEmpty() && descHits >= 2) return 50;
        }

        return 0;
    }

    private String extractBracketedAbbr(String name) {
        if (name == null) return null;
        int open = name.indexOf('(');
        int close = name.indexOf(')', open + 1);
        if (open >= 0 && close > open) return name.substring(open + 1, close).trim();
        return null;
    }

    /**
     * FIX: \b word-boundary regex breaks for multi-word phrases like "brain mri"
     * because \b doesn't work across spaces.
     * Phrases (with space) → String.contains()
     * Single words → \b regex to avoid substring false positives
     */
    private boolean containsPhrase(String haystack, String needle) {
        if (needle == null || needle.isBlank()) return false;
        try {
            if (needle.contains(" ")) return haystack.contains(needle);
            return Pattern.compile("\\b" + Pattern.quote(needle) + "\\b")
                          .matcher(haystack).find();
        } catch (Throwable t) {
            return haystack.contains(needle);
        }
    }
}