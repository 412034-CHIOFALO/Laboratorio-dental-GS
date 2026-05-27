import { Component } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { clonar, MOCK_PEDIDOS, MockPedido } from '../../../services/mock-data';

type EstadoPedido = MockPedido['estado'] | '';

@Component({
  selector: 'app-pedidos',
  standalone: true,
  imports: [FormsModule],
  templateUrl: './pedidos.html',
  styleUrls: ['./pedidos.css'],
})
export class PedidosComponent {
  pedidos: MockPedido[];
  filtrados: MockPedido[];
  busqueda = '';
  estadoFiltro: EstadoPedido = '';

  readonly estados: { valor: EstadoPedido; label: string }[] = [
    { valor: '',              label: 'Todos'             },
    { valor: 'BORRADOR',      label: 'Borrador (bot)'    },
    { valor: 'RECEPCIONADO',  label: 'Recepcionado'      },
    { valor: 'EN_PRODUCCION', label: 'En producción'     },
    { valor: 'LISTO',         label: 'Listo'             },
    { valor: 'ENTREGADO',     label: 'Entregado'         },
    { valor: 'CANCELADO',     label: 'Cancelado'         },
  ];

  constructor() {
    this.pedidos  = clonar(MOCK_PEDIDOS).sort((a: MockPedido, b: MockPedido) =>
      new Date(b.fechaIngreso).getTime() - new Date(a.fechaIngreso).getTime());
    this.filtrados = this.pedidos;
  }

  filtrar(): void {
    const q = this.busqueda.toLowerCase();
    this.filtrados = this.pedidos.filter(p => {
      const matchEstado = !this.estadoFiltro || p.estado === this.estadoFiltro;
      const matchText   = !q || [p.nroPedido, p.paciente, p.odontologo, p.tipoTrabajo].some(s => s.toLowerCase().includes(q));
      return matchEstado && matchText;
    });
  }

  colorEstado(e: string): string {
    const m: Record<string, string> = {
      BORRADOR: 'purple', RECEPCIONADO: 'blue', EN_PRODUCCION: 'amber',
      LISTO: 'green', ENTREGADO: 'cyan', CANCELADO: 'rose',
    };
    return m[e] ?? 'muted';
  }

  labelEstado(e: string): string {
    const m: Record<string, string> = {
      BORRADOR: 'Borrador', RECEPCIONADO: 'Recepcionado', EN_PRODUCCION: 'En producción',
      LISTO: 'Listo', ENTREGADO: 'Entregado', CANCELADO: 'Cancelado',
    };
    return m[e] ?? e;
  }

  labelCanal(c: string): string {
    return { MANUAL: '🖊 Manual', WHATSAPP: '💬 WhatsApp', EMAIL: '📧 Email' }[c] ?? c;
  }

  formatFecha(iso: string): string {
    return new Date(iso).toLocaleDateString('es-AR', { day: '2-digit', month: '2-digit' });
  }

  formatPrecio(n: number): string {
    return '$' + n.toLocaleString('es-AR');
  }

  get borradores(): number { return this.pedidos.filter(p => p.estado === 'BORRADOR').length; }
}
