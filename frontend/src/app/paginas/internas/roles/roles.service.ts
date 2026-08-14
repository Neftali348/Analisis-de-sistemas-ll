import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';

import { CodigoPermiso, CodigoRol } from '../../../nucleo/modelos';
import { ServicioApi } from '../../../nucleo/servicio-api';

export interface VistaRol {
  code: CodigoRol;
  description: string;
  active: boolean;
  permissions: CodigoPermiso[];
}

@Injectable({ providedIn: 'root' })
export class ServicioRoles {
  constructor(private api: ServicioApi) {}

  listarRoles(): Observable<VistaRol[]> {
    return this.api.get<VistaRol[]>('/api/admin/roles');
  }

  actualizarPermisos(codigo: CodigoRol, permisos: CodigoPermiso[]): Observable<VistaRol> {
    return this.api.put<VistaRol>(`/api/admin/roles/${codigo}/permissions`, { permissions: permisos });
  }

  cambiarEstado(codigo: CodigoRol, activo: boolean): Observable<VistaRol> {
    return this.api.patch<VistaRol>(`/api/admin/roles/${codigo}/status`, { active: activo });
  }
}
