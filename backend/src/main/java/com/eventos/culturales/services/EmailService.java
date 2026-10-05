package com.eventos.culturales.services;

import lombok.RequiredArgsConstructor;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

/**
 * Envío de emails de notificación (US6 007). El envío es un efecto secundario:
 * cualquier fallo se loguea y NUNCA se propaga, para no revertir la operación
 * de BD ya confirmada que lo originó (FR-013).
 */
@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    public void enviar(String destinatario, String asunto, String cuerpo) {
        try {
            SimpleMailMessage mensaje = new SimpleMailMessage();
            mensaje.setTo(destinatario);
            mensaje.setSubject(asunto);
            mensaje.setText(cuerpo);
            mailSender.send(mensaje);
        } catch (Exception e) {
            System.err.println("[EmailService] No se pudo enviar email"
                    + " (destinatario omitido por RGPD): " + e.getClass().getSimpleName());
        }
    }
}
