import { Component, OnInit, inject, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { PedidosService, PedidoResponse } from '../../../services/pedidos.service';
import { EscaneosService, EscaneoResponse } from '../../../services/escaneos.service';
import { Visor3dComponent } from './visor3d.component';

const EXTENSIONES_3D = ['.stl', '.obj', '.ply', '.3ds', '.step', '.stp', '.iges', '.igs'];
// Formatos que el visor 3D embebido sabe renderizar.
const VISUALIZABLES_3D = ['.stl', '.obj'];

@Component({
  selector: 'app-escaneos',
  standalone: true,
  imports: [CommonModule, FormsModule, Visor3dComponent],
  templateUrl: './escaneos.html',
  styleUrls: ['./escaneos.css'],
})
export class EscaneosComponent implements OnInit {
  private pedidosService = inject(PedidosService);
  private escaneosService = inject(EscaneosService);

  cargandoPedidos = signal(true);
  errorPedidos    = signal('');
  pedidos         = signal<PedidoResponse[]>([]);
  busqueda        = signal('');

  pedidoSeleccionado = signal<PedidoResponse | null>(null);
  escaneos           = signal<EscaneoResponse[]>([]);
  cargandoEscaneos   = signal(false);
  subiendo           = signal(false);
  eliminandoId       = signal<number | null>(null);
  arrastrando        = signal(false);

  // Campo descripción para el upload
  descripcionInput = '';

  // Visor 3D
  visorAbierto    = signal(false);
  visorUrl        = signal('');
  visorFileName   = signal('');
  cargandoVisorId = signal<number | null>(null);

  pedidosFiltrados = computed(() => {
    const q = this.busqueda().toLowerCase().trim();
    if (!q) return this.pedidos();
    return this.pedidos().filter(p =>
      p.nroPedido.toLowerCase().includes(q) ||
      p.paciente.toLowerCase().includes(q) ||
      p.odontologoNombre.toLowerCase().includes(q)
    );
  });

  ngOnInit(): void {
    this.pedidosService.listarTodos().subscribe({
      next: ps => {
        this.pedidos.set(ps.sort((a, b) =>
          new Date(b.fechaCreacion).getTime() - new Date(a.fechaCreacion).getTime()
        ));
        this.cargandoPedidos.set(false);
      },
      error: () => {
        this.errorPedidos.set('No se pudieron cargar los pedidos.');
        this.cargandoPedidos.set(false);
      },
    });
  }

  seleccionar(p: PedidoResponse): void {
    this.pedidoSeleccionado.set(p);
    this.escaneos.set([]);
    this.cargandoEscaneos.set(true);
    this.escaneosService.listar(p.id).subscribe({
      next: es => { this.escaneos.set(es); this.cargandoEscaneos.set(false); },
      error: ()  => { this.cargandoEscaneos.set(false); },
    });
  }

  onFileInput(event: Event): void {
    const input = event.target as HTMLInputElement;
    if (input.files?.length) this.subirArchivo(input.files[0]);
    input.value = '';
  }

  onDrop(event: DragEvent): void {
    event.preventDefault();
    this.arrastrando.set(false);
    const file = event.dataTransfer?.files?.[0];
    if (file) this.subirArchivo(file);
  }

  onDragOver(event: DragEvent): void { event.preventDefault(); this.arrastrando.set(true); }
  onDragLeave(): void { this.arrastrando.set(false); }

  private subirArchivo(file: File): void {
    const p = this.pedidoSeleccionado();
    if (!p) return;
    this.subiendo.set(true);
    const desc = this.descripcionInput.trim() || undefined;
    this.escaneosService.subir(p.id, file, desc).subscribe({
      next: escaneo => {
        this.escaneos.update(es => [escaneo, ...es]);
        this.descripcionInput = '';
        this.subiendo.set(false);
      },
      error: () => { this.subiendo.set(false); },
    });
  }

  eliminar(escaneo: EscaneoResponse): void {
    const p = this.pedidoSeleccionado();
    if (!p) return;
    this.eliminandoId.set(escaneo.id);
    this.escaneosService.eliminar(p.id, escaneo.id).subscribe({
      next: () => {
        this.escaneos.update(es => es.filter(e => e.id !== escaneo.id));
        this.eliminandoId.set(null);
      },
      error: () => { this.eliminandoId.set(null); },
    });
  }

  /** ¿El formato se puede abrir en el visor 3D embebido? */
  esVisualizable3d(fileName: string): boolean {
    return VISUALIZABLES_3D.includes(this.extension(fileName));
  }

  /** Pide la URL del escaneo y abre el visor 3D. */
  verEscaneo(escaneo: EscaneoResponse): void {
    const p = this.pedidoSeleccionado();
    if (!p) return;
    this.cargandoVisorId.set(escaneo.id);
    this.escaneosService.url(p.id, escaneo.id).subscribe({
      next: ({ url }) => {
        this.visorUrl.set(url);
        this.visorFileName.set(escaneo.fileName);
        this.visorAbierto.set(true);
        this.cargandoVisorId.set(null);
      },
      error: () => { this.cargandoVisorId.set(null); },
    });
  }

  cerrarVisor(): void {
    this.visorAbierto.set(false);
    this.visorUrl.set('');
  }

  extension(fileName: string): string {
    const idx = fileName.lastIndexOf('.');
    return idx >= 0 ? fileName.substring(idx).toLowerCase() : '';
  }

  colorExt(fileName: string): string {
    const ext = this.extension(fileName);
    const m: Record<string, string> = {
      '.stl': 'ext-stl', '.obj': 'ext-obj', '.ply': 'ext-ply',
      '.3ds': 'ext-3ds', '.step': 'ext-step', '.stp': 'ext-step',
      '.iges': 'ext-iges', '.igs': 'ext-iges',
    };
    return m[ext] ?? 'ext-otro';
  }

  formatSize(bytes: number | null): string {
    if (!bytes) return '—';
    if (bytes < 1024)        return bytes + ' B';
    if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + ' KB';
    return (bytes / (1024 * 1024)).toFixed(1) + ' MB';
  }

  claseEstado(estado: string): string {
    const m: Record<string, string> = {
      RECIBIDO: 'est-recibido', EN_PROCESO: 'est-proceso',
      CONTROL: 'est-control',   LISTO: 'est-listo',
      ENTREGADO: 'est-entregado', CANCELADO: 'est-cancelado',
    };
    return m[estado] ?? '';
  }

  readonly accept = EXTENSIONES_3D.join(',');
}
