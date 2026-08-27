import { CommonModule } from '@angular/common';
import { Component, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import Swal from 'sweetalert2';

import { VistaCasoPublico } from '../../../nucleo/modelos';
import { ServicioConsultarCaso } from './consultar-caso.service';

@Component({
  selector: 'app-consultar-caso',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    RouterLink
  ],
  templateUrl: './consultar-caso.component.html',
  styleUrl: './consultar-caso.component.css'
})
export class ComponenteConsultarCaso implements OnInit {

  // =========================================================
  // CONSULTA
  // =========================================================

  code = '';
  email = '';
  trackingKey = '';

  securityChallengeId = '';
  securityQuestion = '';
  securityAnswer = '';

  securityLoading = false;
  loading = false;

  caseData: VistaCasoPublico | null = null;

  // =========================================================
  // ACCIONES
  // =========================================================

  response = '';
  responseFiles: File[] = [];

  reason = '';

  reopenReason = '';

  rating = 5;
  ratingComment = '';

  // =========================================================
  // ARCHIVOS
  // =========================================================

  readonly maxArchivos = 5;

  // Máximo 2 MB por archivo
  readonly maxArchivoBytes = 2 * 1024 * 1024;

  readonly extensionesPermitidas = [
    'jpg',
    'jpeg',
    'png',
    'pdf'
  ];

  readonly tiposMimePermitidos = [
    'image/jpeg',
    'image/png',
    'application/pdf'
  ];

  constructor(
    private servicioConsultarCaso: ServicioConsultarCaso,
    private router: Router
  ) {}

  ngOnInit(): void {
    this.cargarVerificacion();
  }

  // =========================================================
  // VERIFICACIÓN
  // =========================================================

  cargarVerificacion(): void {

    this.securityLoading = true;

    this.securityChallengeId = '';
    this.securityQuestion = '';
    this.securityAnswer = '';

    this.servicioConsultarCaso
      .obtenerVerificacion()
      .subscribe({

        next: desafio => {

          this.securityChallengeId = desafio.id;
          this.securityQuestion = desafio.question;

          this.securityLoading = false;
        },

        error: e => {

          this.securityLoading = false;

          this.mostrarError(
            this.obtenerMensajeError(e)
          );
        }
      });
  }

  // =========================================================
  // ESTADOS
  // =========================================================

  get canCancel(): boolean {

    if (!this.caseData) {
      return false;
    }

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

    if (!this.caseData) {
      return false;
    }

    return [
      'CERRADO',
      'RECHAZADO',
      'CANCELADO'
    ].includes(this.caseData.status);
  }

  // =========================================================
  // CONSULTAR CASO
  // =========================================================

