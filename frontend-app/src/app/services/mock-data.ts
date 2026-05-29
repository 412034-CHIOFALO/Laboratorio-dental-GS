// ═══════════════════════════════════════════════════════════════
// MOCK DATA — datos hardcodeados para modo demo sin backend
// Se usa cuando environment.useMocks === true
// ═══════════════════════════════════════════════════════════════

import { TipoTrabajoResponse } from './catalogo.service';
import { TareaResponse } from './produccion.service';

// ── JWT FAKE ──────────────────────────────────────────────────
// Token con payload { sub: "admin", roles: "ROLE_ADMIN", exp: 9999999999 }
// La firma es inválida pero el frontend solo decodifica el payload (no verifica firma).
// exp = 9999999999 → año 2286, nunca expira.
export const FAKE_JWT =
  'eyJhbGciOiJIUzI1NiJ9.' +
  // base64url de { "sub":"admin","roles":"ROLE_ADMIN","exp":9999999999 }
  'eyJzdWIiOiJhZG1pbiIsInJvbGVzIjoiUk9MRV9BRE1JTiIsImV4cCI6OTk5OTk5OTk5OX0.' +
  'fake-signature-no-verificar';

// ── CATÁLOGO (10 tipos de trabajo) ────────────────────────────
const HOY = new Date().toISOString();

export const MOCK_CATALOGO: TipoTrabajoResponse[] = [
  {
    id: 1,
    nombre: 'Corona Metal Porcelana',
    descripcion: 'Corona de aleación metálica recubierta con cerámica feldespática. Estética y resistencia.',
    precio: 45000,
    categoria: 'FIJA',
    tiempoEstimadoDias: 7,
    fotoUrl: null,
    activo: true,
    fechaCreacion: HOY,
    fechaModificacion: HOY,
  },
  {
    id: 2,
    nombre: 'Corona Zirconio',
    descripcion: 'Corona de zirconio monolítico. Alta estética, ideal para sector anterior.',
    precio: 75000,
    categoria: 'FIJA',
    tiempoEstimadoDias: 10,
    fotoUrl: null,
    activo: true,
    fechaCreacion: HOY,
    fechaModificacion: HOY,
  },
  {
    id: 3,
    nombre: 'Carilla Porcelana',
    descripcion: 'Carilla estética en porcelana feldespática. Solo cara vestibular.',
    precio: 60000,
    categoria: 'FIJA',
    tiempoEstimadoDias: 8,
    fotoUrl: null,
    activo: true,
    fechaCreacion: HOY,
    fechaModificacion: HOY,
  },
  {
    id: 4,
    nombre: 'Prótesis Acrílica Total',
    descripcion: 'Prótesis removible completa en acrílico termocurable. Incluye dientes de stock.',
    precio: 80000,
    categoria: 'REMOVIBLE',
    tiempoEstimadoDias: 14,
    fotoUrl: null,
    activo: true,
    fechaCreacion: HOY,
    fechaModificacion: HOY,
  },
  {
    id: 5,
    nombre: 'Prótesis Esqueletal',
    descripcion: 'Prótesis parcial removible con estructura de cromo-cobalto.',
    precio: 120000,
    categoria: 'REMOVIBLE',
    tiempoEstimadoDias: 21,
    fotoUrl: null,
    activo: true,
    fechaCreacion: HOY,
    fechaModificacion: HOY,
  },
  {
    id: 6,
    nombre: 'Aparato Ortodóntico Móvil',
    descripcion: 'Placa ortodóntica removible con tornillo de expansión.',
    precio: 35000,
    categoria: 'ORTODONCIA',
    tiempoEstimadoDias: 10,
    fotoUrl: null,
    activo: true,
    fechaCreacion: HOY,
    fechaModificacion: HOY,
  },
  {
    id: 7,
    nombre: 'Placa Mio-relajante',
    descripcion: 'Férula oclusal para tratamiento de bruxismo y trastornos ATM.',
    precio: 40000,
    categoria: 'ATM',
    tiempoEstimadoDias: 7,
    fotoUrl: null,
    activo: true,
    fechaCreacion: HOY,
    fechaModificacion: HOY,
  },
  {
    id: 8,
    nombre: 'Provisorio Acrílico',
    descripcion: 'Corona provisoria de acrílico autocurado. Para uso temporal.',
    precio: 15000,
    categoria: 'FIJA',
    tiempoEstimadoDias: 3,
    fotoUrl: null,
    activo: true,
    fechaCreacion: HOY,
    fechaModificacion: HOY,
  },
  {
    id: 9,
    nombre: 'Mantenedor de Espacio',
    descripcion: 'Aparato fijo o removible para conservar espacio en dentición mixta.',
    precio: 28000,
    categoria: 'ORTODONCIA',
    tiempoEstimadoDias: 8,
    fotoUrl: null,
    activo: true,
    fechaCreacion: HOY,
    fechaModificacion: HOY,
  },
  {
    id: 10,
    nombre: 'Modelo de Estudio',
    descripcion: 'Modelo de yeso piedra para análisis y diagnóstico.',
    precio: 12000,
    categoria: 'PERSONALIZADO',
    tiempoEstimadoDias: 2,
    fotoUrl: null,
    activo: true,
    fechaCreacion: HOY,
    fechaModificacion: HOY,
  },
];

