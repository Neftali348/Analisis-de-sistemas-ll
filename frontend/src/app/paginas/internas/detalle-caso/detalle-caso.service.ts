import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';

import { Sucursal, VistaCaso } from '../../../nucleo/modelos';
import { ServicioApi } from '../../../nucleo/servicio-api';

@Injectable({ providedIn: 'root' })
export class ServicioDetalleCaso {
  constructor(private api: ServicioApi) {}

  listarSucursales(): Observable<Sucursal[]> {
    return this.api.get<Sucursal[]>('/api/public/branches');
  }

  obtenerCaso(id: number): Observable<VistaCaso> {
    return this.api.get<VistaCaso>(`/api/cases/${id}`);
  }

  listarAgentes(id: number): Observable<any[]> {
    return this.api.get<any[]>(`/api/cases/${id}/agents`);
  }

  asignarResponsable(id: number, asignacion: unknown): Observable<VistaCaso> {
    return this.api.post<VistaCaso>(`/api/cases/${id}/assign`, asignacion);
  }

  actualizarCaso(id: number, datos: unknown): Observable<VistaCaso> {
    return this.api.put<VistaCaso>(`/api/cases/${id}`, datos);
  }

  cambiarPrioridad(id: number, prioridad: string): Observable<VistaCaso> {
    return this.api.patch<VistaCaso>(`/api/cases/${id}/priority`, { priority: prioridad });
  }

  agregarSeguimiento(id: number, datos: FormData): Observable<VistaCaso> {
    return this.api.post<VistaCaso>(`/api/cases/${id}/follow-ups`, datos);
  }

  adjuntarEvidencia(
    id: number,
    datos: FormData,
    descripcion: string,
    visibleCliente: boolean
  ): Observable<any> {
    return this.api.post<any>(`/api/cases/${id}/evidences`, datos, {
      params: {
        description: descripcion,
        visibleToClient: String(visibleCliente)
      }
    });
  }

  descargarEvidencia(idEvidencia: number): Observable<Blob> {
    return this.api.download(`/api/evidences/${idEvidencia}/download`);
  }

  resolverCaso(id: number, resolucion: string): Observable<VistaCaso> {
    return this.api.post<VistaCaso>(`/api/cases/${id}/resolve`, { resolution: resolucion });
  }

  cerrarCaso(id: number, datos: unknown): Observable<VistaCaso> {
    return this.api.post<VistaCaso>(`/api/cases/${id}/close`, datos);
  }

  reabrirCaso(id: number, datos: unknown): Observable<VistaCaso> {
    return this.api.post<VistaCaso>(`/api/cases/${id}/reopen`, datos);
  }

  cambiarEstado(id: number, estado: string, motivo: string): Observable<VistaCaso> {
    return this.api.patch<VistaCaso>(`/api/cases/${id}/status`, { status: estado, reason: motivo });
  }
}
