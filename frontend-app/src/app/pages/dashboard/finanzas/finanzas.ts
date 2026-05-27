import { Component } from '@angular/core';
import { clonar, MOCK_CLIENTES_FINANCIERO, MockClienteFinanciero } from '../../../services/mock-data';

@Component({
  selector: 'app-finanzas',
  standalone: true,
  imports: [],
  templateUrl: './finanzas.html',
  styleUrls: ['./finanzas.css'],
})
export class FinanzasComponent {
  clientes: MockClienteFinanciero[];

  readonly cajas = [
    { label: 'Caja Física',       valor:  85_000, color: 'green',  desc: 'Efectivo en mano' },
    { label: 'Caja Bancaria',     valor: 420_000, color: 'cyan',   desc: 'Transferencias del mes' },
    { label: 'Caja Compensación', valor:       0, color: 'amber',  desc: 'Triangulados — debe cerrar en 0' },
  ];

  readonly kpis = [
    { label: 'Deuda total clientes', valor: '$785.000', color: 'rose'  },
    { label: 'Cobrado este mes',      valor: '$980.000', color: 'green' },
    { label: 'Promedio por cliente',  valor: '$157.000', color: 'cyan'  },
    { label: 'Clientes al día',       valor: '1 / 5',    color: 'amber' },
  ];

  constructor() {
    this.clientes = clonar(MOCK_CLIENTES_FINANCIERO)
      .sort((a: MockClienteFinanciero, b: MockClienteFinanciero) => b.deuda - a.deuda);
  }

  formatPeso(n: number): string {
    return n === 0 ? '$0' : '$' + n.toLocaleString('es-AR');
  }

  diasDesde(iso: string): number {
    return Math.floor((Date.now() - new Date(iso).getTime()) / 86_400_000);
  }

  colorDeuda(deuda: number, dias: number): string {
    if (deuda === 0) return 'green';
    if (dias > 30)   return 'rose';
    if (dias > 15)   return 'amber';
    return 'muted';
  }
}
