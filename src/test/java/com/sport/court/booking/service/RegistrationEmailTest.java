package com.sport.court.booking.service;

import com.sport.court.booking.AbstractIntegrationTest;
import com.sport.court.booking.domain.Role;
import com.sport.court.booking.domain.User;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.MediaType;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** HU19 (opcional) — Confirmación de registro por correo (TC-31..35). */
class RegistrationEmailTest extends AbstractIntegrationTest {

    @MockitoBean JavaMailSender mailSender;

    @BeforeEach
    void stubMime() {
        when(mailSender.createMimeMessage()).thenAnswer(inv -> new MimeMessage((Session) null));
    }

    private void registerMaria() throws Exception {
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"firstName\":\"María\",\"lastName\":\"González\",\"email\":\"maria@test.com\",\"password\":\"Segura123\"}"))
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("TC-31 Un registro exitoso envía el correo de confirmación")
    void tc31_emailSentAfterRegister() throws Exception {
        registerMaria();
        ArgumentCaptor<MimeMessage> captor = ArgumentCaptor.forClass(MimeMessage.class);
        verify(mailSender, times(1)).send(captor.capture());
        assertThat(captor.getValue().getAllRecipients()[0].toString()).isEqualTo("maria@test.com");
        assertThat(captor.getValue().getSubject()).contains("Sport Court Booking");
    }

    @Test
    @DisplayName("TC-31b Un registro fallido (correo duplicado) NO envía correo")
    void tc31b_noEmailOnFailure() throws Exception {
        createUser("maria@test.com", Role.USER);
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"firstName\":\"María\",\"lastName\":\"González\",\"email\":\"maria@test.com\",\"password\":\"Segura123\"}"))
                .andExpect(status().isConflict());
        verify(mailSender, never()).send(any(MimeMessage.class));
    }

    @Test
    @DisplayName("TC-32/33/34 El contenido incluye mensaje claro, nombre, correo y enlace a iniciar sesión")
    void tc32_33_34_content(@org.springframework.beans.factory.annotation.Autowired EmailService emailService) {
        User u = createUser("maria@test.com", Role.USER);
        u.setFirstName("María");
        u.setLastName("González");
        String html = emailService.buildConfirmationHtml(u);
        assertThat(html)
                .contains("¡Tu registro fue exitoso!")        // TC-32
                .contains("Mar&iacute;a Gonz&aacute;lez")                   // TC-33
                .contains("maria@test.com")                   // TC-33
                .contains("href=\"http://localhost:5173/login\""); // TC-34
        assertThat(emailService.loginUrl()).isEqualTo("http://localhost:5173/login");
    }

    @Test
    @DisplayName("TC-32b Los datos del usuario se escapan en el HTML del correo")
    void tc32b_htmlEscaped(@org.springframework.beans.factory.annotation.Autowired EmailService emailService) {
        User u = createUser("x@test.com", Role.USER);
        u.setFirstName("<script>alert(1)</script>");
        assertThat(emailService.buildConfirmationHtml(u)).doesNotContain("<script>");
    }

    @Test
    @DisplayName("TC-35 Solicitar reenvío del correo de confirmación")
    void tc35_resend() throws Exception {
        createUser("maria@test.com", Role.USER);
        mvc.perform(post("/api/auth/resend-confirmation").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"maria@test.com\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").exists());
        verify(mailSender, times(1)).send(any(MimeMessage.class));
    }

    @Test
    @DisplayName("TC-35b El reenvío no revela si un correo existe y no envía nada si no está registrado")
    void tc35b_resendUnknown() throws Exception {
        mvc.perform(post("/api/auth/resend-confirmation").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"nadie@test.com\"}"))
                .andExpect(status().isOk());
        verify(mailSender, never()).send(any(MimeMessage.class));
    }

    @Test
    @DisplayName("Un fallo del servidor SMTP no impide el registro")
    void smtpFailureDoesNotBreakRegistration() throws Exception {
        doThrow(new org.springframework.mail.MailSendException("SMTP caído"))
                .when(mailSender).send(any(MimeMessage.class));
        registerMaria();
        assertThat(userRepository.existsByEmailIgnoreCase("maria@test.com")).isTrue();
    }
}
