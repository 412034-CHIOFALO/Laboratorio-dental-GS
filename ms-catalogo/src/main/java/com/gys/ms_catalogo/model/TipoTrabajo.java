package com.gys.ms_catalogo.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "tipos_trabajo")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TipoTrabajo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String nombre;

    @Column(columnDefinition = "TEXT")
    private String descripcion;

    @Column(precision = 12, scale = 2)
    private BigDecimal precio;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Categoria categoria;

    @Column(name = "tiempo_estimado_dias")
    private Integer tiempoEstimadoDias;

    // URL o base64 de la foto del trabajo
    @Column(name = "foto_url", columnDefinition = "TEXT")
    private String fotoUrl;

    @Column(nullable = false)
    @Builder.Default
    private boolean activo = true;

    /**
     * Receta: materiales que se consumen al fabricar este tipo de trabajo.
     * Cascade ALL + orphanRemoval para que se persistan/borren junto con el TipoTrabajo.
     */
    @OneToMany(mappedBy = "tipoTrabajo", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @Builder.Default
    private List<IngredienteReceta> receta = new ArrayList<>();

    @Column(name = "fecha_creacion", nullable = false, updatable = false)
    private LocalDateTime fechaCreacion;

    @Column(name = "fecha_modificacion")
    private LocalDateTime fechaModificacion;

    // ── Helpers para mantener consistencia con la receta ───────────

    public void reemplazarReceta(List<IngredienteReceta> nuevos) {
        this.receta.clear();
        if (nuevos != null) {
            for (IngredienteReceta i : nuevos) {
                i.setTipoTrabajo(this);
                this.receta.add(i);
            }
        }
    }

    @PrePersist
    protected void onCreate() {
        this.fechaCreacion = LocalDateTime.now();
        this.fechaModificacion = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.fechaModificacion = LocalDateTime.now();
    }
}
