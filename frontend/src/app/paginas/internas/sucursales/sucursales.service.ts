import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';

import { ServicioApi } from '../../../nucleo/servicio-api';

@Injectable({ providedIn: 'root' })
export class ServicioSucursales {
  constructor(private api: ServicioApi) {}

  listarSucursales(): Observable<any[]> {
    return this.api.get<any[]>('/api/admin/branches');
  }

  listarUsuarios(): Observable<any[]> {
    return this.api.get<any[]>('/api/admin/users');
  }

  crearSucursal(datos: unknown): Observable<any> {
    return this.api.post<any>('/api/admin/branches', datos);
  }

  actualizarSucursal(id: number, datos: unknown): Observable<any> {
    return this.api.put<any>(`/api/admin/branches/${id}`, datos);
  }
}
