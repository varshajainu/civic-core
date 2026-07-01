package com.googledream.civic.concierge.controller;

import com.google.cloud.vertexai.VertexAI;
import com.google.cloud.vertexai.generativeai.GenerativeModel;
import com.google.cloud.vertexai.api.GenerateContentResponse;
import com.google.cloud.vertexai.generativeai.ResponseHandler;
import com.google.cloud.vertexai.generativeai.ContentMaker;
import com.google.cloud.vertexai.generativeai.PartMaker;
import com.google.cloud.bigquery.BigQuery;
import com.google.cloud.bigquery.BigQueryOptions;
import com.google.cloud.bigquery.InsertAllRequest;
import com.google.cloud.bigquery.InsertAllResponse;
import com.google.cloud.bigquery.TableId;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/civic")
public class CivicController {

    private final GenerativeModel model;
    private final List<String> chatHistory = new ArrayList<>();
    private final List<String> bannedKeywords = List.of("ignore previous", "hacker", "jailbreak", "write a poem");
    private final String projectId = "civic-concierge-hackathon";

    public CivicController() {
        VertexAI vertexAI = new VertexAI(projectId, "us-central1");
        this.model = new GenerativeModel("gemini-2.5-flash", vertexAI);
    }

    @PostMapping("/report")
    public ResponseEntity<?> processReport(
            @RequestParam(value = "text", required = false) String userInput,
            @RequestParam(value = "image", required = false) MultipartFile imageFile) {

        if ((userInput == null || userInput.trim().isEmpty()) && (imageFile == null || imageFile.isEmpty())) {
            return ResponseEntity.badRequest().body(Map.of("error", "Must provide text or an image."));
        }

        if (userInput != null) {
            String lowerInput = userInput.toLowerCase();
            for (String keyword : bannedKeywords) {
                if (lowerInput.contains(keyword)) {
                    return ResponseEntity.badRequest().body(Map.of("error", "Security violation detected."));
                }
            }
            chatHistory.add("User: " + userInput);
        } else {
            chatHistory.add("User uploaded an image for analysis.");
        }

        try {
            String generatedTicketId = "TKT-" + (int)(Math.random() * 90000 + 10000);

            StringBuilder textPrompt = new StringBuilder();
            textPrompt.append("You are an automated Civic Triage Agent. Identify the user's city or region based on their prompt, if provided. ")
                    .append("If the user refers to an image but none is provided, explicitly ask them to upload the photo. Do not assume damage without an image. ")
                    .append("If a civic issue is identified, structure your response in TWO parts:\n")
                    .append("PART 1 - CITIZEN ADVICE: Provide a concise 2-step bulleted list advising the citizen on next steps. \n")
                    .append("PART 2 - SYSTEM ALERT TICKET: Generate a structured text block formatted EXACTLY like this:\n")
                    .append("--- TICKET GENERATED ---\n")
                    .append("**Ticket ID:** ").append(generatedTicketId).append("\n")
                    .append("**Hazard Category:** [Identify category]\n")
                    .append("**Risk Score:** [Assign a number 1-10 based on severity]\n")
                    .append("**Routing To:** [Relevant City Department]\n\n")
                    .append("Conversation History:\n");

            // Loop history so the AI has context of what is happening
            int start = Math.max(0, chatHistory.size() - 6);
            for (int i = start; i < chatHistory.size(); i++) {
                textPrompt.append(chatHistory.get(i)).append("\n");
            }
            textPrompt.append("AI:");

            GenerateContentResponse response;

            if (imageFile != null && !imageFile.isEmpty()) {
                byte[] imageBytes = imageFile.getBytes();
                String mimeType = imageFile.getContentType();

                response = model.generateContent(
                        ContentMaker.fromMultiModalData(
                                PartMaker.fromMimeTypeAndData(mimeType, imageBytes),
                                textPrompt.toString()
                        )
                );
            } else {
                response = model.generateContent(textPrompt.toString());
            }

            String aiResponse = ResponseHandler.getText(response);
            chatHistory.add("AI: " + aiResponse);

            if (aiResponse.contains("Risk Score:")) {
                System.out.println("ALERT: High Risk Ticket " + generatedTicketId + " queued for BigQuery ingestion.");
                saveToBigQuery(generatedTicketId, aiResponse);
            }

            return ResponseEntity.ok(Map.of("response", aiResponse));

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    private void saveToBigQuery(String ticketId, String aiResponse) {
        try {
            BigQuery bigquery = BigQueryOptions.newBuilder().setProjectId(projectId).build().getService();
            TableId tableId = TableId.of("civic_data", "triage_tickets");
            Map<String, Object> rowContent = new HashMap<>();
            rowContent.put("ticket_id", ticketId);
            rowContent.put("ai_analysis", aiResponse);
            rowContent.put("timestamp", java.time.Instant.now().toString());

            InsertAllResponse response = bigquery.insertAll(
                    InsertAllRequest.newBuilder(tableId)
                            .addRow(rowContent)
                            .build()
            );

            if (response.hasErrors()) {
                System.err.println("Error inserting into BigQuery: " + response.getInsertErrors());
            } else {
                System.out.println("Successfully saved Ticket " + ticketId + " to BigQuery.");
            }
        } catch (Exception e) {
            System.err.println("BigQuery connection failed: " + e.getMessage());
        }
    }

    @PostMapping("/reset")
    public ResponseEntity<?> resetChat() {
        chatHistory.clear();
        return ResponseEntity.ok(Map.of("status", "Chat history cleared"));
    }
}