// ── PRODUCCIÓN / KANBAN (10 tareas distribuidas) ──────────────
const hoyISO = (offsetDias: number): string => {
  const d = new Date();
  d.setDate(d.getDate() + offsetDias);
  return d.toISOString().split('T')[0];
};

export const MOCK_KANBAN: TareaResponse[] = [
  // ── RECIBIDOS (3) ──
  {
    id: 1,
    nroPedido: 'GYS-2026-0042',
    paciente: 'María González',
    odontologo: 'Dr. Pérez',
    trabajo: 'Corona Metal Porcelana',
    tecnico: null,
    estado: 'RECIBIDO',
    prioridad: 'URGENTE',
    fechaIngreso: hoyISO(-1),
    fechaEntrega: hoyISO(3),
    observaciones: 'Color A2, pieza 16',
  },
  {
    id: 2,
    nroPedido: 'GYS-2026-0043',
    paciente: 'Juan López',
    odontologo: 'Dra. Martínez',
    trabajo: 'Prótesis Acrílica Total',
    tecnico: null,
    estado: 'RECIBIDO',
    prioridad: 'NORMAL',
    fechaIngreso: hoyISO(0),
    fechaEntrega: hoyISO(14),
    observaciones: 'Prótesis superior completa',
  },
  {
    id: 3,
    nroPedido: 'GYS-2026-0044',
    paciente: 'Ana Rodríguez',
    odontologo: 'Dr. Gómez',
    trabajo: 'Carilla Porcelana',
    tecnico: null,
    estado: 'RECIBIDO',
    prioridad: 'NORMAL',
    fechaIngreso: hoyISO(0),
    fechaEntrega: hoyISO(8),
    observaciones: 'Pieza 11, color B1',
  },
  // ── EN PROCESO (3) ──
  {
    id: 4,
    nroPedido: 'GYS-2026-0040',
    paciente: 'Carlos Sánchez',
    odontologo: 'Dr. Pérez',
    trabajo: 'Corona Zirconio',
    tecnico: 'Juan Pereyra',
    estado: 'EN_PROCESO',
    prioridad: 'NORMAL',
    fechaIngreso: hoyISO(-3),
    fechaEntrega: hoyISO(5),
    observaciones: 'Pieza 26',
  },
  {
    id: 5,
    nroPedido: 'GYS-2026-0041',
    paciente: 'Laura Fernández',
    odontologo: 'Dra. Martínez',
    trabajo: 'Aparato Ortodóntico Móvil',
    tecnico: 'María Torres',
    estado: 'EN_PROCESO',
    prioridad: 'URGENTE',
    fechaIngreso: hoyISO(-2),
    fechaEntrega: hoyISO(1),
    observaciones: 'Expansión maxilar',
  },
  {
    id: 6,
    nroPedido: 'GYS-2026-0038',
    paciente: 'Roberto Díaz',
    odontologo: 'Dr. Gómez',
    trabajo: 'Prótesis Esqueletal',
    tecnico: 'Carlos Núñez',
    estado: 'EN_PROCESO',
    prioridad: 'NORMAL',
    fechaIngreso: hoyISO(-5),
    fechaEntrega: hoyISO(10),
    observaciones: 'Pieza 35 a 37 ausentes',
  },
  // ── CONTROL (2) ──
  {
    id: 7,
    nroPedido: 'GYS-2026-0036',
    paciente: 'Patricia Vega',
    odontologo: 'Dr. Pérez',
    trabajo: 'Placa Mio-relajante',
    tecnico: 'Juan Pereyra',
    estado: 'CONTROL',
    prioridad: 'NORMAL',
    fechaIngreso: hoyISO(-6),
    fechaEntrega: hoyISO(1),
    observaciones: 'Control de oclusión',
  },
  {
    id: 8,
    nroPedido: 'GYS-2026-0037',
    paciente: 'Marcos Herrera',
    odontologo: 'Dra. Suárez',
    trabajo: 'Corona Metal Porcelana',
    tecnico: 'María Torres',
    estado: 'CONTROL',
    prioridad: 'URGENTE',
    fechaIngreso: hoyISO(-7),
    fechaEntrega: hoyISO(0),
    observaciones: 'Revisar color cervical',
  },
  // ── LISTO (2) ──
  {
    id: 9,
    nroPedido: 'GYS-2026-0034',
    paciente: 'Sofía Romero',
    odontologo: 'Dr. Gómez',
    trabajo: 'Provisorio Acrílico',
    tecnico: 'Carlos Núñez',
    estado: 'LISTO',
    prioridad: 'NORMAL',
    fechaIngreso: hoyISO(-4),
    fechaEntrega: hoyISO(-1),
    observaciones: 'Esperando retiro',
  },
  {
    id: 10,
    nroPedido: 'GYS-2026-0035',
    paciente: 'Federico Aguirre',
    odontologo: 'Dr. Pérez',
    trabajo: 'Modelo de Estudio',
    tecnico: 'Juan Pereyra',
    estado: 'LISTO',
    prioridad: 'NORMAL',
    fechaIngreso: hoyISO(-3),
    fechaEntrega: hoyISO(0),
    observaciones: '',
  },
];

