import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { StockService, ConfiguracionAlerta } from '../../../services/stock.service';
import { NotificationService } from '../../../services/notification.service';
import { AuthService } from '../../../services/auth';

@Component({
  selector: 'app-configuracion',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './configuracion.html',
  styleUrls: ['./configuracion.css'],
})
export class ConfiguracionComponent implements OnInit {

  private stockService = inject(StockService);
  private notif        = inject(NotificationService);
  private auth         = inject(AuthService);

  config: ConfiguracionAlerta = { id: 1, adminWhatsappPhone: '', alertasActivas: false };
  loading = true;
  saving  = false;

  get isAdmin(): boolean { return this.auth.isAdmin(); }

  ngOnInit(): void {
    this.stockService.obtenerConfigAlerta().subscribe({
      next:  c => { this.config = c; this.loading = false; },
      error: () => { this.loading = false; },
    });
  }

  guardar(): void {
    if (!this.isAdmin) return;
    this.saving = true;
    this.stockService.actualizarConfigAlerta({
      adminWhatsappPhone: this.config.adminWhatsappPhone?.trim() || null,
      alertasActivas: this.config.alertasActivas,
    }).subscribe({
      next: c => {
        this.config  = c;
        this.saving  = false;
        this.notif.exito('Configuración guardada correctamente');
      },
      error: err => {
        this.saving = false;
        this.notif.errorHttp(err, 'No se pudo guardar la configuración');
      },
    });
  }
}
