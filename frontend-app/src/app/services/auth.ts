import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Observable, of, throwError } from 'rxjs';
import { delay } from 'rxjs/operators';
import { environment } from '../../environments/environment';
import { FAKE_JWT } from './mock-data';

export interface RegisterPayload {
  nombre: string;
  apellido: string;
  username: string;
  password: string;
  rol: 'TECNICO' | 'ADMINISTRATIVO' | 'ODONTOLOGO' | 'ADMIN';
}

interface JwtPayload {
  sub: string;
  roles: string;
  exp: number;
}

@Injectable({
  providedIn: 'root'
})
export class AuthService {
  private gatewayUrl = environment.gatewayUrl || 'http://localhost:8080';
  private readonly TOKEN_KEY = 'gs_token';

  constructor(private http: HttpClient) {}

  login(username: string, password: string): Observable<{ access_token: string }> {
    // 🎬 MODO DEMO: cualquier user/pass válido entra
    if (environment.useMocks) {
      if (username && password) {
        return of({ access_token: FAKE_JWT }).pipe(delay(400));
      }
      return throwError(() => ({ status: 401, error: { error: 'Credenciales incorrectas' } }));
    }

    // El endpoint de login viene de environment.loginUrl: en dev va por el
    // gateway (:8080) y en prod va relativo (vía nginx). Siempre /api/auth/login.
    return this.http.post<{ access_token: string }>(
      environment.loginUrl,
      { username, password }
    );
  }

  register(payload: RegisterPayload): Observable<{ mensaje: string; username: string }> {
    if (environment.useMocks) {
      return of({ mensaje: 'Usuario creado (demo)', username: payload.username }).pipe(delay(300));
    }
    return this.http.post<{ mensaje: string; username: string }>(
      `${this.gatewayUrl}/api/auth/register`,
      payload,
      { headers: this.authHeaders() }
    );
  }

  aprobarUsuario(id: number): Observable<any> {
    if (environment.useMocks) {
      return of({ ok: true }).pipe(delay(200));
    }
    return this.http.put(
      `${this.gatewayUrl}/api/auth/usuarios/${id}/aprobar`,
      {},
      { headers: this.authHeaders() }
    );
  }

  /** Activa/desactiva un integrante (entrada/salida de personal). */
  cambiarEstadoUsuario(id: number, activo: boolean): Observable<any> {
    if (environment.useMocks) {
      return of({ id, enabled: activo }).pipe(delay(200));
    }
    return this.http.patch(
      `${this.gatewayUrl}/api/auth/usuarios/${id}/estado`,
      { activo },
      { headers: this.authHeaders() }
    );
  }

  /** Actualiza el teléfono del integrante (lo usa el bot para identificarlo). */
  actualizarTelefonoUsuario(id: number, telefono: string): Observable<any> {
    if (environment.useMocks) {
      return of({ id, telefono }).pipe(delay(200));
    }
    return this.http.patch(
      `${this.gatewayUrl}/api/auth/usuarios/${id}/telefono`,
      { telefono },
      { headers: this.authHeaders() }
    );
  }

  saveToken(token: string): void {
    localStorage.setItem(this.TOKEN_KEY, token);
  }

  getToken(): string | null {
    return localStorage.getItem(this.TOKEN_KEY);
  }

  isLoggedIn(): boolean {
    const token = this.getToken();
    if (!token) return false;
    try {
      const payload = this.decodePayload(token);
      return payload.exp * 1000 > Date.now();
    } catch {
      return false;
    }
  }

  getUsername(): string {
    const token = this.getToken();
    if (!token) return '';
    try {
      return this.decodePayload(token).sub;
    } catch {
      return '';
    }
  }

  getRoles(): string[] {
    const token = this.getToken();
    if (!token) return [];
    try {
      const roles = this.decodePayload(token).roles;
      return roles ? roles.split(',') : [];
    } catch {
      return [];
    }
  }

  isAdmin(): boolean {
    return this.getRoles().includes('ROLE_ADMIN');
  }

  logout(): void {
    localStorage.removeItem(this.TOKEN_KEY);
  }

  private decodePayload(token: string): JwtPayload {
    const base64 = token.split('.')[1].replace(/-/g, '+').replace(/_/g, '/');
    return JSON.parse(atob(base64));
  }

  private authHeaders(): HttpHeaders {
    return new HttpHeaders({ Authorization: `Bearer ${this.getToken()}` });
  }
}
