import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable, of } from 'rxjs';
import { delay } from 'rxjs/operators';
import { environment } from '../../environments/environment';
import { MOCK_RESUMEN_CAJAS, MOCK_MOVIMIENTOS_CAJA, clonar } from './mock-data';

export type TipoCaja = 'FISICA' | 'BANCARIA' | 'COMPENSACION';
export type TipoMovimientoCaja = 'INGRESO' | 'EGRESO';

export interface ResumenCajasResponse {
  saldoFisica: number;
  saldoBancaria: number;
  saldoCompensacion: number;
  totalDeudaProveedores: number;
  totalSueldosPendientes: number;
  alertas: string[];
}

export interface CajaMovimientoResponse {
  id: number;
  tipo: TipoMovimientoCaja;
  tipoCaja: TipoCaja;
  concepto: string;
  monto: number;
  referencia: string | null;
  fechaMovimiento: string;
  creadoPor: string | null;
}

/** Categorías rápidas para clasificar un movimiento manual. */
export type CategoriaMovimiento =
  | 'COBRO_ODONTOLOGO' | 'EFECTIVO_CADETE' | 'PAGO_PROVEEDOR' | 'PAGO_SUELDO'
  | 'GASTO_MENOR' | 'AJUSTE' | 'OTRO';

export interface CajaMovimientoRequest {
  tipo: TipoMovimientoCaja;
  tipoCaja: TipoCaja;
  concepto: string;
  monto: number;
  categoria: CategoriaMovimiento;
  referencia?: string | null;
  fechaMovimiento?: string | null;   // default: hoy
  creadoPor?: string | null;
}

export type SeveridadDeuda = 'AL_DIA' | 'BAJA' | 'MEDIA' | 'ALTA' | 'CRITICA';

export interface CuentaCorrienteOdontologoResponse {
  odontologoId: number;
  odontologoNombre: string;
  totalDeuda: number;
  comprobantesPendientes: number;
  fechaMasAntigua: string | null;
  diasSinPagar: number;
  severidad: SeveridadDeuda;
}

@Injectable({ providedIn: 'root' })
export class FinanzasService {

  private readonly base = `${environment.gatewayUrl}/api/finanzas`;

  /** Store mutable en memoria para mocks (permite agregar movimientos a mano). */
  private movStore: CajaMovimientoResponse[] = clonar(MOCK_MOVIMIENTOS_CAJA);
  private nextMovId = 9000;

  constructor(private http: HttpClient) {}

  obtenerResumen(): Observable<ResumenCajasResponse> {
    if (environment.useMocks) {
      return of(clonar(MOCK_RESUMEN_CAJAS)).pipe(delay(200));
    }
    return this.http.get<ResumenCajasResponse>(`${this.base}/cajas/resumen`);
  }

  movimientosPorCaja(tipoCaja: TipoCaja): Observable<CajaMovimientoResponse[]> {
    if (environment.useMocks) {
      const filtrados = this.movStore
        .filter(m => m.tipoCaja === tipoCaja)
        .sort((a, b) => new Date(b.fechaMovimiento).getTime() - new Date(a.fechaMovimiento).getTime());
      return of(clonar(filtrados)).pipe(delay(180));
    }
    return this.http.get<CajaMovimientoResponse[]>(`${this.base}/cajas/${tipoCaja}/movimientos`);
  }

  /** Carga manual de un movimiento en una caja. */
  registrarMovimientoCaja(req: CajaMovimientoRequest): Observable<CajaMovimientoResponse> {
    if (environment.useMocks) {
      const nuevo: CajaMovimientoResponse = {
        id: this.nextMovId++,
        tipo: req.tipo,
        tipoCaja: req.tipoCaja,
        concepto: req.concepto,
        monto: req.monto,
        referencia: req.referencia ?? null,
        fechaMovimiento: req.fechaMovimiento ?? new Date().toISOString().slice(0, 10),
        creadoPor: req.creadoPor ?? 'admin',
      };
      this.movStore.unshift(nuevo);
      return of(clonar(nuevo)).pipe(delay(200));
    }
    return this.http.post<CajaMovimientoResponse>(`${this.base}/cajas/movimiento`, req);
  }

