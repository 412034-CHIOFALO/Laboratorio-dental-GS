import { Component } from '@angular/core';
import { clonar, MOCK_PEDIDOS, MockPedido } from '../../../services/mock-data';

interface EntregaItem {
  nroPedido: string;
  paciente: string;
  odontologo: string;
  direccionOdontologo: string;
  tipoTrabajo: string;
  prioridad: 'NORMAL' | 'URGENTE';
  fechaEntrega: string;
  entregadoEn?: string;
}

@Component({
  selector: 'app-entregas',
  standalone: true,
  imports: [],
  templateUrl: './entregas.html',
  styleUrls: ['./entregas.css'],
})
export class EntregasComponent {
  pendientes: EntregaItem[];
  historial: EntregaItem[];
  mensajeCopiado = false;

  constructor() {
    const pedidos: MockPedido[] = clonar(MOCK_PEDIDOS);
    const toItem = (p: MockPedido): EntregaItem => ({
      nroPedido: p.nroPedido,
      paciente: p.paciente,
      odontologo: p.odontologo,
      direccionOdontologo: p.direccionOdontologo,
      tipoTrabajo: p.tipoTrabajo,
      prioridad: p.prioridad,
      fechaEntrega: p.fechaEntrega,
    });
    this.pendientes = pedidos.filter(p => p.estado === 'LISTO').map(toItem);
    this.historial  = pedidos.filter(p => p.estado === 'ENTREGADO').map(toItem);
  }

  get mensajeWhatsApp(): string {
    const hoy = new Date().toLocaleDateString('es-AR', { day: '2-digit', month: '2-digit', year: 'numeric' });
    if (this.pendientes.length === 0) return `📦 *Entregas G&S — ${hoy}*\n\nNo hay entregas pendientes hoy. ✅`;
    let msg = `📦 *Entregas G&S — ${hoy}*\n\n`;
    this.pendientes.forEach((p, i) => {
      msg += `${i + 1}️⃣ *${p.odontologo}*\n`;
      msg += `   📍 ${p.direccionOdontologo}\n`;
      msg += `   🦷 ${p.tipoTrabajo} — Pac: ${p.paciente}\n`;
      if (p.prioridad === 'URGENTE') msg += `   ⚡ URGENTE\n`;
      msg += '\n';
    });
    msg += `📊 Total: ${this.pendientes.length} entrega${this.pendientes.length !== 1 ? 's' : ''} pendiente${this.pendientes.length !== 1 ? 's' : ''}`;
    return msg;
  }

  copiarMensaje(): void {
    navigator.clipboard.writeText(this.mensajeWhatsApp).then(() => {
      this.mensajeCopiado = true;
      setTimeout(() => (this.mensajeCopiado = false), 2500);
    });
  }

  marcarEntregado(nroPedido: string): void {
    const idx = this.pendientes.findIndex(p => p.nroPedido === nroPedido);
    if (idx >= 0) {
      const [item] = this.pendientes.splice(idx, 1);
      item.entregadoEn = new Date().toISOString();
      this.historial.unshift(item);
    }
  }

  formatFecha(iso: string): string {
    return new Date(iso).toLocaleDateString('es-AR', { day: '2-digit', month: '2-digit' });
  }
}
