import { Component } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from '../../services/auth';
import { FormsModule } from '@angular/forms';
import { HttpErrorResponse } from '@angular/common/http';

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
  errorMessage = '';
  loginLoading = false;

  constructor(private authService: AuthService, private router: Router) {}

  onSubmit() {
    this.errorMessage = '';
    this.loginLoading = true;

    this.authService.login(this.username, this.password).subscribe({
      next: (response) => {
        this.authService.saveToken(response.access_token);
        this.router.navigate(['/dashboard']);
      },
      error: (err: HttpErrorResponse) => {
        this.loginLoading = false;
        this.errorMessage = err.error?.error ?? 'Error al iniciar sesión. Intentá de nuevo.';
      }
    });
  }
}
