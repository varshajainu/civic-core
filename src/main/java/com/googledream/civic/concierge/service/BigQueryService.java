package com.googledream.civic.concierge.service;

import com.google.cloud.bigquery.*;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@Service
public class BigQueryService {

    private final BigQuery bigQuery;

    public BigQueryService() {
        this.bigQuery = BigQueryOptions.getDefaultInstance().getService();
    }

    /**
     * Inserts one ticket row into civic_data.triage_tickets.
     * Row order in the map doesn't matter to BigQuery — the TABLE SCHEMA
     * controls column order. Make sure the table itself is defined as:
     * ticket_id (STRING), ai_analysis (STRING), category (STRING), risk_score (INTEGER), timestamp (TIMESTAMP)
     */
    public void insertTicket(String ticketId, String aiAnalysis, String category, int riskScore) {
        TableId tableId = TableId.of("civic_data", "triage_tickets");

        Map<String, Object> rowContent = new HashMap<>();
        rowContent.put("ticket_id", ticketId);
        rowContent.put("ai_analysis", aiAnalysis);
        rowContent.put("category", category);
        rowContent.put("risk_score", riskScore); // Sending as a true integer
        rowContent.put("timestamp", Instant.now().toString()); // ISO string perfectly maps to BQ TIMESTAMP

        InsertAllRequest request = InsertAllRequest.newBuilder(tableId)
                .addRow(rowContent)
                .build();

        InsertAllResponse response = bigQuery.insertAll(request);

        if (response.hasErrors()) {
            response.getInsertErrors().forEach((idx, errors) ->
                    errors.forEach(err -> System.err.println("BigQuery insert error: " + err.getMessage()))
            );
        }
    }
}