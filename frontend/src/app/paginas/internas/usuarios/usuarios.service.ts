import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';

import { Sucursal } from '../../../nucleo/modelos';
import { ServicioApi } from '../../../nucleo/servicio-api';

@Injectable({ providedIn: 'root' })
export class ServicioUsuarios {
  constructor(private api: ServicioApi) {}

  listarUsuarios(): Observable<any[]> {
    return this.api.get<any[]>('/api/admin/users');
  }

  listarSucursales(): Observable<Sucursal[]> {
    return this.api.get<Sucursal[]>('/api/admin/branches');
  }

  crearUsuario(datos: unknown): Observable<any> {
    return this.api.post<any>('/api/admin/users', datos);
  }

  actualizarUsuario(id: number, datos: unknown): Observable<any> {
    return this.api.put<any>(`/api/admin/users/${id}`, datos);
  }

  cambiarEstado(id: number, estado: string): Observable<any> {
    return this.api.patch<any>(`/api/admin/users/${id}/status`, { status: estado });
  }

  desbloquear(id: number): Observable<any> {
    return this.api.post<any>(`/api/admin/users/${id}/unlock`, {});
  }

  restablecerContrasena(id: number): Observable<any> {
    return this.api.post<any>(`/api/admin/users/${id}/reset-password`, {});
  }
}