  lookup(): void {

    const codigo = this.code
      .trim()
      .toUpperCase();

    const correo = this.email.trim();

    const clave = this.trackingKey.trim();

    const respuestaSeguridad =
      String(this.securityAnswer ?? '').trim();

    // =======================================================
    // FA03 - AN02 No. 6
    // Complete todos los campos obligatorios.
    // =======================================================

    if (
      !codigo ||
      (!correo && !clave) ||
      !this.securityChallengeId ||
      !respuestaSeguridad
    ) {

      this.mostrarError(
        'Complete todos los campos obligatorios.'
      );

      return;
    }

    // =======================================================
    // FA04 - AN02 No. 15
    // =======================================================

    const patronCodigo =
      /^(QUE|REC|DEN|SUG)-\d{4}-\d{6}$/;

    if (!patronCodigo.test(codigo)) {

      this.mostrarError(
        'El código de seguimiento no existe o los datos de consulta son incorrectos.'
      );

      return;
    }

    // =======================================================
    // FA01 - AN02 No. 7
    // =======================================================

    if (
      correo &&
      !this.correoValido(correo)
    ) {

      this.mostrarError(
        'El correo electrónico ingresado no es válido.'
      );

      return;
    }

    this.loading = true;

    this.servicioConsultarCaso
      .consultar({

        code: codigo,

        email:
          correo || null,

        trackingKey:
          clave || null,

        securityChallengeId:
          this.securityChallengeId,

        securityAnswer:
          respuestaSeguridad

      })
      .subscribe({

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

          this.loading = false;

          this.mostrarError(
            this.obtenerMensajeError(e)
          );

          // Generar una nueva verificación
          // para el siguiente intento.
          this.cargarVerificacion();
        }
      });
  }

  // =========================================================
  // SELECCIONAR EVIDENCIAS
  // =========================================================

  pickResponseFiles(event: Event): void {

    const input =
      event.target as HTMLInputElement;

    const archivos =
      Array.from(input.files ?? []);

    // =======================================================
    // AN02 No. 13
    // Máximo de archivos permitido
    // =======================================================

    if (
      archivos.length >
      this.maxArchivos
    ) {

      this.limpiarArchivos(input);

      this.mostrarError(
        'Se alcanzó la cantidad máxima de archivos permitidos.'
      );

      return;
    }

    for (const archivo of archivos) {

      // =====================================================
      // AN02 No. 12
      // Máximo 2 MB
      // =====================================================

      if (
        archivo.size >
        this.maxArchivoBytes
      ) {

        this.limpiarArchivos(input);

        this.mostrarError(
          'El archivo supera el tamaño máximo permitido de 2 MB.'
        );

        return;
      }

      const extension =
        archivo.name
          .split('.')
          .pop()
          ?.toLowerCase() ?? '';

      // =====================================================
      // AN02 No. 11
      // Formato no permitido
      // =====================================================

      if (
        !this.extensionesPermitidas
          .includes(extension)
      ) {

        this.limpiarArchivos(input);

        this.mostrarError(
          'El formato del archivo no está permitido.'
        );

        return;
      }

      // =====================================================
      // Validación MIME
      // =====================================================

      if (
        archivo.type &&
        !this.tiposMimePermitidos
          .includes(archivo.type)
      ) {

        this.limpiarArchivos(input);

        this.mostrarError(
          'El formato del archivo no está permitido.'
        );

        return;
      }
    }

    this.responseFiles = archivos;
  }

  // =========================================================
  // RESPONDER SOLICITUD
  // FA11
  // =========================================================

  respond(): void {

    if (!this.caseData) {
      return;
    }

    // =======================================================
    // AN02 No. 16
    // =======================================================

    if (
      this.caseData.status !==
      'EN_ESPERA_CLIENTE'
    ) {

      this.mostrarError(
        this.mensajeOperacionNoPermitida(
          this.caseData.status
        )
      );

      return;
    }

    const respuesta =
      this.response.trim();

    // =======================================================
    // AN02 No. 6
    // =======================================================

    if (!respuesta) {

      this.mostrarError(
        'Complete todos los campos obligatorios.'
      );

      return;
    }

    const datos = new FormData();

    datos.append(
      'data',
      new Blob(
        [
          JSON.stringify({
            response: respuesta
          })
        ],
        {
          type: 'application/json'
        }
      )
    );

    this.responseFiles
      .forEach(archivo => {

        datos.append(
          'files',
          archivo
        );
      });

    this.loading = true;

    this.servicioConsultarCaso
      .responder(
        this.caseData.code,
        datos,
        this.cleanParams()
      )
      .subscribe({

        next: caso => {

          this.caseData = caso;

          this.response = '';
          this.responseFiles = [];

          this.loading = false;

          // ===============================================
          // AN01 No. 6
          // ===============================================

          this.mostrarExito(
            'El seguimiento se registró con éxito.'
          );
        },

        error: e => {

          this.loading = false;

          this.mostrarError(
            this.obtenerMensajeError(e)
          );
        }
      });
  }

  // =========================================================
  // CANCELAR CASO
  // FA10
  // =========================================================

  async cancel(): Promise<void> {

    if (!this.caseData) {
      return;
    }

    // =======================================================
    // AN02 No. 16
    // =======================================================

    if (!this.canCancel) {

      this.mostrarError(
        this.mensajeOperacionNoPermitida(
          this.caseData.status
        )
      );

      return;
    }

    const motivo =
      this.reason.trim();

    // =======================================================
    // AN02 No. 6
    // =======================================================

    if (!motivo) {

      this.mostrarError(
        'Complete todos los campos obligatorios.'
      );

      return;
    }

    // =======================================================
    // Confirmación solicitada por FA10
    // =======================================================

    const resultado =
      await Swal.fire({

        icon: 'warning',

        title: '¿Cancelar el caso?',

        html:
          `Está a punto de cancelar el caso ` +
          `<strong>${this.caseData.code}</strong>.` +
          `<br><br>` +
          `Esta acción cambiará el estado del caso a cancelado.`,

        showCancelButton: true,

        confirmButtonText:
          'Sí, cancelar caso',

        cancelButtonText:
          'No, regresar',

        reverseButtons: true,

        focusCancel: true,

        allowOutsideClick: false
      });

    if (!resultado.isConfirmed) {
      return;
    }

    this.loading = true;

    this.servicioConsultarCaso
      .cancelar(
        this.caseData.code,
        motivo,
        this.cleanParams()
      )
      .subscribe({

        next: caso => {

          this.caseData = caso;

          this.reason = '';

          this.loading = false;

          // ===============================================
          // AN01 No. 9
          // ===============================================

          this.mostrarExito(
            'El estado del caso se actualizó con éxito.'
          );
        },

        error: e => {

          this.loading = false;

          this.mostrarError(
            this.obtenerMensajeError(e)
          );
        }
      });
  }

  // =========================================================
  // SOLICITAR REAPERTURA
  // FA13 / FA14
  // =========================================================

  requestReopen(): void {

    if (!this.caseData) {
      return;
    }

    // =======================================================
    // AN02 No. 16
    // =======================================================

    if (
      this.caseData.status !==
      'CERRADO'
    ) {

      this.mostrarError(
        this.mensajeOperacionNoPermitida(
          this.caseData.status
        )
      );

      return;
    }

    // =======================================================
    // AN02 No. 23
    // =======================================================

    if (
      !this.caseData.canRequestReopen &&
      !this.caseData.reopenRequested
    ) {

      this.mostrarError(
        'El plazo ordinario para reabrir el caso ha vencido.'
      );

      return;
    }

    if (
      this.caseData.reopenRequested
    ) {

      this.mostrarError(
        'La solicitud de reapertura ya fue registrada y se encuentra pendiente de revisión.'
      );

      return;
    }

    const motivo =
      this.reopenReason.trim();

    // =======================================================
    // AN02 No. 6
    // =======================================================

    if (!motivo) {

      this.mostrarError(
        'Complete todos los campos obligatorios.'
      );

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

          // ===============================================
          // AN01 No. 20
          // ===============================================

          this.mostrarExito(
            'La operación se realizó con éxito.'
          );
        },

        error: e => {

          this.loading = false;

          this.mostrarError(
            this.obtenerMensajeError(e)
          );
        }
      });
  }

  // =========================================================
  // CALIFICAR
  // =========================================================

  rate(): void {

    if (!this.caseData) {
      return;
    }

    if (
      this.caseData.status !==
      'CERRADO'
    ) {

      this.mostrarError(
        this.mensajeOperacionNoPermitida(
          this.caseData.status
        )
      );

      return;
    }

    if (
      this.rating < 1 ||
      this.rating > 5
    ) {

      this.mostrarError(
        'Complete todos los campos obligatorios.'
      );

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

          // ===============================================
          // AN01 No. 20
          // ===============================================

          this.mostrarExito(
            'La operación se realizó con éxito.'
          );
        },

        error: e => {

          this.loading = false;

          this.mostrarError(
            this.obtenerMensajeError(e)
          );
        }
      });
  }

  // =========================================================
  // DESCARGAR EVIDENCIA
  // FA12
  // =========================================================

  download(
    id: number,
    name: string
  ): void {

    if (!this.caseData) {
      return;
    }

    this.servicioConsultarCaso
      .descargarEvidencia(
        this.caseData.code,
        id,
        this.cleanParams()
      )
      .subscribe({

        next: archivo => {

          this.saveBlob(
            archivo,
            name
          );

          // ===============================================
          // AN01 No. 18
          // ===============================================

          this.mostrarExito(
            'El archivo se descargó con éxito.'
          );
        },

        error: e => {

          this.mostrarError(
            this.obtenerMensajeError(e)
          );
        }
      });
  }

  // =========================================================
  // FINALIZAR CONSULTA
  // =========================================================

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

    this.router.navigate(['/']);
  }

  // =========================================================
  // PARÁMETROS DE VERIFICACIÓN
  // =========================================================

  cleanParams(): Record<string, unknown> {

    const parametros:
      Record<string, unknown> = {};

    const correo =
      this.email.trim();

    const clave =
      this.trackingKey.trim();

    if (correo) {

      parametros['email'] =
        correo;
    }

    if (clave) {

      parametros['trackingKey'] =
        clave;
    }

    return parametros;
  }

  // =========================================================
  // SWEETALERT - ÉXITO
  // =========================================================

  private mostrarExito(
    mensaje: string
  ): void {

    Swal.fire({

      icon: 'success',

      title: 'Operación exitosa',

      text: mensaje,

      confirmButtonText: 'Aceptar',

      allowOutsideClick: false
    });
  }

  // =========================================================
  // SWEETALERT - ERROR
  // =========================================================

  private mostrarError(
    mensaje: string
  ): void {

    Swal.fire({

      icon: 'error',

      title: 'No fue posible realizar la operación',

      text: mensaje,

      confirmButtonText: 'Aceptar',

      allowOutsideClick: false
    });
  }

  // =========================================================
  // LIMPIAR ARCHIVOS
  // =========================================================

  private limpiarArchivos(
    input: HTMLInputElement
  ): void {

    input.value = '';

    this.responseFiles = [];
  }

  // =========================================================
  // AN02 No. 16
  // =========================================================

  private mensajeOperacionNoPermitida(
    estado: string
  ): string {

    return (
      `El caso se encuentra en estado ` +
      `${this.estadoTexto(estado)} ` +
      `y no permite esta operación.`
    );
  }

  // =========================================================
  // MANEJO DE ERRORES DEL BACKEND
  // =========================================================

  private obtenerMensajeError(
    e: any
  ): string {

    // =======================================================
    // AN02 No. 30
    // Error de conexión
    // =======================================================

    if (
      e?.status === 0
    ) {

      return (
        'Error de conexión con el servidor.'
      );
    }

    // =======================================================
    // Mensaje controlado enviado por backend.
    //
    // Ejemplos AN02:
    // No. 15 código incorrecto
    // No. 16 estado incorrecto
    // No. 22 caso cerrado
    // No. 23 plazo vencido
    // No. 28 notificación
    // No. 32 conflicto de información
    // =======================================================

    const mensajeBackend =
      e?.error?.message;

    if (
      typeof mensajeBackend ===
        'string' &&
      mensajeBackend.trim()
    ) {

      return mensajeBackend.trim();
    }

    // Algunos backends pueden devolver:
    // {
    //   error: "mensaje"
    // }

    const errorBackend =
      e?.error?.error;

    if (
      typeof errorBackend ===
        'string' &&
      errorBackend.trim()
    ) {

      return errorBackend.trim();
    }

    // Si el backend devuelve directamente texto.
    if (
      typeof e?.error ===
        'string' &&
      e.error.trim()
    ) {

      return e.error.trim();
    }

    // =======================================================
    // AN02 No. 31
    // =======================================================

    return (
      'Error interno del sistema. Intente nuevamente.'
    );
  }

  // =========================================================
  // TEXTO DE ESTADOS
  // =========================================================

  private estadoTexto(
    estado: string
  ): string {

    return estado.replaceAll(
      '_',
      ' '
    );
  }

  // =========================================================
  // DESCARGAR BLOB
  // =========================================================

  private saveBlob(
    blob: Blob,
    name: string
  ): void {

    const url =
      URL.createObjectURL(blob);

    const enlace =
      document.createElement('a');

    enlace.href = url;

    enlace.download =
      name || 'archivo';

    document.body
      .appendChild(enlace);

    enlace.click();

    document.body
      .removeChild(enlace);

    URL.revokeObjectURL(url);
  }

  // =========================================================
  // VALIDAR CORREO
  // =========================================================

  private correoValido(
    correo: string
  ): boolean {

    return /^[^\s@]+@[^\s@]+\.[^\s@]+$/
      .test(correo);
  }
}