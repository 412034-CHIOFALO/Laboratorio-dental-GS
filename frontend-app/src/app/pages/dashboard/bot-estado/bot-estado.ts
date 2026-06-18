import { Component, OnInit, OnDestroy, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Subscription, interval, of } from 'rxjs';
import { startWith, switchMap, catchError } from 'rxjs/operators';
import { BotEstadoService, EstadoBot } from '../../../services/bot-estado.service';

/**
 * Pantalla "Estado del bot": muestra en vivo si el bot de WhatsApp está
 * vinculado. Si se desvinculó, el bot regenera el QR y acá aparece para volver
 * a escanearlo. Hace polling cada pocos segundos contra `/api/bot/estado`.
 */
@Component({
  selector: 'app-bot-estado',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './bot-estado.html',
  styleUrl: './bot-estado.css',
})
export class BotEstadoComponent implements OnInit, OnDestroy {
  private service = inject(BotEstadoService);

  estado = signal<EstadoBot | null>(null);
  cargando = signal(true);
  /** El gateway no pudo contactar al bot (no está corriendo / no responde). */
  sinConexion = signal(false);
  ultimoChequeo = signal<Date | null>(null);

  private sub?: Subscription;
  private readonly INTERVALO_MS = 3000;

  ngOnInit(): void {
    this.sub = interval(this.INTERVALO_MS).pipe(
      startWith(0),
      switchMap(() => this.service.estado().pipe(catchError(() => of(null)))),
    ).subscribe((e) => {
      this.cargando.set(false);
      this.ultimoChequeo.set(new Date());
      if (e) { this.estado.set(e); this.sinConexion.set(false); }
      else { this.sinConexion.set(true); }
    });
  }

  ngOnDestroy(): void { this.sub?.unsubscribe(); }

  /** Chequeo manual inmediato (no espera al próximo tick). */
  refrescar(): void {
    this.cargando.set(true);
    this.service.estado().subscribe({
      next: (e) => { this.estado.set(e); this.sinConexion.set(false); this.cargando.set(false); this.ultimoChequeo.set(new Date()); },
      error: () => { this.sinConexion.set(true); this.cargando.set(false); this.ultimoChequeo.set(new Date()); },
    });
  }

  // ── Helpers para el template ──────────────────────────────────────
  estadoLabel(): string {
    if (this.sinConexion()) return 'Sin conexión con el bot';
    const e = this.estado();
    if (!e) return 'Consultando…';
    return e.conectado ? 'Conectado' : 'Desvinculado';
  }

  claseEstado(): 'ok' | 'warn' | 'err' {
    if (this.sinConexion()) return 'err';
    const e = this.estado();
    if (!e) return 'warn';
    return e.conectado ? 'ok' : 'warn';
  }

  motivo(): string {
    if (this.sinConexion()) return 'No se pudo contactar al bot. Verificá que esté corriendo.';
    return this.estado()?.motivo ?? '';
  }

  /** Mostrar el bloque del QR sólo cuando está desvinculado y hay un QR para escanear. */
  mostrarQr(): boolean {
    const e = this.estado();
    return !this.sinConexion() && !!e && !e.conectado && !!e.qrDataUrl;
  }
}
