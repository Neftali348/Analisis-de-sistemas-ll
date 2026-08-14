import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';

import { ServicioApi } from '../../../nucleo/servicio-api';

@Injectable({ providedIn: 'root' })
export class ServicioAuditoria {
  constructor(private api: ServicioApi) {}

  listarEventos(filtros: Record<string, unknown>): Observable<any> {
    return this.api.get<any>('/api/audit', filtros);
  }

  obtenerDetalle(id: number): Observable<any> {
    return this.api.get<any>(`/api/audit/${id}`);
  }

  verificarIntegridad(): Observable<any> {
    return this.api.get<any>('/api/audit/integrity');
  }
}
