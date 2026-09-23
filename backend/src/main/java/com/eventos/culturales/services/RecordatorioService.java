package com.eventos.culturales.services;

import com.eventos.culturales.entities.EstadoEvento;
import com.eventos.culturales.entities.Favorito;
import com.eventos.culturales.repositories.FavoritoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

/**
 * Recordatorios por email de eventos favoritos que ocurren mañana (US4 008).
 * El cron es configurable (default 20:00). Cada favorito se procesa de forma
 * aislada: un fallo de envío no detiene al resto, y el flag se marca tras el
 * intento en cualquier caso para no reenviar (idempotencia ante reinicios).
 */
@Service
@RequiredArgsConstructor
public class RecordatorioService {

    private final FavoritoRepository favoritoRepository;
    private final EmailService emailService;

    @Scheduled(cron = "${app.recordatorios.cron:0 0 20 * * *}")
    @Transactional
    public void enviarRecordatorios() {
        List<Favorito> pendientes = favoritoRepository
                .findByEventoFechaAndEventoEstadoAndRecordatorioEnviadoFalse(
                        LocalDate.now().plusDays(1), EstadoEvento.APROBADO, false);
        for (Favorito favorito : pendientes) {
            try {
                emailService.enviar(
                        favorito.getUsuario().getEmail(),
                        "Recordatorio: " + favorito.getEvento().getDescripcion() + " es mañana",
                        "Hola " + favorito.getUsuario().getNombre() + ", te recordamos que mañana "
                                + favorito.getEvento().getFecha() + " es el evento '"
                                + favorito.getEvento().getDescripcion() + "' en "
                                + favorito.getEvento().getEstablecimiento() + ".");
            } catch (Exception e) {
                System.err.println("[RecordatorioService] Fallo enviando a "
                        + favorito.getUsuario().getEmail() + ": " + e.getMessage());
            } finally {
                favorito.setRecordatorioEnviado(true);
                favoritoRepository.save(favorito);
            }
        }
    }
}
