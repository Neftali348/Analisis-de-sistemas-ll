import { Injectable } from '@angular/core';

import { ServicioAutenticacion } from '../../nucleo/servicio-autenticacion';

@Injectable({ providedIn: 'root' })
export class ServicioPrincipal {
  constructor(private autenticacion: ServicioAutenticacion) {}

  mostrarAdministracion(): boolean {
    return this.autenticacion.has('USER_ADMIN') ||
      this.autenticacion.has('ROLE_ADMIN') ||
      this.autenticacion.has('BRANCH_ADMIN') ||
      this.autenticacion.has('NOTIFICATION_ADMIN') ||
      this.autenticacion.has('AUDIT_VIEW');
  }

  obtenerIniciales(): string {
    return (this.autenticacion.session?.fullName ?? 'U')
      .split(' ')
      .slice(0, 2)
      .map(parte => parte[0])
      .join('')
      .toUpperCase();
  }

  obtenerNombreRol(): string {
    return (this.autenticacion.session?.role ?? '').replaceAll('_', ' ');
  }

  cerrarSesion(): void {

    const confirmar = window.confirm(
      '¿Está seguro de que desea cerrar sesión?'
    );
  
    // FA11
    if (!confirmar) {
      return;
    }
  
    // FA10
    this.autenticacion.logout();
  }
}
