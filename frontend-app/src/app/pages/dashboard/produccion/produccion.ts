import { Component, OnInit, OnDestroy } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ProduccionService, TareaResponse } from '../../../services/produccion.service';

export type EstadoPedido = 'RECIBIDO' | 'EN_PROCESO' | 'CONTROL' | 'LISTO';

export interface PedidoKanban {
  id: number;
  nroPedido: string;
  odontologo: string;
  paciente: string;
  trabajo: string;
  tecnico: string;
  fechaEntrega: Date | null;
  estado: EstadoPedido;
  prioridad: 'NORMAL' | 'URGENTE';
}

interface Columna {
  estado: EstadoPedido;
  labelLargo: string;
  labelCorto: string;
  accionLabel: string;
}

@Component({
  selector: 'app-produccion',
  standalone: true,
  imports: [FormsModule],
  templateUrl: './produccion.html',
  styleUrls: ['./produccion.css']
})
export class ProduccionComponent implements OnInit, OnDestroy {

  pedidos: PedidoKanban[] = [];
  tecnicoFiltro = 'TODOS';
  loading = false;
  error = '';

  // Mobile state
  activeTab: EstadoPedido = 'RECIBIDO';
  toastMsg = '';
  toastVisible = false;
  private toastTimer: ReturnType<typeof setTimeout> | null = null;

  // Desktop drag state
  draggingPedido: PedidoKanban | null = null;
  dragOverColumn: EstadoPedido | null = null;

  readonly columnas: Columna[] = [
    { estado: 'RECIBIDO',   labelLargo: 'Recibido',          labelCorto: 'Recibido', accionLabel: 'Iniciar producción' },
    { estado: 'EN_PROCESO', labelLargo: 'En proceso',         labelCorto: 'En proc.', accionLabel: 'Enviar a control'   },
    { estado: 'CONTROL',    labelLargo: 'Control de calidad', labelCorto: 'Control',  accionLabel: 'Marcar como listo'  },
    { estado: 'LISTO',      labelLargo: 'Listo para entregar',labelCorto: 'P/Entregar',accionLabel: ''                   },
  ];

  readonly ordenEstados: EstadoPedido[] = ['RECIBIDO', 'EN_PROCESO', 'CONTROL', 'LISTO'];

  constructor(private produccionService: ProduccionService) {}

  ngOnInit() {
    this.cargar();
  }

  ngOnDestroy() {
    if (this.toastTimer) clearTimeout(this.toastTimer);
  }

  // Mapea TareaResponse del backend al modelo local
  private mapear(t: TareaResponse): PedidoKanban {
    return {
      id:           t.id,
      nroPedido:    t.nroPedido,
      odontologo:   t.odontologo,
      paciente:     t.paciente,
      trabajo:      t.trabajo,
      tecnico:      t.tecnico ?? 'Sin asignar',
      fechaEntrega: t.fechaEntrega ? new Date(t.fechaEntrega + 'T00:00:00') : null,
      estado:       t.estado,
      prioridad:    t.prioridad,
    };
  }

  private cargar() {
    this.loading = true;
    this.error = '';
    this.produccionService.listarKanban().subscribe({
      next: (data) => {
        this.pedidos = data.map(t => this.mapear(t));
        this.loading = false;
      },
      error: (err) => {
        this.error = 'No se pudo cargar la producción. Verificá que ms-produccion esté corriendo.';
        this.loading = false;
        console.error('Error al cargar produccion:', err);
      }
    });
  }

  // Getters
  get tecnicos(): string[] {
    const ts = this.pedidos
      .map(p => p.tecnico)
      .filter(t => t && t !== 'Sin asignar');
    return [...new Set(ts)].sort();
  }

  get totalActivos(): number {
    return this.pedidos.filter(p => p.estado !== 'LISTO').length;
  }

  get columnaActiva(): Columna {
    return this.columnas.find(c => c.estado === this.activeTab)!;
  }

  get urgentesActivos(): number {
    return this.pedidos.filter(p => p.prioridad === 'URGENTE' && p.estado !== 'LISTO').length;
  }

  pedidosPorEstado(estado: EstadoPedido): PedidoKanban[] {
    return this.pedidos.filter(p => {
      if (p.estado !== estado) return false;
      if (this.tecnicoFiltro !== 'TODOS' && p.tecnico !== this.tecnicoFiltro) return false;
      return true;
    });
  }

  // Mobile: tab navigation
  setActiveTab(estado: EstadoPedido) {
    this.activeTab = estado;
  }

