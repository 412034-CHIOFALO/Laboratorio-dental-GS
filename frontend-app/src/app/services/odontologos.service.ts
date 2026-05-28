import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable, of } from 'rxjs';
import { delay } from 'rxjs/operators';
import { environment } from '../../environments/environment';
import { MOCK_ODONTOLOGOS, clonar } from './mock-data';

export interface OdontologoResponse {
  id: number;
  nombre: string;
  telefono: string | null;
  email: string | null;
  matricula: string | null;
  activo: boolean;
  fechaCreacion: string;
  fechaModificacion: string;
}

export interface OdontologoRequest {
  nombre: string;
  telefono?: string | null;
  email?: string | null;
  matricula?: string | null;
}

@Injectable({ providedIn: 'root' })
export class OdontologosService {

  private readonly base = `${environment.gatewayUrl}/api/odontologos`;

  // Mock store en memoria — para crear/buscar/agregar funcione sin backend
  private mockStore: OdontologoResponse[] = clonar(MOCK_ODONTOLOGOS);
  private nextMockId = 100;

  constructor(private http: HttpClient) {}

  /** Lista todos los activos o filtra por fragmento (q=) */
  buscar(q?: string): Observable<OdontologoResponse[]> {
    if (environment.useMocks) {
      let resultados = this.mockStore.filter(o => o.activo);
      if (q && q.trim()) {
        const frag = q.trim().toLowerCase();
        resultados = resultados.filter(o => o.nombre.toLowerCase().includes(frag));
      }
      return of(clonar(resultados)).pipe(delay(150));
    }

    let params = new HttpParams();
    if (q && q.trim()) params = params.set('q', q.trim());
    return this.http.get<OdontologoResponse[]>(this.base, { params });
  }

  buscarPorId(id: number): Observable<OdontologoResponse> {
    if (environment.useMocks) {
      const o = this.mockStore.find(x => x.id === id);
      if (!o) throw new Error('Odontólogo no encontrado');
      return of(clonar(o)).pipe(delay(120));
    }
    return this.http.get<OdontologoResponse>(`${this.base}/${id}`);
  }

  crear(request: OdontologoRequest): Observable<OdontologoResponse> {
    if (environment.useMocks) {
      const ahora = new Date().toISOString();
      const nuevo: OdontologoResponse = {
        id: this.nextMockId++,
        nombre: request.nombre.trim(),
        telefono: request.telefono ?? null,
        email: request.email ?? null,
        matricula: request.matricula ?? null,
        activo: true,
        fechaCreacion: ahora,
        fechaModificacion: ahora,
      };
      this.mockStore.push(nuevo);
      return of(clonar(nuevo)).pipe(delay(250));
    }
    return this.http.post<OdontologoResponse>(this.base, request);
  }

  actualizar(id: number, request: OdontologoRequest): Observable<OdontologoResponse> {
    if (environment.useMocks) {
      const idx = this.mockStore.findIndex(o => o.id === id);
      if (idx === -1) throw new Error('Odontólogo no encontrado');
      this.mockStore[idx] = {
        ...this.mockStore[idx],
        ...request,
        telefono: request.telefono ?? this.mockStore[idx].telefono,
        email: request.email ?? this.mockStore[idx].email,
        matricula: request.matricula ?? this.mockStore[idx].matricula,
        fechaModificacion: new Date().toISOString(),
      };
      return of(clonar(this.mockStore[idx])).pipe(delay(200));
    }
    return this.http.put<OdontologoResponse>(`${this.base}/${id}`, request);
  }
}