// ── USUARIOS DEL SISTEMA ──────────────────────────────────────
export interface MockUsuario {
  id: number;
  username: string;
  nombre: string;
  apellido: string;
  rol: string;
  enabled: boolean;
}

export const MOCK_USUARIOS: MockUsuario[] = [
  { id: 1, username: 'admin',    nombre: 'Nicolás',  apellido: 'Chiofalo', rol: 'ADMIN',          enabled: true  },
  { id: 2, username: 'mariana',  nombre: 'Mariana',  apellido: 'Suárez',   rol: 'ADMINISTRATIVO', enabled: true  },
  { id: 3, username: 'juan',     nombre: 'Juan',     apellido: 'Pereyra',  rol: 'TECNICO',        enabled: true  },
  { id: 4, username: 'maria',    nombre: 'María',    apellido: 'Torres',   rol: 'TECNICO',        enabled: true  },
  { id: 5, username: 'carlos',   nombre: 'Carlos',   apellido: 'Núñez',    rol: 'TECNICO',        enabled: true  },
  { id: 6, username: 'drperez',  nombre: 'Roberto',  apellido: 'Pérez',    rol: 'ODONTOLOGO',     enabled: true  },
  { id: 7, username: 'pendiente', nombre: 'Lucía',   apellido: 'García',   rol: 'TECNICO',        enabled: false },
];

// ── PEDIDOS ───────────────────────────────────────────────────
export interface MockPedido {
  id: number;
  nroPedido: string;
  paciente: string;
  odontologo: string;
  direccionOdontologo: string;
  tipoTrabajo: string;
  estado: 'BORRADOR' | 'RECEPCIONADO' | 'EN_PRODUCCION' | 'LISTO' | 'ENTREGADO' | 'CANCELADO';
  prioridad: 'NORMAL' | 'URGENTE';
  fechaIngreso: string;
  fechaEntrega: string;
  precio: number;
  canal: 'MANUAL' | 'WHATSAPP' | 'EMAIL';
}

