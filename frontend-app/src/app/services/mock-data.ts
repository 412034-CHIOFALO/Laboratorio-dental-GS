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

// ── HELPER: clonar mock para no mutar el original ────────────
export function clonar<T>(data: T): T {
  return JSON.parse(JSON.stringify(data));
}
