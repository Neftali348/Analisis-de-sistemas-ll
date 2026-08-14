import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';

import { Sucursal } from '../../../nucleo/modelos';
import { ServicioApi } from '../../../nucleo/servicio-api';

@Injectable({ providedIn: 'root' })
export class ServicioRegistrarCaso {
  constructor(private api: ServicioApi) {}

  listarSucursales(): Observable<Sucursal[]> {
    return this.api.get<Sucursal[]>('/api/public/branches');
  }

  registrar(datos: FormData): Observable<any> {
    return this.api.post<any>('/api/public/cases', datos);
  }
}
