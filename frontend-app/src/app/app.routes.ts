import { Routes } from '@angular/router';
import { LandingPage } from './pages/landing-page/landing-page';
import { LoginComponent } from './pages/login/login';
import { DashboardComponent } from './pages/dashboard/dashboard';
import { DashboardHomeComponent } from './pages/dashboard/home/dashboard-home';
import { UsuariosComponent } from './pages/dashboard/usuarios/usuarios';
import { PedidosComponent } from './pages/dashboard/pedidos/pedidos';
import { ProduccionComponent } from './pages/dashboard/produccion/produccion';
import { CatalogoComponent } from './pages/dashboard/catalogo/catalogo';
import { FinanzasComponent } from './pages/dashboard/finanzas/finanzas';
import { StockComponent } from './pages/dashboard/stock/stock';
import { EntregasComponent } from './pages/dashboard/entregas/entregas';
import { ReportesComponent } from './pages/dashboard/reportes/reportes';
import { DocumentosComponent } from './pages/dashboard/documentos/documentos';
import { EscaneosComponent } from './pages/dashboard/escaneos/escaneos';
import { AuditoriaComponent } from './pages/dashboard/auditoria/auditoria';
import { OdontologosComponent } from './pages/dashboard/odontologos/odontologos';
import { OdontologoHistorialComponent } from './pages/dashboard/odontologos/historial/odontologo-historial';
import { BotRegistrosComponent } from './pages/dashboard/bot-registros/bot-registros';
import { ProveedoresComponent } from './pages/dashboard/proveedores/proveedores';
import { ConfiguracionComponent } from './pages/dashboard/configuracion/configuracion';
import { ManualComponent } from './pages/dashboard/manual/manual';
import { ErrorPageComponent } from './pages/error/error-page';
import { authGuard } from './guards/auth.guard';

export const routes: Routes = [
  { path: '', component: LandingPage },
  { path: 'login', component: LoginComponent },
  {
    path: 'dashboard',
    component: DashboardComponent,
    canActivate: [authGuard],
    children: [
      { path: '',            component: DashboardHomeComponent },
      { path: 'pedidos',     component: PedidosComponent },
      { path: 'odontologos', component: OdontologosComponent },
      { path: 'odontologos/:id/historial', component: OdontologoHistorialComponent },
      { path: 'produccion',  component: ProduccionComponent },
      { path: 'entregas',    component: EntregasComponent },
      { path: 'catalogo',    component: CatalogoComponent },
      { path: 'stock',       component: StockComponent },
      { path: 'finanzas',    component: FinanzasComponent },
      { path: 'proveedores', component: ProveedoresComponent },
      { path: 'bot-registros', component: BotRegistrosComponent },
      { path: 'reportes',    component: ReportesComponent },
      { path: 'documentos',  component: DocumentosComponent },
      { path: 'escaneos',    component: EscaneosComponent },
      { path: 'auditoria',      component: AuditoriaComponent },
      { path: 'usuarios',       component: UsuariosComponent },
      { path: 'configuracion',  component: ConfiguracionComponent },
      { path: 'manual',         component: ManualComponent },
    ]
  },
  // Páginas de error
  { path: 'sin-permisos', component: ErrorPageComponent, data: { tipo: 'forbidden' } },
  { path: 'error',        component: ErrorPageComponent, data: { tipo: 'server' } },
  { path: '**',           component: ErrorPageComponent, data: { tipo: 'not-found' } },
];
