package com.example.demo.service.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;

/**
 * Thin wrapper around Groq's OpenAI-compatible /chat/completions endpoint.
 * Reads credentials from .env via application.properties.
 */
@Component
public class GroqClient {

    private static final Logger log = LoggerFactory.getLogger(GroqClient.class);

    private final String apiKey;
    private final String model;
    private final String baseUrl;
    private final RestTemplate restTemplate;
    private final ObjectMapper mapper = new ObjectMapper();

    public GroqClient(
            @Value("${app.ai.groq-api-key:}") String apiKey,
            @Value("${app.ai.groq-model:llama-3.3-70b-versatile}") String model,
            @Value("${app.ai.groq-base-url:https://api.groq.com/openai/v1}") String baseUrl
    ) {
        this.apiKey = apiKey == null ? "" : apiKey.trim();
        this.model = model;
        this.baseUrl = baseUrl;
        this.restTemplate = new RestTemplate();
    }

    public boolean isEnabled() {
        return !apiKey.isBlank();
    }

    public static class Message {
        public final String role;
        public final String content;
        public Message(String role, String content) { this.role = role; this.content = content; }
        public static Message system(String content)    { return new Message("system", content); }
        public static Message user(String content)      { return new Message("user", content); }
    }

    /**
     * Send a chat-completion request and return the assistant's content string.
     * If {@code jsonMode} is true, the response will be a JSON object string.
     */
    public String complete(List<Message> messages, double temperature, boolean jsonMode) {
        if (!isEnabled()) {
            throw new AiNotConfiguredException();
        }

        try {
            ObjectNode body = mapper.createObjectNode();
            body.put("model", model);
            body.put("temperature", temperature);
            if (jsonMode) {
                ObjectNode rf = body.putObject("response_format");
                rf.put("type", "json_object");
            }
            var arr = body.putArray("messages");
            for (Message m : messages) {
                ObjectNode msg = arr.addObject();
                msg.put("role", m.role);
                msg.put("content", m.content);
            }

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(apiKey);

            HttpEntity<String> entity = new HttpEntity<>(mapper.writeValueAsString(body), headers);
            ResponseEntity<String> resp = restTemplate.exchange(
                    baseUrl + "/chat/completions", HttpMethod.POST, entity, String.class);

            JsonNode root = mapper.readTree(resp.getBody());
            JsonNode content = root.path("choices").path(0).path("message").path("content");
            return content.asText("");
        } catch (HttpClientErrorException.Unauthorized e) {
            log.warn("Groq API key is invalid or revoked: {}", e.getResponseBodyAsString());
            throw new AiKeyInvalidException();
        } catch (HttpClientErrorException e) {
            log.warn("Groq API error: {} {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new RuntimeException("AI service rejected the request: " + e.getStatusCode());
        } catch (Exception e) {
            log.error("Groq call failed", e);
            throw new RuntimeException("AI service is unavailable. Please try again.");
        }
    }

    public JsonNode completeJson(List<Message> messages, double temperature) {
        String raw = complete(messages, temperature, true);
        try {
            return mapper.readTree(raw);
        } catch (Exception e) {
            log.warn("Groq returned non-JSON despite json_object format: {}", raw);
            throw new RuntimeException("AI returned an unparseable response.");
        }
    }

    public static class AiNotConfiguredException extends RuntimeException {
        public AiNotConfiguredException() {
            super("AI is not configured. Set APP_AI_GROQ_API_KEY in your .env file.");
        }
    }

    public static class AiKeyInvalidException extends RuntimeException {
        public AiKeyInvalidException() {
            super("The Groq API key is invalid or revoked. Update APP_AI_GROQ_API_KEY in .env "
                    + "(get a new key at https://console.groq.com/keys) and restart the backend.");
        }
    }
}
