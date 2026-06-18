package com.gs.ms_catalogo.model;

/**
 * Categorías odontológicas que agrupan los tipos de trabajo dental del laboratorio.
 * <p>
 * Se almacena como {@code STRING} en la columna {@code categoria} de la tabla
 * {@code tipos_trabajo} para facilitar la lectura directa en la base de datos.
 * </p>
 *
 * <ul>
 *   <li>{@link #FIJA}         — Trabajos cementados permanentemente: coronas, puentes, incrustaciones, carillas.</li>
 *   <li>{@link #REMOVIBLE}    — Prótesis que el paciente puede colocar y retirar: parciales, totales.</li>
 *   <li>{@link #ORTODONCIA}   — Aparatos fijos o removibles de ortodoncia: retenedores, expansores, Hawley.</li>
 *   <li>{@link #ATM}          — Dispositivos oclusales y de articulación temporo-mandibular: férulas de descarga y reposicionamiento.</li>
 *   <li>{@link #PERSONALIZADO}— Trabajos a medida que no encajan en las categorías anteriores.</li>
 * </ul>
 */
public enum Categoria {

    /** Trabajos protésicos cementados de forma permanente (coronas, puentes, incrustaciones, carillas). */
    FIJA,

    /** Prótesis que el paciente puede colocar y retirar (parciales acrílicas, totales, esqueléticas). */
    REMOVIBLE,

    /** Aparatos ortodóncicos fijos o removibles (retenedores, expansores, placas de Hawley). */
    ORTODONCIA,

    /** Dispositivos oclusales para tratamiento de la articulación temporo-mandibular (férulas de descarga, reposicionamiento). */
    ATM,

    /** Trabajos a medida que no encajan en las categorías estándar del laboratorio. */
    PERSONALIZADO
}
