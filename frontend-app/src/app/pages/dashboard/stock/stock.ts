import { Component } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { clonar, MOCK_MATERIALES, MockMaterial } from '../../../services/mock-data';

@Component({
  selector: 'app-stock',
  standalone: true,
  imports: [FormsModule],
  templateUrl: './stock.html',
  styleUrls: ['./stock.css'],
})
export class StockComponent {
  materiales: MockMaterial[];
  filtrados: MockMaterial[];
  busqueda = '';
  soloAlertas = false;

  constructor() {
    this.materiales = clonar(MOCK_MATERIALES);
    this.filtrados  = this.materiales;
  }

  filtrar(): void {
    const q = this.busqueda.toLowerCase();
    this.filtrados = this.materiales.filter(m => {
      const matchText   = !q || m.nombre.toLowerCase().includes(q) || m.proveedor.toLowerCase().includes(q);
      const matchAlerta = !this.soloAlertas || this.esCritico(m);
      return matchText && matchAlerta;
    });
  }

  esCritico(m: MockMaterial): boolean { return m.stockActual <= m.stockMinimo; }
  esBajo(m: MockMaterial): boolean    { return m.stockActual <= m.stockMinimo * 1.3 && !this.esCritico(m); }

  pct(m: MockMaterial): number {
    return Math.min(100, Math.round((m.stockActual / (m.stockMinimo * 2)) * 100));
  }

  colorPct(m: MockMaterial): string {
    if (this.esCritico(m)) return '#f43f5e';
    if (this.esBajo(m))    return '#f59e0b';
    return '#22c55e';
  }

  get alertCount(): number { return this.materiales.filter(m => this.esCritico(m)).length; }

  formatFecha(iso: string): string {
    return new Date(iso).toLocaleDateString('es-AR', { day: '2-digit', month: '2-digit' });
  }

  labelMov(tipo: MockMaterial['tipoUltimoMov']): string {
    return { ENTRADA: '↑ Entrada', SALIDA: '↓ Salida', AJUSTE: '⇆ Ajuste' }[tipo];
  }
}
