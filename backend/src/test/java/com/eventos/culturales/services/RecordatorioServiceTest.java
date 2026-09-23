package com.eventos.culturales.services;

import com.eventos.culturales.entities.EstadoEvento;
import com.eventos.culturales.entities.Evento;
import com.eventos.culturales.entities.Favorito;
import com.eventos.culturales.entities.Usuario;
import com.eventos.culturales.repositories.FavoritoRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

// US4: se invoca el método directamente, sin esperar al cron real.
@ExtendWith(MockitoExtension.class)
class RecordatorioServiceTest {

    @Mock
    private FavoritoRepository favoritoRepository;

    @Mock
    private EmailService emailService;

    @InjectMocks
    private RecordatorioService recordatorioService;

    private Usuario usuario(String email) {
        return Usuario.builder().id(1L).email(email).roles(Set.of("ROLE_USER"))
                .enabled(true).nombre("User").apellidos("Test").build();
    }

    private Evento evento(LocalDate fecha) {
        Evento e = new Evento();
        e.setId(1L);
        e.setEstablecimiento("Sala");
        e.setDireccion("Calle 1");
        e.setFecha(fecha);
        e.setDescripcion("Concierto");
        e.setEstado(EstadoEvento.APROBADO);
        return e;
    }

    private Favorito favorito(Usuario u, Evento e) {
        Favorito f = new Favorito();
        f.setUsuario(u);
        f.setEvento(e);
        f.setRecordatorioEnviado(false);
        return f;
    }

    @Test
    void favoritoDeManana_enviaEmailYMarca() {
        Favorito f = favorito(usuario("a@test.com"), evento(LocalDate.now().plusDays(1)));
        when(favoritoRepository.findByEventoFechaAndEventoEstadoAndRecordatorioEnviadoFalse(
                eq(LocalDate.now().plusDays(1)), eq(EstadoEvento.APROBADO), eq(false)))
                .thenReturn(List.of(f));

        recordatorioService.enviarRecordatorios();

        verify(emailService).enviar(eq("a@test.com"), anyString(), anyString());
        ArgumentCaptor<Favorito> captor = ArgumentCaptor.forClass(Favorito.class);
        verify(favoritoRepository).save(captor.capture());
        assertTrue(captor.getValue().isRecordatorioEnviado());
    }

    @Test
    void yaMarcado_noSeReenvia() {
        when(favoritoRepository.findByEventoFechaAndEventoEstadoAndRecordatorioEnviadoFalse(
                any(), any(), eq(false)))
                .thenReturn(List.of());

        recordatorioService.enviarRecordatorios();

        verify(emailService, never()).enviar(anyString(), anyString(), anyString());
    }

    @Test
    void fechaDistintaDeManana_noSeProcesa() {
        // El servicio pregunta siempre por mañana: un favorito de otro día no aparece
        when(favoritoRepository.findByEventoFechaAndEventoEstadoAndRecordatorioEnviadoFalse(
                eq(LocalDate.now().plusDays(1)), any(), anyBoolean()))
                .thenReturn(List.of());

        recordatorioService.enviarRecordatorios();

        ArgumentCaptor<LocalDate> captorFecha = ArgumentCaptor.forClass(LocalDate.class);
        verify(favoritoRepository).findByEventoFechaAndEventoEstadoAndRecordatorioEnviadoFalse(
                captorFecha.capture(), any(), anyBoolean());
        assertEquals(LocalDate.now().plusDays(1), captorFecha.getValue());
        verify(emailService, never()).enviar(anyString(), anyString(), anyString());
    }

    @Test
    void falloEnUno_noDetieneAlResto() {
        Favorito f1 = favorito(usuario("a@test.com"), evento(LocalDate.now().plusDays(1)));
        Favorito f2 = favorito(usuario("b@test.com"), evento(LocalDate.now().plusDays(1)));
        when(favoritoRepository.findByEventoFechaAndEventoEstadoAndRecordatorioEnviadoFalse(
                any(), any(), anyBoolean()))
                .thenReturn(List.of(f1, f2));
        doThrow(new RuntimeException("SMTP caído")).when(emailService)
                .enviar(eq("a@test.com"), anyString(), anyString());

        recordatorioService.enviarRecordatorios();

        verify(emailService).enviar(eq("b@test.com"), anyString(), anyString());
        verify(favoritoRepository, times(2)).save(any(Favorito.class));
        assertTrue(f1.isRecordatorioEnviado());
        assertTrue(f2.isRecordatorioEnviado());
    }
}
