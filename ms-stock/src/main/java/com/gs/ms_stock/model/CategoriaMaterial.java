package com.gs.ms_stock.model;

/**
 * Categorías funcionales de los materiales e insumos del laboratorio dental G&amp;S.
 *
 * <p>Se utiliza para agrupar los materiales en el inventario, facilitar filtros
 * de búsqueda y aplicar reglas de negocio diferenciadas (por ejemplo, definir
 * qué categorías requieren trazabilidad de lote o tienen vencimiento).</p>
 */
public enum CategoriaMaterial {

    /** Yeso dental: tipo I (impresión), tipo II (modelado), tipo III y IV (piedra). */
    YESO,

    /** Cerámica feldespática y materiales cerámicos de recubrimiento. */
    CERAMICA,

    /** Porcelana para prótesis: capas de dentina, esmalte, incisales, etc. */
    PORCELANA,

    /** Acrílicos: termopolimerizables, autopolimerizables, termoplásticos para bases protéticas. */
    ACRILICO,

    /** Metales y aleaciones: cromo-cobalto, titanio, níquel-cromo para estructuras metálicas. */
    METAL,

    /** Resinas compuestas y resinas de alta resistencia para prótesis y carillas. */
    RESINA,

    /** Alambres ortodónticos: níquel-titanio, acero inoxidable, TMA, etc. */
    ALAMBRE,

    /** Bloques y discos de zirconia para fresado CAD/CAM. */
    ZIRCONIA,

    /** Ceras de modelado: cera rosada, cera inlay, cera para encerado de modelos. */
    CERA,

    /** Adhesivos, cementos, silicones de adición y condensación, impresiones. */
    ADHESIVO,

    /** Herramientas reutilizables: fresas, discos de corte, espátulas, articuladores. */
    HERRAMIENTA,

    /** Consumibles generales: guantes, mascarillas, papel articular, bolsas de esterilización. */
    CONSUMIBLE,

    /** Materiales que no encajan en ninguna de las categorías anteriores. */
    OTRO
}
