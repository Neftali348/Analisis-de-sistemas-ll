import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';

import { Sucursal } from '../../../nucleo/modelos';
import { ServicioApi } from '../../../nucleo/servicio-api';

@Injectable({ providedIn: 'root' })
export class ServicioReportes {
  constructor(private api: ServicioApi) {}

  listarSucursales(): Observable<Sucursal[]> {
    return this.api.get<Sucursal[]>('/api/public/branches');
  }

  generarVistaPrevia(filtros: unknown): Observable<any> {
    return this.api.post<any>('/api/reports/preview', filtros);
  }

  exportar(formato: string, filtros: unknown): Observable<Blob> {
    return this.api.postBlob(`/api/reports/export/${formato}`, filtros);
  }
}
