package com.eventos.culturales.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

// Etiqueta (punto 2): catálogo gestionado por el admin; un evento lleva varias.
// Sin referencia inversa a Evento (evita recursión en el JSON).
@Data
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "etiquetas")
@Entity
public class Etiqueta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String nombre;
}
