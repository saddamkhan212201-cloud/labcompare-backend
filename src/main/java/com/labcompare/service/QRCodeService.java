package com.labcompare.service;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import com.labcompare.dto.BookingDTO;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

/**
 * Generates UPI QR code for LabCompare.
 * ALL payments go to LabCompare UPI — settled with labs nightly.
 */
@Service
public class QRCodeService {

    // LabCompare's UPI ID — all user payments come here
    private static final String LABCOMPARE_UPI_ID  = "saddamkhan212201@oksbi";
    private static final String LABCOMPARE_UPI_NAME = "LabCompare";

    /**
     * Generates a scannable UPI QR code for the booking.
     * User scans → GPay/PhonePe/Paytm opens → amount pre-filled → one tap to pay.
     * Returns base64-encoded PNG string for embedding in email.
     */
    public String generateBookingQR(BookingDTO booking) {
        double amount = booking.getTotalAmount() != null ? booking.getTotalAmount() : 0.0;

        // Standard UPI deep link — works with ALL UPI apps
        String upiUrl = String.format(
                "upi://pay?pa=%s&pn=%s&am=%.0f&tn=%s&cu=INR",
                LABCOMPARE_UPI_ID,
                LABCOMPARE_UPI_NAME,
                amount,
                ("LabCompare-" + booking.getBookingRef()).replace(" ", "%20")
        );

        try {
            Map<EncodeHintType, Object> hints = new HashMap<>();
            hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.H); // highest quality
            hints.put(EncodeHintType.MARGIN, 2);
            hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");

            QRCodeWriter writer = new QRCodeWriter();
            BitMatrix matrix = writer.encode(upiUrl, BarcodeFormat.QR_CODE, 300, 300, hints);

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(matrix, "PNG", out);
            return Base64.getEncoder().encodeToString(out.toByteArray());

        } catch (WriterException | IOException e) {
            throw new RuntimeException("Failed to generate QR code: " + e.getMessage(), e);
        }
    }
}