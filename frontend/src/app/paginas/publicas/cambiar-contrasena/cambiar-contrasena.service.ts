import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';

import { ServicioApi } from '../../../nucleo/servicio-api';

@Injectable({ providedIn: 'root' })
export class ServicioCambiarContrasena {
  constructor(private api: ServicioApi) {}

  cambiar(actual: string, nueva: string, confirmacion: string): Observable<any> {
    return this.api.post<any>('/api/auth/change-password', {
      currentPassword: actual,
      newPassword: nueva,
      confirmPassword: confirmacion
    });
  }
}
