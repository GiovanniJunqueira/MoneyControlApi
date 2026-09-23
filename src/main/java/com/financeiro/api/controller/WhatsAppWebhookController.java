package com.financeiro.api.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.financeiro.api.service.WhatsAppBotService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;

/**
 * Webhook do WhatsApp Cloud API (Meta). Endpoint público (sem JWT - ver SecurityConfig) porque quem
 * chama é o Meta, não um usuário logado; a autenticidade da chamada é validada pela assinatura
 * HMAC no header X-Hub-Signature-256 (app secret), não por token de sessão.
 */
@Slf4j
@RestController
@RequestMapping("/whatsapp/webhook")
public class WhatsAppWebhookController {

    private final WhatsAppBotService whatsAppBotService;
    private final ObjectMapper objectMapper;
    private final String verifyToken;
    private final String appSecret;

    public WhatsAppWebhookController(WhatsAppBotService whatsAppBotService, ObjectMapper objectMapper,
                                      @Value("${app.whatsapp.verify-token:}") String verifyToken,
                                      @Value("${app.whatsapp.app-secret:}") String appSecret) {
        this.whatsAppBotService = whatsAppBotService;
        this.objectMapper = objectMapper;
        this.verifyToken = verifyToken;
        this.appSecret = appSecret;
    }

    /** Handshake de verificação que o Meta faz UMA VEZ, quando você configura a URL do webhook no
     * painel deles - precisa devolver exatamente o "hub.challenge" recebido, em texto puro. */
    @GetMapping
    public ResponseEntity<String> verify(@RequestParam("hub.mode") String mode,
                                          @RequestParam("hub.verify_token") String token,
                                          @RequestParam("hub.challenge") String challenge) {
        if ("subscribe".equals(mode) && !verifyToken.isBlank() && verifyToken.equals(token)) {
            return ResponseEntity.ok(challenge);
        }
        return ResponseEntity.status(403).build();
    }

    @PostMapping
    public ResponseEntity<Void> receive(@RequestBody String rawBody, HttpServletRequest request) {
        if (!isValidSignature(rawBody, request.getHeader("X-Hub-Signature-256"))) {
            log.warn("Webhook do WhatsApp recebido com assinatura inválida - ignorado.");
            return ResponseEntity.status(403).build();
        }

        try {
            JsonNode root = objectMapper.readTree(rawBody);
            for (JsonNode entry : root.path("entry")) {
                for (JsonNode change : entry.path("changes")) {
                    JsonNode value = change.path("value");
                    for (JsonNode message : value.path("messages")) {
                        handleMessage(message);
                    }
                }
            }
        } catch (Exception e) {
            log.error("Falha ao processar webhook do WhatsApp", e);
        }
        // sempre 200 - o Meta reenvia (várias vezes, com backoff) se não receber 2xx rápido.
        return ResponseEntity.ok().build();
    }

    private void handleMessage(JsonNode message) {
        String from = message.path("from").asText(null);
        String text = message.path("text").path("body").asText(null);
        if (from == null || text == null) {
            return; // outros tipos de mensagem (imagem, áudio, figurinha...) - v1 só entende texto.
        }
        whatsAppBotService.handleIncomingMessage(from, text);
    }

    private boolean isValidSignature(String rawBody, String signatureHeader) {
        if (appSecret.isBlank()) {
            log.warn("WHATSAPP_APP_SECRET não configurado - pulando validação de assinatura (só ok em teste local).");
            return true;
        }
        if (signatureHeader == null || !signatureHeader.startsWith("sha256=")) {
            return false;
        }
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(appSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] computed = mac.doFinal(rawBody.getBytes(StandardCharsets.UTF_8));
            String computedHex = HexFormat.of().formatHex(computed);
            String receivedHex = signatureHeader.substring("sha256=".length());
            return MessageDigest.isEqual(computedHex.getBytes(StandardCharsets.UTF_8), receivedHex.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            log.error("Falha ao validar assinatura do webhook do WhatsApp", e);
            return false;
        }
    }
}
