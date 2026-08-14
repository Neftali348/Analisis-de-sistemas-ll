import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';

import { ServicioApi } from '../../../nucleo/servicio-api';

@Injectable({ providedIn: 'root' })
export class ServicioPanel {
  constructor(private api: ServicioApi) {}

  obtenerResumen(): Observable<any> {
    return this.api.get<any>('/api/dashboard');
  }
}
