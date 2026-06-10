import { Component, OnInit, HostListener, inject } from '@angular/core';
import { Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { AuthService } from '../../services/auth';
import { ThemeService } from '../../services/theme.service';

interface NavItem {
  type: 'item';
  label: string;
  icon: string;
  route: string;
  roles?: string[];
}

interface NavGroup {
  type: 'group';
  label: string;
}

type NavEntry = NavItem | NavGroup;

@Component({
  selector: 'app-dashboard',
  standalone: true,
  templateUrl: './dashboard.html',
  styleUrls: ['./dashboard.css'],
  imports: [RouterLink, RouterLinkActive, RouterOutlet]
})
export class DashboardComponent implements OnInit {
  username = '';
  roles: string[] = [];
  sidebarOpen = true;
  mobileMenuOpen = false;

  navEntries: NavEntry[] = [
    { type: 'group', label: 'Operativo' },
    { type: 'item', label: 'Inicio',      icon: 'home',     route: '/dashboard' },
    { type: 'item', label: 'Pedidos',     icon: 'package',  route: '/dashboard/pedidos' },
    { type: 'item', label: 'Producción',  icon: 'layers',   route: '/dashboard/produccion' },
    { type: 'item', label: 'Entregas',    icon: 'truck',    route: '/dashboard/entregas' },

    { type: 'group', label: 'Gestión' },
    { type: 'item', label: 'Catálogo',    icon: 'list',     route: '/dashboard/catalogo' },
    { type: 'item', label: 'Odontólogos', icon: 'tooth',    route: '/dashboard/odontologos' },
    { type: 'item', label: 'Stock',       icon: 'box',      route: '/dashboard/stock' },
    { type: 'item', label: 'Finanzas',    icon: 'dollar',   route: '/dashboard/finanzas' },
    { type: 'item', label: 'Bot WhatsApp', icon: 'chat',    route: '/dashboard/bot-registros', roles: ['ROLE_ADMIN'] },
    { type: 'item', label: 'Reportes',    icon: 'chart',    route: '/dashboard/reportes' },

    { type: 'group', label: 'Archivo' },
    { type: 'item', label: 'Documentos',  icon: 'file',     route: '/dashboard/documentos' },
    { type: 'item', label: 'Escaneos 3D', icon: 'cube',     route: '/dashboard/escaneos' },
    { type: 'item', label: 'Auditoría',   icon: 'shield',   route: '/dashboard/auditoria', roles: ['ROLE_ADMIN'] },

    { type: 'group', label: 'Administración' },
    { type: 'item', label: 'Usuarios',    icon: 'users',    route: '/dashboard/usuarios', roles: ['ROLE_ADMIN'] },
  ];

  readonly themeService = inject(ThemeService);

  constructor(private authService: AuthService, private router: Router) {}

  toggleTheme(): void {
    this.themeService.toggle();
  }

  ngOnInit() {
    this.username = this.authService.getUsername();
    this.roles = this.authService.getRoles();
    this.sidebarOpen = window.innerWidth >= 1024;
  }

  @HostListener('window:resize', ['$event'])
  onResize(event: Event) {
    const w = (event.target as Window).innerWidth;
    if (w >= 1024) {
      this.mobileMenuOpen = false;
    }
  }

  visibleNavEntries(): NavEntry[] {
    return this.navEntries.filter(entry =>
      entry.type === 'group' || !entry.roles || entry.roles.some(r => this.roles.includes(r))
    );
  }

  isItem(entry: NavEntry): entry is NavItem {
    return entry.type === 'item';
  }

  isAdmin(): boolean {
    return this.authService.isAdmin();
  }

  logout() {
    this.authService.logout();
    this.router.navigate(['/login']);
  }

  isMobile(): boolean {
    return window.innerWidth < 1024;
  }

  toggleSidebar() {
    if (this.isMobile()) {
      this.mobileMenuOpen = !this.mobileMenuOpen;
    } else {
      this.sidebarOpen = !this.sidebarOpen;
    }
  }

  closeMobileMenu() {
    this.mobileMenuOpen = false;
  }

  getRolLabel(): string {
    if (this.roles.includes('ROLE_ADMIN')) return 'Administrador';
    if (this.roles.includes('ROLE_TECNICO')) return 'Técnico';
    if (this.roles.includes('ROLE_ADMINISTRATIVO')) return 'Administrativo';
    if (this.roles.includes('ROLE_ODONTOLOGO')) return 'Odontólogo';
    return '';
  }
}
