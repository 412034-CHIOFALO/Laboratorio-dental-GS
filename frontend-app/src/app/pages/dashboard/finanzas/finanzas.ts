import { Component, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import {
  FinanzasService, ResumenCajasResponse, CajaMovimientoResponse, TipoCaja
} from '../../../services/finanzas.service';

@Component({
  selector: 'app-finanzas',
  standalone: true,
  imports: [FormsModule],
  templateUrl: './finanzas.html',
  styleUrls: ['./finanzas.css'],
})
export class FinanzasComponent implements OnInit {

  resumen: ResumenCajasResponse | null = null;
  loadingResumen = false;
  errorResumen = '';

  cajaActiva: TipoCaja = 'FISICA';
  movimientos: CajaMovimientoResponse[] = [];
  loadingMovimientos = false;

  readonly cajas: { tipo: TipoCaja; label: string; descripcion: string; color: string }[] = [
    { tipo: 'FISICA',       label: 'Caja Física',       descripcion: 'Efectivo en mano',                     color: 'green' },
    { tipo: 'BANCARIA',     label: 'Caja Bancaria',     descripcion: 'Transferencias y movimientos bancarios', color: 'cyan'  },
    { tipo: 'COMPENSACION', label: 'Caja Compensación', descripcion: 'Pagos triangulados (siempre $0 neto)', color: 'purple' },
  ];

  constructor(private finanzasService: FinanzasService) {}

  ngOnInit(): void {
    this.cargarResumen();
    this.cargarMovimientos();
  }

  // ─────────────────────────────────────────────────────────────
  // CARGAS
  // ─────────────────────────────────────────────────────────────

  cargarResumen(): void {
    this.loadingResumen = true;
    this.errorResumen = '';
    this.finanzasService.obtenerResumen().subscribe({
      next: data => { this.resumen = data; this.loadingResumen = false; },
      error: err => {
        this.errorResumen = 'No se pudo cargar el resumen. ¿ms-finanzas está corriendo?';
        this.loadingResumen = false;
        console.error(err);
      },
    });
  }

  cambiarCaja(tipo: TipoCaja): void {
    this.cajaActiva = tipo;
    this.cargarMovimientos();
  }

  cargarMovimientos(): void {
    this.loadingMovimientos = true;
    this.finanzasService.movimientosPorCaja(this.cajaActiva).subscribe({
      next: data => { this.movimientos = data; this.loadingMovimientos = false; },
      error: err => {
        this.loadingMovimientos = false;
        console.error(err);
      },
    });
  }

  // ─────────────────────────────────────────────────────────────
  // STATS POR CAJA SELECCIONADA
  // ─────────────────────────────────────────────────────────────

  get saldoCajaActiva(): number {
    if (!this.resumen) return 0;
    switch (this.cajaActiva) {
      case 'FISICA':       return this.resumen.saldoFisica;
      case 'BANCARIA':     return this.resumen.saldoBancaria;
      case 'COMPENSACION': return this.resumen.saldoCompensacion;
    }
  }

  get ingresosCajaActiva(): number {
    return this.movimientos
      .filter(m => m.tipo === 'INGRESO')
      .reduce((sum, m) => sum + m.monto, 0);
  }

  get egresosCajaActiva(): number {
    return this.movimientos
      .filter(m => m.tipo === 'EGRESO')
      .reduce((sum, m) => sum + m.monto, 0);
  }

  get tieneCompensacionDesbalanceada(): boolean {
    return !!this.resumen && this.resumen.saldoCompensacion !== 0;
  }

  get cajaActivaLabel(): string {
    return this.cajas.find(c => c.tipo === this.cajaActiva)?.label ?? '';
  }

  // ─────────────────────────────────────────────────────────────
  // HELPERS DE VISTA
  // ─────────────────────────────────────────────────────────────

  formatMoney(n: number): string {
    return new Intl.NumberFormat('es-AR', {
      style: 'currency', currency: 'ARS', maximumFractionDigits: 0
    }).format(n);
  }

  formatFecha(iso: string): string {
    return new Date(iso).toLocaleDateString('es-AR',
      { day: '2-digit', month: '2-digit', year: 'numeric' });
  }

  colorSaldo(saldo: number, esCompensacion = false): 'positivo' | 'negativo' | 'neutro' {
    if (esCompensacion) return saldo === 0 ? 'neutro' : 'negativo';
    if (saldo < 0) return 'negativo';
    if (saldo === 0) return 'neutro';
    return 'positivo';
  }
}
