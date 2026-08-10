package com.resume.analyzer.controller;

import com.resume.analyzer.dto.ApiResponse;
import com.resume.analyzer.service.VrezerAiAgentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/chat")
@CrossOrigin(origins = "*")
public class AiChatController {

    @Autowired
    private VrezerAiAgentService vrezerAiAgentService;

    @PostMapping("/ask")
    public ResponseEntity<ApiResponse<Map<String, String>>> askChatbot(
            @RequestBody Map<String, Object> request,
            @RequestHeader(value = "X-GEMINI-API-KEY", required = false) String headerApiKey) {
        
        String prompt = String.valueOf(request.getOrDefault("prompt", "")).trim();
        if (prompt.isEmpty()) {
            return ResponseEntity.ok(ApiResponse.ok("Please ask a question.", Map.of("reply", "Please enter a valid query.")));
        }

        String customKey = headerApiKey;
        if (request.containsKey("apiKey") && request.get("apiKey") != null && !String.valueOf(request.get("apiKey")).trim().isEmpty()) {
            customKey = String.valueOf(request.get("apiKey")).trim();
        }

        @SuppressWarnings("unchecked")
        Map<String, Object> candidateContext = (Map<String, Object>) request.get("candidateContext");

        String answer = vrezerAiAgentService.askGeneralQuestion(prompt, candidateContext, customKey);

        Map<String, String> body = new HashMap<>();
        body.put("reply", answer);
        body.put("timestamp", new Date().toString());

        return ResponseEntity.ok(ApiResponse.ok("Chatbot reply generated", body));
    }
}
