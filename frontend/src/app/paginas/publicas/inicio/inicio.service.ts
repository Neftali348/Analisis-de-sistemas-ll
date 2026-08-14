import { Injectable } from '@angular/core';

@Injectable({ providedIn: 'root' })
export class ServicioInicio {
  // Esta pantalla es informativa y no necesita consumir la API.
  readonly nombreSistema = 'Sistema de Gestión de Quejas';
}