export const MOCK_PEDIDOS: MockPedido[] = [
  { id: 101, nroPedido: 'GYS-2026-0047', paciente: 'Pedro Ruiz',        odontologo: 'Dra. Martínez', direccionOdontologo: 'San Martín 567, Palermo',          tipoTrabajo: 'Prótesis Acrílica Total',   estado: 'BORRADOR',      prioridad: 'NORMAL',  fechaIngreso: hoyISO(0),   fechaEntrega: hoyISO(14), precio: 80000,  canal: 'EMAIL'     },
  { id: 102, nroPedido: 'GYS-2026-0046', paciente: 'Ana Lima',          odontologo: 'Dr. Pérez',      direccionOdontologo: 'Av. Corrientes 1234, CABA',         tipoTrabajo: 'Corona Metal Porcelana',    estado: 'BORRADOR',      prioridad: 'NORMAL',  fechaIngreso: hoyISO(0),   fechaEntrega: hoyISO(7),  precio: 45000,  canal: 'WHATSAPP'  },
  { id: 103, nroPedido: 'GYS-2026-0045', paciente: 'Clara Méndez',      odontologo: 'Dr. Gómez',      direccionOdontologo: 'Corrientes 3456, Almagro',           tipoTrabajo: 'Carilla Porcelana',         estado: 'RECEPCIONADO',  prioridad: 'URGENTE', fechaIngreso: hoyISO(-1),  fechaEntrega: hoyISO(5),  precio: 60000,  canal: 'MANUAL'    },
  { id: 104, nroPedido: 'GYS-2026-0044', paciente: 'Ana Rodríguez',     odontologo: 'Dr. Gómez',      direccionOdontologo: 'Corrientes 3456, Almagro',           tipoTrabajo: 'Carilla Porcelana',         estado: 'EN_PRODUCCION', prioridad: 'NORMAL',  fechaIngreso: hoyISO(0),   fechaEntrega: hoyISO(8),  precio: 60000,  canal: 'MANUAL'    },
  { id: 105, nroPedido: 'GYS-2026-0043', paciente: 'Juan López',        odontologo: 'Dra. Martínez',  direccionOdontologo: 'San Martín 567, Palermo',            tipoTrabajo: 'Prótesis Acrílica Total',   estado: 'EN_PRODUCCION', prioridad: 'NORMAL',  fechaIngreso: hoyISO(0),   fechaEntrega: hoyISO(14), precio: 80000,  canal: 'MANUAL'    },
  { id: 106, nroPedido: 'GYS-2026-0042', paciente: 'María González',    odontologo: 'Dr. Pérez',      direccionOdontologo: 'Av. Corrientes 1234, CABA',         tipoTrabajo: 'Corona Metal Porcelana',    estado: 'EN_PRODUCCION', prioridad: 'URGENTE', fechaIngreso: hoyISO(-1),  fechaEntrega: hoyISO(3),  precio: 45000,  canal: 'MANUAL'    },
  { id: 109, nroPedido: 'GYS-2026-0034', paciente: 'Sofía Romero',      odontologo: 'Dr. Gómez',      direccionOdontologo: 'Corrientes 3456, Almagro',           tipoTrabajo: 'Provisorio Acrílico',       estado: 'LISTO',         prioridad: 'NORMAL',  fechaIngreso: hoyISO(-4),  fechaEntrega: hoyISO(-1), precio: 15000,  canal: 'MANUAL'    },
  { id: 110, nroPedido: 'GYS-2026-0035', paciente: 'Federico Aguirre',  odontologo: 'Dr. Pérez',      direccionOdontologo: 'Av. Corrientes 1234, CABA',         tipoTrabajo: 'Modelo de Estudio',         estado: 'LISTO',         prioridad: 'NORMAL',  fechaIngreso: hoyISO(-3),  fechaEntrega: hoyISO(0),  precio: 12000,  canal: 'MANUAL'    },
  { id: 111, nroPedido: 'GYS-2026-0030', paciente: 'Lucas Torres',      odontologo: 'Dr. Pérez',      direccionOdontologo: 'Av. Corrientes 1234, CABA',         tipoTrabajo: 'Corona Zirconio',           estado: 'ENTREGADO',     prioridad: 'NORMAL',  fechaIngreso: hoyISO(-10), fechaEntrega: hoyISO(-3), precio: 75000,  canal: 'MANUAL'    },
  { id: 112, nroPedido: 'GYS-2026-0031', paciente: 'Valentina Cruz',    odontologo: 'Dra. Martínez',  direccionOdontologo: 'San Martín 567, Palermo',            tipoTrabajo: 'Aparato Ortodóntico Móvil', estado: 'ENTREGADO',     prioridad: 'URGENTE', fechaIngreso: hoyISO(-8),  fechaEntrega: hoyISO(-2), precio: 35000,  canal: 'WHATSAPP'  },
  { id: 113, nroPedido: 'GYS-2026-0032', paciente: 'Martín Ríos',       odontologo: 'Dr. Gómez',      direccionOdontologo: 'Corrientes 3456, Almagro',           tipoTrabajo: 'Placa Mio-relajante',       estado: 'ENTREGADO',     prioridad: 'NORMAL',  fechaIngreso: hoyISO(-7),  fechaEntrega: hoyISO(-1), precio: 40000,  canal: 'MANUAL'    },
  { id: 114, nroPedido: 'GYS-2026-0029', paciente: 'Romina Castro',     odontologo: 'Dr. Gómez',      direccionOdontologo: 'Corrientes 3456, Almagro',           tipoTrabajo: 'Prótesis Esqueletal',       estado: 'CANCELADO',     prioridad: 'NORMAL',  fechaIngreso: hoyISO(-15), fechaEntrega: hoyISO(-5), precio: 120000, canal: 'MANUAL'    },
];

