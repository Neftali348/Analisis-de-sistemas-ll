import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';

import { ServicioApi } from '../../../nucleo/servicio-api';

@Injectable({ providedIn: 'root' })
export class ServicioNotificaciones {
  constructor(private api: ServicioApi) {}

  listarHistorial(): Observable<any[]> {
    return this.api.get<any[]>('/api/admin/notifications');
  }

  listarPlantillas(): Observable<any[]> {
    return this.api.get<any[]>('/api/admin/notifications/templates');
  }

  actualizarPlantilla(id: number, datos: unknown): Observable<any> {
    return this.api.put<any>(`/api/admin/notifications/templates/${id}`, datos);
  }

  reintentar(id: number, motivo: string): Observable<any> {
    return this.api.post<any>(`/api/admin/notifications/${id}/resend`, { reason: motivo });
  }

  cancelar(id: number, motivo: string): Observable<any> {
    return this.api.post<any>(`/api/admin/notifications/${id}/cancel`, { reason: motivo });
  }
}
