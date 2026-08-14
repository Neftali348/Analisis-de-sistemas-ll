import { CommonModule } from '@angular/common';
import { Component, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, RouterLink } from '@angular/router';

import { Sucursal, VistaCaso } from '../../../nucleo/modelos';
import { ServicioAutenticacion } from '../../../nucleo/servicio-autenticacion';
import { ServicioAvisos } from '../../../nucleo/servicio-avisos';
import { ServicioDetalleCaso } from './detalle-caso.service';

@Component({
  selector: 'app-detalle-caso',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './detalle-caso.component.html',
  styleUrl: './detalle-caso.component.css'
})
export class ComponenteDetalleCaso implements OnInit {
  id = 0;
  c: VistaCaso | null = null;
  error = '';
  branches: Sucursal[] = [];
  agents: any[] = [];

  types = ['QUEJA', 'RECLAMO', 'DENUNCIA', 'SUGERENCIA'];
  categories = ['PRODUCTO', 'ATENCION', 'ENTREGA', 'COBRO', 'HIGIENE', 'INSTALACIONES', 'OTRA'];
  priorities = ['BAJA', 'MEDIA', 'ALTA', 'CRITICA'];
  statuses = ['ASIGNADO', 'EN_PROCESO', 'EN_ESPERA_CLIENTE', 'RESUELTO'];
  followTypes = ['COMENTARIO_INTERNO', 'RESPUESTA_CLIENTE', 'SOLICITUD_INFORMACION', 'ACCION_CORRECTIVA'];
  closeReasons = ['SOLUCIONADO', 'IMPROCEDENTE', 'DUPLICADO', 'CLIENTE_NO_RESPONDIO', 'CANCELADO', 'OTRO'];

  assign: any = { responsibleId: null, reason: '' };
  edit: any = {};
  newPriority = 'MEDIA';
  follow: any = { type: 'COMENTARIO_INTERNO', description: '', visibleToClient: false, newStatus: null };
  followFiles: File[] = [];
  evidenceDescription = '';
  evidenceFile: File | null = null;
  evidenceVisible = false;
  resolution = '';
  close: any = {
    reason: 'SOLUCIONADO', summary: '', internalObservation: '', detailReason: '',
    duplicateCaseCode: '', notifyClient: true, sendSurvey: true, criticalReviewConfirmed: false
  };
  reopen: any = { reason: '', specialJustification: false };
  specialStatus = 'RECHAZADO';
  specialReason = '';

  constructor(
    private route: ActivatedRoute,
    private servicioDetalle: ServicioDetalleCaso,
    public auth: ServicioAutenticacion,
    private toast: ServicioAvisos
  ) {}

  ngOnInit(): void {
    this.id = Number(this.route.snapshot.paramMap.get('id'));
    this.servicioDetalle.listarSucursales().subscribe({
      next: sucursales => this.branches = sucursales,
      error: () => this.branches = []
    });
    this.load();
  }

  label(valor: string): string {
    return (valor || '').replaceAll('_', ' ');
  }

  bytes(tamano: number): string {
    return tamano < 1048576 ? `${Math.round(tamano / 1024)} KB` : `${(tamano / 1048576).toFixed(2)} MB`;
  }

  load(): void {
    this.error = '';
    this.servicioDetalle.obtenerCaso(this.id).subscribe({
      next: caso => {
        this.c = caso;
        this.newPriority = caso.priority;
        this.edit = {
          type: caso.type,
          branchId: caso.branchId,
          category: caso.category,
          priority: caso.priority,
          orderNumber: caso.orderNumber,
          administrativeObservation: caso.administrativeObservation
        };
        if (this.auth.has('CASE_ASSIGN')) this.loadAgents();
      },
      error: e => this.error = e?.error?.message ?? 'No fue posible cargar el caso.'
    });
  }

  loadAgents(): void {
    this.servicioDetalle.listarAgentes(this.id).subscribe({
      next: agentes => this.agents = agentes,
      error: () => this.agents = []
    });
  }

  ok(mensaje: string): void {
    this.toast.show(mensaje);
    this.load();
  }

  fail(e: any): void {
    this.error = e?.error?.message ?? 'No se pudo completar la operación.';
    window.scrollTo({ top: 0, behavior: 'smooth' });
  }

