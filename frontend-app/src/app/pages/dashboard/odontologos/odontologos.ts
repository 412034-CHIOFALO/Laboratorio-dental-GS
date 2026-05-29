import { Component, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import {
  OdontologosService, OdontologoResponse, OdontologoRequest
} from '../../../services/odontologos.service';

@Component({
  selector: 'app-odontologos',
  standalone: true,
  imports: [FormsModule],
  templateUrl: './odontologos.html',
  styleUrls: ['./odontologos.css'],
})
export class OdontologosComponent implements OnInit {

  odontologos: OdontologoResponse[] = [];
  filtrados: OdontologoResponse[] = [];
  loading = false;
  error = '';

  busqueda = '';
  tipoMatch: 'NOMBRE' | 'DNI' | 'CUIT' | 'MATRICULA' = 'NOMBRE';

  // Modal
  showModal = false;
  editMode = false;
  saving = false;
  odontologoEditandoId: number | null = null;

  form: OdontologoRequest = this.formVacio();

  // Detalle
  detalleAbierto: OdontologoResponse | null = null;

  // Confirm desactivar
  confirmDesactivarId: number | null = null;

  constructor(private service: OdontologosService) {}

  ngOnInit(): void {
    this.cargar();
  }

  // ── CARGA / FILTROS ──────────────────────────────────────────

  private cargar(): void {
    this.loading = true;
    this.error = '';
    this.service.buscar().subscribe({
      next: data => {
        this.odontologos = data.sort((a, b) => a.nombre.localeCompare(b.nombre));
        this.filtrar();
        this.loading = false;
      },
      error: err => {
        this.error = 'No se pudieron cargar los odontólogos. ¿ms-pedidos está corriendo?';
        this.loading = false;
        console.error(err);
      },
    });
  }

  filtrar(): void {
    const q = this.busqueda.trim();
    this.tipoMatch = this.detectarTipo(q);
    if (!q) {
      this.filtrados = this.odontologos;
      return;
    }
    this.service.buscar(q).subscribe({
      next: data => this.filtrados = data,
      error: err => console.error(err),
    });
  }

  private detectarTipo(q: string): 'NOMBRE' | 'DNI' | 'CUIT' | 'MATRICULA' {
    if (/^[0-9]{7,8}$/.test(q))                          return 'DNI';
    if (/^[0-9]{2}-?[0-9]{8}-?[0-9]{1}$/.test(q))        return 'CUIT';
    if (/^(MN|MP|MAT)[\s-]*[0-9]+$/i.test(q))            return 'MATRICULA';
    return 'NOMBRE';
  }

  // ── MODAL CRUD ───────────────────────────────────────────────

  abrirCrear(): void {
    this.editMode = false;
    this.odontologoEditandoId = null;
    this.form = this.formVacio();
    this.showModal = true;
  }

  abrirEditar(o: OdontologoResponse): void {
    this.editMode = true;
    this.odontologoEditandoId = o.id;
    this.form = {
      nombre: o.nombre,
      dni: o.dni ?? '',
      cuit: o.cuit ?? '',
      telefono: o.telefono ?? '',
      email: o.email ?? '',
      matricula: o.matricula ?? '',
      clinica: o.clinica ?? '',
      direccion: o.direccion ?? '',
    };
    this.detalleAbierto = null;
    this.showModal = true;
  }

  cerrarModal(): void {
    this.showModal = false;
  }

  private formVacio(): OdontologoRequest {
    return {
      nombre: '', dni: '', cuit: '', telefono: '',
      email: '', matricula: '', clinica: '', direccion: '',
    };
  }

  get formValido(): boolean {
    return !!this.form.nombre?.trim();
  }

  guardar(): void {
    if (!this.formValido) return;
    this.saving = true;

    const request: OdontologoRequest = {
      nombre: this.form.nombre.trim(),
      dni: this.form.dni?.trim() || null,
      cuit: this.form.cuit?.trim() || null,
      telefono: this.form.telefono?.trim() || null,
      email: this.form.email?.trim() || null,
      matricula: this.form.matricula?.trim() || null,
      clinica: this.form.clinica?.trim() || null,
      direccion: this.form.direccion?.trim() || null,
    };

    const op$ = this.editMode && this.odontologoEditandoId
      ? this.service.actualizar(this.odontologoEditandoId, request)
      : this.service.crear(request);

    op$.subscribe({
      next: res => {
        if (this.editMode) {
          const idx = this.odontologos.findIndex(o => o.id === res.id);
          if (idx !== -1) this.odontologos[idx] = res;
        } else {
          this.odontologos.unshift(res);
        }
        this.odontologos.sort((a, b) => a.nombre.localeCompare(b.nombre));
        this.filtrar();
        this.saving = false;
        this.cerrarModal();
      },
      error: err => {
        this.saving = false;
        const msg = err?.error?.mensaje ?? 'No se pudo guardar el odontólogo.';
        alert(msg);
        console.error(err);
      },
    });
  }

  // ── DETALLE ──────────────────────────────────────────────────

  abrirDetalle(o: OdontologoResponse): void {
    this.detalleAbierto = o;
  }

  cerrarDetalle(): void {
    this.detalleAbierto = null;
  }

  // ── DESACTIVAR ───────────────────────────────────────────────

  pedirConfirmDesactivar(id: number): void {
    this.confirmDesactivarId = id;
  }
  abortarDesactivar(): void {
    this.confirmDesactivarId = null;
  }
  confirmarDesactivar(id: number): void {
    this.service.desactivar(id).subscribe({
      next: () => {
        this.odontologos = this.odontologos.filter(o => o.id !== id);
        this.confirmDesactivarId = null;
        this.filtrar();
        if (this.detalleAbierto?.id === id) this.detalleAbierto = null;
      },
      error: err => {
        this.confirmDesactivarId = null;
        console.error(err);
      },
    });
  }

  // ── STATS ────────────────────────────────────────────────────

  get totalConCuit(): number {
    return this.odontologos.filter(o => !!o.cuit).length;
  }
  get totalConClinica(): number {
    return this.odontologos.filter(o => !!o.clinica).length;
  }

  // ── HELPERS DE VISTA ─────────────────────────────────────────

  iniciales(nombre: string): string {
    return nombre.replace(/^(Dr\.|Dra\.)\s*/i, '')
      .split(' ')
      .filter(Boolean)
      .slice(0, 2)
      .map(p => p[0]?.toUpperCase() ?? '')
      .join('');
  }

  get modalTitle(): string {
    return this.editMode ? 'Editar odontólogo' : 'Nuevo odontólogo';
  }
}
