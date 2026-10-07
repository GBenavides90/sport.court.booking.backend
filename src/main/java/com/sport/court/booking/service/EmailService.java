package com.sport.court.booking.service;

import com.sport.court.booking.domain.User;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.web.util.HtmlUtils;

/**
 * HU19 (opcional) — Correo de confirmación de registro.
 * Si no hay SMTP configurado (spring.mail.host), el correo se registra en el log y el registro no se ve afectado.
 */
@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    private final ObjectProvider<JavaMailSender> mailSender;
    private final String from;
    private final String frontendUrl;

    public EmailService(ObjectProvider<JavaMailSender> mailSender,
                        @Value("${app.mail.from:no-reply@sportcourtbooking.com}") String from,
                        @Value("${app.frontend-url:http://localhost:5173}") String frontendUrl) {
        this.mailSender = mailSender;
        this.from = from;
        this.frontendUrl = frontendUrl;
    }

    public String loginUrl() {
        return frontendUrl.replaceAll("/+$", "") + "/login";
    }

    /** @return true si el correo fue entregado al servidor SMTP. */
    public boolean sendRegistrationConfirmation(User user) {
        String subject = "¡Bienvenido a Sport Court Booking! Confirma tu registro";
        String html = buildConfirmationHtml(user);
        JavaMailSender sender = mailSender.getIfAvailable();
        if (sender == null) {
            log.warn("SMTP no configurado — correo de confirmación para {} no enviado. Enlace: {}",
                    user.getEmail(), loginUrl());
            return false;
        }
        try {
            MimeMessage message = sender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, "UTF-8");
            helper.setFrom(from);
            helper.setTo(user.getEmail());
            helper.setSubject(subject);
            helper.setText(html, true);
            sender.send(message);
            return true;
        } catch (Exception e) {
            log.error("No se pudo enviar el correo de confirmación a {}: {}", user.getEmail(), e.getMessage());
            return false;
        }
    }

    /** Contenido: mensaje claro, nombre, correo y enlace para iniciar sesión (colores de la identidad visual). */
    public String buildConfirmationHtml(User user) {
        String name = HtmlUtils.htmlEscape(user.getFirstName() + " " + user.getLastName());
        String email = HtmlUtils.htmlEscape(user.getEmail());
        return """
                <div style="font-family:Inter,Arial,sans-serif;background:#F5F7FA;padding:24px;">
                  <div style="max-width:520px;margin:auto;background:#fff;border-radius:12px;overflow:hidden;">
                    <div style="background:#0A2540;padding:20px;text-align:center;">
                      <span style="color:#fff;font-weight:800;letter-spacing:.04em;">SPORT COURT</span>
                      <span style="color:#2FBF71;font-weight:800;letter-spacing:.04em;">BOOKING</span>
                    </div>
                    <div style="padding:28px;color:#2E2E2E;">
                      <h2 style="margin-top:0;color:#0A2540;">¡Tu registro fue exitoso!</h2>
                      <p>Hola <strong>%s</strong>, tu cuenta en Sport Court Booking ya está lista.</p>
                      <p style="color:#6B7280;">Nombre: %s<br/>Correo electrónico: %s</p>
                      <p style="text-align:center;margin:28px 0;">
                        <a href="%s" style="background:#1E7F3B;color:#fff;padding:12px 28px;border-radius:8px;text-decoration:none;font-weight:700;">Iniciar sesión</a>
                      </p>
                      <p style="color:#6B7280;font-size:12px;">Si no creaste esta cuenta, ignora este mensaje.</p>
                    </div>
                  </div>
                </div>
                """.formatted(name, name, email, loginUrl());
    }
}
