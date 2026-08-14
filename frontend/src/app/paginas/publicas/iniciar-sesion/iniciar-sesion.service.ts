import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';

import { Sesion } from '../../../nucleo/modelos';
import { ServicioAutenticacion } from '../../../nucleo/servicio-autenticacion';

@Injectable({ providedIn: 'root' })
export class ServicioInicioSesion {
  constructor(private autenticacion: ServicioAutenticacion) {}

  iniciarSesion(usuario: string, contrasena: string): Observable<Sesion> {
    return this.autenticacion.login(usuario, contrasena);
  }

  solicitarRecuperacion(usuarioOCorreo: string): Observable<{ message: string }> {
    return this.autenticacion.requestRecovery(usuarioOCorreo);
  }
}
