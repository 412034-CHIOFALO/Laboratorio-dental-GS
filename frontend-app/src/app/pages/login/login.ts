import { Component, inject } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from '../../services/auth';
import { FormsModule } from '@angular/forms';
import { HttpErrorResponse } from '@angular/common/http';
import { NotificationService } from '../../services/notification.service';
import { environment } from '../../../environments/environment';

@Component({
  selector: 'app-login',
  standalone: true,
  templateUrl: './login.html',
  styleUrls: ['./login.css'],
  imports: [FormsModule, RouterLink]
})
export class LoginComponent {
  username = '';
  password = '';
  mostrarPassword = false;
  errorMessage = '';
  loginLoading = false;

  // Mostrar hint con credenciales de prueba SOLO en dev/mocks
  readonly mostrarDemoHint = !environment.production;

  private notif = inject(NotificationService);

  constructor(private authService: AuthService, private router: Router) {}

  /** Botón del hint demo — autocompleta los campos */
  usarCreds(user: string, pass: string): void {
    this.username = user;
    this.password = pass;
    this.errorMessage = '';
  }

  onSubmit() {
    this.errorMessage = '';
    this.loginLoading = true;

    // Validación rápida client-side antes de pegarle al back
    if (!this.username.trim() || !this.password) {
      this.loginLoading = false;
      this.notif.alerta('Completá usuario y contraseña');
      return;
    }

    this.authService.login(this.username, this.password).subscribe({
      next: (response) => {
        this.authService.saveToken(response.access_token);
        this.notif.exito(`Bienvenido ${this.username}`, 'Sesión iniciada');
        this.router.navigate(['/dashboard']);
      },
      error: (err: HttpErrorResponse) => {
        this.loginLoading = false;
        const msg = err.error?.error ?? err.error?.mensaje ?? 'Usuario o contraseña incorrectos';
        this.errorMessage = msg; // mantener la versión inline para accesibilidad
        // status 0 = sin conexión al back. Mensaje específico, no genérico.
        if (err.status === 0) {
          this.notif.error('No se pudo conectar al servidor. Verificá que el backend esté corriendo.', 'Sin conexión');
        } else if (err.status === 401) {
          this.notif.error(msg, 'Acceso denegado');
        } else if (err.status === 403) {
          this.notif.alerta(msg, 'Cuenta no activada');
        } else {
          this.notif.errorHttp(err, 'No se pudo iniciar sesión');
        }
      }
    });
  }
}
