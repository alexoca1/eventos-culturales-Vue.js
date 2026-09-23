package com.eventos.culturales.entities;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
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
import java.time.LocalTime;

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

    // Horario opcional (US1 004): ambas null = sin horario, igual que antes de esta feature
    private LocalTime horaInicio;

    private LocalTime horaFin;

    // Fecha real de fin (006, nullable): null en entrada = un día (aplicar() la normaliza a fecha).
    // Si fin < inicio → 400. El antiguo cálculo +1 de cruce nocturno desaparece:
    // el admin indica la fecha fin explícita con el selector de fecha.
    private LocalDate fechaFin;

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

    // Estado de moderación (007): default APROBADO en código para compatibilidad.
    // Columna nullable a propósito: así el ALTER de Hibernate no falla en BDs con filas antiguas
    // (el backfill de DataInitializer las pone a APROBADO al arrancar).
    @Enumerated(EnumType.STRING)
    private EstadoEvento estado = EstadoEvento.APROBADO;

    // Organizador/admin que lo creó (null en filas legacy)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "creado_por_id")
    @JsonIgnoreProperties({"password"})
    private Usuario creadoPor;

    // Motivo informado por el admin al rechazar (null si no hay rechazo)
    private String motivoRechazo;

    // Ubicación: iframe o URL de Google Maps embebido
    @Column(length = 2000)
    private String mapaEmbed;

    // Categoría única opcional (US1 005): null = sin categoría, igual que antes
    // STRING (nunca ORDINAL) para no romper datos si el enum se reordena
    @Enumerated(EnumType.STRING)
    private CategoriaEvento categoria;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime fechaAlta;

    @UpdateTimestamp
    private LocalDateTime fechaModificacion;
}
