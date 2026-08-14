import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';

import { VistaCaso } from '../../../nucleo/modelos';
import { ServicioApi } from '../../../nucleo/servicio-api';

@Injectable({ providedIn: 'root' })
export class ServicioConsultarCaso {
  constructor(private api: ServicioApi) {}

  consultar(datos: unknown): Observable<VistaCaso> {
    return this.api.post<VistaCaso>('/api/public/cases/lookup', datos);
  }

  responder(codigo: string, datos: FormData, parametros: Record<string, unknown>): Observable<VistaCaso> {
    return this.api.post<VistaCaso>(`/api/public/cases/${codigo}/response`, datos, { params: parametros });
  }

  cancelar(codigo: string, motivo: string, parametros: Record<string, unknown>): Observable<VistaCaso> {
    return this.api.post<VistaCaso>(`/api/public/cases/${codigo}/cancel`, { reason: motivo }, { params: parametros });
  }

  solicitarReapertura(codigo: string, motivo: string, parametros: Record<string, unknown>): Observable<any> {
    return this.api.post<any>(`/api/public/cases/${codigo}/reopen-request`, { reason: motivo }, { params: parametros });
  }

  calificar(
    codigo: string,
    calificacion: number,
    comentario: string | null,
    parametros: Record<string, unknown>
  ): Observable<any> {
    return this.api.post<any>(
      `/api/public/cases/${codigo}/satisfaction`,
      { rating: calificacion, comment: comentario },
      { params: parametros }
    );
  }

  descargarEvidencia(codigo: string, id: number, parametros: Record<string, unknown>): Observable<Blob> {
    return this.api.download(`/api/public/cases/${codigo}/evidences/${id}/download`, parametros);
  }
}