  // Avanzar al siguiente estado — llama al backend
  avanzar(pedido: PedidoKanban) {
    const siguiente = this.columnSiguiente(pedido.estado);
    if (!siguiente) return;
    this.produccionService.actualizarEstado(pedido.id, siguiente).subscribe({
      next: (res) => {
        const label = this.columnas.find(c => c.estado === siguiente)?.labelLargo ?? siguiente;
        pedido.estado = res.estado;
        this.showToast(`Movido a "${label}"`);
      },
      error: () => this.showToast('Error al actualizar el estado')
    });
  }

  // Retroceder al estado anterior — llama al backend
  retroceder(pedido: PedidoKanban) {
    const anterior = this.columnAnterior(pedido.estado);
    if (!anterior) return;
    this.produccionService.actualizarEstado(pedido.id, anterior).subscribe({
      next: (res) => {
        const label = this.columnas.find(c => c.estado === anterior)?.labelLargo ?? anterior;
        pedido.estado = res.estado;
        this.showToast(`Devuelto a "${label}"`);
      },
      error: () => this.showToast('Error al actualizar el estado')
    });
  }

  columnSiguiente(estado: EstadoPedido): EstadoPedido | null {
    const idx = this.ordenEstados.indexOf(estado);
    return idx < this.ordenEstados.length - 1 ? this.ordenEstados[idx + 1] : null;
  }

  columnAnterior(estado: EstadoPedido): EstadoPedido | null {
    const idx = this.ordenEstados.indexOf(estado);
    return idx > 0 ? this.ordenEstados[idx - 1] : null;
  }

  private showToast(msg: string) {
    this.toastMsg = msg;
    this.toastVisible = true;
    if (this.toastTimer) clearTimeout(this.toastTimer);
    this.toastTimer = setTimeout(() => { this.toastVisible = false; }, 2500);
  }

  // Desktop drag & drop — actualiza estado en backend al soltar
  onDragStart(event: DragEvent, pedido: PedidoKanban) {
    this.draggingPedido = pedido;
    if (event.dataTransfer) event.dataTransfer.effectAllowed = 'move';
  }

  onDragOver(event: DragEvent, estado: EstadoPedido) {
    event.preventDefault();
    if (event.dataTransfer) event.dataTransfer.dropEffect = 'move';
    this.dragOverColumn = estado;
  }

  onDragLeave(event: DragEvent) {
    const target  = event.currentTarget as HTMLElement;
    const related = event.relatedTarget as Node | null;
    if (!related || !target.contains(related)) this.dragOverColumn = null;
  }

  onDrop(event: DragEvent, nuevoEstado: EstadoPedido) {
    event.preventDefault();
    this.dragOverColumn = null;
    const pedido = this.draggingPedido;
    this.draggingPedido = null;

    if (!pedido || pedido.estado === nuevoEstado) return;

    // Actualización optimista: mueve la card de inmediato
    const estadoAnterior = pedido.estado;
    pedido.estado = nuevoEstado;

    this.produccionService.actualizarEstado(pedido.id, nuevoEstado).subscribe({
      next: (res) => {
        pedido.estado = res.estado;
        const label = this.columnas.find(c => c.estado === nuevoEstado)?.labelLargo ?? nuevoEstado;
        this.showToast(`Movido a "${label}"`);
      },
      error: () => {
        // Revierte si falla
        pedido.estado = estadoAnterior;
        this.showToast('Error al mover la tarea');
      }
    });
  }

  onDragEnd() {
    this.draggingPedido = null;
    this.dragOverColumn = null;
  }

  // Helpers de fecha y UI
  diasRestantes(fecha: Date | null): number {
    if (!fecha) return 999;
    const hoy = new Date();
    hoy.setHours(0, 0, 0, 0);
    const t = new Date(fecha);
    t.setHours(0, 0, 0, 0);
    return Math.ceil((t.getTime() - hoy.getTime()) / 86400000);
  }

  diasLabel(fecha: Date | null): string {
    if (!fecha) return 'Sin fecha';
    const d = this.diasRestantes(fecha);
    if (d < 0)   return `${Math.abs(d)}d vencido`;
    if (d === 0) return 'Vence hoy';
    if (d === 1) return 'Mañana';
    return `${d} días`;
  }

  diasClass(fecha: Date | null): string {
    if (!fecha) return 'ok';
    const d = this.diasRestantes(fecha);
    if (d < 0)  return 'overdue';
    if (d <= 1) return 'urgent';
    if (d <= 4) return 'soon';
    return 'ok';
  }

  tecnicoColor(nombre: string): string {
    const colors = ['#3b82f6', '#8b5cf6', '#f59e0b', '#14b8a6', '#ec4899'];
    let h = 0;
    for (const c of nombre) h = (h * 31 + c.charCodeAt(0)) % colors.length;
    return colors[h];
  }

  tecnicoInitials(nombre: string): string {
    return nombre.split(' ').slice(0, 2).map(p => p[0]).join('').toUpperCase();
  }
}
