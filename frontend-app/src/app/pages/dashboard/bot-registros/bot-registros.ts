import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import {
  SueldosService, RegistroBot, EstadoRegistroBot, TipoReceptorBot,
} from '../../../services/sueldos.service';
import { NotificationService } from '../../../services/notification.service';

/**
 * Historial del bot de WhatsApp: tabla con TODO lo que el bot procesó de cada
 * comprobante (sueldos, pagos a proveedor y también los rechazos/duplicados),
 * para tener visibilidad de cómo interpreta cada uno.
 */
@Component({
  selector: 'app-bot-registros',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './bot-registros.html',
  styleUrl: './bot-registros.css',
})
export class BotRegistrosComponent implements OnInit {
  private sueldos = inject(SueldosService);
  private notif = inject(NotificationService);

  registros = signal<RegistroBot[]>([]);
  cargando = signal(false);
  filtroEstado = signal<'TODOS' | EstadoRegistroBot>('TODOS');

  ngOnInit(): void { this.cargar(); }

  cargar(): void {
    this.cargando.set(true);
    this.sueldos.registrosBot().subscribe({
      next: (r) => { this.registros.set(r); this.cargando.set(false); },
      error: (e) => { this.notif.errorHttp(e, 'No se pudo cargar el historial del bot'); this.cargando.set(false); },
    });
  }

  setFiltro(f: 'TODOS' | EstadoRegistroBot): void { this.filtroEstado.set(f); }

  filtrados(): RegistroBot[] {
    const f = this.filtroEstado();
    return f === 'TODOS' ? this.registros() : this.registros().filter(r => r.estado === f);
  }

  contar(estado: EstadoRegistroBot): number {
    return this.registros().filter(r => r.estado === estado).length;
  }

  verComprobante(r: RegistroBot): void {
    if (!r.tieneComprobante) return;
    this.sueldos.urlComprobanteRegistro(r.id).subscribe({
      next: ({ url }) => window.open(url, '_blank'),
      error: (e) => this.notif.errorHttp(e, 'No se pudo abrir el comprobante'),
    });
  }

  claseEstado(e: EstadoRegistroBot): string {
    return ({ REGISTRADO: 'badge-ok', RECHAZADO: 'badge-err', DUPLICADO: 'badge-dup' } as const)[e] ?? '';
  }
  claseTipo(t: TipoReceptorBot | null): string {
    return ({ EMPLEADO: 'tipo-emp', PROVEEDOR: 'tipo-prov', DESCONOCIDO: 'tipo-desc' } as const)[t ?? 'DESCONOCIDO'] ?? '';
  }
  labelTipo(t: TipoReceptorBot | null): string {
    return ({ EMPLEADO: 'Sueldo', PROVEEDOR: 'Proveedor', DESCONOCIDO: '—' } as const)[t ?? 'DESCONOCIDO'] ?? '—';
  }
}
