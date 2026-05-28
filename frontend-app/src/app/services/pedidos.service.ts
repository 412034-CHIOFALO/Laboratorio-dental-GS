import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable, of } from 'rxjs';
import { delay } from 'rxjs/operators';
import { environment } from '../../environments/environment';
import { MOCK_PEDIDOS_BACKEND, clonar } from './mock-data';

export type EstadoPedido = 'RECIBIDO' | 'EN_PROCESO' | 'CONTROL' | 'LISTO' | 'ENTREGADO' | 'CANCELADO';
export type Prioridad = 'NORMAL' | 'URGENTE';

export interface PedidoResponse {
  id: number;
  nroPedido: string;
  odontologoId: number;
  odontologoNombre: string;
  paciente: string;
  catalogoTrabajoId: number | null;
  trabajo: string;
  tecnicoId: number | null;
  tecnicoNombre: string | null;
  fechaEntrega: string;
  estado: EstadoPedido;
  prioridad: Prioridad;
  precioAcordado: number | null;
  observaciones: string | null;
  fechaCreacion: string;
  fechaUltimaModificacion: string;
}

export interface PedidoRequest {
  /** Si viene, se usa directo; si no, find-or-create por nombre. */
  odontologoId?: number | null;
  odontologoNombre: string;
  paciente: string;
  catalogoTrabajoId?: number | null;
  trabajo: string;
  tecnicoId?: number | null;
  tecnicoNombre?: string | null;
  fechaEntrega: string;
  prioridad: Prioridad;
  precioAcordado?: number | null;
  observaciones?: string | null;
}

@Injectable({ providedIn: 'root' })
export class PedidosService {

  private readonly base = `${environment.gatewayUrl}/api/pedidos`;

  // Mock store en memoria
  private mockStore: PedidoResponse[] = clonar(MOCK_PEDIDOS_BACKEND);
  private nextMockId = 1000;
  private nextMockNro = 100;

  constructor(private http: HttpClient) {}

  listarTodos(): Observable<PedidoResponse[]> {
    if (environment.useMocks) {
      return of(clonar(this.mockStore)).pipe(delay(200));
    }
    return this.http.get<PedidoResponse[]>(this.base);
  }

  listarActivos(): Observable<PedidoResponse[]> {
    if (environment.useMocks) {
      const activos = this.mockStore.filter(p =>
        p.estado !== 'ENTREGADO' && p.estado !== 'CANCELADO'
      );
      return of(clonar(activos)).pipe(delay(200));
    }
    return this.http.get<PedidoResponse[]>(`${this.base}/activos`);
  }

  listarPorEstado(estado: EstadoPedido): Observable<PedidoResponse[]> {
    if (environment.useMocks) {
      const filtrados = this.mockStore.filter(p => p.estado === estado);
      return of(clonar(filtrados)).pipe(delay(180));
    }
    return this.http.get<PedidoResponse[]>(`${this.base}/estado/${estado}`);
  }

  buscarPorId(id: number): Observable<PedidoResponse> {
    if (environment.useMocks) {
      const p = this.mockStore.find(x => x.id === id);
      if (!p) throw new Error('Pedido no encontrado');
      return of(clonar(p)).pipe(delay(120));
    }
    return this.http.get<PedidoResponse>(`${this.base}/${id}`);
  }

  crear(request: PedidoRequest): Observable<PedidoResponse> {
    if (environment.useMocks) {
      const ahora = new Date().toISOString();
      const anio = new Date().getFullYear();
      const nuevo: PedidoResponse = {
        id: this.nextMockId++,
        nroPedido: `PED-${anio}-${String(this.nextMockNro++).padStart(4, '0')}`,
        odontologoId: request.odontologoId ?? 0,
        odontologoNombre: request.odontologoNombre,
        paciente: request.paciente,
        catalogoTrabajoId: request.catalogoTrabajoId ?? null,
        trabajo: request.trabajo,
        tecnicoId: request.tecnicoId ?? null,
        tecnicoNombre: request.tecnicoNombre ?? null,
        fechaEntrega: request.fechaEntrega,
        estado: 'RECIBIDO',
        prioridad: request.prioridad,
        precioAcordado: request.precioAcordado ?? null,
        observaciones: request.observaciones ?? null,
        fechaCreacion: ahora,
        fechaUltimaModificacion: ahora,
      };
      this.mockStore.unshift(nuevo);
      return of(clonar(nuevo)).pipe(delay(300));
    }
    return this.http.post<PedidoResponse>(this.base, request);
  }

  actualizar(id: number, request: PedidoRequest): Observable<PedidoResponse> {
    if (environment.useMocks) {
      const idx = this.mockStore.findIndex(p => p.id === id);
      if (idx === -1) throw new Error('Pedido no encontrado');
      this.mockStore[idx] = {
        ...this.mockStore[idx],
        ...request,
        odontologoId: request.odontologoId ?? this.mockStore[idx].odontologoId,
        catalogoTrabajoId: request.catalogoTrabajoId ?? null,
        tecnicoId: request.tecnicoId ?? null,
        tecnicoNombre: request.tecnicoNombre ?? null,
        precioAcordado: request.precioAcordado ?? null,
        observaciones: request.observaciones ?? null,
        fechaUltimaModificacion: new Date().toISOString(),
      };
      return of(clonar(this.mockStore[idx])).pipe(delay(250));
    }
    return this.http.put<PedidoResponse>(`${this.base}/${id}`, request);
  }

  actualizarEstado(id: number, nuevoEstado: EstadoPedido): Observable<PedidoResponse> {
    if (environment.useMocks) {
      const idx = this.mockStore.findIndex(p => p.id === id);
      if (idx === -1) throw new Error('Pedido no encontrado');
      this.mockStore[idx].estado = nuevoEstado;
      this.mockStore[idx].fechaUltimaModificacion = new Date().toISOString();
      return of(clonar(this.mockStore[idx])).pipe(delay(180));
    }
    const params = new HttpParams().set('nuevoEstado', nuevoEstado);
    return this.http.patch<PedidoResponse>(`${this.base}/${id}/estado`, null, { params });
  }

  eliminar(id: number): Observable<void> {
    if (environment.useMocks) {
      this.mockStore = this.mockStore.filter(p => p.id !== id);
      return of(void 0).pipe(delay(150));
    }
    return this.http.delete<void>(`${this.base}/${id}`);
  }
}
