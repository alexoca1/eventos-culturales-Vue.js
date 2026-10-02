package com.eventos.culturales.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

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

    // 015 US1: nombre del evento, obligatorio. Es lo que antes guardábamos en
    // `descripcion`; las filas viejas se migran al arrancar (DataInitializer).
    @NotBlank(message = "El nombre es obligatorio")
    @Column(name = "nombre", nullable = false)
    private String nombre;

    // 015 US1: pasa a ser el texto largo opcional. Nullable y TEXT: el nombre
    // del evento ya no vive aquí.
    @Column(name = "descripcion", nullable = true, columnDefinition = "TEXT")
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

    // Etiquetas múltiples (punto 2): reemplaza a la antigua categoria única.
    // EAGER para que el JSON directo del controller no falle fuera de sesión.
    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(name = "evento_etiquetas",
            joinColumns = @JoinColumn(name = "evento_id"),
            inverseJoinColumns = @JoinColumn(name = "etiqueta_id"))
    private java.util.Set<Etiqueta> etiquetas = new java.util.HashSet<>();

    // 015 US2: galería de fotos (posiciones 0-4).
    // Aquí, y no en FotoGaleria.evento, van cascade + orphanRemoval: es lo único que hace
    // que borrar el evento arrastre sus fotos (FR-010). OrphanRemoval cubre además el
    // borrado de una foto suelta desde la colección.
    // @JsonIgnore porque son LONGBLOB: el JSON de /eventos no debe llevar las fotos
    // embebidas en base64 (se sirven por GET /eventos/{id}/galeria/{orden}).
    // Excluida de equals/toString para que no haya ciclo con FotoGaleria.evento.
    @OneToMany(mappedBy = "evento", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore
    @EqualsAndHashCode.Exclude
    @ToString.Exclude
    private List<FotoGaleria> fotos = new ArrayList<>();

    // 016 (Phase 4): redes sociales del evento + datos de contacto opcionales.
    // EAGER por el mismo motivo que etiquetas: el JSON directo del controller no
    // debe fallar fuera de sesión. Lista pequeña (máx. 8 redes, 2 URLs), sin riesgo.
    // La unicidad (evento_id, red) vive en el @CollectionTable: un evento no repite
    // la misma red (el controller deduplica con "último gana").
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "evento_redes_sociales",
            joinColumns = @JoinColumn(name = "evento_id"),
            uniqueConstraints = @UniqueConstraint(columnNames = {"evento_id", "red"}))
    private List<RedSocialEvento> redesSociales = new ArrayList<>();

    @Column(length = 20)
    private String telefonoEvento;

    @Column(length = 500)
    private String urlEvento;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime fechaAlta;

    @UpdateTimestamp
    private LocalDateTime fechaModificacion;
}
