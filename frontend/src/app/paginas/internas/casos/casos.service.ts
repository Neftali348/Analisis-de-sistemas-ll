import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';

import { PaginaCasos, Sucursal } from '../../../nucleo/modelos';
import { ServicioApi } from '../../../nucleo/servicio-api';

@Injectable({ providedIn: 'root' })
export class ServicioCasos {
  constructor(private api: ServicioApi) {}

  listarSucursales(): Observable<Sucursal[]> {
    return this.api.get<Sucursal[]>('/api/public/branches');
  }

  listarCasos(filtros: Record<string, unknown>): Observable<PaginaCasos> {
    return this.api.get<PaginaCasos>('/api/cases', filtros);
  }
}
