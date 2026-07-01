import { Component, signal } from '@angular/core';

interface Captura {
  /** id estable para reemplazar el placeholder por la imagen real. */
  id: string;
  /** qué debe mostrar la captura y qué resaltar. */
  descripcion: string;
}

interface Seccion {
  id: string;
  titulo: string;
  intro: string;
  queVes?: string[];
  queHaces?: string[];
  flujo?: string[];
  captura?: Captura;
}

@Component({
  selector: 'app-manual',
  standalone: true,
  imports: [],
  templateUrl: './manual.html',
  styleUrls: ['./manual.css'],
})
export class ManualComponent {

  readonly activa = signal('intro');

  /** ids de captura cuya imagen (/manual/<id>.png) cargó OK. */
  readonly conImagen = signal<Set<string>>(new Set());

  seleccionar(id: string): void {
    this.activa.set(id);
    document.querySelector('.manual-contenido')?.scrollTo({ top: 0, behavior: 'smooth' });
  }

  /** La imagen existe → la mostramos y ocultamos el placeholder. */
  marcarImagen(id: string): void {
    this.conImagen.update(s => new Set(s).add(id));
  }

  readonly secciones: Seccion[] = [
    {
      id: 'intro',
      titulo: 'Bienvenida',
      intro: 'Este manual explica, pantalla por pantalla, qué ves, qué podés hacer y cómo se conecta cada cosa con el resto del sistema. Al final hay una sección de "Flujos completos" que sigue un trabajo de principio a fin.',
      queVes: [
        'A la izquierda, el menú con todas las secciones del sistema, agrupadas por Operativo, Gestión, Archivo y Administración.',
        'Arriba a la derecha, el tema claro/oscuro, la campanita de notificaciones y tu perfil.',
      ],
      queHaces: [
        'Navegás por el menú lateral; cada ítem es una pantalla explicada acá.',
        'Si es tu primera vez, conviene arrancar por el Tour guiado (menú de perfil → Tour guiado).',
      ],
    },
    {
      id: 'inicio',
      titulo: 'Inicio (panel del día)',
      intro: 'Es la pantalla a la que volvés siempre: un resumen del estado del laboratorio hoy.',
      queVes: ['Tarjetas con pedidos en curso, atrasados y entregas pendientes.', 'Accesos rápidos a los módulos que más usás.'],
      queHaces: ['Mirás de un vistazo qué requiere atención y saltás directo al módulo con un clic.'],
      captura: { id: 'cap-inicio', descripcion: 'Pantalla de Inicio completa, mostrando las tarjetas de resumen y los accesos rápidos.' },
    },
    {
      id: 'pedidos',
      titulo: 'Pedidos',
      intro: 'El corazón del sistema: acá cargás cada trabajo que pide un odontólogo y seguís su estado.',
      queVes: ['La lista de pedidos con paciente, trabajo, técnico, fecha de entrega, precio y estado.', 'Los atrasados resaltados y un badge "sin precio" en los que aún no tienen monto.'],
      queHaces: ['Creás un pedido (odontólogo, paciente, tipo de trabajo, fecha).', 'Editás, cancelás o abrís el detalle de cada uno.'],
      flujo: ['Al crear el pedido, si elegís un tipo del Catálogo, queda vinculada su receta de materiales para el descuento automático de stock más adelante.'],
      captura: { id: 'cap-pedidos-lista', descripcion: 'La tabla de pedidos con al menos un atrasado y uno "sin precio" visibles.' },
    },
    {
      id: 'produccion',
      titulo: 'Producción (Kanban)',
      intro: 'El tablero del taller: movés cada trabajo por sus etapas.',
      queVes: ['Columnas: Recibido → En proceso → Control → Listo.', 'Los urgentes en rojo y los días restantes con colores.'],
      queHaces: ['Arrastrás (o con el botón "Avanzar" en el celu) cada trabajo a la siguiente etapa.', 'Filtrás por técnico.'],
      flujo: ['Cuando un pedido pasa a EN PROCESO, el sistema descuenta solo los materiales de la receta (ver Stock).', 'Cuando pasa a LISTO, el bot avisa al odontólogo por WhatsApp.'],
      captura: { id: 'cap-produccion', descripcion: 'El tablero Kanban con tarjetas en varias columnas.' },
    },
    {
      id: 'entregas',
      titulo: 'Entregas',
      intro: 'Cuando el trabajo está listo, confirmás la entrega. Acá es donde se genera la deuda del odontólogo.',
      queVes: ['Los trabajos LISTO esperando retiro y el historial de entregados.'],
      queHaces: ['Marcás "Entregar": indicás quién retiró, la fecha y el Monto a facturar (viene pre-cargado con el precio del pedido).'],
      flujo: ['Al confirmar, se crea automáticamente la cuenta por cobrar del odontólogo por ese monto → aparece en su cuenta corriente y en el ranking de morosos. La entrega genera la DEUDA, no el cobro.'],
      captura: { id: 'cap-entregas-modal', descripcion: 'El modal de entrega con el campo "Monto a facturar" resaltado.' },
    },
    {
      id: 'catalogo',
      titulo: 'Catálogo',
      intro: 'Tus tipos de trabajo (Hawley, placas, expansores…) con precio y receta de materiales.',
      queVes: ['Las tarjetas de cada trabajo con foto, precio y categoría.'],
      queHaces: ['Creás/editás trabajos, cambiás el precio rápido, definís la receta (qué materiales y cuánto consume cada uno).'],
      flujo: ['Esa receta es la que Producción usa para descontar stock automáticamente.'],
      captura: { id: 'cap-catalogo', descripcion: 'La grilla del catálogo con las tarjetas de trabajos.' },
    },
    {
      id: 'stock',
      titulo: 'Stock',
      intro: 'El inventario de materiales, con alertas cuando algo cae por debajo del mínimo.',
      queVes: ['Los materiales con stock actual, mínimo y estado (Normal / Bajo / Crítico).'],
      queHaces: ['Registrás entradas (compras) y salidas, configurás la alerta y el WhatsApp del admin.'],
      flujo: ['Cuando una salida perfora el mínimo, el sistema te avisa por WhatsApp (si está configurado el número y activa la alerta).'],
      captura: { id: 'cap-stock', descripcion: 'La lista de materiales con al menos uno en estado Bajo/Crítico.' },
    },
    {
      id: 'finanzas',
      titulo: 'Finanzas',
      intro: 'Las 3 cajas (Física, Bancaria, Compensación), los cobros y los sueldos.',
      queVes: ['El resumen de saldos de las 3 cajas, la deuda con proveedores y lo que se debe en sueldos.', 'El ranking de odontólogos morosos.'],
      queHaces: ['Cargás movimientos de caja a mano, gestionás sueldos y ves quién te debe.'],
      flujo: ['Los pagos a cuenta corriente (efectivo→Física, transferencia→Bancaria) y los sueldos impactan las cajas automáticamente.'],
      captura: { id: 'cap-finanzas', descripcion: 'El dashboard de finanzas con las 3 cajas y el ranking de morosos.' },
    },
    {
      id: 'odontologos',
      titulo: 'Odontólogos y cuenta corriente',
      intro: 'El directorio de odontólogos y, dentro de cada ficha, su cuenta corriente: la solución al "quién me debe cuánto".',
      queVes: ['La ficha del odontólogo con sus datos y su Cuenta corriente: saldo, comprobantes (con estado Pendiente/Parcial/Cobrado) e historial de pagos.'],
      queHaces: ['Tocás "Registrar pago": monto, medio (efectivo/transferencia) y fecha. El pago se imputa a las deudas más viejas primero (puede ser parcial).'],
      flujo: ['El pago baja el saldo, actualiza el ranking de morosos e ingresa a la caja según el medio. Nunca se pierde quién debe cuánto.'],
      captura: { id: 'cap-cuenta-corriente', descripcion: 'La ficha del odontólogo con la sección Cuenta corriente (saldo + tabla de comprobantes) y el botón "Registrar pago".' },
    },
    {
      id: 'bot',
      titulo: 'Bot de WhatsApp',
      intro: 'El bot lee los comprobantes del grupo y registra los pagos solo (con IA).',
      queVes: ['El estado del bot (conectado / QR para vincular) y los registros que fue detectando.'],
      queHaces: ['Vinculás el WhatsApp escaneando el QR, y confirmás o rechazás lo que el bot detectó.'],
      flujo: ['El bot lee monto/operación/emisor/receptor del comprobante, evita duplicados, guarda el archivo y clasifica: pago a empleado (sueldo), a proveedor o triangulado.'],
      captura: { id: 'cap-bot', descripcion: 'La pantalla de estado del bot (con el QR o el estado "conectado") y/o la lista de registros.' },
    },
    {
      id: 'escaneos',
      titulo: 'Escaneos 3D',
      intro: 'Los archivos 3D (STL/OBJ) de cada pedido, con visor incorporado.',
      queVes: ['Los escaneos adjuntos a cada pedido.'],
      queHaces: ['Subís archivos y con "Ver 3D" los previsualizás dentro del sistema (rotar, zoom) sin abrir otro programa.'],
      captura: { id: 'cap-visor-3d', descripcion: 'El visor 3D abierto mostrando un modelo.' },
    },
    {
      id: 'reportes',
      titulo: 'Reportes',
      intro: 'Los KPIs del laboratorio para ver cómo viene el negocio.',
      queVes: ['Indicadores de pedidos (por estado, atrasados) y finanzas (cajas, morosos).'],
      queHaces: ['Filtrás por período para analizar un rango.'],
      captura: { id: 'cap-reportes', descripcion: 'La pantalla de reportes con los indicadores.' },
    },
    {
      id: 'mi-perfil',
      titulo: 'Mi perfil',
      intro: 'Gestionás tu propia cuenta (distinto de Usuarios, que es del admin sobre otros).',
      queVes: ['Tus datos y tu rol.'],
      queHaces: ['Editás tu nombre/apellido/teléfono y cambiás tu contraseña (pidiendo la actual).'],
      captura: { id: 'cap-mi-perfil', descripcion: 'La pantalla Mi perfil con las tarjetas de datos y cambio de contraseña.' },
    },
    {
      id: 'flujos',
      titulo: 'Flujos completos',
      intro: 'Cómo se conecta todo, siguiendo un caso real de principio a fin.',
      queHaces: [
        'Un trabajo de principio a fin: cargás el Pedido → en Producción pasa a EN PROCESO y se descuenta el Stock de la receta → pasa a LISTO y el bot avisa al odontólogo → en Entregas confirmás con el monto y se genera la DEUDA → el odontólogo paga (total o parcial, cuando puede) y lo registrás en su Cuenta corriente → el saldo baja y entra a la caja.',
        'Cobranza / "quién me debe": la deuda vive en la cuenta corriente de cada odontólogo; el ranking de morosos te ordena quién debe más y hace cuántos días. Registrás cada pago manual (efectivo/transferencia) y todo queda al día.',
        'El bot y los comprobantes: el odontólogo manda el comprobante al grupo → el bot lo lee con IA, evita duplicados y lo clasifica → vos confirmás o rechazás.',
        'Sueldos: configurás el sueldo objetivo, registrás pagos (que egresan de la caja); si pagás de más, el excedente se descuenta del próximo ciclo.',
        'Stock: cada producción descuenta materiales; si algo perfora el mínimo, te llega la alerta por WhatsApp.',
      ],
    },
  ];
}