// ── CLIENTES FINANCIEROS ──────────────────────────────────────
export interface MockClienteFinanciero {
  id: number;
  nombre: string;
  telefono: string;
  deuda: number;
  ultimoPago: string;
  ultimoPagoMonto: number;
  pedidosPendientes: number;
}

export const MOCK_CLIENTES_FINANCIERO: MockClienteFinanciero[] = [
  { id: 1, nombre: 'Dr. Roberto Pérez',    telefono: '+54 11 4234-5678', deuda: 245000, ultimoPago: hoyISO(-5),  ultimoPagoMonto: 45000,  pedidosPendientes: 3 },
  { id: 2, nombre: 'Dra. Laura Martínez',  telefono: '+54 11 4567-8901', deuda: 80000,  ultimoPago: hoyISO(-2),  ultimoPagoMonto: 120000, pedidosPendientes: 1 },
  { id: 3, nombre: 'Dr. Carlos Gómez',     telefono: '+54 11 4890-1234', deuda: 310000, ultimoPago: hoyISO(-12), ultimoPagoMonto: 60000,  pedidosPendientes: 2 },
  { id: 4, nombre: 'Dra. Susana López',    telefono: '+54 11 4123-4567', deuda: 0,      ultimoPago: hoyISO(-1),  ultimoPagoMonto: 75000,  pedidosPendientes: 1 },
  { id: 5, nombre: 'Dr. Miguel Suárez',    telefono: '+54 11 4345-6789', deuda: 150000, ultimoPago: hoyISO(-20), ultimoPagoMonto: 30000,  pedidosPendientes: 2 },
];

// ── STOCK / MATERIALES ────────────────────────────────────────
export interface MockMaterial {
  id: number;
  nombre: string;
  unidad: string;
  stockActual: number;
  stockMinimo: number;
  proveedor: string;
  ultimoMovimiento: string;
  tipoUltimoMov: 'ENTRADA' | 'SALIDA' | 'AJUSTE';
}

export const MOCK_MATERIALES: MockMaterial[] = [
  { id: 1, nombre: 'Yeso Piedra Tipo IV',               unidad: 'potes',    stockActual: 3,   stockMinimo: 5,   proveedor: 'Casa Dental Norte',  ultimoMovimiento: hoyISO(-1), tipoUltimoMov: 'SALIDA'  },
  { id: 2, nombre: 'Cerámica Vita VM13',                unidad: 'frascos',  stockActual: 2,   stockMinimo: 4,   proveedor: 'Dental Supply SRL',  ultimoMovimiento: hoyISO(-2), tipoUltimoMov: 'SALIDA'  },
  { id: 3, nombre: 'Aleación Cr-Co NPG',                unidad: 'gramos',   stockActual: 450, stockMinimo: 200, proveedor: 'MetalDent SA',       ultimoMovimiento: hoyISO(-3), tipoUltimoMov: 'ENTRADA' },
  { id: 4, nombre: 'Acrílico Rosa Termocurable',        unidad: 'frascos',  stockActual: 12,  stockMinimo: 6,   proveedor: 'Casa Dental Norte',  ultimoMovimiento: hoyISO(-1), tipoUltimoMov: 'SALIDA'  },
  { id: 5, nombre: 'Alambre Inox 0.7mm',                unidad: 'rollos',   stockActual: 8,   stockMinimo: 3,   proveedor: 'Dental Supply SRL',  ultimoMovimiento: hoyISO(-5), tipoUltimoMov: 'ENTRADA' },
  { id: 6, nombre: 'Resina Acrílica Autopolimerizable', unidad: 'kits',     stockActual: 4,   stockMinimo: 4,   proveedor: 'Casa Dental Norte',  ultimoMovimiento: hoyISO(-2), tipoUltimoMov: 'SALIDA'  },
  { id: 7, nombre: 'Discos Zirconia 98mm',              unidad: 'unidades', stockActual: 6,   stockMinimo: 3,   proveedor: 'Zirkon Dental',      ultimoMovimiento: hoyISO(-4), tipoUltimoMov: 'SALIDA'  },
  { id: 8, nombre: 'Separadores de Goma',               unidad: 'bolsas',   stockActual: 15,  stockMinimo: 5,   proveedor: 'Casa Dental Norte',  ultimoMovimiento: hoyISO(-7), tipoUltimoMov: 'ENTRADA' },
];

