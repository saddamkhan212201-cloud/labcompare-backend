//package com.labcompare.controller;
//
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.http.ResponseEntity;
//import org.springframework.web.bind.annotation.*;
//import org.springframework.web.multipart.MultipartFile;
//
//import java.io.*;
//import java.net.HttpURLConnection;
//import java.net.URL;
//import java.util.Map;
//
//@RestController
//@RequestMapping("/api/prescription")
//@CrossOrigin(origins = "*")
//public class PrescriptionController {
//
//    @Value("${ocr.api.key:helloworld}")
//    private String ocrApiKey;
//
//    @PostMapping("/ocr")
//    public ResponseEntity<String> extractText(@RequestParam("file") MultipartFile file) {
//        try {
//            String boundary = "----Boundary" + System.currentTimeMillis();
//            URL url = new URL("https://api.ocr.space/parse/image");
//            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
//            conn.setDoOutput(true);
//            conn.setRequestMethod("POST");
//            conn.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);
//            conn.setConnectTimeout(30000);
//            conn.setReadTimeout(30000);
//
//            try (OutputStream os = conn.getOutputStream();
//                 PrintWriter writer = new PrintWriter(new OutputStreamWriter(os, "UTF-8"), true)) {
//
//                // apikey
//                writer.append("--").append(boundary).append("\r\n");
//                writer.append("Content-Disposition: form-data; name=\"apikey\"").append("\r\n");
//                writer.append("\r\n").append(ocrApiKey).append("\r\n").flush();
//
//                // language
//                writer.append("--").append(boundary).append("\r\n");
//                writer.append("Content-Disposition: form-data; name=\"language\"").append("\r\n");
//                writer.append("\r\n").append("eng").append("\r\n").flush();
//
//                // OCREngine
//                writer.append("--").append(boundary).append("\r\n");
//                writer.append("Content-Disposition: form-data; name=\"OCREngine\"").append("\r\n");
//                writer.append("\r\n").append("2").append("\r\n").flush();
//
//                // scale
//                writer.append("--").append(boundary).append("\r\n");
//                writer.append("Content-Disposition: form-data; name=\"scale\"").append("\r\n");
//                writer.append("\r\n").append("true").append("\r\n").flush();
//
//                // detectOrientation
//                writer.append("--").append(boundary).append("\r\n");
//                writer.append("Content-Disposition: form-data; name=\"detectOrientation\"").append("\r\n");
//                writer.append("\r\n").append("true").append("\r\n").flush();
//
//                // isOverlayRequired
//                writer.append("--").append(boundary).append("\r\n");
//                writer.append("Content-Disposition: form-data; name=\"isOverlayRequired\"").append("\r\n");
//                writer.append("\r\n").append("false").append("\r\n").flush();
//
//                // file
//                String fileName = file.getOriginalFilename() != null ? file.getOriginalFilename() : "prescription.jpg";
//                writer.append("--").append(boundary).append("\r\n");
//                writer.append("Content-Disposition: form-data; name=\"file\"; filename=\"").append(fileName).append("\"").append("\r\n");
//                writer.append("Content-Type: ").append(file.getContentType()).append("\r\n");
//                writer.append("\r\n").flush();
//
//                os.write(file.getBytes());
//                os.flush();
//
//                writer.append("\r\n").flush();
//                writer.append("--").append(boundary).append("--").append("\r\n").flush();
//            }
//
//            // Read response
//            int status = conn.getResponseCode();
//            InputStream is = status >= 400 ? conn.getErrorStream() : conn.getInputStream();
//            BufferedReader reader = new BufferedReader(new InputStreamReader(is, "UTF-8"));
//            StringBuilder sb = new StringBuilder();
//            String line;
//            while ((line = reader.readLine()) != null) sb.append(line);
//            reader.close();
//
//            if (status != 200) {
//                return ResponseEntity.status(status).body("{\"error\":\"OCR API returned status " + status + ": " + sb + "\"}");
//            }
//
//            return ResponseEntity.ok(sb.toString());
//
//        } catch (Exception e) {
//            e.printStackTrace();
//            return ResponseEntity.status(500).body("{\"error\":\"" + e.getMessage() + "\"}");
//        }
//    }
//}

