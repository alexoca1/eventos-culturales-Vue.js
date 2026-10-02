package com.eventos.culturales.config;

import com.eventos.culturales.entities.Etiqueta;
import com.eventos.culturales.entities.Evento;
import com.eventos.culturales.entities.Usuario;
import com.eventos.culturales.repositories.EtiquetaRepository;
import com.eventos.culturales.repositories.EventoRepository;
import com.eventos.culturales.repositories.UsuarioRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 015 T150/T151: migración del nombre al arrancar.
 * <p>
 * Se cubren los DOS valores que puede tener una fila vieja. Al añadir la columna
 * `nombre` como NOT NULL, MySQL rellena las filas existentes con '' (no null), así
 * que un backfill con "WHERE nombre IS NULL" a secas no migraría nada y el nombre
 * de los eventos anteriores se perdería. Por eso los dos casos van en el mismo test.
 */
@ExtendWith(MockitoExtension.class)
class DataInitializerTest {

    @Mock
    UsuarioRepository usuarioRepository;
    @Mock
    EventoRepository eventoRepository;
    @Mock
    EtiquetaRepository etiquetaRepository;
    @Mock
    PasswordEncoder passwordEncoder;

    @Test
    void init_copiaDescripcionANombre_enEventosLegacy() {
        Evento nombreNull = eventoLegacy(1L, null, "Monólogo Danni Robira");
        Evento nombreVacio = eventoLegacy(2L, "", "Fiesta Mexicana");

        when(usuarioRepository.findByEmail("admin@test.com")).thenReturn(Optional.of(adminCompleto()));
        when(etiquetaRepository.findByNombre(anyString()))
                .thenReturn(Optional.of(new Etiqueta(1L, "OTROS")));
        when(eventoRepository.findSinNombre()).thenReturn(List.of(nombreNull, nombreVacio));
        // Ya hay datos: no debe sembrar los 5 eventos de ejemplo.
        when(eventoRepository.count()).thenReturn(1L);
        // Vacío a propósito: aísla este backfill de las migraciones de carteles/etiquetas.
        when(eventoRepository.findAll()).thenReturn(List.of());

        dataInitializer().init();

        assertEquals("Monólogo Danni Robira", nombreNull.getNombre());
        assertEquals("Fiesta Mexicana", nombreVacio.getNombre());

        ArgumentCaptor<Evento> captor = ArgumentCaptor.forClass(Evento.class);
        verify(eventoRepository, times(2)).save(captor.capture());
        assertEquals(List.of(1L, 2L), captor.getAllValues().stream().map(Evento::getId).toList());
    }

    private DataInitializer dataInitializer() {
        return new DataInitializer(usuarioRepository, eventoRepository, etiquetaRepository,
                passwordEncoder, "admin123");
    }

    private Evento eventoLegacy(Long id, String nombre, String descripcion) {
        Evento e = new Evento();
        e.setId(id);
        e.setNombre(nombre);
        e.setDescripcion(descripcion);
        return e;
    }

    private Usuario adminCompleto() {
        Usuario u = new Usuario();
        u.setEmail("admin@test.com");
        u.setTelefono("600123123");
        u.setRoles(Set.of("ROLE_ADMIN"));
        u.setEnabled(true);
        return u;
    }
}
