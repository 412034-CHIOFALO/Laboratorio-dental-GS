import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, of } from 'rxjs';
import { delay } from 'rxjs/operators';
import { environment } from '../../environments/environment';

/**
 * Frecuencia con la que se le paga a un integrante del laboratorio.
 *  - DIARIO:    cobra por día trabajado
 *  - SEMANAL:   cobra una vez por semana
 *  - QUINCENAL: cada 15 días
 *  - MENSUAL:   una vez al mes
 */
export type FrecuenciaPago = 'DIARIO' | 'SEMANAL' | 'QUINCENAL' | 'MENSUAL';

export type RolEmpleado = 'TECNICO' | 'ADMINISTRATIVO' | 'ADMIN' | 'OTRO';

/**
 * Configuración de sueldo de un integrante del laboratorio.
 * Está ligado a un usuario del sistema (usuarioId → ms-auth).
 *
 *  - saldoDevengado: lo que el laboratorio le debe acumulado (positivo = a favor del empleado)
 *  - saldoSobrante:  lo que se le pagó de MÁS, a descontar del próximo ciclo
 */
export interface EmpleadoSueldo {
  usuarioId: number;
  nombre: string;
  rol: RolEmpleado;
  telefono: string | null;       // sirve también para que el bot lo identifique
  activo: boolean;
  frecuencia: FrecuenciaPago;
  montoBase: number;             // monto por ciclo completo
  saldoDevengado: number;        // lo que el lab le debe hoy
  saldoSobrante: number;         // pagado de más, a descontar
  ultimoPago: string | null;     // fecha ISO del último pago
}

export interface ConfigSueldoRequest {
  frecuencia: FrecuenciaPago;
  montoBase: number;
}

export interface PagoSueldoRequest {
  usuarioId: number;
  monto: number;
  /** Qué hacer si se paga de más respecto al devengado. */
  manejoSobrante: 'DESCONTAR_PROXIMO' | 'CUBRE_LAB' | 'DEVUELVE_EMPLEADO';
  fecha?: string | null;
  nota?: string | null;
}

export type OrigenPago = 'MANUAL' | 'BOT_WHATSAPP';

/** Item del histórico de pagos. */
export interface PagoSueldoResponse {
  id: number;
  empleadoId: number;
  empleadoNombre: string;
  monto: number;
  fecha: string;
  origen: OrigenPago;
  manejoSobrante: string | null;
  montoExcedente: number;
  nota: string | null;
  cargadoPorNombre: string | null;
  emisor: string | null;
  comprobanteUrl: string | null;
  grupoOrigen: string | null;
}

@Injectable({ providedIn: 'root' })
export class SueldosService {

  private readonly base = `${environment.gatewayUrl}/api/finanzas/sueldos`;

  // Store mutable en memoria para mocks
  private store: EmpleadoSueldo[] = this.seedMock();
  private pagosMock: PagoSueldoResponse[] = this.seedPagosMock();
  private nextPagoId = 7000;

  constructor(private http: HttpClient) {}

  listarEmpleados(): Observable<EmpleadoSueldo[]> {
    if (environment.useMocks) {
      return of(this.store.map(e => ({ ...e }))).pipe(delay(200));
    }
    return this.http.get<EmpleadoSueldo[]>(`${this.base}/empleados`);
  }

  /** Actualiza manualmente la configuración de sueldo (frecuencia + monto). */
  actualizarConfig(usuarioId: number, req: ConfigSueldoRequest): Observable<EmpleadoSueldo> {
    if (environment.useMocks) {
      const e = this.store.find(x => x.usuarioId === usuarioId);
      if (!e) throw new Error('Empleado no encontrado');
      e.frecuencia = req.frecuencia;
      e.montoBase = req.montoBase;
      return of({ ...e }).pipe(delay(180));
    }
    return this.http.put<EmpleadoSueldo>(`${this.base}/empleados/${usuarioId}/config`, req);
  }

  /** Histórico de pagos de un empleado. */
  historialPagos(usuarioId: number): Observable<PagoSueldoResponse[]> {
    if (environment.useMocks) {
      const lista = this.pagosMock
        .filter(p => p.empleadoId === usuarioId)
        .sort((a, b) => new Date(b.fecha).getTime() - new Date(a.fecha).getTime());
      return of(lista.map(p => ({ ...p }))).pipe(delay(180));
    }
    return this.http.get<PagoSueldoResponse[]>(`${this.base}/empleados/${usuarioId}/pagos`);
  }

  /** Histórico global de pagos. */
  historialPagosGlobal(): Observable<PagoSueldoResponse[]> {
    if (environment.useMocks) {
      const lista = [...this.pagosMock].sort((a, b) => new Date(b.fecha).getTime() - new Date(a.fecha).getTime());
      return of(lista).pipe(delay(180));
    }
    return this.http.get<PagoSueldoResponse[]>(`${this.base}/pagos`);
  }

  /** URL temporal para ver el comprobante guardado de un pago. */
  urlComprobante(pagoId: number): Observable<{ url: string }> {
    if (environment.useMocks) {
      return of({ url: 'https://ejemplo.com/comprobante-demo.pdf' }).pipe(delay(150));
    }
    return this.http.get<{ url: string }>(`${this.base}/pagos/${pagoId}/comprobante`);
  }

