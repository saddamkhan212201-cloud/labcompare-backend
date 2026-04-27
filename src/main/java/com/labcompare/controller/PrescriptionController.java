package com.labcompare.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Map;

@RestController
@RequestMapping("/api/prescription")
@CrossOrigin(origins = "*")
public class PrescriptionController {

    @Value("${ocr.api.key:helloworld}")
    private String ocrApiKey;

    @PostMapping("/ocr")
    public ResponseEntity<String> extractText(@RequestParam("file") MultipartFile file) {
        try {
            String boundary = "----Boundary" + System.currentTimeMillis();
            URL url = new URL("https://api.ocr.space/parse/image");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setDoOutput(true);
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);
            conn.setConnectTimeout(30000);
            conn.setReadTimeout(30000);

            try (OutputStream os = conn.getOutputStream();
                 PrintWriter writer = new PrintWriter(new OutputStreamWriter(os, "UTF-8"), true)) {

                // apikey
                writer.append("--").append(boundary).append("\r\n");
                writer.append("Content-Disposition: form-data; name=\"apikey\"").append("\r\n");
                writer.append("\r\n").append(ocrApiKey).append("\r\n").flush();

                // language
                writer.append("--").append(boundary).append("\r\n");
                writer.append("Content-Disposition: form-data; name=\"language\"").append("\r\n");
                writer.append("\r\n").append("eng").append("\r\n").flush();

                // OCREngine
                writer.append("--").append(boundary).append("\r\n");
                writer.append("Content-Disposition: form-data; name=\"OCREngine\"").append("\r\n");
                writer.append("\r\n").append("2").append("\r\n").flush();

                // scale
                writer.append("--").append(boundary).append("\r\n");
                writer.append("Content-Disposition: form-data; name=\"scale\"").append("\r\n");
                writer.append("\r\n").append("true").append("\r\n").flush();

                // detectOrientation
                writer.append("--").append(boundary).append("\r\n");
                writer.append("Content-Disposition: form-data; name=\"detectOrientation\"").append("\r\n");
                writer.append("\r\n").append("true").append("\r\n").flush();

                // isOverlayRequired
                writer.append("--").append(boundary).append("\r\n");
                writer.append("Content-Disposition: form-data; name=\"isOverlayRequired\"").append("\r\n");
                writer.append("\r\n").append("false").append("\r\n").flush();

                // file
                String fileName = file.getOriginalFilename() != null ? file.getOriginalFilename() : "prescription.jpg";
                writer.append("--").append(boundary).append("\r\n");
                writer.append("Content-Disposition: form-data; name=\"file\"; filename=\"").append(fileName).append("\"").append("\r\n");
                writer.append("Content-Type: ").append(file.getContentType()).append("\r\n");
                writer.append("\r\n").flush();

                os.write(file.getBytes());
                os.flush();

                writer.append("\r\n").flush();
                writer.append("--").append(boundary).append("--").append("\r\n").flush();
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
                return ResponseEntity.status(status).body("{\"error\":\"OCR API returned status " + status + ": " + sb + "\"}");
            }

            return ResponseEntity.ok(sb.toString());

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body("{\"error\":\"" + e.getMessage() + "\"}");
        }
    }
}