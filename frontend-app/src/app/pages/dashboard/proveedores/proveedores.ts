import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import {
  ProveedoresService, Proveedor, DeudaProveedor, ProveedorRequest,
} from '../../../services/proveedores.service';
import { NotificationService } from '../../../services/notification.service';

/**
 * Pantalla de Proveedores: lista de proveedores con su deuda pendiente y, al
 * expandir, el detalle de sus deudas (incluidas las saldadas por el bot vía
 * pagos directos o triangulados). Permite dar de alta proveedores.
 */
@Component({
  selector: 'app-proveedores',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './proveedores.html',
  styleUrl: './proveedores.css',
})
export class ProveedoresComponent implements OnInit {
  private prov = inject(ProveedoresService);
  private notif = inject(NotificationService);

  proveedores = signal<Proveedor[]>([]);
  cargando = signal(false);

  expandido = signal<number | null>(null);
  deudas = signal<DeudaProveedor[]>([]);
  cargandoDeudas = signal(false);

  modalAbierto = signal(false);
  guardando = signal(false);
  form: ProveedorRequest = this.formVacio();

  ngOnInit(): void { this.cargar(); }

  cargar(): void {
    this.cargando.set(true);
    this.prov.listar().subscribe({
      next: (p) => { this.proveedores.set(p); this.cargando.set(false); },
      error: (e) => { this.notif.errorHttp(e, 'No se pudieron cargar los proveedores'); this.cargando.set(false); },
    });
  }

  totalProveedores(): number { return this.proveedores().length; }
  totalDeuda(): number { return this.proveedores().reduce((s, p) => s + (p.deudaPendiente || 0), 0); }

  toggle(p: Proveedor): void {
    if (this.expandido() === p.id) { this.expandido.set(null); return; }
    this.expandido.set(p.id);
    this.deudas.set([]);
    this.cargandoDeudas.set(true);
    this.prov.deudas(p.id).subscribe({
      next: (d) => { this.deudas.set(d); this.cargandoDeudas.set(false); },
      error: (e) => { this.notif.errorHttp(e, 'No se pudieron cargar las deudas'); this.cargandoDeudas.set(false); },
    });
  }

  abrirModal(): void { this.form = this.formVacio(); this.modalAbierto.set(true); }
  cerrarModal(): void { this.modalAbierto.set(false); }

  guardar(): void {
    if (!this.form.nombre?.trim()) {
      this.notif.alerta('El nombre es obligatorio.', 'Falta el nombre');
      return;
    }
    this.guardando.set(true);
    this.prov.crear(this.form).subscribe({
      next: () => {
        this.notif.exito('Proveedor creado correctamente.', 'Listo');
        this.guardando.set(false);
        this.modalAbierto.set(false);
        this.cargar();
      },
      error: (e) => { this.notif.errorHttp(e, 'No se pudo crear el proveedor'); this.guardando.set(false); },
    });
  }

  private formVacio(): ProveedorRequest {
    return { nombre: '', cuit: '', email: '', telefono: '', direccion: '' };
  }
}
