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
        if (sender instanceof org.springframework.mail.javamail.JavaMailSenderImpl impl) {
            String host = impl.getHost();
            if (host == null || host.isBlank() || "localhost".equalsIgnoreCase(host)) {
                log.info("SMTP no configurado en entorno de despliegue (host: {}) — confirmación simulada para {}",
                        host, user.getEmail());
                return false;
            }
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

    /** HU35: Envío automático de correo con los datos de la reserva tras su confirmación. */
    public boolean sendBookingConfirmation(com.sport.court.booking.domain.Booking booking) {
        if (booking == null || booking.getUser() == null || booking.getUser().getEmail() == null) {
            log.warn("No se pudo enviar confirmación de reserva: datos de reserva o usuario nulos.");
            return false;
        }

        String userEmail = booking.getUser().getEmail();
        String subject = "¡Tu reserva está confirmada! Código #" + booking.getBookingCode() + " - Sport Court Booking";
        String html = buildBookingConfirmationHtml(booking);

        JavaMailSender sender = mailSender.getIfAvailable();
        if (sender == null) {
            log.warn("SMTP no configurado — confirmación de reserva #{} simulada para {}. Enlace a mis reservas: {}",
                    booking.getBookingCode(), userEmail, myBookingsUrl());
            return false;
        }
        if (sender instanceof org.springframework.mail.javamail.JavaMailSenderImpl impl) {
            String host = impl.getHost();
            if (host == null || host.isBlank() || "localhost".equalsIgnoreCase(host)) {
                log.info("SMTP no configurado en entorno de despliegue (host: {}) — confirmación de reserva #{} simulada para {}",
                        host, booking.getBookingCode(), userEmail);
                return false;
            }
        }
        try {
            MimeMessage message = sender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, "UTF-8");
            helper.setFrom(from);
            helper.setTo(userEmail);
            helper.setSubject(subject);
            helper.setText(html, true);
            sender.send(message);
            log.info("Correo de confirmación de reserva #{} enviado exitosamente a {}", booking.getBookingCode(), userEmail);
            return true;
        } catch (Exception e) {
            log.error("No se pudo enviar el correo de confirmación de reserva #{} a {}: {}",
                    booking.getBookingCode(), userEmail, e.getMessage());
            return false;
        }
    }

    public String myBookingsUrl() {
        return frontendUrl.replaceAll("/+$", "") + "/mis-reservas";
    }

    /** Plantilla HTML responsive para la confirmación de reserva con los datos del proveedor y de la cancha. */
    public String buildBookingConfirmationHtml(com.sport.court.booking.domain.Booking booking) {
        String clientName = HtmlUtils.htmlEscape(booking.getUser().getFirstName() + " " + booking.getUser().getLastName());
        String courtName = HtmlUtils.htmlEscape(booking.getCourt() != null ? booking.getCourt().getName() : "Cancha Deportiva");
        String category = HtmlUtils.htmlEscape(booking.getCourt() != null && booking.getCourt().getCategory() != null ? booking.getCourt().getCategory().getTitle() : "Deporte");
        String dates = HtmlUtils.htmlEscape(booking.getStartDate() + " al " + booking.getEndDate());
        String time = HtmlUtils.htmlEscape(booking.getStartTime() != null ? booking.getStartTime() : "Horario coordinado");
        String code = HtmlUtils.htmlEscape(booking.getBookingCode());
        String notes = HtmlUtils.htmlEscape(booking.getNotes() != null && !booking.getNotes().isBlank() ? booking.getNotes() : "Sin observaciones adicionales");

        return """
                <div style="font-family:'Inter',Arial,sans-serif;background:#F8FAFC;padding:28px 12px;margin:0;">
                  <div style="max-width:560px;margin:auto;background:#FFFFFF;border-radius:16px;overflow:hidden;box-shadow:0 4px 20px rgba(0,0,0,0.06);border:1px solid #E2E8F0;">
                    <!-- Cabecera -->
                    <div style="background:#0A2540;padding:26px 20px;text-align:center;">
                      <h1 style="margin:0;font-size:22px;letter-spacing:.05em;">
                        <span style="color:#FFFFFF;font-weight:800;">SPORT COURT</span>
                        <span style="color:#2FBF71;font-weight:800;">BOOKING</span>
                      </h1>
                      <p style="color:#94A3B8;margin:6px 0 0 0;font-size:13px;">Comprobante Digital de Reserva</p>
                    </div>

                    <!-- Contenido Principal -->
                    <div style="padding:32px 28px;color:#1E293B;line-height:1.6;">
                      <div style="text-align:center;margin-bottom:24px;">
                        <span style="display:inline-block;background:#E8F5E9;color:#1E7F3B;padding:6px 16px;border-radius:20px;font-weight:700;font-size:14px;">
                          ✓ Reserva Confirmada
                        </span>
                        <h2 style="margin:16px 0 6px 0;color:#0A2540;font-size:20px;">¡Todo listo para tu partido, %s!</h2>
                        <p style="margin:0;color:#64748B;font-size:14px;">Tu cancha ha sido reservada con éxito. A continuación encontrarás el resumen de tu reserva:</p>
                      </div>

                      <!-- Ficha Resumen -->
                      <div style="background:#F1F5F9;border-radius:12px;padding:20px;margin-bottom:24px;border:1px solid #E2E8F0;">
                        <table style="width:100%%;border-collapse:collapse;font-size:14px;">
                          <tr>
                            <td style="padding:6px 0;color:#64748B;width:40%%;">Código de Reserva:</td>
                            <td style="padding:6px 0;color:#0A2540;font-weight:700;">#%s</td>
                          </tr>
                          <tr>
                            <td style="padding:6px 0;color:#64748B;">Cancha Deportiva:</td>
                            <td style="padding:6px 0;color:#0A2540;font-weight:700;">%s (%s)</td>
                          </tr>
                          <tr>
                            <td style="padding:6px 0;color:#64748B;">Período de Reserva:</td>
                            <td style="padding:6px 0;color:#0A2540;font-weight:700;">%s</td>
                          </tr>
                          <tr>
                            <td style="padding:6px 0;color:#64748B;">Hora de Inicio:</td>
                            <td style="padding:6px 0;color:#0A2540;font-weight:700;">%s</td>
                          </tr>
                          <tr>
                            <td style="padding:6px 0;color:#64748B;">Observaciones:</td>
                            <td style="padding:6px 0;color:#334155;">%s</td>
                          </tr>
                        </table>
                      </div>

                      <!-- Datos del Proveedor y Soporte -->
                      <div style="border-left:4px solid #2FBF71;padding-left:14px;margin-bottom:24px;">
                        <h4 style="margin:0 0 4px 0;color:#0A2540;font-size:14px;">Información de Contacto del Complejo:</h4>
                        <p style="margin:0;font-size:13px;color:#475569;">
                          Dirección: Complejo Deportivo Central Sport Court<br/>
                          WhatsApp de Atención: <strong>+57 300 123 4567</strong><br/>
                          Email: soporte@sportcourtbooking.com
                        </p>
                      </div>

                      <!-- Botón de acción -->
                      <div style="text-align:center;margin:32px 0 16px 0;">
                        <a href="%s" style="background:#1E7F3B;color:#FFFFFF;padding:14px 32px;border-radius:10px;text-decoration:none;font-weight:700;display:inline-block;box-shadow:0 4px 12px rgba(30,127,59,0.3);">
                          Ver mis reservas
                        </a>
                      </div>
                    </div>

                    <!-- Pie -->
                    <div style="background:#F8FAFC;padding:16px 20px;text-align:center;border-top:1px solid #E2E8F0;">
                      <p style="margin:0;color:#94A3B8;font-size:11px;">
                        Sport Court Booking © 2026. Todos los derechos reservados.<br/>
                        Por favor presenta este comprobante al momento de ingresar a la cancha deportiva.
                      </p>
                    </div>
                  </div>
                </div>
                """.formatted(clientName, code, courtName, category, dates, time, notes, myBookingsUrl());
    }
}