  /** Registra un pago al empleado, ajustando devengado/sobrante. */
  registrarPago(req: PagoSueldoRequest): Observable<EmpleadoSueldo> {
    if (environment.useMocks) {
      const e = this.store.find(x => x.usuarioId === req.usuarioId);
      if (!e) throw new Error('Empleado no encontrado');

      let excedente = 0;
      const restante = e.saldoDevengado - req.monto;
      if (restante >= 0) {
        e.saldoDevengado = restante;
      } else {
        excedente = Math.abs(restante);
        e.saldoDevengado = 0;
        if (req.manejoSobrante === 'DESCONTAR_PROXIMO') e.saldoSobrante += excedente;
      }
      e.ultimoPago = req.fecha ?? new Date().toISOString().slice(0, 10);

      // Guardar en el histórico mock
      this.pagosMock.unshift({
        id: this.nextPagoId++,
        empleadoId: e.usuarioId,
        empleadoNombre: e.nombre,
        monto: req.monto,
        fecha: e.ultimoPago,
        origen: 'MANUAL',
        manejoSobrante: req.manejoSobrante,
        montoExcedente: excedente,
        nota: req.nota ?? null,
        cargadoPorNombre: null,
        emisor: null,
        comprobanteUrl: null,
        grupoOrigen: null,
      });
      return of({ ...e }).pipe(delay(220));
    }
    return this.http.post<EmpleadoSueldo>(`${this.base}/pago`, req);
  }

  /** Ajuste manual del saldo devengado (por si el cálculo automático no cuadra). */
  ajustarDevengado(usuarioId: number, nuevoDevengado: number): Observable<EmpleadoSueldo> {
    if (environment.useMocks) {
      const e = this.store.find(x => x.usuarioId === usuarioId);
      if (!e) throw new Error('Empleado no encontrado');
      e.saldoDevengado = nuevoDevengado;
      return of({ ...e }).pipe(delay(150));
    }
    return this.http.patch<EmpleadoSueldo>(`${this.base}/empleados/${usuarioId}/devengado`, { devengado: nuevoDevengado });
  }

  // ── Comprobantes demo (mock) — algunos del bot, algunos manuales ──
  private seedPagosMock(): PagoSueldoResponse[] {
    const d = (n: number) => { const x = new Date(); x.setDate(x.getDate() - n); return x.toISOString().slice(0, 10); };
    return [
      { id: 6001, empleadoId: 2, empleadoNombre: 'Carlos López', monto: 60000, fecha: d(1), origen: 'BOT_WHATSAPP', manejoSobrante: null, montoExcedente: 0, nota: null, cargadoPorNombre: 'Mario Giménez', emisor: 'Dr. Delgado', comprobanteUrl: 'comprobantes/2026/06/abc.pdf', grupoOrigen: 'Comprobantes Transferencias' },
      { id: 6002, empleadoId: 3, empleadoNombre: 'Mario Giménez', monto: 30000, fecha: d(2), origen: 'BOT_WHATSAPP', manejoSobrante: null, montoExcedente: 0, nota: null, cargadoPorNombre: 'Valentina Torres', emisor: 'Dra. Sánchez', comprobanteUrl: 'comprobantes/2026/06/def.pdf', grupoOrigen: 'Comprobantes Transferencias' },
      { id: 6003, empleadoId: 4, empleadoNombre: 'Valentina Torres', monto: 90000, fecha: d(3), origen: 'MANUAL', manejoSobrante: 'DESCONTAR_PROXIMO', montoExcedente: 0, nota: 'Pago quincena', cargadoPorNombre: null, emisor: 'Dr. García', comprobanteUrl: null, grupoOrigen: null },
      { id: 6004, empleadoId: 2, empleadoNombre: 'Carlos López', monto: 50000, fecha: d(5), origen: 'BOT_WHATSAPP', manejoSobrante: null, montoExcedente: 0, nota: null, cargadoPorNombre: 'Mario Giménez', emisor: 'Dr. Delgado', comprobanteUrl: 'comprobantes/2026/06/ghi.jpg', grupoOrigen: 'Comprobantes Efectivo' },
    ];
  }

  // ── Datos demo (mock) — integrantes del laboratorio ──────────────
  private seedMock(): EmpleadoSueldo[] {
    return [
      { usuarioId: 1, nombre: 'Rebeca González', rol: 'ADMIN',          telefono: '351-655-1001', activo: true, frecuencia: 'MENSUAL',   montoBase: 0,      saldoDevengado: 0,      saldoSobrante: 0, ultimoPago: null },
      { usuarioId: 2, nombre: 'Carlos López',    rol: 'TECNICO',        telefono: '351-655-1002', activo: true, frecuencia: 'SEMANAL',   montoBase: 120000, saldoDevengado: 120000, saldoSobrante: 0, ultimoPago: '2026-05-29' },
      { usuarioId: 3, nombre: 'Mario Giménez',   rol: 'TECNICO',        telefono: '351-655-1003', activo: true, frecuencia: 'DIARIO',    montoBase: 30000,  saldoDevengado: 90000,  saldoSobrante: 0, ultimoPago: '2026-06-03' },
      { usuarioId: 4, nombre: 'Valentina Torres',rol: 'ADMINISTRATIVO', telefono: '351-655-1004', activo: true, frecuencia: 'QUINCENAL', montoBase: 180000, saldoDevengado: 90000,  saldoSobrante: 15000, ultimoPago: '2026-05-31' },
      { usuarioId: 5, nombre: 'Diego Ferreyra',  rol: 'TECNICO',        telefono: '351-655-1005', activo: true, frecuencia: 'SEMANAL',   montoBase: 110000, saldoDevengado: 55000,  saldoSobrante: 0, ultimoPago: '2026-06-01' },
    ];
  }
}