  /**
   * Ranking de odontólogos con deuda pendiente. Ordenado de mayor deuda a menor.
   * Mocks: se generan desde el listado de pedidos sin pagar.
   */
  rankingMorosos(): Observable<CuentaCorrienteOdontologoResponse[]> {
    if (environment.useMocks) {
      return of(this.generarRankingMock()).pipe(delay(220));
    }
    return this.http.get<CuentaCorrienteOdontologoResponse[]>(`${this.base}/cuentas-corrientes`);
  }

  /** Genera datos demo para el ranking (solo en modo mocks). */
  private generarRankingMock(): CuentaCorrienteOdontologoResponse[] {
    const hoy = new Date();
    const candidatos: CuentaCorrienteOdontologoResponse[] = [
      { odontologoId: 1, odontologoNombre: 'Dr. Martín García',  totalDeuda: 287500, comprobantesPendientes: 5, fechaMasAntigua: this.diasAtras(78), diasSinPagar: 78, severidad: 'ALTA' },
      { odontologoId: 2, odontologoNombre: 'Dra. Laura Sánchez', totalDeuda: 195000, comprobantesPendientes: 3, fechaMasAntigua: this.diasAtras(42), diasSinPagar: 42, severidad: 'MEDIA' },
      { odontologoId: 3, odontologoNombre: 'Dr. José Pérez',     totalDeuda: 580000, comprobantesPendientes: 9, fechaMasAntigua: this.diasAtras(112), diasSinPagar: 112, severidad: 'CRITICA' },
      { odontologoId: 4, odontologoNombre: 'Dra. Romina Castro', totalDeuda: 45000,  comprobantesPendientes: 1, fechaMasAntigua: this.diasAtras(8),  diasSinPagar: 8,  severidad: 'BAJA' },
      { odontologoId: 5, odontologoNombre: 'Dr. Pablo Gómez',    totalDeuda: 132000, comprobantesPendientes: 4, fechaMasAntigua: this.diasAtras(35), diasSinPagar: 35, severidad: 'MEDIA' },
      { odontologoId: 6, odontologoNombre: 'Dra. Sofía Romero',  totalDeuda: 78500,  comprobantesPendientes: 2, fechaMasAntigua: this.diasAtras(15), diasSinPagar: 15, severidad: 'MEDIA' },
      { odontologoId: 7, odontologoNombre: 'Dr. Lucas Torres',   totalDeuda: 28000,  comprobantesPendientes: 1, fechaMasAntigua: this.diasAtras(4),  diasSinPagar: 4,  severidad: 'BAJA' },
      { odontologoId: 8, odontologoNombre: 'Dra. Valeria Núñez', totalDeuda: 412000, comprobantesPendientes: 7, fechaMasAntigua: this.diasAtras(95), diasSinPagar: 95, severidad: 'CRITICA' },
    ];
    return candidatos.sort((a, b) => b.totalDeuda - a.totalDeuda);
  }

  private diasAtras(n: number): string {
    const d = new Date();
    d.setDate(d.getDate() - n);
    return d.toISOString().slice(0, 10);
  }

  /** Saldo pendiente (deuda) de un odontólogo puntual. */
  saldoPorOdontologo(odontologoId: number): Observable<number> {
    if (environment.useMocks) {
      const c = this.generarRankingMock().find(x => x.odontologoId === odontologoId);
      return of(c?.totalDeuda ?? 0).pipe(delay(100));
    }
    return this.http.get<number>(`${this.base}/saldo/odontologo/${odontologoId}`);
  }

  movimientosPorPeriodo(desde: string, hasta: string): Observable<CajaMovimientoResponse[]> {
    if (environment.useMocks) {
      const desdeMs = new Date(desde).getTime();
      const hastaMs = new Date(hasta).getTime();
      const filtrados = MOCK_MOVIMIENTOS_CAJA
        .filter(m => {
          const ms = new Date(m.fechaMovimiento).getTime();
          return ms >= desdeMs && ms <= hastaMs;
        })
        .sort((a, b) => new Date(b.fechaMovimiento).getTime() - new Date(a.fechaMovimiento).getTime());
      return of(clonar(filtrados)).pipe(delay(200));
    }
    const params = new HttpParams().set('desde', desde).set('hasta', hasta);
    return this.http.get<CajaMovimientoResponse[]>(`${this.base}/cajas/movimientos`, { params });
  }
}
