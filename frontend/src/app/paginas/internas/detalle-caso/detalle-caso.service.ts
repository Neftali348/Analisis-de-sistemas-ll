
import { Observable } from 'rxjs';
import { Injectable } from '@angular/core';
import { Sucursal, VistaCaso } from '../../../nucleo/modelos';
import { ServicioApi } from '../../../nucleo/servicio-api';

export interface AgenteDisponible {
  id: number;
  fullName: string;
  username: string;

  branchId: number | null;
  branch: string;

  categories: string[];

  openCases: number;
  overdueCases: number;

  available: boolean;
}

export interface FiltrosAgentes {
  q?: string;
  branchId?: number;
  category?: string;
  available?: boolean;
  maxOpenCases?: number;
}

export type TipoAsociacionEvidencia =
  | 'CASO'
  | 'SEGUIMIENTO';

export interface DisponibilidadEvidencia {
  limit: number;
  used: number;
  available: number;
  visibilityEditable: boolean;
  visibleToClient: boolean;
}

@Injectable({ providedIn: 'root' })
export class ServicioDetalleCaso {

  constructor(private api: ServicioApi) {}

  listarSucursales(): Observable<Sucursal[]> {
    return this.api.get<Sucursal[]>('/api/public/branches');
  }

  obtenerCaso(id: number): Observable<VistaCaso> {
    return this.api.get<VistaCaso>(`/api/cases/${id}`);
  }

  listarAgentes(
    id: number,
    filtros: FiltrosAgentes = {}
  ): Observable<AgenteDisponible[]> {

    return this.api.get<AgenteDisponible[]>(
      `/api/cases/${id}/agents`,
      filtros as Record<string, unknown>
    );
  }

  asignarResponsable(
    id: number,
    asignacion: {
      responsibleId: number;
      reason: string;
      version: number;
    }
  ): Observable<VistaCaso> {

    return this.api.post<VistaCaso>(
      `/api/cases/${id}/assign`,
      asignacion
    );
  }

  actualizarCaso(
    id: number,
    datos: {
      type: string;
      branchId: number;
      category: string;
      priority: string;
      orderNumber: string;
      administrativeObservation: string;
    }
  ): Observable<VistaCaso> {

    return this.api.put<VistaCaso>(
      `/api/cases/${id}`,
      datos
    );
  }

  cambiarPrioridad(
    id: number,
    prioridad: string
  ): Observable<VistaCaso> {

    return this.api.patch<VistaCaso>(
      `/api/cases/${id}/priority`,
      { priority: prioridad }
    );
  }

  agregarSeguimiento(
    id: number,
    datos: FormData
  ): Observable<VistaCaso> {

    return this.api.post<VistaCaso>(
      `/api/cases/${id}/follow-ups`,
      datos
    );
  }

  consultarDisponibilidadEvidencia(
    id: number,
    associationType: TipoAsociacionEvidencia,
    followUpId?: number | null
  ): Observable<DisponibilidadEvidencia> {

    const parametros: Record<string, unknown> = {
      associationType
    };

    if (followUpId != null) {
      parametros['followUpId'] = followUpId;
    }

    return this.api.get<DisponibilidadEvidencia>(
      `/api/cases/${id}/evidences/availability`,
      parametros
    );
  }

  adjuntarEvidencia(
    id: number,
    datos: FormData,
    parametros: {
      associationType: TipoAsociacionEvidencia;
      followUpId?: number | null;
      description: string;
      visibleToClient: boolean;
      version: number;
    }
  ): Observable<unknown> {

    const params: Record<string, string> = {
      associationType: parametros.associationType,
      description: parametros.description,
      visibleToClient: String(parametros.visibleToClient),
      version: String(parametros.version)
    };

    if (parametros.followUpId != null) {
      params['followUpId'] =
        String(parametros.followUpId);
    }

    return this.api.post<unknown>(
      `/api/cases/${id}/evidences`,
      datos,
      { params }
    );
  }

  descargarEvidencia(
    idEvidencia: number
  ): Observable<Blob> {

    return this.api.download(
      `/api/evidences/${idEvidencia}/download`
    );
  }

  resolverCaso(
    id: number,
    resolucion: string
  ): Observable<VistaCaso> {

    return this.api.post<VistaCaso>(
      `/api/cases/${id}/resolve`,
      { resolution: resolucion }
    );
  }

  cerrarCaso(
    id: number,
    datos: {
      reason: string;
      summary: string;
      internalObservation: string;
      detailReason: string;
      duplicateCaseCode: string;
      notifyClient: boolean;
      sendSurvey: boolean;
      criticalReviewConfirmed: boolean;
    }
  ): Observable<VistaCaso> {

    return this.api.post<VistaCaso>(
      `/api/cases/${id}/close`,
      datos
    );
  }

  reabrirCaso(
    id: number,
    datos: {
      reason: string;
      specialJustification: boolean;
    }
  ): Observable<VistaCaso> {

    return this.api.post<VistaCaso>(
      `/api/cases/${id}/reopen`,
      datos
    );
  }

  cambiarEstado(
    id: number,
    estado: string,
    motivo: string
  ): Observable<VistaCaso> {

    return this.api.patch<VistaCaso>(
      `/api/cases/${id}/status`,
      {
        status: estado,
        reason: motivo
      }
    );
  }
}
