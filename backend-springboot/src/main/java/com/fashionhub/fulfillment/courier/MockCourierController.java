package com.fashionhub.fulfillment.courier;

import com.fashionhub.fulfillment.config.AppProperties;
import com.fasterxml.jackson.databind.JsonNode;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Mock courier modelled on the ShipEngine "create label" API. Called by the REST connector of the
 * carrier_label_generation process. To switch to the real ShipEngine, change the connector secrets
 * COURIER_API_BASE_URL / COURIER_API_KEY and adapt the result expression.
 *
 * <p>Demo behaviour: postal code {@code 00000} is rejected with HTTP 422 (address not deliverable),
 * which drives the ADDRESS_INVALID error path of the BPMN model.
 */
@RestController
@RequestMapping("/mock-courier")
public class MockCourierController {
    private static final Logger log = LoggerFactory.getLogger(MockCourierController.class);

    private record Label(String labelId, String trackingNumber, String reference, Instant createdAt) {
    }

    private final Map<String, Label> labels = new ConcurrentHashMap<>();
    private final Map<String, Label> byTracking = new ConcurrentHashMap<>();
    private final AppProperties.Courier props;

    public MockCourierController(AppProperties.Courier props) {
        this.props = props;
    }

    @PostMapping(value = "/v1/labels", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> createLabel(
            @RequestHeader(value = "API-Key", required = false) String apiKey,
            @RequestBody JsonNode body) {

        if (apiKey == null || !apiKey.equals(props.apiKey())) {
            return error(HttpStatus.UNAUTHORIZED, "unauthorized", "Invalid or missing API-Key header");
        }
        JsonNode to = body.path("ship_to");
        String postal = to.path("postal_code").asText("").trim();
        if (to.path("address_line1").asText("").isBlank() || to.path("city_locality").asText("").isBlank()
                || postal.isBlank() || postal.equals("00000")) {
            log.warn("Mock courier rejected address (postal code '{}')", postal);
            return error(HttpStatus.UNPROCESSABLE_ENTITY, "invalid_address",
                    "Address is not deliverable (postal code '" + postal + "')");
        }
        double weight = body.path("weight").path("value").asDouble(0);
        if (weight <= 0) {
            return error(HttpStatus.UNPROCESSABLE_ENTITY, "invalid_weight", "Weight must be positive");
        }

        String labelId = "se-" + UUID.randomUUID().toString().substring(0, 12);
        String tracking = "MC" + String.format("%012d", ThreadLocalRandom.current().nextLong(1_000_000_000_000L));
        Label label = new Label(labelId, tracking, body.path("reference").asText(""), Instant.now());
        labels.put(labelId, label);
        byTracking.put(tracking, label);

        BigDecimal cost = BigDecimal.valueOf(49 + weight * 20).setScale(2, RoundingMode.HALF_UP); // INR
        Map<String, Object> download = new LinkedHashMap<>();
        download.put("pdf", props.publicBaseUrl().replaceAll("/+$", "") + "/mock-courier/labels/" + labelId + ".pdf");
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("label_id", labelId);
        out.put("status", "completed");
        out.put("tracking_number", tracking);
        out.put("carrier_code", "mock_dhl");
        out.put("service_code", body.path("service_code").asText("dhl_parcel"));
        out.put("shipment_cost", Map.of("currency", "inr", "amount", cost));
        out.put("label_download", download);
        out.put("created_at", label.createdAt().toString());
        log.info("Mock courier created label {} tracking {} for {}", labelId, tracking, label.reference());
        return ResponseEntity.status(HttpStatus.CREATED).body(out);
    }

    @GetMapping("/v1/tracking/{trackingNumber}")
    public ResponseEntity<Map<String, Object>> track(@PathVariable String trackingNumber) {
        Label l = byTracking.get(trackingNumber);
        if (l == null) {
            return error(HttpStatus.NOT_FOUND, "not_found", "Unknown tracking number");
        }
        return ResponseEntity.ok(Map.of("tracking_number", l.trackingNumber(), "status_code", "IT",
                "status_description", "In transit", "reference", l.reference()));
    }

    @GetMapping(value = "/labels/{labelId}.pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> pdf(@PathVariable String labelId) {
        Label l = labels.get(labelId);
        if (l == null) {
            return ResponseEntity.notFound().build();
        }
        byte[] pdf = simplePdf(List.of("MOCK DHL PARCEL LABEL", "", "Tracking: " + l.trackingNumber(),
                "Reference: " + l.reference(), "Label id: " + l.labelId(), "Created: " + l.createdAt()));
        return ResponseEntity.ok().contentType(MediaType.APPLICATION_PDF).body(pdf);
    }

    private ResponseEntity<Map<String, Object>> error(HttpStatus status, String code, String message) {
        Map<String, Object> err = new LinkedHashMap<>();
        err.put("error_type", status.is4xxClientError() ? "validation" : "system");
        err.put("error_code", code);
        err.put("message", message);
        return ResponseEntity.status(status).body(Map.of("request_id", UUID.randomUUID().toString(),
                "errors", List.of(err), "message", message));
    }

    /** Builds a minimal, valid single-page PDF (no external library needed). */
    private static byte[] simplePdf(List<String> lines) {
        StringBuilder content = new StringBuilder("BT /F1 14 Tf 40 540 Td 20 TL\n");
        for (String line : lines) {
            String safe = line.replaceAll("[^\\x20-\\x7E]", "?").replace("\\", "\\\\")
                    .replace("(", "\\(").replace(")", "\\)");
            content.append('(').append(safe).append(") Tj T*\n");
        }
        content.append("ET");
        List<String> objects = List.of(
                "<< /Type /Catalog /Pages 2 0 R >>",
                "<< /Type /Pages /Kids [3 0 R] /Count 1 >>",
                "<< /Type /Page /Parent 2 0 R /MediaBox [0 0 420 595] /Contents 4 0 R "
                        + "/Resources << /Font << /F1 5 0 R >> >> >>",
                "<< /Length " + content.length() + " >>\nstream\n" + content + "\nendstream",
                "<< /Type /Font /Subtype /Type1 /BaseFont /Courier >>");
        StringBuilder pdf = new StringBuilder("%PDF-1.4\n");
        int[] offsets = new int[objects.size()];
        for (int i = 0; i < objects.size(); i++) {
            offsets[i] = pdf.length();
            pdf.append(i + 1).append(" 0 obj\n").append(objects.get(i)).append("\nendobj\n");
        }
        int xref = pdf.length();
        pdf.append("xref\n0 ").append(objects.size() + 1).append("\n0000000000 65535 f \n");
        for (int off : offsets) {
            pdf.append(String.format("%010d 00000 n \n", off));
        }
        pdf.append("trailer\n<< /Size ").append(objects.size() + 1).append(" /Root 1 0 R >>\nstartxref\n")
                .append(xref).append("\n%%EOF");
        return pdf.toString().getBytes(StandardCharsets.ISO_8859_1);
    }
}
