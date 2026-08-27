import { CommonModule } from '@angular/common';
import { Component, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import Swal from 'sweetalert2';

import { PaginaCasos, Sucursal } from '../../../nucleo/modelos';
import { ResponsableFiltro, ServicioCasos } from './casos.service';

interface FiltrosCasos {
  code: string;
  status: string[];
  type: string;
  priority: string;
  branchId: number | '';
  category: string;
  responsibleId: number | '';
  sla: string;
  from: string;
  to: string;
}

@Component({
  selector: 'app-casos',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './casos.component.html',
  styleUrl: './casos.component.css'
})
export class ComponenteCasos implements OnInit {

  page: PaginaCasos | null = null;
  branches: Sucursal[] = [];
  responsables: ResponsableFiltro[] = [];
  loading = false;

  private readonly STORAGE_KEY = 'sgq-bandeja-casos-filtros';
  private paginaRestaurada = 0;

  f: FiltrosCasos = this.crearFiltrosVacios();

  readonly statuses = [
    'REGISTRADO',
    'PENDIENTE_ASIGNACION',
    'ASIGNADO',
    'EN_PROCESO',
    'EN_ESPERA_CLIENTE',
    'RESUELTO',
    'CERRADO',
    'RECHAZADO',
    'CANCELADO',
    'REABIERTO'
  ];

  readonly types = [
    'QUEJA',
    'RECLAMO',
    'DENUNCIA',
    'SUGERENCIA'
  ];

  readonly priorities = [
    'BAJA',
    'MEDIA',
    'ALTA',
    'CRITICA'
  ];

  readonly categories = [
    'PRODUCTO',
    'ATENCION',
    'ENTREGA',
    'COBRO',
    'HIGIENE',
    'INSTALACIONES',
    'OTRA'
  ];

  constructor(
    private servicioCasos: ServicioCasos
  ) {}

  ngOnInit(): void {
    this.restaurarEstadoBandeja();
    this.cargarSucursales();
    this.cargarResponsables();
    this.load(this.paginaRestaurada);
  }

  private cargarSucursales(): void {
    this.servicioCasos.listarSucursales().subscribe({
      next: sucursales => {
        this.branches = sucursales;
      },
      error: e => {
        this.mostrarErrorHttp(e);
      }
    });
  }

  private cargarResponsables(): void {
    this.servicioCasos.listarResponsables().subscribe({
      next: responsables => {
        this.responsables = responsables;
      },
      error: e => {
        this.mostrarErrorHttp(e);
      }
    });
  }

  buscar(): void {
    this.load(0);
  }

  clear(): void {
    this.f = this.crearFiltrosVacios();
    this.paginaRestaurada = 0;

    sessionStorage.removeItem(this.STORAGE_KEY);

    this.load(0);
  }

  toggleEstado(estado: string, event: Event): void {
    const marcado = (event.target as HTMLInputElement).checked;

    if (marcado) {
      if (!this.f.status.includes(estado)) {
        this.f.status = [...this.f.status, estado];
      }
      return;
    }

    this.f.status = this.f.status.filter(item => item !== estado);
  }

  load(pagina: number): void {
    if (this.loading) {
      return;
    }

    const codigo = this.f.code.trim().toUpperCase();
    this.f.code = codigo;

    /*
     * FA01 permite código completo o parcial.
     * Si parece un código completo, validamos RN05.
     */
    if (
      codigo.length >= 15 &&
      !this.codigoCompletoValido(codigo)
    ) {
      this.mostrarValidacion(
        'El código de seguimiento no existe o los datos de consulta son incorrectos.'
      );
      return;
    }

    /*
     * FA02: la fecha inicial no puede ser posterior
     * a la fecha final.
     */
    if (
      this.f.from &&
      this.f.to &&
      this.f.from > this.f.to
    ) {
      this.mostrarValidacion(
        'La fecha inicial no puede ser posterior a la fecha final.'
      );
      return;
    }

    const numeroPagina = Math.max(0, pagina);

    this.loading = true;
    this.guardarEstado(numeroPagina);

    this.servicioCasos.listarCasos({
      /*
       * El ControladorCasos ACTUAL recibe "q".
       * El campo visual sigue siendo "Código de seguimiento".
       */
      q: this.f.code,

      status: this.f.status,
      type: this.f.type,
      priority: this.f.priority,
      branchId: this.f.branchId,
      category: this.f.category,
      responsibleId: this.f.responsibleId,
      sla: this.f.sla,
      from: this.f.from,
      to: this.f.to,
      page: numeroPagina,
      size: 20
    }).subscribe({
      next: respuesta => {
        this.page = respuesta;
        this.loading = false;
        this.guardarEstado(respuesta.page);
      },
      error: e => {
        this.loading = false;
        this.mostrarErrorHttp(e);
      }
    });
  }

  guardarEstadoBandeja(): void {
    this.guardarEstado(this.page?.page ?? 0);
  }

  private guardarEstado(pagina: number): void {
    sessionStorage.setItem(
      this.STORAGE_KEY,
      JSON.stringify({
        filtros: this.f,
        pagina
      })
    );
  }

  private restaurarEstadoBandeja(): void {
    const guardado = sessionStorage.getItem(this.STORAGE_KEY);

    if (!guardado) {
      return;
    }

    try {
      const estado = JSON.parse(guardado);
      const filtrosGuardados = estado?.filtros ?? {};

      this.f = {
        ...this.crearFiltrosVacios(),
        ...filtrosGuardados,
        status: Array.isArray(filtrosGuardados.status)
          ? filtrosGuardados.status
          : []
      };

      this.paginaRestaurada = Math.max(
        0,
        Number(estado?.pagina ?? 0)
      );

    } catch {
      sessionStorage.removeItem(this.STORAGE_KEY);
      this.f = this.crearFiltrosVacios();
      this.paginaRestaurada = 0;
    }
  }

  private crearFiltrosVacios(): FiltrosCasos {
    return {
      code: '',
      status: [],
      type: '',
      priority: '',
      branchId: '',
      category: '',
      responsibleId: '',
      sla: '',
      from: '',
      to: ''
    };
  }

  private codigoCompletoValido(codigo: string): boolean {
    return /^(QUE|REC|DEN|SUG)-\d{4}-\d{6}$/.test(codigo);
  }

  label(valor: string | null | undefined): string {
    if (!valor) {
      return '—';
    }

    return valor.replaceAll('_', ' ');
  }

  clienteTexto(caso: any): string {
    if (caso.anonymous) {
      return 'Anónimo';
    }

    return caso.fullName || '—';
  }

  /*
   * El DTO real NO trae slaStatus.
   * Trae slaWarningSent y slaBreached.
   */
  slaTexto(caso: any): string {
    if (caso.slaBreached) {
      return 'Vencido';
    }

    if (caso.slaWarningSent) {
      return 'Próximo a vencer';
    }

    return 'En tiempo';
  }

  slaClase(caso: any): string {
    if (caso.slaBreached) {
      return 'sla-danger';
    }

    if (caso.slaWarningSent) {
      return 'sla-warning';
    }

    return 'sla-ok';
  }

  prioridadClase(prioridad: string): string {
    switch (prioridad) {
      case 'CRITICA':
        return 'priority-critical';

      case 'ALTA':
        return 'priority-high';

      case 'MEDIA':
        return 'priority-medium';

      default:
        return 'priority-low';
    }
  }

  private mostrarValidacion(mensaje: string): void {
    void Swal.fire({
      icon: 'warning',
      title: 'Validación',
      text: mensaje,
      confirmButtonText: 'OK',
      confirmButtonColor: '#12263a',
      allowOutsideClick: true
    });
  }

  private mostrarErrorHttp(e: any): void {
    let mensaje: string;

    if (e?.status === 0) {
      mensaje = 'Error de conexión con el servidor.';

    } else if (e?.status === 401) {
      mensaje = 'La sesión ha expirado. Inicie sesión nuevamente.';

    } else if (e?.status === 403) {
      mensaje = 'No posee permisos para realizar esta acción.';

    } else if (
      typeof e?.error?.message === 'string' &&
      e.error.message.trim()
    ) {
      mensaje = e.error.message.trim();

    } else if (
      typeof e?.error === 'string' &&
      e.error.trim()
    ) {
      mensaje = e.error.trim();

    } else {
      mensaje = 'Error interno del sistema. Intente nuevamente.';
    }

    void Swal.fire({
      icon: 'error',
      title: 'Error',
      text: mensaje,
      confirmButtonText: 'OK',
      confirmButtonColor: '#12263a',
      allowOutsideClick: true
    });
  }
}