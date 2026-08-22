import { CommonModule } from '@angular/common';
import {
  Component,
  OnDestroy,
  OnInit
} from '@angular/core';

import { FormsModule } from '@angular/forms';
import { interval, Subscription } from 'rxjs';

import { ServicioAuditoria } from './auditoria.service';

@Component({
  selector: 'app-auditoria',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './auditoria.component.html',
  styleUrl: './auditoria.component.css'
})
export class ComponenteAuditoria
  implements OnInit, OnDestroy {

  page: any = null;
  selected: any = null;
  integrityResult: any = null;

  error = '';

  f: any = {
    from: '',
    to: '',
    username: '',
    module: '',
    action: '',
    ip: ''
  };

  paginaActual = 0;

  private actualizacion?: Subscription;

  constructor(
    private servicioAuditoria: ServicioAuditoria
  ) {}

  ngOnInit(): void {

    const hoy = new Date();

    const haceTreintaDias =
      new Date(
        Date.now() - 30 * 86400000
      );

    this.f.from =
      haceTreintaDias
        .toISOString()
        .slice(0, 10);

    this.f.to =
      hoy
        .toISOString()
        .slice(0, 10);

    // Primera carga normal
    this.load(0);

    // =====================================================
    // ACTUALIZACIÓN AUTOMÁTICA CADA 3 SEGUNDOS
    // =====================================================
    this.actualizacion =
      interval(3000)
        .subscribe(() => {

          /*
           * Solo refrescamos automáticamente
           * cuando está viendo la primera página.
           *
           * Así, si está revisando registros antiguos,
           * la tabla no le cambia sola.
           */
          if (
            this.paginaActual === 0 &&
            !this.selected
          ) {
            this.load(0, true);
          }
        });
  }

  load(
    pagina: number,
    automatica = false
  ): void {

    this.paginaActual = pagina;

    this.servicioAuditoria
      .listarEventos({
        ...this.f,
        page: pagina,
        size: 25,
        autoRefresh: automatica
      })
      .subscribe({

        next: respuesta => {

          this.page = respuesta;

          if (!automatica) {
            this.error = '';
          }
        },

        error: e => {

          /*
           * Si falla un refresco automático,
           * no llenamos la pantalla de errores.
           *
           * Un error de una consulta manual sí
           * se muestra.
           */
          if (!automatica) {
            this.error =
              e?.error?.message ??
              'No se pudo consultar la bitácora.';
          }
        }
      });
  }

  detail(id: number): void {

    this.servicioAuditoria
      .obtenerDetalle(id)
      .subscribe({

        next: respuesta =>
          this.selected = respuesta,

        error: e =>
          this.error =
            e?.error?.message ??
            'No se pudo abrir el evento.'
      });
  }

  integrity(): void {

    this.servicioAuditoria
      .verificarIntegridad()
      .subscribe({

        next: respuesta =>
          this.integrityResult = respuesta,

        error: e =>
          this.error =
            e?.error?.message ??
            'No se pudo verificar la integridad.'
      });
  }

  ngOnDestroy(): void {

    /*
     * Cuando salimos de Auditoría,
     * detenemos el intervalo.
     */
    this.actualizacion?.unsubscribe();
  }
}