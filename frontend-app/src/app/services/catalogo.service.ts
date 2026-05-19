import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable, of } from 'rxjs';
import { delay } from 'rxjs/operators';
import { environment } from '../../environments/environment';
import { MOCK_CATALOGO, clonar } from './mock-data';

export type Categoria = 'FIJA' | 'REMOVIBLE' | 'ORTODONCIA' | 'ATM' | 'PERSONALIZADO';

export interface TipoTrabajoResponse {
  id: number;
  nombre: string;
  descripcion: string;
  precio: number;
  categoria: Categoria;
  tiempoEstimadoDias: number;
  fotoUrl: string | null;
  activo: boolean;
  fechaCreacion: string;
  fechaModificacion: string;
}

export interface TipoTrabajoRequest {
  nombre: string;
  descripcion: string;
  precio: number;
  categoria: Categoria;
  tiempoEstimadoDias: number;
  fotoUrl?: string | null;
}

@Injectable({ providedIn: 'root' })
export class CatalogoService {

  private readonly base = `${environment.apiUrl}/api/catalogo`;

  // 🎬 Estado mock en memoria (se mutila para que crear/editar/eliminar funcionen visualmente)
  private mockStore: TipoTrabajoResponse[] = clonar(MOCK_CATALOGO);
  private nextMockId = 11;

  constructor(private http: HttpClient) {}

  listar(categoria?: string, nombre?: string): Observable<TipoTrabajoResponse[]> {
    if (environment.useMocks) {
      let filtrados = this.mockStore.filter(t => t.activo);
      if (categoria && categoria !== 'TODOS') {
        filtrados = filtrados.filter(t => t.categoria === categoria);
      }
      if (nombre && nombre.trim()) {
        const q = nombre.trim().toLowerCase();
        filtrados = filtrados.filter(t => t.nombre.toLowerCase().includes(q));
      }
      return of(clonar(filtrados)).pipe(delay(200));
    }

    let params = new HttpParams();
    if (categoria && categoria !== 'TODOS') params = params.set('categoria', categoria);
    if (nombre && nombre.trim())            params = params.set('nombre', nombre.trim());
    return this.http.get<TipoTrabajoResponse[]>(this.base, { params });
  }

  crear(request: TipoTrabajoRequest): Observable<TipoTrabajoResponse> {
    if (environment.useMocks) {
      const HOY = new Date().toISOString();
      const nuevo: TipoTrabajoResponse = {
        id: this.nextMockId++,
        ...request,
        fotoUrl: request.fotoUrl ?? null,
        activo: true,
        fechaCreacion: HOY,
        fechaModificacion: HOY,
      };
      this.mockStore.push(nuevo);
      return of(clonar(nuevo)).pipe(delay(300));
    }
    return this.http.post<TipoTrabajoResponse>(this.base, request);
  }

  actualizar(id: number, request: TipoTrabajoRequest): Observable<TipoTrabajoResponse> {
    if (environment.useMocks) {
      const idx = this.mockStore.findIndex(t => t.id === id);
      if (idx === -1) throw new Error('No encontrado');
      this.mockStore[idx] = {
        ...this.mockStore[idx],
        ...request,
        fotoUrl: request.fotoUrl ?? this.mockStore[idx].fotoUrl,
        fechaModificacion: new Date().toISOString(),
      };
      return of(clonar(this.mockStore[idx])).pipe(delay(250));
    }
    return this.http.put<TipoTrabajoResponse>(`${this.base}/${id}`, request);
  }

  eliminar(id: number): Observable<void> {
    if (environment.useMocks) {
      const idx = this.mockStore.findIndex(t => t.id === id);
      if (idx !== -1) {
        this.mockStore[idx].activo = false;
      }
      return of(void 0).pipe(delay(200));
    }
    return this.http.delete<void>(`${this.base}/${id}`);
  }
}