// ── AUDITORÍA ─────────────────────────────────────────────────
export type TipoAudit = 'LOGIN' | 'CREAR' | 'EDITAR' | 'PAGO' | 'ESTADO' | 'ELIMINAR';

export interface MockAuditEvent {
  id: number;
  timestamp: string;
  usuario: string;
  accion: string;
  entidad: string;
  detalle: string;
  tipo: TipoAudit;
}

const tsISO = (offsetHoras: number): string => {
  const d = new Date();
  d.setTime(d.getTime() - offsetHoras * 3_600_000);
  return d.toISOString();
};

export const MOCK_AUDIT: MockAuditEvent[] = [
  { id:  1, timestamp: tsISO(0.1),  usuario: 'admin',   accion: 'Inicio de sesión',       entidad: 'Sesión',               detalle: 'Login exitoso desde 192.168.1.10',                   tipo: 'LOGIN'    },
  { id:  2, timestamp: tsISO(0.5),  usuario: 'mariana', accion: 'Comprobante aprobado',    entidad: 'Transacción #4521',    detalle: 'Pago Dr. Pérez $45.000 confirmado',                  tipo: 'PAGO'     },
  { id:  3, timestamp: tsISO(1),    usuario: 'juan',    accion: 'Estado de tarea avanzado', entidad: 'GYS-2026-0040',       detalle: 'EN_PROCESO → CONTROL',                               tipo: 'ESTADO'   },
  { id:  4, timestamp: tsISO(2),    usuario: 'admin',   accion: 'Pedido validado',          entidad: 'GYS-2026-0045',       detalle: 'Borrador de WhatsApp validado',                      tipo: 'EDITAR'   },
  { id:  5, timestamp: tsISO(3),    usuario: 'mariana', accion: 'Cliente editado',          entidad: 'Dr. Gómez',           detalle: 'Teléfono actualizado',                               tipo: 'EDITAR'   },
  { id:  6, timestamp: tsISO(5),    usuario: 'maria',   accion: 'Estado de tarea avanzado', entidad: 'GYS-2026-0034',       detalle: 'CONTROL → LISTO',                                    tipo: 'ESTADO'   },
  { id:  7, timestamp: tsISO(8),    usuario: 'admin',   accion: 'Trabajo creado',           entidad: 'Catálogo',             detalle: 'Nuevo: "Inlay Porcelana" — $55.000',                 tipo: 'CREAR'    },
  { id:  8, timestamp: tsISO(12),   usuario: 'mariana', accion: 'Pago registrado',          entidad: 'Dr. Martínez',        detalle: 'Efectivo $30.000 — Caja Física',                     tipo: 'PAGO'     },
  { id:  9, timestamp: tsISO(24),   usuario: 'admin',   accion: 'Usuario creado',           entidad: 'Lucía García',        detalle: 'Rol: TECNICO — pendiente activación',                tipo: 'CREAR'    },
  { id: 10, timestamp: tsISO(25),   usuario: 'carlos',  accion: 'Estado de tarea avanzado', entidad: 'GYS-2026-0036',       detalle: 'EN_PROCESO → CONTROL',                               tipo: 'ESTADO'   },
  { id: 11, timestamp: tsISO(26),   usuario: 'mariana', accion: 'Inicio de sesión',        entidad: 'Sesión',               detalle: 'Login exitoso desde 192.168.1.15',                   tipo: 'LOGIN'    },
  { id: 12, timestamp: tsISO(28),   usuario: 'admin',   accion: 'Trabajo desactivado',     entidad: 'Catálogo',             detalle: '"Modelo Diagnóstico" desactivado',                   tipo: 'ELIMINAR' },
  { id: 13, timestamp: tsISO(36),   usuario: 'mariana', accion: 'Pago triangulado',        entidad: 'Dr. Suárez',           detalle: '$150.000 → Juan Pereyra (sueldo)',                    tipo: 'PAGO'     },
  { id: 14, timestamp: tsISO(48),   usuario: 'admin',   accion: 'Stock ajustado',          entidad: 'Yeso Piedra Tipo IV',  detalle: 'Conteo físico: teórico 5 → real 3',                 tipo: 'EDITAR'   },
];

// ── ODONTÓLOGOS (alineados con OdontologoResponse del backend) ─
import type { OdontologoResponse } from './odontologos.service';
import type { PedidoResponse } from './pedidos.service';

