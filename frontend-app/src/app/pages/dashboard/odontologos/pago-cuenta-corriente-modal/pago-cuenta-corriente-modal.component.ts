import { Component, EventEmitter, Input, Output, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { FinanzasService, MedioPago, PagoCuentaCorrienteResponse } from '../../../../services/finanzas.service';
import { NotificationService } from '../../../../services/notification.service';
import { hoyComoLocalDate } from '../../../../services/date-utils';

/**
 * Modal de "Registrar pago a cuenta corriente" reutilizable: lo usan tanto la lista
 * de Odontólogos como el ranking de morosos en Finanzas y la ficha de historial,
 * para no duplicar la lógica de imputación (deudas más viejas primero) en cada lugar.
 */
@Component({
  selector: 'app-pago-cuenta-corriente-modal',
  standalone: true,
  imports: [FormsModule],
  templateUrl: './pago-cuenta-corriente-modal.component.html',
  styleUrls: ['./pago-cuenta-corriente-modal.component.css'],
})
export class PagoCuentaCorrienteModalComponent {
  @Input({ required: true }) odontologoId!: number;
  @Input({ required: true }) odontologoNombre!: string;
  @Input() saldoActual = 0;
  @Output() cerrar = new EventEmitter<void>();
  @Output() pagado = new EventEmitter<PagoCuentaCorrienteResponse>();

  private finanzas = inject(FinanzasService);
  private notif = inject(NotificationService);

  saving = signal(false);

  form = {
    monto: null as number | null,
    medio: 'TRANSFERENCIA' as MedioPago,
    fecha: hoyComoLocalDate(),
    nota: '',
  };

  get valido(): boolean {
    return this.form.monto != null && this.form.monto > 0;
  }

  confirmar(): void {
    if (!this.valido || this.saving()) return;
    this.saving.set(true);
    this.finanzas.registrarPagoCuentaCorriente(this.odontologoId, {
      monto: this.form.monto!,
      medio: this.form.medio,
      fecha: this.form.fecha,
      nota: this.form.nota.trim() || null,
    }).subscribe({
      next: res => {
        this.saving.set(false);
        this.notif.exito(res.mensaje);
        this.pagado.emit(res);
      },
      error: err => {
        this.saving.set(false);
        this.notif.errorHttp(err, 'No se pudo registrar el pago');
      },
    });
  }

  formatPrecio(n: number): string {
    return new Intl.NumberFormat('es-AR', { style: 'currency', currency: 'ARS', maximumFractionDigits: 0 }).format(n);
  }
}
