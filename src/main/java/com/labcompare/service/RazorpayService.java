package com.labcompare.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Map;

@Service
public class RazorpayService {

    private static final Logger log = LoggerFactory.getLogger(RazorpayService.class);
    private static final String RAZORPAY_ORDERS_URL = "https://api.razorpay.com/v1/orders";

    @Value("${razorpay.key-id}")
    private String keyId;

    @Value("${razorpay.key-secret}")
    private String keySecret;

    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Creates a Razorpay order.
     * @param amountInPaise  amount in paise (₹1 = 100 paise, so ₹99 → 9900)
     * @param receipt        your internal ref shown in Razorpay dashboard
     * @param notes          any description string
     * @return               full Razorpay order object (id, amount, currency, status…)
     */
    public Map<String, Object> createOrder(long amountInPaise, String receipt, String notes) throws Exception {

        Map<String, Object> payload = Map.of(
            "amount",   amountInPaise,
            "currency", "INR",
            "receipt",  receipt,
            "notes",    Map.of("description", notes != null ? notes : "LabChain Payment")
        );

        String body        = objectMapper.writeValueAsString(payload);
        String credentials = Base64.getEncoder()
                .encodeToString((keyId + ":" + keySecret).getBytes(StandardCharsets.UTF_8));

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(RAZORPAY_ORDERS_URL))
                .header("Authorization", "Basic " + credentials)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() == 200 || response.statusCode() == 201) {
            @SuppressWarnings("unchecked")
            Map<String, Object> result = objectMapper.readValue(response.body(), Map.class);
            log.info("[Razorpay] ✅ Order created | id={} receipt={}", result.get("id"), receipt);
            return result;
        }

        log.error("[Razorpay] ❌ Order creation failed {} | {}", response.statusCode(), response.body());
        throw new Exception("Razorpay order creation failed: " + response.body());
    }

    /**
     * Verifies the payment signature sent by Razorpay after checkout.
     *
     * Razorpay signs:   razorpay_order_id + "|" + razorpay_payment_id
     * using HMAC-SHA256 with your key_secret.
     *
     * If the computed hex matches razorpay_signature → payment is genuine.
     * This runs fully offline — no HTTP call needed.
     */
    public boolean verifySignature(String orderId, String paymentId, String signature) {
        try {
            String message = orderId + "|" + paymentId;

            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(keySecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] hash     = mac.doFinal(message.getBytes(StandardCharsets.UTF_8));
            String computed = HexFormat.of().formatHex(hash);

            boolean valid = computed.equals(signature);
            if (valid) {
                log.info("[Razorpay] ✅ Signature OK | orderId={} paymentId={}", orderId, paymentId);
            } else {
                log.warn("[Razorpay] ⚠️  Signature MISMATCH | orderId={}", orderId);
            }
            return valid;

        } catch (Exception e) {
            log.error("[Razorpay] ❌ Signature verification error: {}", e.getMessage());
            return false;
        }
    }
}