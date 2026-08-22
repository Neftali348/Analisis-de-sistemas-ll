import { inject } from '@angular/core';
import {
  CanActivateFn,
  Router
} from '@angular/router';

import { ServicioAutenticacion } from './servicio-autenticacion';

export const guardiaAutenticacion: CanActivateFn = () => {

  const autenticacion =
    inject(ServicioAutenticacion);

  const router =
    inject(Router);

  if (autenticacion.logged) {
    return true;
  }

  return router.createUrlTree([
    '/login'
  ]);
};


export const guardiaPermiso =
  (permiso: string): CanActivateFn =>
  (route, state) => {

    const autenticacion =
      inject(ServicioAutenticacion);

    const router =
      inject(Router);

    if (autenticacion.has(permiso)) {
      return true;
    }

    // Registrar en auditoría
    autenticacion
      .registrarAccesoDenegado(
        permiso,
        state.url
      )
      .subscribe({
        error: () => {}
      });

    window.alert(
      'No posee permisos para realizar esta acción.'
    );

    return router.createUrlTree([
      '/app/dashboard'
    ]);
  };