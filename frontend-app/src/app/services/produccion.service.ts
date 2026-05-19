import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable, of } from 'rxjs';
import { delay } from 'rxjs/operators';
import { environment } from '../../environments/environment';
import { MOCK_KANBAN, clonar } from './mock-data';

export interface TareaResponse {
  id: number;
  nroPedido: string;
  paciente: string;
  odontologo: string;
  trabajo: string;
  tecnico: string | null;
  estado: 'RECIBIDO' | 'EN_PROCESO' | 'CONTROL' | 'LISTO';
  prioridad: 'NORMAL' | 'URGENTE';
  fechaIngreso: string;
  fechaEntrega: string | null;
  observaciones: string | null;
}

@Injectable({ providedIn: 'root' })
export class ProduccionService {

  private readonly base = `${environment.produccionUrl}/api/produccion`;

  // 🎬 Estado mock persistido en memoria del servicio
  private mockStore: TareaResponse[] = clonar(MOCK_KANBAN);

  constructor(private http: HttpClient) {}

  listarKanban(): Observable<TareaResponse[]> {
    if (environment.useMocks) {
      return of(clonar(this.mockStore)).pipe(delay(200));
    }
    return this.http.get<TareaResponse[]>(`${this.base}/kanban`);
  }

  listarPorEstado(estado: string): Observable<TareaResponse[]> {
    if (environment.useMocks) {
      return of(clonar(this.mockStore.filter(t => t.estado === estado))).pipe(delay(150));
    }
    return this.http.get<TareaResponse[]>(`${this.base}/kanban/${estado}`);
  }

  actualizarEstado(id: number, nuevoEstado: string): Observable<TareaResponse> {
    if (environment.useMocks) {
      const idx = this.mockStore.findIndex(t => t.id === id);
      if (idx !== -1) {
        this.mockStore[idx].estado = nuevoEstado as TareaResponse['estado'];
      }
      return of(clonar(this.mockStore[idx])).pipe(delay(250));
    }
    const params = new HttpParams().set('nuevoEstado', nuevoEstado);
    return this.http.patch<TareaResponse>(`${this.base}/${id}/estado`, null, { params });
  }

  asignarTecnico(id: number, nombre: string): Observable<TareaResponse> {
    if (environment.useMocks) {
      const idx = this.mockStore.findIndex(t => t.id === id);
      if (idx !== -1) {
        this.mockStore[idx].tecnico = nombre;
      }
      return of(clonar(this.mockStore[idx])).pipe(delay(200));
    }
    const params = new HttpParams().set('nombre', nombre);
    return this.http.patch<TareaResponse>(`${this.base}/${id}/tecnico`, null, { params });
  }
}
