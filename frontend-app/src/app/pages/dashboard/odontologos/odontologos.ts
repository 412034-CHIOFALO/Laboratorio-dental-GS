import { Component, OnInit, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import {
  OdontologosService, OdontologoResponse, OdontologoRequest
} from '../../../services/odontologos.service';
import {
  FinanzasService, ComprobanteResponse, PagoCuentaCorrienteResponse, MedioPago
} from '../../../services/finanzas.service';
import { NotificationService } from '../../../services/notification.service';
import { hoyComoLocalDate } from '../../../services/date-utils';

@Component({
  selector: 'app-odontologos',
  standalone: true,
  imports: [FormsModule, RouterLink],
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

  // Filtro de actividad (calculada por último pedido)
  filtroActividad: 'TODOS' | 'ACTIVOS' | 'INACTIVOS' = 'TODOS';

  // Modal
  showModal = false;
  editMode = false;
  saving = false;
  odontologoEditandoId: number | null = null;

  form: OdontologoRequest = this.formVacio();

  // Detalle
  detalleAbierto: OdontologoResponse | null = null;

  // Cuenta corriente del detalle
  ccComprobantes: ComprobanteResponse[] = [];
  ccHistorial: PagoCuentaCorrienteResponse[] = [];
  ccSaldo = 0;
  ccCargando = false;

  // Modal registrar pago
  showModalPago = false;
  pagoSaving = false;
  formPago: { monto: number | null; medio: MedioPago; fecha: string; nota: string } = this.formPagoVacio();

  // Confirm desactivar
  confirmDesactivarId: number | null = null;

  private notif = inject(NotificationService);
  private finanzas = inject(FinanzasService);

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
      this.filtrados = this.aplicarFiltroActividad(this.odontologos);
      return;
    }
    this.service.buscar(q).subscribe({
      next: data => this.filtrados = this.aplicarFiltroActividad(data),
      error: err => console.error(err),
    });
  }

  /** Aplica el filtro Activos/Inactivos según el estado calculado. */
  private aplicarFiltroActividad(lista: OdontologoResponse[]): OdontologoResponse[] {
    if (this.filtroActividad === 'ACTIVOS')   return lista.filter(o => !o.inactivoPorTiempo);
    if (this.filtroActividad === 'INACTIVOS') return lista.filter(o => o.inactivoPorTiempo);
    return lista;
  }

  setFiltroActividad(f: 'TODOS' | 'ACTIVOS' | 'INACTIVOS'): void {
    this.filtroActividad = f;
    this.filtrar();
  }

  get countActivos(): number {
    return this.odontologos.filter(o => !o.inactivoPorTiempo).length;
  }
  get countInactivos(): number {
    return this.odontologos.filter(o => o.inactivoPorTiempo).length;
  }

  /** Texto "hace X meses/días" del último pedido. */
  ultimoPedidoLabel(o: OdontologoResponse): string {
    if (!o.ultimoPedido) return 'Sin pedidos';
    const dias = Math.floor((Date.now() - new Date(o.ultimoPedido).getTime()) / 86_400_000);
    if (dias < 1)   return 'Pidió hoy';
    if (dias < 30)  return `Hace ${dias}d`;
    const meses = Math.floor(dias / 30);
    return `Hace ${meses} mes${meses !== 1 ? 'es' : ''}`;
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

  /** Deja solo dígitos (DNI). */
  sanitizarDni(v: string): string { return (v || '').replace(/[^0-9]/g, '').slice(0, 8); }
  /** Deja solo dígitos y guiones (CUIT). */
  sanitizarCuit(v: string): string { return (v || '').replace(/[^0-9-]/g, '').slice(0, 13); }
  /** Deja solo números y símbolos de teléfono. */
  sanitizarTelefono(v: string): string { return (v || '').replace(/[^0-9+()\-\s]/g, '').slice(0, 30); }

  guardar(): void {
    if (!this.formValido) return;

    // Validaciones de formato (además del backend).
    const dni = this.form.dni?.trim();
    if (dni && !/^[0-9]{7,8}$/.test(dni)) {
      this.notif.alerta('El DNI debe tener 7 u 8 dígitos.', 'DNI inválido'); return;
    }
    const cuit = this.form.cuit?.trim();
    if (cuit && !/^[0-9]{2}-?[0-9]{8}-?[0-9]{1}$/.test(cuit)) {
      this.notif.alerta('El CUIT debe tener 11 dígitos (ej: 20-28456789-3).', 'CUIT inválido'); return;
    }
    const tel = this.form.telefono?.trim();
    if (tel && !/^[0-9+()\-\s]{6,30}$/.test(tel)) {
      this.notif.alerta('El teléfono solo puede tener números y los símbolos + - ( ).', 'Teléfono inválido'); return;
    }

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
          this.notif.exito(`${res.nombre} actualizado correctamente`);
        } else {
          this.odontologos.unshift(res);
          this.notif.exito(`${res.nombre} agregado a la cartera`);
        }
        this.odontologos.sort((a, b) => a.nombre.localeCompare(b.nombre));
        this.filtrar();
        this.saving = false;
        this.cerrarModal();
      },
      error: err => {
        this.saving = false;
        this.notif.errorHttp(err, 'No se pudo guardar el odontólogo');
        console.error(err);
      },
    });
  }

  // ── DETALLE ──────────────────────────────────────────────────

  abrirDetalle(o: OdontologoResponse): void {
    this.detalleAbierto = o;
    this.cargarCuentaCorriente(o.id);
  }

  cerrarDetalle(): void {
    this.detalleAbierto = null;
    this.ccComprobantes = [];
    this.ccHistorial = [];
    this.ccSaldo = 0;
  }

  // ── CUENTA CORRIENTE ─────────────────────────────────────────

  private cargarCuentaCorriente(odontologoId: number): void {
    this.ccCargando = true;
    this.finanzas.comprobantesPorOdontologo(odontologoId).subscribe({
      next: comps => {
        this.ccComprobantes = comps;
        this.ccSaldo = comps.reduce((acc, c) => acc + (c.saldoPendiente ?? 0), 0);
        this.ccCargando = false;
      },
      error: () => { this.ccCargando = false; },
    });
    this.finanzas.historialPagosOdontologo(odontologoId).subscribe({
      next: pagos => { this.ccHistorial = pagos; },
      error: () => {},
    });
  }

  private formPagoVacio() {
    return {
      monto: null as number | null,
      medio: 'TRANSFERENCIA' as MedioPago,
      fecha: hoyComoLocalDate(),
      nota: '',
    };
  }

  abrirModalPago(): void {
    this.formPago = this.formPagoVacio();
    this.showModalPago = true;
  }

  cerrarModalPago(): void {
    this.showModalPago = false;
  }

  get formPagoValido(): boolean {
    return this.formPago.monto != null && this.formPago.monto > 0;
  }

  confirmarPago(): void {
    if (!this.detalleAbierto || !this.formPagoValido) return;
    this.pagoSaving = true;
    const id = this.detalleAbierto.id;
    this.finanzas.registrarPagoCuentaCorriente(id, {
      monto: this.formPago.monto!,
      medio: this.formPago.medio,
      fecha: this.formPago.fecha,
      nota: this.formPago.nota?.trim() || null,
    }).subscribe({
      next: res => {
        this.pagoSaving = false;
        this.showModalPago = false;
        this.notif.exito(res.mensaje);
        this.cargarCuentaCorriente(id);   // refresca saldo/comprobantes/historial
      },
      error: err => {
        this.pagoSaving = false;
        this.notif.errorHttp(err, 'No se pudo registrar el pago');
      },
    });
  }

  formatPrecio(n: number | null | undefined): string {
    if (n == null) return '—';
    return new Intl.NumberFormat('es-AR', { style: 'currency', currency: 'ARS', maximumFractionDigits: 0 }).format(n);
  }

  // ── DESACTIVAR ───────────────────────────────────────────────

  pedirConfirmDesactivar(id: number): void {
    // Cierra el modal de edición si estaba abierto, para no superponer modales
    this.showModal = false;
    this.confirmDesactivarId = id;
  }
  abortarDesactivar(): void {
    this.confirmDesactivarId = null;
  }
  confirmarDesactivar(id: number): void {
    const odontologo = this.odontologos.find(o => o.id === id);
    const nombre = odontologo?.nombre ?? 'Odontólogo';
    this.service.desactivar(id).subscribe({
      next: () => {
        this.odontologos = this.odontologos.filter(o => o.id !== id);
        this.confirmDesactivarId = null;
        this.filtrar();
        if (this.detalleAbierto?.id === id) this.detalleAbierto = null;
        this.notif.alerta(`${nombre} desactivado`);
      },
      error: err => {
        this.confirmDesactivarId = null;
        this.notif.errorHttp(err, 'No se pudo desactivar el odontólogo');
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
