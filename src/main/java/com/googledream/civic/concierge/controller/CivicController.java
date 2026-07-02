package com.googledream.civic.concierge.controller;

import com.googledream.civic.concierge.service.BigQueryService;
import com.googledream.civic.concierge.service.GeminiService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@RestController
@RequestMapping("/api/civic")
public class CivicController {

    private final GeminiService geminiService;
    private final BigQueryService bigQueryService;

    private static final Pattern TICKET_ID_PATTERN = Pattern.compile("TKT-\\d+");
    private static final Pattern RISK_SCORE_PATTERN = Pattern.compile("(?:Risk\\s*Score)\\**:?\\**\\s*(\\d+)", Pattern.CASE_INSENSITIVE);
    private static final Pattern CATEGORY_PATTERN = Pattern.compile("(?:Hazard\\s*Category)\\**:?\\**\\s*([^\\n]+)", Pattern.CASE_INSENSITIVE);

    public CivicController(GeminiService geminiService, BigQueryService bigQueryService) {
        this.geminiService = geminiService;
        this.bigQueryService = bigQueryService;
    }

    @PostMapping("/report")
    public ResponseEntity<Map<String, String>> handleReport(
            @RequestParam(value = "text", required = false) String text,
            @RequestParam(value = "images", required = false) MultipartFile[] images) {

        Map<String, String> responseBody = new HashMap<>();
        boolean hasImages = images != null && images.length > 0;
        boolean hasText = text != null && !text.isBlank();

        if (!hasImages && !hasText) {
            responseBody.put("response", "Error: No text or image provided.");
            return ResponseEntity.badRequest().body(responseBody);
        }

        String userMessage = hasText ? text : "No description provided.";

        try {
            String systemPrompt;

            if (hasImages) {
                systemPrompt = "You are a friendly, professional Civic Infrastructure AI Concierge. " +
                        "Analyze the attached images and the user's message: '" + userMessage + "'.\n" +
                        "You MUST follow this exact structure with no deviation:\n\n" +
                        "**PART 1 - CITIZEN ADVICE:**\n" +
                        "[Polite, conversational paragraph acknowledging the user, confirming location only if given, safety advice.]\n\n" +
                        "Rule 1: If an image is clearly NOT a civic hazard (person, pet, random indoor object), state in Part 1 that you cannot process that specific image.\n\n" +
                        "Rule 2 (MANDATORY): For EVERY valid civic hazard image, you MUST output a ticket block in EXACTLY this format — never omit it, never paraphrase the field names:\n\n" +
                        "--- TICKET GENERATED ---\n" +
                        "**Ticket ID:** TKT-[Random 5 Digits]\n" +
                        "**Hazard Category:** [e.g. Road Hazard (Pothole)]\n" +
                        "**Risk Score:** [1-10] / 10\n" +
                        "**Routing To:** [Appropriate Department]\n\n" +
                        "If there is at least one valid hazard image, the response is INVALID unless it contains at least one '--- TICKET GENERATED ---' block.";
            } else {
                systemPrompt = "You are a friendly, professional Civic Infrastructure AI Concierge. " +
                        "A citizen sent this message with no photo attached: '" + userMessage + "'.\n" +
                        "Reply in 2-4 warm, conversational sentences acknowledging what they described, " +
                        "and ask them to attach at least one photo so you can assess it and open a ticket. " +
                        "Do not invent a ticket and do not use the '--- TICKET GENERATED ---' format.";
            }

            String aiResult = geminiService.analyzeMultipleImages(systemPrompt, images);
            responseBody.put("response", aiResult);

            if (hasImages) {
                try {
                    persistTickets(aiResult);
                } catch (Exception bqEx) {
                    System.err.println("BigQuery persistence failed: " + bqEx.getMessage());
                    bqEx.printStackTrace();
                }
            }

            return ResponseEntity.ok(responseBody);

        } catch (Exception e) {
            e.printStackTrace();
            responseBody.put("response", "Internal Server Error: Could not process the files or connect to Vertex AI.");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(responseBody);
        }
    }

    private void persistTickets(String aiResult) {
        String[] blocks = aiResult.split("(?i)TICKET GENERATED");

        for (int i = 1; i < blocks.length; i++) {
            String block = blocks[i];

            Matcher idMatcher = TICKET_ID_PATTERN.matcher(block);
            String ticketId = idMatcher.find()
                    ? idMatcher.group()
                    : "TKT-" + ThreadLocalRandom.current().nextInt(10000, 99999);

            Matcher riskMatcher = RISK_SCORE_PATTERN.matcher(block);
            int riskScore = riskMatcher.find() ? Integer.parseInt(riskMatcher.group(1)) : 5;

            Matcher categoryMatcher = CATEGORY_PATTERN.matcher(block);
            String category = categoryMatcher.find()
                    ? categoryMatcher.group(1).replace("*", "").trim()
                    : "Uncategorized";

            bigQueryService.insertTicket(ticketId, block.trim(), category, riskScore);
        }
    }
}