import { Component, OnInit } from '@angular/core';
import { NgClass } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { AuthService } from '../../../services/auth';
import { environment } from '../../../../environments/environment';
import { MOCK_USUARIOS, MockUsuario, clonar } from '../../../services/mock-data';

@Component({
  selector: 'app-usuarios',
  standalone: true,
  imports: [NgClass, FormsModule],
  templateUrl: './usuarios.html',
  styleUrls: ['./usuarios.css']
})
export class UsuariosComponent implements OnInit {
  private gatewayUrl = environment.gatewayUrl || 'http://localhost:8080';

  usuarios: MockUsuario[] = [];
  loading = false;
  error   = '';

  // ── Modal crear usuario ──────────────────────────────────────
  showModal  = false;
  saving     = false;
  saveError  = '';
  saveSuccess = '';

  form = {
    nombre: '', apellido: '', username: '', password: '',
    rol: '' as 'TECNICO' | 'ADMINISTRATIVO' | 'ODONTOLOGO' | 'ADMIN' | ''
  };

  // ── Modal editar teléfono ────────────────────────────────────
  showTelModal  = false;
  editandoUsuario: MockUsuario | null = null;
  formTel       = { telefono: '' };
  savingTel     = false;
  telError      = '';
  telSuccess    = '';

  // Mock store
  private mockStore: MockUsuario[] = clonar(MOCK_USUARIOS);
  private nextMockId = 100;

  constructor(private http: HttpClient, private authService: AuthService) {}

  ngOnInit() { this.cargarUsuarios(); }

  private headers(): HttpHeaders {
    return new HttpHeaders({ Authorization: `Bearer ${this.authService.getToken()}` });
  }

  cargarUsuarios() {
    this.loading = true;
    this.error   = '';

    if (environment.useMocks) {
      setTimeout(() => { this.usuarios = clonar(this.mockStore); this.loading = false; }, 200);
      return;
    }

    this.http.get<MockUsuario[]>(`${this.gatewayUrl}/api/auth/usuarios`, { headers: this.headers() })
      .subscribe({
        next: (data) => { this.usuarios = data; this.loading = false; },
        error: () => { this.error = 'No se pudo cargar la lista de usuarios.'; this.loading = false; }
      });
  }

  // ── Crear ────────────────────────────────────────────────────

  abrirModal() {
    this.form = { nombre: '', apellido: '', username: '', password: '', rol: '' };
    this.saveError = ''; this.saveSuccess = '';
    this.showModal = true;
  }

  cerrarModal() { this.showModal = false; }

  crearUsuario() {
    if (!this.form.rol) { this.saveError = 'Seleccioná un rol.'; return; }
    this.saving = true; this.saveError = '';

    if (environment.useMocks) {
      setTimeout(() => {
        if (this.mockStore.some(u => u.username === this.form.username)) {
          this.saving = false; this.saveError = 'El nombre de usuario ya está en uso.'; return;
        }
        this.mockStore.push({
          id: this.nextMockId++, username: this.form.username,
          nombre: this.form.nombre, apellido: this.form.apellido,
          rol: this.form.rol as string, enabled: false
        });
        this.saving = false; this.saveSuccess = 'Usuario creado correctamente.';
        setTimeout(() => { this.cerrarModal(); this.cargarUsuarios(); }, 1000);
      }, 300);
      return;
    }

    this.http.post(`${this.gatewayUrl}/api/auth/register`, this.form, { headers: this.headers() })
      .subscribe({
        next: () => {
          this.saving = false; this.saveSuccess = 'Usuario creado correctamente.';
          setTimeout(() => { this.cerrarModal(); this.cargarUsuarios(); }, 1200);
        },
        error: (err) => { this.saving = false; this.saveError = this.mensajeError(err, 'Error al crear el usuario.'); }
      });
  }

  // ── Activar / Desactivar ─────────────────────────────────────

  activar(id: number) {
    this.cambiarEstado(id, true);
  }

  desactivar(id: number) {
    this.cambiarEstado(id, false);
  }

  private cambiarEstado(id: number, activo: boolean) {
    if (environment.useMocks) {
      const u = this.mockStore.find(x => x.id === id);
      if (u) u.enabled = activo;
      this.cargarUsuarios();
      return;
    }
    this.http.patch(`${this.gatewayUrl}/api/auth/usuarios/${id}/estado`,
      { activo }, { headers: this.headers() })
      .subscribe({ next: () => this.cargarUsuarios(), error: () => {} });
  }

  // ── Editar teléfono ──────────────────────────────────────────

  abrirEditarTel(u: MockUsuario) {
    this.editandoUsuario = u;
    this.formTel         = { telefono: u.telefono ?? '' };
    this.telError        = ''; this.telSuccess = '';
    this.showTelModal    = true;
  }

  cerrarTelModal() { this.showTelModal = false; this.editandoUsuario = null; }

  /** Deja solo números y símbolos de teléfono (+ - ( ) espacio). */
  sanitizarTelefono(v: string): string { return (v || '').replace(/[^0-9+()\-\s]/g, '').slice(0, 30); }

  /**
   * Extrae el mejor mensaje de un error HTTP del backend para mostrarlo inline.
   * Prioriza el detalle de validación por campo, luego el mensaje de negocio.
   */
  private mensajeError(err: any, fallback: string): string {
    const body = err?.error ?? {};
    if (Array.isArray(body.campos) && body.campos.length > 0) {
      return body.campos.map((c: any) => c.mensaje).join(' · ');
    }
    return body.mensaje ?? body.error ?? fallback;
  }

  guardarTelefono() {
    if (!this.editandoUsuario) return;
    const tel = this.formTel.telefono?.trim();
    if (tel && !/^[0-9+()\-\s]{6,30}$/.test(tel)) {
      this.telError = 'El teléfono solo puede tener números y los símbolos + - ( ).';
      return;
    }
    this.savingTel = true; this.telError = '';
    const id = this.editandoUsuario.id;

    if (environment.useMocks) {
      setTimeout(() => {
        const u = this.mockStore.find(x => x.id === id);
        if (u) u.telefono = this.formTel.telefono;
        this.savingTel = false; this.telSuccess = 'Teléfono actualizado.';
        setTimeout(() => { this.cerrarTelModal(); this.cargarUsuarios(); }, 1000);
      }, 300);
      return;
    }

    this.http.patch(`${this.gatewayUrl}/api/auth/usuarios/${id}/telefono`,
      { telefono: this.formTel.telefono }, { headers: this.headers() })
      .subscribe({
        next: () => {
          this.savingTel = false; this.telSuccess = 'Teléfono actualizado correctamente.';
          setTimeout(() => { this.cerrarTelModal(); this.cargarUsuarios(); }, 1000);
        },
        error: (err) => { this.savingTel = false; this.telError = this.mensajeError(err, 'Error al actualizar el teléfono.'); }
      });
  }

  // ── Helpers ──────────────────────────────────────────────────

  rolLabel(rol: string): string {
    const map: Record<string, string> = {
      ADMIN: 'Administrador', TECNICO: 'Técnico',
      ADMINISTRATIVO: 'Administrativo', ODONTOLOGO: 'Odontólogo'
    };
    return map[rol] ?? rol;
  }
}
