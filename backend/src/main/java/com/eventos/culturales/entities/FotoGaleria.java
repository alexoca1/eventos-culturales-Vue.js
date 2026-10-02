package com.eventos.culturales.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 015 US2: una foto de la galería de un evento (posiciones 0-4).
 * <p>
 * Los bytes van tal cual, en su formato original: aquí no hay compresión dual
 * como en el cartel (ver "Assumptions" de la spec 015).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "foto_galeria",
        uniqueConstraints = @UniqueConstraint(name = "uq_foto_galeria_evento_orden",
                columnNames = {"evento_id", "orden"}))
@Entity
public class FotoGaleria {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Sin cascade a propósito, aunque la spec lo pida en este lado: cascade=ALL en un
    // @ManyToOne hace que borrar la FOTO arrastre el Evento padre (borrar /galeria/{orden}
    // se llevaría por delante el evento entero). Lo que necesita cascada es el evento al
    // borrarse, y eso va en Evento.fotos, que es el lado de la colección.
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "evento_id", nullable = false)
    @JsonIgnore
    private Evento evento;

    // Posición en la galería, 0-4 (lo valida el controller). El índice único
    // (evento_id, orden) es lo que hace que "reemplazar" sea un upsert y no un duplicado.
    @Column(nullable = false)
    private int orden;

    @Lob
    @Column(name = "datos", nullable = false, columnDefinition = "LONGBLOB")
    private byte[] datos;

    @Column(nullable = false)
    private String contentType;
}
