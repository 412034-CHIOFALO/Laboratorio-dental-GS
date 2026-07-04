import { Component, OnInit, inject, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { PedidosService, PedidoResponse } from '../../../services/pedidos.service';
import { DocumentosService, DocumentoResponse } from '../../../services/documentos.service';
import { FinanzasService, ReporteMensualResponse } from '../../../services/finanzas.service';
import { AuthService } from '../../../services/auth';

@Component({
  selector: 'app-documentos',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './documentos.html',
  styleUrls: ['./documentos.css'],
})
export class DocumentosComponent implements OnInit {
  private pedidosService = inject(PedidosService);
  private docService      = inject(DocumentosService);
  private finanzasService = inject(FinanzasService);
  private auth            = inject(AuthService);

  readonly esAdmin = this.auth.isAdmin();

  // ── Vista activa: documentos por pedido | reportes mensuales ─────────────
  vista = signal<'pedidos' | 'reportes'>('pedidos');

  // ── Reportes mensuales archivados (solo ADMIN) ───────────────────────────
  reportes         = signal<ReporteMensualResponse[]>([]);
  cargandoReportes = signal(false);
  errorReportes    = signal('');
  generandoReporte = signal(false);

  // ── Estado general ──────────────────────────────────────────────────────
  cargandoPedidos = signal(true);
  errorPedidos    = signal('');
  pedidos         = signal<PedidoResponse[]>([]);
  busqueda        = signal('');

  pedidoSeleccionado = signal<PedidoResponse | null>(null);
  docs               = signal<DocumentoResponse[]>([]);
  cargandoDocs       = signal(false);
  subiendo           = signal(false);
  eliminandoId       = signal<number | null>(null);

  // ── Upload drag & drop ──────────────────────────────────────────────────
  arrastrando = signal(false);

  // ── Computed ────────────────────────────────────────────────────────────
  pedidosFiltrados = computed(() => {
    const q = this.busqueda().toLowerCase().trim();
    if (!q) return this.pedidos();
    return this.pedidos().filter(p =>
      p.nroPedido.toLowerCase().includes(q) ||
      p.paciente.toLowerCase().includes(q) ||
      p.odontologoNombre.toLowerCase().includes(q) ||
      p.trabajo.toLowerCase().includes(q)
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

  // ── Reportes mensuales ────────────────────────────────────────────────────

  cambiarVista(v: 'pedidos' | 'reportes'): void {
    this.vista.set(v);
    if (v === 'reportes' && this.reportes().length === 0 && !this.cargandoReportes()) {
      this.cargarReportes();
    }
  }

  cargarReportes(): void {
    this.cargandoReportes.set(true);
    this.errorReportes.set('');
    this.finanzasService.listarReportesMensuales().subscribe({
      next: rs => { this.reportes.set(rs); this.cargandoReportes.set(false); },
      error: () => {
        this.errorReportes.set('No se pudieron cargar los reportes.');
        this.cargandoReportes.set(false);
      },
    });
  }

  generarReporteActual(): void {
    const now = new Date();
    this.generandoReporte.set(true);
    this.errorReportes.set('');
    this.finanzasService.generarReporteMensual(now.getFullYear(), now.getMonth() + 1).subscribe({
      next: () => { this.generandoReporte.set(false); this.cargarReportes(); },
      error: () => {
        this.errorReportes.set('No se pudo generar el reporte de este mes.');
        this.generandoReporte.set(false);
      },
    });
  }

  descargarReporte(r: ReporteMensualResponse): void {
    this.finanzasService.urlDescargaReporte(r.id).subscribe({
      next: res => { if (res?.url) window.open(res.url, '_blank'); },
      error: () => { this.errorReportes.set('No se pudo abrir el reporte.'); },
    });
  }

  formatFecha(iso: string): string {
    return new Date(iso).toLocaleDateString('es-AR', { day: '2-digit', month: '2-digit', year: 'numeric' });
  }

  seleccionar(p: PedidoResponse): void {
    this.pedidoSeleccionado.set(p);
    this.docs.set([]);
    this.cargandoDocs.set(true);
    this.docService.listar(p.id).subscribe({
      next: ds => { this.docs.set(ds); this.cargandoDocs.set(false); },
      error: ()  => { this.cargandoDocs.set(false); },
    });
  }

  // ── Upload ──────────────────────────────────────────────────────────────

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

  onDragOver(event: DragEvent): void {
    event.preventDefault();
    this.arrastrando.set(true);
  }

  onDragLeave(): void {
    this.arrastrando.set(false);
  }

  private subirArchivo(file: File): void {
    const p = this.pedidoSeleccionado();
    if (!p) return;
    this.subiendo.set(true);
    this.docService.subir(p.id, file).subscribe({
      next: doc => {
        this.docs.update(ds => [doc, ...ds]);
        this.subiendo.set(false);
      },
      error: () => { this.subiendo.set(false); },
    });
  }

  // ── Eliminar ─────────────────────────────────────────────────────────────

  eliminar(doc: DocumentoResponse): void {
    const p = this.pedidoSeleccionado();
    if (!p) return;
    this.eliminandoId.set(doc.id);
    this.docService.eliminar(p.id, doc.id).subscribe({
      next: () => {
        this.docs.update(ds => ds.filter(d => d.id !== doc.id));
        this.eliminandoId.set(null);
      },
      error: () => { this.eliminandoId.set(null); },
    });
  }

  // ── Helpers ───────────────────────────────────────────────────────────────

  esImagen(doc: DocumentoResponse): boolean {
    const ct = doc.contentType ?? '';
    return ct.startsWith('image/');
  }

  esPdf(doc: DocumentoResponse): boolean {
    return (doc.contentType ?? '').includes('pdf');
  }

  formatSize(bytes: number | null): string {
    if (!bytes) return '—';
    if (bytes < 1024)       return bytes + ' B';
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
}
