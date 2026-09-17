package com.financeiro.api.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class MailService {

    private final JavaMailSender mailSender;
    private final String fromAddress;
    private final String frontendUrl;

    public MailService(JavaMailSender mailSender,
                        @Value("${spring.mail.username:}") String fromAddress,
                        @Value("${app.frontend-url}") String frontendUrl) {
        this.mailSender = mailSender;
        this.fromAddress = fromAddress;
        this.frontendUrl = frontendUrl;
    }

    /**
     * Falha de envio (SMTP mal configurado, fora do ar etc.) fica só no log - o chamador
     * (AuthService.forgotPassword) sempre responde com sucesso genérico pro front, pra não
     * revelar se um e-mail existe ou não na base.
     */
    public void sendPasswordReset(String toEmail, String name, String token) {
        String link = frontendUrl + "/reset-password?token=" + token;
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromAddress);
        message.setTo(toEmail);
        message.setSubject("Redefinir senha - Financeiro");
        message.setText("Oi " + name + ",\n\n"
                + "Recebemos um pedido para redefinir a senha da sua conta. Clique no link abaixo (válido por 1 hora):\n\n"
                + link + "\n\n"
                + "Se você não pediu isso, pode ignorar este e-mail - sua senha continua a mesma.");
        try {
            mailSender.send(message);
        } catch (MailException e) {
            log.error("Falha ao enviar e-mail de reset de senha para {}", toEmail, e);
        }
    }
}
