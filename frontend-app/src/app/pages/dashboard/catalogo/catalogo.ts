import { Component, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { CatalogoService, TipoTrabajoResponse, TipoTrabajoRequest } from '../../../services/catalogo.service';

export type Categoria = 'FIJA' | 'REMOVIBLE' | 'ORTODONCIA' | 'ATM' | 'PERSONALIZADO';

/** Modelo local de la vista — alinea con TipoTrabajoResponse del back */
export interface TipoTrabajo {
  id: number;
  nombre: string;
  descripcion: string;
  precio: number;
  categoria: Categoria;
  tiempoEstimadoDias: number;
  foto?: string; // mapea a fotoUrl del backend
}

@Component({
  selector: 'app-catalogo',
  standalone: true,
  imports: [FormsModule],
  templateUrl: './catalogo.html',
  styleUrls: ['./catalogo.css']
})
export class CatalogoComponent implements OnInit {

  trabajos: TipoTrabajo[]  = [];
  filtrados: TipoTrabajo[] = [];

  loading = false;
  error   = '';

  searchQuery      = '';
  categoriaActiva: Categoria | 'TODOS' = 'TODOS';

  showModal  = false;
  editMode   = false;
  saving     = false;
  deleteConfirmId: number | null = null;

  editandoPrecioId: number | null = null;
  precioTemporal   = 0;

  form: Partial<TipoTrabajo> = {};
  fotoPreview?: string;
  arrastrando = false;

  readonly categorias: { valor: Categoria | 'TODOS'; label: string }[] = [
    { valor: 'TODOS',         label: 'Todos'              },
    { valor: 'FIJA',          label: 'Prótesis Fija'      },
    { valor: 'REMOVIBLE',     label: 'Removible'          },
    { valor: 'ORTODONCIA',    label: 'Ortodoncia'         },
    { valor: 'ATM',           label: 'ATM'                },
    { valor: 'PERSONALIZADO', label: 'Personalizado'      },
  ];

  readonly categoriasForm: { valor: Categoria; label: string }[] = [
    { valor: 'FIJA',          label: 'Prótesis Fija'      },
    { valor: 'REMOVIBLE',     label: 'Prótesis Removible' },
    { valor: 'ORTODONCIA',    label: 'Ortodoncia'         },
    { valor: 'ATM',           label: 'ATM'                },
    { valor: 'PERSONALIZADO', label: 'Personalizado'      },
  ];

  constructor(private catService: CatalogoService) {}

  ngOnInit() {
    this.cargar();
  }

  // ── CARGA DESDE EL BACKEND ─────────────────────────────────────
  private cargar() {
    this.loading = true;
    this.error   = '';
    this.catService.listar().subscribe({
      next: (data) => {
        this.trabajos = data.map(this.mapear);
        this.filtrar();
        this.loading = false;
      },
      error: (err) => {
        this.error   = 'No se pudo cargar el catálogo. Verificá que ms-catalogo esté corriendo.';
        this.loading = false;
        console.error('Error al cargar catálogo:', err);
      }
    });
  }

  /** Mapea TipoTrabajoResponse del backend al modelo local de la vista */
  private mapear = (r: TipoTrabajoResponse): TipoTrabajo => ({
    id:                r.id,
    nombre:            r.nombre,
    descripcion:       r.descripcion ?? '',
    precio:            r.precio ?? 0,
    categoria:         r.categoria,
    tiempoEstimadoDias: r.tiempoEstimadoDias ?? 0,
    foto:              r.fotoUrl ?? undefined,
  });

  // ── FILTRADO LOCAL ─────────────────────────────────────────────
  filtrar() {
    let r = [...this.trabajos];
    if (this.categoriaActiva !== 'TODOS') {
      r = r.filter(t => t.categoria === this.categoriaActiva);
    }
    if (this.searchQuery.trim()) {
      const q = this.searchQuery.toLowerCase();
      r = r.filter(t =>
        t.nombre.toLowerCase().includes(q) ||
        t.descripcion.toLowerCase().includes(q)
      );
    }
    this.filtrados = r;
  }

  setCategoriaActiva(cat: Categoria | 'TODOS') {
    this.categoriaActiva = cat;
    this.filtrar();
  }

  // ── STATS ──────────────────────────────────────────────────────
  get totalItems(): number { return this.trabajos.length; }

  get promedioPrecios(): number {
    const con = this.trabajos.filter(t => t.precio > 0);
    if (!con.length) return 0;
    return Math.round(con.reduce((a, t) => a + t.precio, 0) / con.length);
  }

  get totalCategorias(): number {
    return new Set(this.trabajos.map(t => t.categoria)).size;
  }

  countByCategoria(cat: Categoria | 'TODOS'): number {
    if (cat === 'TODOS') return this.trabajos.length;
    return this.trabajos.filter(t => t.categoria === cat).length;
  }

  // ── MODAL CRUD ─────────────────────────────────────────────────
  abrirCrear() {
    this.editMode    = false;
    this.form        = { categoria: 'FIJA', tiempoEstimadoDias: 5, precio: 0 };
    this.fotoPreview = undefined;
    this.showModal   = true;
  }

  abrirEditar(trabajo: TipoTrabajo) {
    this.editMode    = true;
    this.form        = { ...trabajo };
    this.fotoPreview = trabajo.foto;
    this.showModal   = true;
  }

  cerrarModal() {
    this.showModal   = false;
    this.form        = {};
    this.fotoPreview = undefined;
  }

  get formValido(): boolean {
    return !!(this.form.nombre?.trim() && this.form.categoria && this.form.precio != null);
  }

  guardar() {
    if (!this.formValido) return;
    this.saving = true;

    const request: TipoTrabajoRequest = {
      nombre:             this.form.nombre!,
      descripcion:        this.form.descripcion ?? '',
      precio:             this.form.precio ?? 0,
      categoria:          this.form.categoria!,
      tiempoEstimadoDias: this.form.tiempoEstimadoDias ?? 0,
      fotoUrl:            this.fotoPreview ?? null,
    };

    const op$ = this.editMode && this.form.id
      ? this.catService.actualizar(this.form.id, request)
      : this.catService.crear(request);

    op$.subscribe({
      next: (res) => {
        if (this.editMode) {
          const idx = this.trabajos.findIndex(t => t.id === res.id);
          if (idx !== -1) this.trabajos[idx] = this.mapear(res);
        } else {
          this.trabajos.push(this.mapear(res));
        }
        this.filtrar();
        this.saving = false;
        this.cerrarModal();
      },
      error: (err) => {
        this.saving = false;
        console.error('Error al guardar trabajo:', err);
      }
    });
  }

  // ── QUICK PRICE EDIT ───────────────────────────────────────────
  abrirEditarPrecio(trabajo: TipoTrabajo) {
    this.deleteConfirmId  = null;
    this.editandoPrecioId = trabajo.id;
    this.precioTemporal   = trabajo.precio;
  }

  guardarPrecio(trabajo: TipoTrabajo) {
    const request: TipoTrabajoRequest = {
      nombre:             trabajo.nombre,
      descripcion:        trabajo.descripcion,
      precio:             this.precioTemporal,
      categoria:          trabajo.categoria,
      tiempoEstimadoDias: trabajo.tiempoEstimadoDias,
      fotoUrl:            trabajo.foto ?? null,
    };

    this.catService.actualizar(trabajo.id, request).subscribe({
      next: (res) => {
        const idx = this.trabajos.findIndex(t => t.id === res.id);
        if (idx !== -1) this.trabajos[idx] = this.mapear(res);
        this.editandoPrecioId = null;
        this.filtrar();
      },
      error: (err) => {
        // Actualización optimista — revertir si falla
        this.editandoPrecioId = null;
        console.error('Error al actualizar precio:', err);
      }
    });
  }

  cancelarPrecio() { this.editandoPrecioId = null; }

  // ── DELETE ─────────────────────────────────────────────────────
  pedirConfirmEliminar(id: number) {
    this.editandoPrecioId = null;
    this.deleteConfirmId  = id;
  }
  cancelarEliminar() { this.deleteConfirmId = null; }

  eliminar(id: number) {
    this.catService.eliminar(id).subscribe({
      next: () => {
        this.trabajos        = this.trabajos.filter(t => t.id !== id);
        this.deleteConfirmId = null;
        this.filtrar();
      },
      error: (err) => {
        this.deleteConfirmId = null;
        console.error('Error al eliminar trabajo:', err);
      }
    });
  }

  // ── FOTO ───────────────────────────────────────────────────────
  onFotoChange(event: Event) {
    const file = (event.target as HTMLInputElement).files?.[0];
    if (file) this.leerFoto(file);
  }

  onDragOver(event: DragEvent)  { event.preventDefault(); this.arrastrando = true; }
  onDragLeave()                  { this.arrastrando = false; }

  onDrop(event: DragEvent) {
    event.preventDefault();
    this.arrastrando = false;
    const file = event.dataTransfer?.files?.[0];
    if (file?.type.startsWith('image/')) this.leerFoto(file);
  }

  private leerFoto(file: File) {
    const reader = new FileReader();
    reader.onload = e => {
      this.fotoPreview = e.target?.result as string;
      this.form.foto   = this.fotoPreview;
    };
    reader.readAsDataURL(file);
  }

  quitarFoto() {
    this.fotoPreview = undefined;
    this.form.foto   = undefined;
  }

  // ── HELPERS ────────────────────────────────────────────────────
  categoriaLabel(cat: Categoria): string {
    return this.categoriasForm.find(c => c.valor === cat)?.label ?? cat;
  }

  formatPrecio(precio: number): string {
    if (!precio) return 'A convenir';
    return new Intl.NumberFormat('es-AR', {
      style: 'currency', currency: 'ARS', maximumFractionDigits: 0
    }).format(precio);
  }

  get modalTitle(): string {
    return this.editMode ? 'Editar trabajo' : 'Nuevo trabajo';
  }
}