package com.labcompare.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Base64;

@RestController
@RequestMapping("/api/prescription")
@CrossOrigin(origins = "*")
public class PrescriptionController {

    @Value("${groq.api.key}")
    private String groqApiKey;

    @PostMapping("/ocr")
    public ResponseEntity<String> extractText(@RequestParam("file") MultipartFile file) {
        try {
            // Convert image to base64
            byte[] imageBytes = file.getBytes();
            String base64Image = Base64.getEncoder().encodeToString(imageBytes);
            String mimeType = file.getContentType() != null ? file.getContentType() : "image/jpeg";

            // Build JSON request
            String jsonBody = "{"
                + "\"model\": \"meta-llama/llama-4-scout-17b-16e-instruct\","
                + "\"messages\": ["
                + "  {"
                + "    \"role\": \"user\","
                + "    \"content\": ["
                + "      {"
                + "        \"type\": \"image_url\","
                + "        \"image_url\": {"
                + "          \"url\": \"data:" + mimeType + ";base64," + base64Image + "\""
                + "        }"
                + "      },"
                + "      {"
                + "        \"type\": \"text\","
                + "        \"text\": \"You are a medical prescription reader. Extract ALL lab test names from this prescription image. Return ONLY the test names as plain text, one per line. Include full names and abbreviations like CBC, LFT, KFT, TSH, HbA1c, Vitamin D, Vitamin B12, Lipid Profile, Urine Routine etc. Do not add any explanation, just the test names.\""
                + "      }"
                + "    ]"
                + "  }"
                + "],"
                + "\"max_tokens\": 500"
                + "}";

            // Call Groq API
            URL url = new URL("https://api.groq.com/openai/v1/chat/completions");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setDoOutput(true);
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty("Authorization", "Bearer " + groqApiKey);
            conn.setConnectTimeout(30000);
            conn.setReadTimeout(30000);

            try (OutputStream os = conn.getOutputStream()) {
                os.write(jsonBody.getBytes("UTF-8"));
                os.flush();
            }

            // Read response
            int status = conn.getResponseCode();
            InputStream is = status >= 400 ? conn.getErrorStream() : conn.getInputStream();
            BufferedReader reader = new BufferedReader(new InputStreamReader(is, "UTF-8"));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) sb.append(line);
            reader.close();

            if (status != 200) {
                return ResponseEntity.status(status).body("{\"error\":\"Groq API error " + status + ": " + sb + "\"}");
            }

            // Extract text from Groq response and wrap in OCR.space format
            String groqResponse = sb.toString();
            String extractedText = extractContentFromGroqResponse(groqResponse);

            // Return in same format as before so frontend works without changes
            String ocrFormatResponse = "{\"ParsedResults\":[{\"ParsedText\":\""
                + extractedText.replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "")
                + "\"}],\"IsErroredOnProcessing\":false}";

            return ResponseEntity.ok(ocrFormatResponse);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body("{\"error\":\"" + e.getMessage() + "\"}");
        }
    }

    private String extractContentFromGroqResponse(String json) {
        try {
            // Extract content from: choices[0].message.content
            int contentIndex = json.indexOf("\"content\":");
            if (contentIndex == -1) return "";
            int start = json.indexOf("\"", contentIndex + 10) + 1;
            int end = json.indexOf("\"", start);
            // Handle escaped characters
            StringBuilder result = new StringBuilder();
            while (end != -1 && json.charAt(end - 1) == '\\') {
                result.append(json, start, end - 1);
                start = end;
                end = json.indexOf("\"", end + 1);
            }
            result.append(json, start, end == -1 ? json.length() : end);
            return result.toString()
                .replace("\\n", "\n")
                .replace("\\t", "\t")
                .replace("\\\"", "\"");
        } catch (Exception e) {
            return "";
        }
    }
}