  doAssign(): void {
    if (!this.assign.responsibleId) {
      this.error = 'Seleccione un agente.';
      return;
    }
    this.servicioDetalle.asignarResponsable(this.id, this.assign).subscribe({
      next: () => {
        this.assign = { responsibleId: null, reason: '' };
        this.ok('Asignación guardada.');
      },
      error: e => this.fail(e)
    });
  }

  saveEdit(): void {
    this.servicioDetalle.actualizarCaso(this.id, this.edit).subscribe({
      next: () => this.ok('Información administrativa actualizada.'),
      error: e => this.fail(e)
    });
  }

  changePriority(): void {
    this.servicioDetalle.cambiarPrioridad(this.id, this.newPriority).subscribe({
      next: () => this.ok('Prioridad actualizada.'),
      error: e => this.fail(e)
    });
  }

  pickFollowFiles(e: Event): void {
    const input = e.target as HTMLInputElement;
    this.followFiles = Array.from(input.files ?? []);
    if (this.followFiles.length > 5 || this.followFiles.some(f => f.size > 2 * 1024 * 1024)) {
      this.error = 'Máximo 5 archivos de 2 MB cada uno.';
      this.followFiles = [];
      input.value = '';
    }
  }

  addFollow(): void {
    const datos = new FormData();
    datos.append('data', new Blob([JSON.stringify(this.follow)], { type: 'application/json' }));
    this.followFiles.forEach(archivo => datos.append('files', archivo));

    this.servicioDetalle.agregarSeguimiento(this.id, datos).subscribe({
      next: () => {
        this.follow = { type: 'COMENTARIO_INTERNO', description: '', visibleToClient: false, newStatus: null };
        this.followFiles = [];
        this.ok('Seguimiento registrado.');
      },
      error: e => this.fail(e)
    });
  }

  pickEvidence(e: Event): void {
    const input = e.target as HTMLInputElement;
    this.evidenceFile = input.files?.[0] ?? null;
    if (this.evidenceFile && this.evidenceFile.size > 2 * 1024 * 1024) {
      this.error = 'El archivo supera 2 MB.';
      this.evidenceFile = null;
      input.value = '';
    }
  }

  uploadEvidence(): void {
    if (!this.evidenceFile || !this.evidenceDescription.trim()) {
      this.error = 'Seleccione archivo e indique descripción.';
      return;
    }

    const datos = new FormData();
    datos.append('file', this.evidenceFile);

    this.servicioDetalle.adjuntarEvidencia(
      this.id,
      datos,
      this.evidenceDescription,
      this.evidenceVisible
    ).subscribe({
      next: () => {
        this.evidenceFile = null;
        this.evidenceDescription = '';
        this.ok('Evidencia adjuntada.');
      },
      error: e => this.fail(e)
    });
  }

  download(idEvidencia: number, nombre: string): void {
    this.servicioDetalle.descargarEvidencia(idEvidencia).subscribe({
      next: archivo => this.guardarArchivo(archivo, nombre),
      error: e => this.fail(e)
    });
  }

  resolve(): void {
    this.servicioDetalle.resolverCaso(this.id, this.resolution).subscribe({
      next: () => {
        this.resolution = '';
        this.ok('Caso marcado como resuelto.');
      },
      error: e => this.fail(e)
    });
  }

  doClose(): void {
    this.servicioDetalle.cerrarCaso(this.id, this.close).subscribe({
      next: () => this.ok('Caso cerrado.'),
      error: e => this.fail(e)
    });
  }

  doReopen(): void {
    this.servicioDetalle.reabrirCaso(this.id, this.reopen).subscribe({
      next: () => {
        this.reopen = { reason: '', specialJustification: false };
        this.ok('Caso reabierto.');
      },
      error: e => this.fail(e)
    });
  }

  changeStatus(): void {
    this.servicioDetalle.cambiarEstado(this.id, this.specialStatus, this.specialReason).subscribe({
      next: () => {
        this.specialReason = '';
        this.ok(`Caso ${this.specialStatus.toLowerCase()}.`);
      },
      error: e => this.fail(e)
    });
  }

  private guardarArchivo(blob: Blob, nombre: string): void {
    const url = URL.createObjectURL(blob);
    const enlace = document.createElement('a');
    enlace.href = url;
    enlace.download = nombre;
    enlace.click();
    URL.revokeObjectURL(url);
  }
}
