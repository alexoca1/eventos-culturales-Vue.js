package com.eventos.culturales.entities;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "eventos", indexes = @Index(name = "idx_eventos_fecha", columnList = "fecha"))
@Entity
public class Evento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "El establecimiento es obligatorio")
    @Column(nullable = false)
    private String establecimiento;

    @NotBlank(message = "La dirección es obligatoria")
    @Column(nullable = false)
    private String direccion;

    // Sin UNIQUE: puede haber varios eventos el mismo día
    @NotNull(message = "La fecha es obligatoria")
    @Column(nullable = false)
    private LocalDate fecha;

    @NotBlank(message = "La descripción es obligatoria")
    @Column(nullable = false, length = 2000)
    private String descripcion;

    // Cartel: URL externa (opcional, solo si no hay imagen en BD)
    @Column(columnDefinition = "TEXT")
    private String cartelUrl;

    // Cartel display (400px, para listas) y HD (tope 1600px, para lightbox). Bytes en BD.
    @Lob
    @Column(columnDefinition = "LONGBLOB")
    private byte[] cartel;

    private String cartelContentType;

    @Lob
    @Column(columnDefinition = "LONGBLOB")
    private byte[] cartelHd;

    private String cartelHdContentType;

    // Ubicación: iframe o URL de Google Maps embebido
    @Column(length = 2000)
    private String mapaEmbed;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime fechaAlta;

    @UpdateTimestamp
    private LocalDateTime fechaModificacion;
}
