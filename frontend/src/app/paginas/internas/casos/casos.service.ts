import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';

import {
  PaginaCasos,
  Sucursal
} from '../../../nucleo/modelos';

import {
  ServicioApi
} from '../../../nucleo/servicio-api';

export interface ResponsableFiltro {
  id: number;
  name: string;
}

@Injectable({
  providedIn: 'root'
})
export class ServicioCasos {

  constructor(
    private api: ServicioApi
  ) {}

  listarSucursales(): Observable<Sucursal[]> {
    /*
     * Si ya agregaste el endpoint interno restringido,
     * usa /api/cases/filters/branches.
     *
     * Mientras tu backend actual siga como lo mostraste,
     * esta ruta pública es la que ya existe.
     */
    return this.api.get<Sucursal[]>(
      '/api/public/branches'
    );
  }

  listarResponsables(): Observable<ResponsableFiltro[]> {
    /*
     * Este endpoint debe existir en ControladorCasos
     * para cumplir FA06 sin usar /{id}/agents.
     */
    return this.api.get<ResponsableFiltro[]>(
      '/api/cases/filters/responsibles'
    );
  }

  listarCasos(
    filtros: Record<string, unknown>
  ): Observable<PaginaCasos> {

    return this.api.get<PaginaCasos>(
      '/api/cases',
      filtros
    );
  }
}