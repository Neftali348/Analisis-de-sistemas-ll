import { CommonModule } from '@angular/common';
import { Component, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';

import { VistaCasoPublico } from '../../../nucleo/modelos';
import { ServicioConsultarCaso } from './consultar-caso.service';

@Component({
  selector: 'app-consultar-caso',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './consultar-caso.component.html',
  styleUrl: './consultar-caso.component.css'
})
export class ComponenteConsultarCaso implements OnInit {

  // ==================== CONSULTA ====================

  code = '';
  email = '';
  trackingKey = '';

  securityChallengeId = '';
  securityQuestion = '';
  securityAnswer = '';
  securityLoading = false;

  loading = false;
  error = '';
  success = '';

  caseData: VistaCasoPublico | null = null;

  // ==================== ACCIONES ====================

  response = '';
  responseFiles: File[] = [];

  reason = '';
  reopenReason = '';

  rating = 5;
  ratingComment = '';

  // ==================== ARCHIVOS ====================

  readonly maxArchivos = 5;
  readonly maxArchivoBytes = 2 * 1024 * 1024;

  readonly extensionesPermitidas = ['jpg', 'jpeg', 'png', 'pdf'];
  readonly tiposMimePermitidos = ['image/jpeg', 'image/png', 'application/pdf'];

  constructor(
    private servicioConsultarCaso: ServicioConsultarCaso,
    private router: Router
  ) {}

  ngOnInit(): void {
    this.cargarVerificacion();
  }

  // ==================== VERIFICACIÓN ====================

  cargarVerificacion(): void {
    this.securityLoading = true;
    this.securityChallengeId = '';
    this.securityQuestion = '';
    this.securityAnswer = '';

    this.servicioConsultarCaso.obtenerVerificacion().subscribe({
      next: desafio => {
        this.securityChallengeId = desafio.id;
        this.securityQuestion = desafio.question;
        this.securityLoading = false;
      },
      error: e => {
        this.securityLoading = false;
        this.error = this.obtenerMensajeError(e);
      }
    });
  }

  // ==================== ESTADOS ====================

  get canCancel(): boolean {
    if (!this.caseData) return false;

    return [
      'REGISTRADO',
      'PENDIENTE_ASIGNACION',
      'ASIGNADO',
      'EN_PROCESO'
    ].includes(this.caseData.status);
  }

  get canRespond(): boolean {
    return this.caseData?.status === 'EN_ESPERA_CLIENTE';
  }

  get isResolved(): boolean {
    return this.caseData?.status === 'RESUELTO';
  }

  get isClosed(): boolean {
    return this.caseData?.status === 'CERRADO';
  }

  get isFinalState(): boolean {
    if (!this.caseData) return false;

    return ['CERRADO', 'RECHAZADO', 'CANCELADO']
      .includes(this.caseData.status);
  }

  // ==================== CONSULTAR CASO ====================

  lookup(): void {
    this.limpiarMensajes();

    const codigo = this.code.trim().toUpperCase();
    const correo = this.email.trim();
    const clave = this.trackingKey.trim();
    const respuestaSeguridad = String(this.securityAnswer ?? '').trim();

    // FA03 - AN02 No. 6
    if (
      !codigo ||
      (!correo && !clave) ||
      !this.securityChallengeId ||
      !respuestaSeguridad
    ) {
      this.error = 'Complete todos los campos obligatorios.';
      return;
    }

    // FA04 - AN02 No. 15
    const patronCodigo = /^(QUE|REC|DEN|SUG)-\d{4}-\d{6}$/;

    if (!patronCodigo.test(codigo)) {
      this.error =
        'El código de seguimiento no existe o los datos de consulta son incorrectos.';
      return;
    }

    // FA01 - AN02 No. 7
    if (correo && !this.correoValido(correo)) {
      this.error = 'El correo electrónico ingresado no es válido.';
      return;
    }

    this.loading = true;

    this.servicioConsultarCaso.consultar({
      code: codigo,
      email: correo || null,
      trackingKey: clave || null,
      securityChallengeId: this.securityChallengeId,
      securityAnswer: respuestaSeguridad
    }).subscribe({
      next: caso => {
        this.caseData = caso;
        this.code = caso.code;
        this.loading = false;

        // El desafío es de un solo uso
        this.securityChallengeId = '';
        this.securityQuestion = '';
        this.securityAnswer = '';
      },
      error: e => {
        this.caseData = null;
        this.error = this.obtenerMensajeError(e);
        this.loading = false;

        // Generar otra verificación para un nuevo intento
        this.cargarVerificacion();
      }
    });
  }

  // ==================== EVIDENCIAS ====================

  pickResponseFiles(event: Event): void {
    this.limpiarMensajes();

    const input = event.target as HTMLInputElement;
    const archivos = Array.from(input.files ?? []);

    // AN02 No. 13
    if (archivos.length > this.maxArchivos) {
      this.error = 'Se alcanzó la cantidad máxima de archivos permitidos.';
      input.value = '';
      this.responseFiles = [];
      return;
    }

    for (const archivo of archivos) {

      // AN02 No. 12
      if (archivo.size > this.maxArchivoBytes) {
        this.error = 'El archivo supera el tamaño máximo permitido de 10 MB.';
        input.value = '';
        this.responseFiles = [];
        return;
      }

      const extension = archivo.name.split('.').pop()?.toLowerCase() ?? '';

      // AN02 No. 11
      if (!this.extensionesPermitidas.includes(extension)) {
        this.error = 'El formato del archivo no está permitido.';
        input.value = '';
        this.responseFiles = [];
        return;
      }

      // AN02 No. 11
      if (
        archivo.type &&
        !this.tiposMimePermitidos.includes(archivo.type)
      ) {
        this.error = 'El formato del archivo no está permitido.';
        input.value = '';
        this.responseFiles = [];
        return;
      }
    }

    this.responseFiles = archivos;
  }

  // ==================== RESPONDER ====================

  respond(): void {
    this.limpiarMensajes();

    if (!this.caseData) return;

    // AN02 No. 16
    if (this.caseData.status !== 'EN_ESPERA_CLIENTE') {
      this.error = this.mensajeOperacionNoPermitida(this.caseData.status);
      return;
    }

    const respuesta = this.response.trim();

    // AN02 No. 6
    if (!respuesta) {
      this.error = 'Complete todos los campos obligatorios.';
      return;
    }

    const datos = new FormData();

    datos.append(
      'data',
      new Blob(
        [JSON.stringify({ response: respuesta })],
        { type: 'application/json' }
      )
    );

    this.responseFiles.forEach(archivo => {
      datos.append('files', archivo);
    });

    this.loading = true;

    this.servicioConsultarCaso
      .responder(this.caseData.code, datos, this.cleanParams())
      .subscribe({
        next: caso => {
          this.caseData = caso;
          this.response = '';
          this.responseFiles = [];
          this.loading = false;

          // AN01 No. 6
          this.success = 'El seguimiento se registró con éxito.';
        },
        error: e => {
          this.error = this.obtenerMensajeError(e);
          this.loading = false;
        }
      });
  }

  // ==================== CANCELAR ====================

  cancel(): void {
    this.limpiarMensajes();

    if (!this.caseData) return;

    // AN02 No. 16
    if (!this.canCancel) {
      this.error = this.mensajeOperacionNoPermitida(this.caseData.status);
      return;
    }

    const motivo = this.reason.trim();

    // AN02 No. 6
    if (!motivo) {
      this.error = 'Complete todos los campos obligatorios.';
      return;
    }

    const confirmado = window.confirm(
      `¿Está seguro de cancelar el caso ${this.caseData.code}?`
    );

    if (!confirmado) return;

    this.loading = true;

    this.servicioConsultarCaso
      .cancelar(this.caseData.code, motivo, this.cleanParams())
      .subscribe({
        next: caso => {
          this.caseData = caso;
          this.reason = '';
          this.loading = false;

          // AN01 No. 9
          this.success = 'El estado del caso se actualizó con éxito.';
        },
        error: e => {
          this.error = this.obtenerMensajeError(e);
          this.loading = false;
        }
      });
  }

  // ==================== REAPERTURA ====================

  requestReopen(): void {
    this.limpiarMensajes();

    if (!this.caseData) return;

    // AN02 No. 16
    if (this.caseData.status !== 'CERRADO') {
      this.error = this.mensajeOperacionNoPermitida(this.caseData.status);
      return;
    }

    // AN02 No. 23
    if (
      !this.caseData.canRequestReopen &&
      !this.caseData.reopenRequested
    ) {
      this.error =
        'El plazo ordinario para reabrir el caso ha vencido.';
      return;
    }

    if (this.caseData.reopenRequested) {
      this.error = this.mensajeOperacionNoPermitida(this.caseData.status);
      return;
    }

    const motivo = this.reopenReason.trim();

    // AN02 No. 6
    if (!motivo) {
      this.error = 'Complete todos los campos obligatorios.';
      return;
    }

    this.loading = true;

    this.servicioConsultarCaso
      .solicitarReapertura(
        this.caseData.code,
        motivo,
        this.cleanParams()
      )
      .subscribe({
        next: () => {
          this.reopenReason = '';
          this.loading = false;

          if (this.caseData) {
            this.caseData = {
              ...this.caseData,
              canRequestReopen: false,
              reopenRequested: true
            };
          }

          // AN01 No. 20
          this.success = 'La operación se realizó con éxito.';
        },
        error: e => {
          this.error = this.obtenerMensajeError(e);
          this.loading = false;
        }
      });
  }

  // ==================== CALIFICACIÓN ====================

  rate(): void {
    this.limpiarMensajes();

    if (!this.caseData) return;

    if (this.caseData.status !== 'CERRADO') {
      this.error = this.mensajeOperacionNoPermitida(this.caseData.status);
      return;
    }

    if (this.rating < 1 || this.rating > 5) {
      this.error = 'Complete todos los campos obligatorios.';
      return;
    }

    this.loading = true;

    this.servicioConsultarCaso
      .calificar(
        this.caseData.code,
        this.rating,
        this.ratingComment.trim() || null,
        this.cleanParams()
      )
      .subscribe({
        next: () => {
          this.ratingComment = '';
          this.loading = false;

          // AN01 No. 20
          this.success = 'La operación se realizó con éxito.';
        },
        error: e => {
          this.error = this.obtenerMensajeError(e);
          this.loading = false;
        }
      });
  }

  // ==================== DESCARGAR ====================

  download(id: number, name: string): void {
    this.limpiarMensajes();

    if (!this.caseData) return;

    this.servicioConsultarCaso
      .descargarEvidencia(
        this.caseData.code,
        id,
        this.cleanParams()
      )
      .subscribe({
        next: archivo => {
          this.saveBlob(archivo, name);

          // AN01 No. 18
          this.success = 'El archivo se descargó con éxito.';
        },
        error: e => {
          this.error = this.obtenerMensajeError(e);
        }
      });
  }

  // ==================== FINALIZAR ====================

  finalizeConsultation(): void {
    this.caseData = null;

    this.code = '';
    this.email = '';
    this.trackingKey = '';

    this.securityChallengeId = '';
    this.securityQuestion = '';
    this.securityAnswer = '';

    this.response = '';
    this.responseFiles = [];

    this.reason = '';
    this.reopenReason = '';

    this.rating = 5;
    this.ratingComment = '';

    this.error = '';
    this.success = '';

    this.router.navigate(['/']);
  }

  // ==================== UTILIDADES ====================

  cleanParams(): Record<string, unknown> {
    const parametros: Record<string, unknown> = {};

    const correo = this.email.trim();
    const clave = this.trackingKey.trim();

    if (correo) parametros['email'] = correo;
    if (clave) parametros['trackingKey'] = clave;

    return parametros;
  }

  private limpiarMensajes(): void {
    this.error = '';
    this.success = '';
  }

  private mensajeOperacionNoPermitida(estado: string): string {
    return `El caso se encuentra en estado ${this.estadoTexto(estado)} y no permite esta operación.`;
  }

  private obtenerMensajeError(e: any): string {
    // AN02 No. 30
    if (e?.status === 0) {
      return 'Error de conexión con el servidor.';
    }

    // Respetar mensaje controlado enviado por backend
    const mensajeBackend = e?.error?.message;

    if (
      typeof mensajeBackend === 'string' &&
      mensajeBackend.trim()
    ) {
      return mensajeBackend.trim();
    }

    // AN02 No. 31
    return 'Error interno del sistema. Intente nuevamente.';
  }

  private estadoTexto(estado: string): string {
    return estado.replaceAll('_', ' ');
  }

  private saveBlob(blob: Blob, name: string): void {
    const url = URL.createObjectURL(blob);
    const enlace = document.createElement('a');

    enlace.href = url;
    enlace.download = name || 'archivo';

    document.body.appendChild(enlace);
    enlace.click();
    document.body.removeChild(enlace);

    URL.revokeObjectURL(url);
  }

  private correoValido(correo: string): boolean {
    return /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(correo);
  }
}