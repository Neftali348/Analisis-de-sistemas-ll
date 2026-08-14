import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';

import { ServicioApi } from '../../../nucleo/servicio-api';

@Injectable({ providedIn: 'root' })
export class ServicioRestablecerContrasena {
  constructor(private api: ServicioApi) {}

  restablecer(token: string, nueva: string, confirmacion: string): Observable<any> {
    return this.api.post<any>('/api/auth/reset-password', {
      token,
      newPassword: nueva,
      confirmPassword: confirmacion
    });
  }
}