export const MOCK_ODONTOLOGOS: OdontologoResponse[] = [
  { id: 1, nombre: 'Dr. Martín García',    dni: '28456789', cuit: '20-28456789-3', telefono: '11-4567-8901', email: 'martin.garcia@odontologia.com.ar', matricula: 'MN 12345', clinica: 'Clínica Odontológica Norte',     direccion: 'Av. Cabildo 2350, CABA',        activo: true, fechaCreacion: HOY, fechaModificacion: HOY },
  { id: 2, nombre: 'Dra. Laura Sánchez',   dni: '30123456', cuit: '27-30123456-5', telefono: '11-2345-6789', email: 'laura.sanchez@odonto.com.ar',       matricula: 'MN 23456', clinica: 'Consultorio Dental Belgrano',     direccion: 'Mendoza 1820, CABA',            activo: true, fechaCreacion: HOY, fechaModificacion: HOY },
  { id: 3, nombre: 'Dr. Carlos Ruiz',      dni: '25789012', cuit: '20-25789012-7', telefono: '11-5555-1234', email: 'c.ruiz@dental.com.ar',              matricula: 'MN 34567', clinica: 'Centro Odontológico Palermo',     direccion: 'Scalabrini Ortiz 950, CABA',    activo: true, fechaCreacion: HOY, fechaModificacion: HOY },
  { id: 4, nombre: 'Dra. Verónica Molina', dni: '32654987', cuit: null,            telefono: '11-6789-0123', email: null,                                 matricula: 'MN 45678', clinica: 'Odontología Integral San Telmo', direccion: 'Defensa 750, CABA',             activo: true, fechaCreacion: HOY, fechaModificacion: HOY },
  { id: 5, nombre: 'Dr. Roberto Pérez',    dni: '24567890', cuit: '20-24567890-1', telefono: '11-4234-5678', email: 'r.perez@dentista.com.ar',           matricula: 'MN 56789', clinica: 'Pérez Odontología',               direccion: 'Av. Corrientes 4500, CABA',     activo: true, fechaCreacion: HOY, fechaModificacion: HOY },
  { id: 6, nombre: 'Dra. Susana López',    dni: '29876543', cuit: '27-29876543-2', telefono: '11-4123-4567', email: 'susana.lopez@dental.com',           matricula: 'MN 67890', clinica: 'Clínica Dental López',            direccion: 'Av. Rivadavia 3200, CABA',      activo: true, fechaCreacion: HOY, fechaModificacion: HOY },
];

