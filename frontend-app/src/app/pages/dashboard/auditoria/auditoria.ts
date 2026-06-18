import { Component, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { AuthService } from '../../../services/auth';
import { environment } from '../../../../environments/environment';
import { clonar, MOCK_AUDIT, MockAuditEvent, TipoAudit } from '../../../services/mock-data';

@Component({
  selector: 'app-auditoria',
  standalone: true,
  imports: [FormsModule],
  templateUrl: './auditoria.html',
  styleUrls: ['./auditoria.css'],
})
export class AuditoriaComponent implements OnInit {
  private gatewayUrl = environment.gatewayUrl || 'http://localhost:8080';

  events: MockAuditEvent[] = [];
  filtros: MockAuditEvent[] = [];
  busqueda   = '';
  tipoFiltro: TipoAudit | '' = '';
  loading    = false;
  error      = '';

  readonly tiposAudit: { valor: TipoAudit | ''; label: string }[] = [
    { valor: '',         label: 'Todos los eventos' },
    { valor: 'LOGIN',    label: 'Inicio de sesión'  },
    { valor: 'CREAR',    label: 'Crear'             },
    { valor: 'EDITAR',   label: 'Editar'            },
    { valor: 'ELIMINAR', label: 'Eliminar'          },
    { valor: 'PAGO',     label: 'Pagos'             },
    { valor: 'ESTADO',   label: 'Cambio de estado'  },
  ];

  constructor(private http: HttpClient, private authService: AuthService) {}

  ngOnInit(): void {
    this.cargar();
  }

  private headers(): HttpHeaders {
    return new HttpHeaders({ Authorization: `Bearer ${this.authService.getToken()}` });
  }

  cargar(): void {
    this.loading = true;
    this.error   = '';

    if (environment.useMocks) {
      setTimeout(() => {
        this.events  = clonar(MOCK_AUDIT).sort((a: MockAuditEvent, b: MockAuditEvent) =>
          new Date(b.timestamp).getTime() - new Date(a.timestamp).getTime());
        this.filtros = this.events;
        this.loading = false;
      }, 200);
      return;
    }

    this.http.get<MockAuditEvent[]>(`${this.gatewayUrl}/ms-auth/api/auth/auditoria`, { headers: this.headers() })
      .subscribe({
        next: (data) => {
          this.events  = data;
          this.filtros = data;
          this.loading = false;
        },
        error: () => {
          this.error   = 'No se pudo cargar el registro de auditoría.';
          this.loading = false;
        }
      });
  }

  filtrar(): void {
    this.filtros = this.events.filter(e => {
      const matchTipo = !this.tipoFiltro || e.tipo === this.tipoFiltro;
      const q         = this.busqueda.toLowerCase();
      const matchText = !q || [e.usuario, e.accion, e.entidad, e.detalle]
        .some(s => s?.toLowerCase().includes(q));
      return matchTipo && matchText;
    });
  }

  formatTs(iso: string): string {
    const d   = new Date(iso);
    const hoy = new Date();
    const esHoy = d.toDateString() === hoy.toDateString();
    if (esHoy) return d.toLocaleTimeString('es-AR', { hour: '2-digit', minute: '2-digit' });
    return d.toLocaleDateString('es-AR', { day: '2-digit', month: '2-digit' }) + ' ' +
           d.toLocaleTimeString('es-AR', { hour: '2-digit', minute: '2-digit' });
  }

  colorTipo(tipo: TipoAudit): string {
    const m: Record<TipoAudit, string> = {
      LOGIN: 'cyan', CREAR: 'green', EDITAR: 'blue',
      ELIMINAR: 'rose', PAGO: 'amber', ESTADO: 'purple',
    };
    return m[tipo] ?? 'muted';
  }

  iconTipo(tipo: TipoAudit): string {
    const m: Record<TipoAudit, string> = {
      LOGIN:    'M11 16l-4-4m0 0l4-4m-4 4h14m-5 4v1a3 3 0 01-3 3H6a3 3 0 01-3-3V7a3 3 0 013-3h7a3 3 0 013 3v1',
      CREAR:    'M12 4v16m8-8H4',
      EDITAR:   'M11 5H6a2 2 0 00-2 2v11a2 2 0 002 2h11a2 2 0 002-2v-5m-1.414-9.414a2 2 0 112.828 2.828L11.828 15H9v-2.828l8.586-8.586z',
      ELIMINAR: 'M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16',
      PAGO:     'M12 8c-1.657 0-3 .895-3 2s1.343 2 3 2 3 .895 3 2-1.343 2-3 2m0-8c1.11 0 2.08.402 2.599 1M12 8V7m0 1v8m0 0v1m0-1c-1.11 0-2.08-.402-2.599-1M21 12a9 9 0 11-18 0 9 9 0 0118 0z',
      ESTADO:   'M7 16V4m0 0L3 8m4-4l4 4m6 0v12m0 0l4-4m-4 4l-4-4',
    };
    return m[tipo] ?? '';
  }
}
