package com.financeiro.api.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;

/** Envia mensagens de texto via WhatsApp Cloud API (Meta) - mesmo estilo de HTTP client do
 * MailService (java.net.http, sem SDK). Falha de envio só loga - o webhook já respondeu 200 pro
 * Meta antes disso, então não tem pra quem propagar o erro. */
@Slf4j
@Service
public class WhatsAppClient {

    private final HttpClient httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
    private final ObjectMapper objectMapper;
    private final String accessToken;
    private final String phoneNumberId;

    public WhatsAppClient(ObjectMapper objectMapper,
                           @Value("${app.whatsapp.access-token:}") String accessToken,
                           @Value("${app.whatsapp.phone-number-id:}") String phoneNumberId) {
        this.objectMapper = objectMapper;
        this.accessToken = accessToken;
        this.phoneNumberId = phoneNumberId;
    }

    public void sendText(String toPhone, String text) {
        if (accessToken.isBlank() || phoneNumberId.isBlank()) {
            log.warn("WHATSAPP_ACCESS_TOKEN/WHATSAPP_PHONE_NUMBER_ID não configurados - mensagem não enviada: {}", text);
            return;
        }
        try {
            Map<String, Object> payload = Map.of(
                    "messaging_product", "whatsapp",
                    "to", toPhone,
                    "type", "text",
                    "text", Map.of("body", text)
            );
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://graph.facebook.com/v20.0/" + phoneNumberId + "/messages"))
                    .header("Authorization", "Bearer " + accessToken)
                    .header("Content-Type", "application/json")
                    .timeout(Duration.ofSeconds(10))
                    .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(payload)))
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 300) {
                log.error("Meta recusou a mensagem de WhatsApp pra {}: {} - {}", toPhone, response.statusCode(), response.body());
            }
        } catch (Exception e) {
            log.error("Falha ao enviar mensagem de WhatsApp pra {}", toPhone, e);
        }
    }
}