// ── PEDIDOS (alineados con PedidoResponse del backend) ─────────
export const MOCK_PEDIDOS_BACKEND: PedidoResponse[] = [
  {
    id: 1, nroPedido: 'PED-20260528-0001',
    odontologoId: 1, odontologoNombre: 'Dr. Martín García',
    paciente: 'Martín López',
    catalogoTrabajoId: 1, trabajo: 'Corona Metal-Cerámica',
    tecnicoId: null, tecnicoNombre: null,
    fechaEntrega: hoyISO(5), estado: 'RECIBIDO', prioridad: 'URGENTE',
    precioAcordado: 15000, observaciones: 'Urgente — paciente con cita',
    fechaEntregaReal: null, retiradoPor: null, observacionesEntrega: null,
    fechaCreacion: HOY, fechaUltimaModificacion: HOY,
  },
  {
    id: 2, nroPedido: 'PED-20260528-0002',
    odontologoId: 1, odontologoNombre: 'Dr. Martín García',
    paciente: 'Ana Rodríguez',
    catalogoTrabajoId: 4, trabajo: 'Prótesis Acrílica Total',
    tecnicoId: 2, tecnicoNombre: 'Carlos López',
    fechaEntrega: hoyISO(12), estado: 'EN_PROCESO', prioridad: 'NORMAL',
    precioAcordado: 45000, observaciones: 'Incluir ajuste de mordida',
    fechaEntregaReal: null, retiradoPor: null, observacionesEntrega: null,
    fechaCreacion: HOY, fechaUltimaModificacion: HOY,
  },
  {
    id: 3, nroPedido: 'PED-20260528-0003',
    odontologoId: 2, odontologoNombre: 'Dra. Laura Sánchez',
    paciente: 'Luis Fernández',
    catalogoTrabajoId: 3, trabajo: 'Carilla Porcelana',
    tecnicoId: 2, tecnicoNombre: 'Carlos López',
    fechaEntrega: hoyISO(3), estado: 'EN_PROCESO', prioridad: 'NORMAL',
    precioAcordado: 8000, observaciones: null,
    fechaEntregaReal: null, retiradoPor: null, observacionesEntrega: null,
    fechaCreacion: HOY, fechaUltimaModificacion: HOY,
  },
  {
    id: 4, nroPedido: 'PED-20260528-0004',
    odontologoId: 2, odontologoNombre: 'Dra. Laura Sánchez',
    paciente: 'Elena Gómez',
    catalogoTrabajoId: 6, trabajo: 'Aparato Ortodóntico Móvil',
    tecnicoId: 2, tecnicoNombre: 'Carlos López',
    fechaEntrega: hoyISO(1), estado: 'CONTROL', prioridad: 'NORMAL',
    precioAcordado: 18000, observaciones: 'Verificar alambre labial',
    fechaEntregaReal: null, retiradoPor: null, observacionesEntrega: null,
    fechaCreacion: HOY, fechaUltimaModificacion: HOY,
  },
  {
    id: 5, nroPedido: 'PED-20260528-0005',
    odontologoId: 1, odontologoNombre: 'Dr. Martín García',
    paciente: 'Roberto Díaz',
    catalogoTrabajoId: 7, trabajo: 'Placa Mio-relajante',
    tecnicoId: 2, tecnicoNombre: 'Carlos López',
    fechaEntrega: hoyISO(-1), estado: 'LISTO', prioridad: 'NORMAL',
    precioAcordado: 12000, observaciones: 'Pulido final aprobado',
    fechaEntregaReal: null, retiradoPor: null, observacionesEntrega: null,
    fechaCreacion: HOY, fechaUltimaModificacion: HOY,
  },
  {
    id: 6, nroPedido: 'PED-20260528-0006',
    odontologoId: 5, odontologoNombre: 'Dr. Roberto Pérez',
    paciente: 'Sofía Romero',
    catalogoTrabajoId: 8, trabajo: 'Provisorio Acrílico',
    tecnicoId: 2, tecnicoNombre: 'Carlos López',
    fechaEntrega: hoyISO(-1), estado: 'LISTO', prioridad: 'NORMAL',
    precioAcordado: 15000, observaciones: null,
    fechaEntregaReal: null, retiradoPor: null, observacionesEntrega: null,
    fechaCreacion: HOY, fechaUltimaModificacion: HOY,
  },
  {
    id: 7, nroPedido: 'PED-20260528-0007',
    odontologoId: 2, odontologoNombre: 'Dra. Laura Sánchez',
    paciente: 'Carmen Vidal',
    catalogoTrabajoId: 2, trabajo: 'Corona Zirconio',
    tecnicoId: 2, tecnicoNombre: 'Carlos López',
    fechaEntrega: hoyISO(0), estado: 'LISTO', prioridad: 'URGENTE',
    precioAcordado: 32000, observaciones: 'Paciente con turno mañana',
    fechaEntregaReal: null, retiradoPor: null, observacionesEntrega: null,
    fechaCreacion: HOY, fechaUltimaModificacion: HOY,
  },
  {
    id: 8, nroPedido: 'PED-20260527-0001',
    odontologoId: 1, odontologoNombre: 'Dr. Martín García',
    paciente: 'Esteban Quiroga',
    catalogoTrabajoId: 1, trabajo: 'Corona Metal-Cerámica',
    tecnicoId: 2, tecnicoNombre: 'Carlos López',
    fechaEntrega: hoyISO(-2), estado: 'ENTREGADO', prioridad: 'NORMAL',
    precioAcordado: 15000, observaciones: null,
    fechaEntregaReal: hoyISO(-2),
    retiradoPor: 'Cadetería del consultorio',
    observacionesEntrega: 'Entrega sin novedades.',
    fechaCreacion: HOY, fechaUltimaModificacion: HOY,
  },
  {
    id: 9, nroPedido: 'PED-20260527-0002',
    odontologoId: 2, odontologoNombre: 'Dra. Laura Sánchez',
    paciente: 'Marta Suárez',
    catalogoTrabajoId: 3, trabajo: 'Carilla Porcelana',
    tecnicoId: 2, tecnicoNombre: 'Carlos López',
    fechaEntrega: hoyISO(-1), estado: 'ENTREGADO', prioridad: 'NORMAL',
    precioAcordado: 60000, observaciones: null,
    fechaEntregaReal: hoyISO(-1),
    retiradoPor: 'Dra. Laura Sánchez (en persona)',
    observacionesEntrega: null,
    fechaCreacion: HOY, fechaUltimaModificacion: HOY,
  },
];

// ── HELPER: clonar mock para no mutar el original ────────────
export function clonar<T>(data: T): T {
  return JSON.parse(JSON.stringify(data));
}
