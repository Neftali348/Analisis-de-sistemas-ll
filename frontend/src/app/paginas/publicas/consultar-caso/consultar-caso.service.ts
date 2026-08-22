import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';

import { VistaCasoPublico } from '../../../nucleo/modelos';
import { ServicioApi } from '../../../nucleo/servicio-api';

// =========================================================
// CU-03 - VERIFICACIÓN DE SEGURIDAD
// =========================================================

export interface DesafioSeguridad {
  id: string;
  question: string;
}

@Injectable({
  providedIn: 'root'
})
export class ServicioConsultarCaso {

  constructor(
    private api: ServicioApi
  ) {}

  // =========================================================
  // CU-03 - OBTENER VERIFICACIÓN DE SEGURIDAD
  // =========================================================

  obtenerVerificacion(): Observable<DesafioSeguridad> {

    return this.api.get<DesafioSeguridad>(
      '/api/public/cases/security-challenge'
    );
  }

  // =========================================================
  // CU-03 - CONSULTAR CASO
  // =========================================================

  consultar(
    datos: unknown
  ): Observable<VistaCasoPublico> {

    return this.api.post<VistaCasoPublico>(
      '/api/public/cases/lookup',
      datos
    );
  }

  // =========================================================
  // CU-03 FA11 - RESPONDER SOLICITUD
  // =========================================================

  responder(
    codigo: string,
    datos: FormData,
    parametros: Record<string, unknown>
  ): Observable<VistaCasoPublico> {

    return this.api.post<VistaCasoPublico>(
      `/api/public/cases/${codigo}/response`,
      datos,
      {
        params: parametros
      }
    );
  }

  // =========================================================
  // CU-03 FA10 - CANCELAR CASO
  // =========================================================

  cancelar(
    codigo: string,
    motivo: string,
    parametros: Record<string, unknown>
  ): Observable<VistaCasoPublico> {

    return this.api.post<VistaCasoPublico>(
      `/api/public/cases/${codigo}/cancel`,
      {
        reason: motivo
      },
      {
        params: parametros
      }
    );
  }

  // =========================================================
  // CU-03 FA13 - SOLICITAR REAPERTURA
  // =========================================================

  solicitarReapertura(
    codigo: string,
    motivo: string,
    parametros: Record<string, unknown>
  ): Observable<any> {

    return this.api.post<any>(
      `/api/public/cases/${codigo}/reopen-request`,
      {
        reason: motivo
      },
      {
        params: parametros
      }
    );
  }

  // =========================================================
  // CALIFICACIÓN
  // =========================================================

  calificar(
    codigo: string,
    calificacion: number,
    comentario: string | null,
    parametros: Record<string, unknown>
  ): Observable<any> {

    return this.api.post<any>(
      `/api/public/cases/${codigo}/satisfaction`,
      {
        rating: calificacion,
        comment: comentario
      },
      {
        params: parametros
      }
    );
  }

  // =========================================================
  // CU-03 FA12 - DESCARGAR EVIDENCIA
  // =========================================================

  descargarEvidencia(
    codigo: string,
    id: number,
    parametros: Record<string, unknown>
  ): Observable<Blob> {

    return this.api.download(
      `/api/public/cases/${codigo}/evidences/${id}/download`,
      parametros
    );
  }
}