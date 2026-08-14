import { Routes } from '@angular/router';
import { guardiaAutenticacion, guardiaPermiso } from './nucleo/guardias';

export const routes: Routes = [
  {
    path: '',
    loadComponent: () => import('./paginas/publicas/inicio/inicio.component')
      .then(m => m.ComponenteInicio)
  },
  {
    path: 'cambiar-contrasena',
    canActivate: [guardiaAutenticacion],
    loadComponent: () => import('./paginas/publicas/cambiar-contrasena/cambiar-contrasena.component')
      .then(m => m.ComponenteCambiarContrasena)
  },
  {
    path: 'restablecer-contrasena',
    loadComponent: () => import('./paginas/publicas/restablecer-contrasena/restablecer-contrasena.component')
      .then(m => m.ComponenteRestablecerContrasena)
  },
  {
    path: 'login',
    loadComponent: () => import('./paginas/publicas/iniciar-sesion/iniciar-sesion.component')
      .then(m => m.ComponenteInicioSesion)
  },
  {
    path: 'registrar',
    loadComponent: () => import('./paginas/publicas/registrar-caso/registrar-caso.component')
      .then(m => m.ComponenteRegistrarCaso)
  },
  {
    path: 'consultar',
    loadComponent: () => import('./paginas/publicas/consultar-caso/consultar-caso.component')
      .then(m => m.ComponenteConsultarCaso)
  },
  {
    path: 'app',
    canActivate: [guardiaAutenticacion],
    loadComponent: () => import('./diseno/principal/principal.component')
      .then(m => m.ComponentePrincipal),
    children: [
      { path: '', pathMatch: 'full', redirectTo: 'dashboard' },
      {
        path: 'dashboard',
        loadComponent: () => import('./paginas/internas/panel/panel.component')
          .then(m => m.ComponentePanel)
      },
      {
        path: 'casos',
        canActivate: [guardiaPermiso('CASE_VIEW')],
        loadComponent: () => import('./paginas/internas/casos/casos.component')
          .then(m => m.ComponenteCasos)
      },
      {
        path: 'casos/:id',
        canActivate: [guardiaPermiso('CASE_VIEW')],
        loadComponent: () => import('./paginas/internas/detalle-caso/detalle-caso.component')
          .then(m => m.ComponenteDetalleCaso)
      },
      {
        path: 'usuarios',
        canActivate: [guardiaPermiso('USER_ADMIN')],
        loadComponent: () => import('./paginas/internas/usuarios/usuarios.component')
          .then(m => m.ComponenteUsuarios)
      },
      {
        path: 'roles',
        canActivate: [guardiaPermiso('ROLE_ADMIN')],
        loadComponent: () => import('./paginas/internas/roles/roles.component')
          .then(m => m.ComponenteRoles)
      },
      {
        path: 'sucursales',
        canActivate: [guardiaPermiso('BRANCH_ADMIN')],
        loadComponent: () => import('./paginas/internas/sucursales/sucursales.component')
          .then(m => m.ComponenteSucursales)
      },
      {
        path: 'reportes',
        canActivate: [guardiaPermiso('REPORT_VIEW')],
        loadComponent: () => import('./paginas/internas/reportes/reportes.component')
          .then(m => m.ComponenteReportes)
      },
      {
        path: 'auditoria',
        canActivate: [guardiaPermiso('AUDIT_VIEW')],
        loadComponent: () => import('./paginas/internas/auditoria/auditoria.component')
          .then(m => m.ComponenteAuditoria)
      },
      {
        path: 'notificaciones',
        canActivate: [guardiaPermiso('NOTIFICATION_ADMIN')],
        loadComponent: () => import('./paginas/internas/notificaciones/notificaciones.component')
          .then(m => m.ComponenteNotificaciones)
      }
    ]
  },
  { path: '**', redirectTo: '' }
];
