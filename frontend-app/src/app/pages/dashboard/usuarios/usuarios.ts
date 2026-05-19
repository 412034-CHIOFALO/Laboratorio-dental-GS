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
  private gatewayUrl = 'http://localhost:8080';

  usuarios: MockUsuario[] = [];
  loading = false;
  error = '';

  // Modal crear usuario
  showModal = false;
  saving = false;
  saveError = '';
  saveSuccess = '';

  // 🎬 Estado mock en memoria
  private mockStore: MockUsuario[] = clonar(MOCK_USUARIOS);
  private nextMockId = 100;

  form = {
    nombre: '',
    apellido: '',
    username: '',
    password: '',
    rol: '' as 'TECNICO' | 'ADMINISTRATIVO' | 'ODONTOLOGO' | 'ADMIN' | ''
  };

  constructor(private http: HttpClient, private authService: AuthService) {}

  ngOnInit() {
    this.cargarUsuarios();
  }

  private headers(): HttpHeaders {
    return new HttpHeaders({ Authorization: `Bearer ${this.authService.getToken()}` });
  }

  cargarUsuarios() {
    this.loading = true;
    this.error = '';

    if (environment.useMocks) {
      setTimeout(() => {
        this.usuarios = clonar(this.mockStore);
        this.loading = false;
      }, 200);
      return;
    }

    this.http.get<MockUsuario[]>(`${this.gatewayUrl}/ms-auth/api/auth/usuarios`, { headers: this.headers() })
      .subscribe({
        next: (data) => { this.usuarios = data; this.loading = false; },
        error: () => { this.error = 'No se pudo cargar la lista de usuarios.'; this.loading = false; }
      });
  }

  abrirModal() {
    this.form = { nombre: '', apellido: '', username: '', password: '', rol: '' };
    this.saveError = '';
    this.saveSuccess = '';
    this.showModal = true;
  }

  cerrarModal() {
    this.showModal = false;
  }

  crearUsuario() {
    if (!this.form.rol) { this.saveError = 'Seleccioná un rol.'; return; }
    this.saving = true;
    this.saveError = '';

    if (environment.useMocks) {
      setTimeout(() => {
        if (this.mockStore.some(u => u.username === this.form.username)) {
          this.saving = false;
          this.saveError = 'El nombre de usuario ya está en uso.';
          return;
        }
        this.mockStore.push({
          id: this.nextMockId++,
          username: this.form.username,
          nombre: this.form.nombre,
          apellido: this.form.apellido,
          rol: this.form.rol as string,
          enabled: false
        });
        this.saving = false;
        this.saveSuccess = 'Usuario creado correctamente.';
        setTimeout(() => { this.cerrarModal(); this.cargarUsuarios(); }, 1000);
      }, 300);
      return;
    }

    this.http.post(`${this.gatewayUrl}/ms-auth/api/auth/register`, this.form, { headers: this.headers() })
      .subscribe({
        next: () => {
          this.saving = false;
          this.saveSuccess = 'Usuario creado correctamente.';
          setTimeout(() => { this.cerrarModal(); this.cargarUsuarios(); }, 1200);
        },
        error: (err) => {
          this.saving = false;
          this.saveError = err.error?.error ?? 'Error al crear el usuario.';
        }
      });
  }

  activar(id: number) {
    if (environment.useMocks) {
      const u = this.mockStore.find(x => x.id === id);
      if (u) u.enabled = true;
      this.cargarUsuarios();
      return;
    }
    this.http.put(`${this.gatewayUrl}/ms-auth/api/auth/usuarios/${id}/aprobar`, {}, { headers: this.headers() })
      .subscribe({ next: () => this.cargarUsuarios(), error: () => {} });
  }

  rolLabel(rol: string): string {
    const map: Record<string, string> = {
      ADMIN: 'Administrador', TECNICO: 'Técnico',
      ADMINISTRATIVO: 'Administrativo', ODONTOLOGO: 'Odontólogo'
    };
    return map[rol] ?? rol;
  }
}
