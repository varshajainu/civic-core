package com.googledream.civic.concierge.service;

import com.google.cloud.vertexai.api.GenerateContentResponse;
import com.google.cloud.vertexai.generativeai.ContentMaker;
import com.google.cloud.vertexai.generativeai.GenerativeModel;
import com.google.cloud.vertexai.generativeai.PartMaker;
import com.google.cloud.vertexai.generativeai.ResponseHandler;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Service
public class GeminiService {

    private final GenerativeModel generativeModel;

    public GeminiService(GenerativeModel generativeModel) {
        this.generativeModel = generativeModel;
    }

    /**
     * Sends a text prompt plus zero or more images to Gemini.
     * images may be null or empty for text-only requests.
     */
    public String analyzeMultipleImages(String promptText, MultipartFile[] images) throws IOException {
        List<Object> contentData = new ArrayList<>();
        contentData.add(promptText);

        if (images != null) {
            for (MultipartFile file : images) {
                if (file != null && !file.isEmpty()) {
                    byte[] imageBytes = file.getBytes();
                    String mimeType = file.getContentType();
                    contentData.add(PartMaker.fromMimeTypeAndData(mimeType, imageBytes));
                }
            }
        }

        GenerateContentResponse response = generativeModel.generateContent(
                ContentMaker.fromMultiModalData(contentData.toArray())
        );

        return ResponseHandler.getText(response);
    }
}