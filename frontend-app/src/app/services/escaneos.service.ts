import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, of } from 'rxjs';
import { delay } from 'rxjs/operators';
import { environment } from '../../environments/environment';

export interface EscaneoResponse {
  id: number;
  pedidoId: number;
  fileName: string;
  contentType: string | null;
  tamanioBytes: number | null;
  descripcion: string | null;
  subidoPor: string | null;
  fechaSubida: string;
  urlTemporal: string | null;
}

@Injectable({ providedIn: 'root' })
export class EscaneosService {

  private base(pedidoId: number): string {
    return `${environment.gatewayUrl}/api/pedidos/${pedidoId}/escaneos`;
  }

  constructor(private http: HttpClient) {}

  listar(pedidoId: number): Observable<EscaneoResponse[]> {
    if (environment.useMocks) {
      // Escaneo de muestra para poder demostrar el visor 3D sin backend.
      const muestra: EscaneoResponse = {
        id: 9000 + pedidoId,
        pedidoId,
        fileName: 'modelo-muestra.stl',
        contentType: 'model/stl',
        tamanioBytes: 2048,
        descripcion: 'Modelo de demostración',
        subidoPor: 'demo',
        fechaSubida: new Date().toISOString(),
        urlTemporal: '/sample-escaneo.stl',
      };
      return of([muestra]).pipe(delay(200));
    }
    return this.http.get<EscaneoResponse[]>(this.base(pedidoId));
  }

  /** Devuelve una URL temporal (prefirmada) para ver/descargar el escaneo. */
  url(pedidoId: number, escaneoId: number): Observable<{ url: string; fileName?: string }> {
    if (environment.useMocks) {
      return of({ url: '/sample-escaneo.stl', fileName: 'modelo-muestra.stl' }).pipe(delay(150));
    }
    return this.http.get<{ url: string; fileName?: string }>(`${this.base(pedidoId)}/${escaneoId}/url`);
  }

  subir(pedidoId: number, file: File, descripcion?: string): Observable<EscaneoResponse> {
    if (environment.useMocks) {
      const mock: EscaneoResponse = {
        id: Date.now(), pedidoId,
        fileName: file.name,
        contentType: file.type || 'model/stl',
        tamanioBytes: file.size,
        descripcion: descripcion ?? null,
        subidoPor: 'demo',
        fechaSubida: new Date().toISOString(),
        urlTemporal: null,
      };
      return of(mock).pipe(delay(700));
    }
    const fd = new FormData();
    fd.append('file', file);
    if (descripcion) fd.append('descripcion', descripcion);
    return this.http.post<EscaneoResponse>(this.base(pedidoId), fd);
  }

  eliminar(pedidoId: number, escaneoId: number): Observable<void> {
    if (environment.useMocks) return of(undefined).pipe(delay(300));
    return this.http.delete<void>(`${this.base(pedidoId)}/${escaneoId}`);
  }
}
