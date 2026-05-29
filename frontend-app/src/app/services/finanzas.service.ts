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

@Injectable({ providedIn: 'root' })
export class FinanzasService {

  private readonly base = `${environment.gatewayUrl}/api/finanzas`;

  constructor(private http: HttpClient) {}

  obtenerResumen(): Observable<ResumenCajasResponse> {
    if (environment.useMocks) {
      return of(clonar(MOCK_RESUMEN_CAJAS)).pipe(delay(200));
    }
    return this.http.get<ResumenCajasResponse>(`${this.base}/cajas/resumen`);
  }

  movimientosPorCaja(tipoCaja: TipoCaja): Observable<CajaMovimientoResponse[]> {
    if (environment.useMocks) {
      const filtrados = MOCK_MOVIMIENTOS_CAJA
        .filter(m => m.tipoCaja === tipoCaja)
        .sort((a, b) => new Date(b.fechaMovimiento).getTime() - new Date(a.fechaMovimiento).getTime());
      return of(clonar(filtrados)).pipe(delay(180));
    }
    return this.http.get<CajaMovimientoResponse[]>(`${this.base}/cajas/${tipoCaja}/movimientos`);
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
