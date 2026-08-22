import { CommonModule } from '@angular/common';
import { Component, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';

import { Sucursal } from '../../../nucleo/modelos';
import { ServicioRegistrarCaso } from './registrar-caso.service';

@Component({
  selector: 'app-registrar-caso',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    RouterLink
  ],
  templateUrl: './registrar-caso.component.html',
  styleUrl: './registrar-caso.component.css'
})
export class ComponenteRegistrarCaso implements OnInit {

  branches: Sucursal[] = [];

  files: File[] = [];

  loading = false;

  error = '';

  created: any = null;

  maxNow = '';

  intentoValidar = false;

  mostrarConfirmacion = false;

  readonly maxArchivos = 5;

  // IMPORTANTE:
  // El máximo queda en 2 MB.
  readonly maxArchivoBytes =
    2 * 1024 * 1024;

  categories = [
    'PRODUCTO',
    'ATENCION',
    'ENTREGA',
    'COBRO',
    'HIGIENE',
    'INSTALACIONES',
    'OTRA'
  ];

  // =========================================================
  // DESCRIPCIÓN DE LOS TIPOS
  // =========================================================

  readonly descripcionesTipo:
    Record<string, string> = {

      QUEJA:
        'Expresa una inconformidad relacionada con la atención, producto o servicio recibido.',

      RECLAMO:
        'Solicita la revisión o corrección de una situación que afectó al cliente.',

      DENUNCIA:
        'Permite informar una situación que requiere investigación y puede manejarse de forma confidencial.',

      SUGERENCIA:
        'Permite proponer una mejora relacionada con la atención, productos, instalaciones o servicios.'
    };

  form: any = {

    type: 'QUEJA',

    category: 'PRODUCTO',

    branchId: null,

    incidentAt: '',

    anonymous: false,

    confidential: false,

    fullName: '',

    email: '',

    phone: '',

    orderNumber: '',

    description: '',

    contactAuthorized: false,

    privacyAccepted: false
  };

  constructor(private servicioRegistrarCaso: ServicioRegistrarCaso,private router:Router
  ) { }

  ngOnInit(): void {

    this.maxNow =
      this.localNow();

    this.form.incidentAt = this.maxNow;

    this.cargarSucursales();
  }
  
  // =========================================================
  // SUCURSALES
  // =========================================================

  cargarSucursales(): void {

    this.servicioRegistrarCaso
      .listarSucursales()
      .subscribe({

        next: sucursales => {

          this.branches =
            sucursales;
        },

        error: () => {

          this.error =
            'No se pudieron cargar las sucursales activas.';
        }
      });
  }

  // =========================================================
  // FECHA LOCAL
  // =========================================================

  localNow(): string {

    const fecha =
      new Date();

    const dosDigitos =
      (numero: number) =>
        String(numero)
          .padStart(2, '0');

    return (
      `${fecha.getFullYear()}-` +
      `${dosDigitos(fecha.getMonth() + 1)}-` +
      `${dosDigitos(fecha.getDate())}T` +
      `${dosDigitos(fecha.getHours())}:` +
      `${dosDigitos(fecha.getMinutes())}`
    );
  }

  // =========================================================
  // ETIQUETAS
  // =========================================================

  label(valor: string): string {

    return valor
      .replaceAll('_', ' ');
  }

  get descripcionTipo(): string {

    return this.descripcionesTipo[
      this.form.type
    ] ?? '';
  }

  get sucursalSeleccionada():
    Sucursal | undefined {

    return this.branches.find(
      sucursal =>
        sucursal.id ===
        this.form.branchId
    );
  }

  // =========================================================
  // FA01 - REGISTRO ANÓNIMO
  // =========================================================

  cambiarAnonimo(): void {

    if (!this.form.anonymous) {
      return;
    }

    /*
     * Si el registro es anónimo,
     * los datos personales se eliminan
     * del formulario.
     */
    this.form.fullName = '';

    this.form.email = '';

    this.form.phone = '';

    /*
     * Sin medio de contacto no debe
     * quedar autorización de contacto.
     */
    this.form.contactAuthorized =
      false;
  }

  // =========================================================
  // DENUNCIA CONFIDENCIAL
  // =========================================================

  cambiarTipo(): void {

    /*
     * Solo DENUNCIA puede ser
     * confidencial.
     */
    if (
      this.form.type !== 'DENUNCIA'
    ) {

      this.form.confidential =
        false;
    }
  }

  // =========================================================
  // FA02 - FA05
  // EVIDENCIAS
  // =========================================================

  pickFiles(evento: Event): void {

    const input = evento.target;
  
    // Confirmar que realmente sea un input HTML
    if (!(input instanceof HTMLInputElement)) {
      return;
    }
  
    const nuevos: File[] =
      Array.from(input.files ?? []);
  
    this.error = '';
  
    // Archivos que ya tenía + nuevos
    const total: File[] = [
      ...this.files,
      ...nuevos
    ];
  
    // =========================================================
    // MÁXIMO 5 ARCHIVOS
    // =========================================================
  
    if (total.length > this.maxArchivos) {
  
      this.error =
        'Solo puede adjuntar hasta 5 archivos.';
  
      input.value = '';
  
      return;
    }
  
    // =========================================================
    // FORMATOS PERMITIDOS
    // =========================================================
  
    const extensionPermitida =
      /\.(jpe?g|png|pdf)$/i;
  
    const mimePermitidos: string[] = [
      'image/jpeg',
      'image/png',
      'application/pdf'
    ];
  
    for (const archivo of nuevos) {
  
      // =======================================================
      // MÁXIMO 2 MB
      // =======================================================
  
      if (
        archivo.size >
        this.maxArchivoBytes
      ) {
  
        this.error =
          `${archivo.name} supera el tamaño máximo permitido de 2 MB.`;
  
        input.value = '';
  
        return;
      }
  
      // =======================================================
      // EXTENSIÓN
      // =======================================================
  
      if (
        !extensionPermitida.test(
          archivo.name
        )
      ) {
  
        this.error =
          `${archivo.name} tiene un formato no permitido.`;
  
        input.value = '';
  
        return;
      }
  
      // =======================================================
      // TIPO MIME
      // =======================================================
  
      if (
        archivo.type &&
        !mimePermitidos.includes(
          archivo.type
        )
      ) {
  
        this.error =
          `${archivo.name} tiene un tipo de archivo no permitido.`;
  
        input.value = '';
  
        return;
      }
    }
  
    // Todo correcto
    this.files = total;
  
    // Permite seleccionar nuevamente
    // el mismo archivo si luego se elimina.
    input.value = '';
  }

  // =========================================================
  // FA05 - ELIMINAR ARCHIVO PENDIENTE
  // =========================================================

  eliminarArchivo(
    indice: number
  ): void {

    this.files.splice(
      indice,
      1
    );

    this.files = [
      ...this.files
    ];
  }

  // =========================================================
  // VALIDACIONES DEL FORMULARIO
  // =========================================================

  validarFormulario(): boolean {

    this.error = '';

    this.intentoValidar =
      true;

    // Sucursal
    if (!this.form.branchId) {

      this.error =
        'Complete todos los campos obligatorios.';

      return false;
    }

    // Fecha
    if (!this.form.incidentAt) {

      this.error =
        'Complete todos los campos obligatorios.';

      return false;
    }

    // =====================================================
    // FA09 - FECHA FUTURA
    // =====================================================

    const fechaIncidente =
      new Date(
        this.form.incidentAt
      );

    if (
      fechaIncidente.getTime() >
      Date.now()
    ) {

      this.error =
        'La fecha del incidente no puede ser posterior a la fecha actual.';

      return false;
    }

    // =====================================================
    // REGISTRO IDENTIFICADO
    // =====================================================

    if (!this.form.anonymous) {

      // Nombre
      if (
        !this.form.fullName ||
        this.form.fullName
          .trim()
          .split(/\s+/)
          .length < 2
      ) {

        this.error =
          'Ingrese nombre y apellido.';

        return false;
      }

      // ===================================================
      // FA07 - CORREO
      // ===================================================

      const patronCorreo =
        /^[^\s@]+@[^\s@]+\.[^\s@]+$/;

      if (
        !this.form.email ||
        !patronCorreo.test(
          this.form.email.trim()
        )
      ) {

        this.error =
          'Ingrese un correo electrónico válido.';

        return false;
      }

      // ===================================================
      // FA08 - TELÉFONO
      // ===================================================

      if (
        this.form.phone &&
        !/^\d{8,15}$/.test(
          this.form.phone.trim()
        )
      ) {

        this.error =
          'El teléfono debe contener entre 8 y 15 dígitos.';

        return false;
      }
    }

    // =====================================================
    // FA10 - DESCRIPCIÓN
    // =====================================================

    const descripcion =
      this.form.description
        ?.trim() ?? '';

    if (
      descripcion.length < 20 ||
      descripcion.length > 3000
    ) {

      this.error =
        'La descripción debe contener entre 20 y 3000 caracteres.';

      return false;
    }

    // =====================================================
    // PRIVACIDAD
    // =====================================================

    if (
      !this.form.privacyAccepted
    ) {

      this.error =
        'Debe aceptar el aviso de privacidad para continuar.';

      return false;
    }

    return true;
  }

  // =========================================================
  // PASO 18
  // MOSTRAR RESUMEN ANTES DE REGISTRAR
  // =========================================================

  prepararConfirmacion(): void {

    if (
      !this.validarFormulario()
    ) {
      return;
    }

    this.error = '';

    this.mostrarConfirmacion =
      true;

    window.scrollTo({
      top: 0,
      behavior: 'smooth'
    });
  }

  // =========================================================
  // FA12 - REGRESAR A CORREGIR
  // =========================================================

  regresarFormulario(): void {

    this.mostrarConfirmacion =
      false;
  }

  // =========================================================
  // PASO 19
  // CONFIRMAR REGISTRO
  // =========================================================

  confirmarRegistro(): void {

    if (this.loading) {
      return;
    }

    /*
     * Volvemos a validar por seguridad,
     * porque algún dato pudo cambiar.
     */
    if (
      !this.validarFormulario()
    ) {

      this.mostrarConfirmacion =
        false;

      return;
    }

    if (
      this.form.type !==
      'DENUNCIA'
    ) {

      this.form.confidential =
        false;
    }

    const payload = {

      ...this.form,

      fullName:
        this.form.anonymous
          ? null
          : this.form.fullName.trim(),

      email:
        this.form.anonymous
          ? null
          : this.form.email.trim(),

      phone:
        this.form.anonymous
          ? null
          : (
            this.form.phone
              ?.trim() || ''
          ),

      contactAuthorized:
        this.form.anonymous
          ? false
          : this.form
            .contactAuthorized,

      orderNumber:
        this.form.orderNumber
          ?.trim() || null,

      description:
        this.form.description
          .trim(),

      incidentAt:
        this.form.incidentAt +
        ':00'
    };

    const datos =
      new FormData();

    datos.append(
      'data',
      new Blob(
        [
          JSON.stringify(
            payload
          )
        ],
        {
          type:
            'application/json'
        }
      )
    );

    this.files.forEach(
      archivo =>
        datos.append(
          'files',
          archivo
        )
    );

    this.loading = true;

    this.error = '';

    this.servicioRegistrarCaso
      .registrar(datos)
      .subscribe({

        next: respuesta => {

          this.created =
            respuesta;

          this.mostrarConfirmacion =
            false;

          this.loading =
            false;

          window.scrollTo({
            top: 0,
            behavior: 'smooth'
          });
        },

        error: e => {

          this.loading =
            false;

          // =================================================
          // FA16 - ERROR DE CONEXIÓN
          // =================================================

          if (
            e.status === 0 ||
            e.status === 503
          ) {

            this.error =
              'No fue posible conectar con el servidor. Los datos permanecen en el formulario para que pueda intentarlo nuevamente.';

            this.mostrarConfirmacion =
              false;

            return;
          }

          this.error =
            e?.error?.message ??
            'No fue posible registrar el caso.';

          // =================================================
          // FA11 / FA17
          // SUCURSAL CAMBIÓ O SE INACTIVÓ
          // =================================================

          if (
            this.error
              .toLowerCase()
              .includes(
                'sucursal'
              )
          ) {

            this.form.branchId =
              null;

            this.cargarSucursales();
          }

          this.mostrarConfirmacion =
            false;
        }
      });
  }

  // =========================================================
  // FA15 - CANCELAR REGISTRO
  // =========================================================

  cancelarRegistro(): void {

    const confirmar =
      window.confirm(
        '¿Está seguro de que desea cancelar el registro? Los datos ingresados se perderán.'
      );

    // Cliente seleccionó Cancelar
    if (!confirmar) {
      return;
    }

    /*
     * Los File están solamente en memoria
     * del navegador porque todavía no
     * fueron enviados al backend.
     */
    this.files = [];

    this.router.navigateByUrl('/');
  }

  // =========================================================
  // COMPROBANTE
  // =========================================================

  imprimirComprobante(): void {

    window.print();
  }

  descargarComprobante(): void {

    if (!this.created) {
      return;
    }

    const contenido = [
      'SISTEMA DE GESTIÓN DE QUEJAS',
      '',
      'COMPROBANTE DE REGISTRO',
      '',
      `Código de seguimiento: ${this.created.code}`,
      `Estado: ${this.created.status ?? 'PENDIENTE_ASIGNACION'}`,

      this.created.trackingKey
        ? `Clave temporal: ${this.created.trackingKey}`
        : '',

      '',
      'Conserve este comprobante para consultar posteriormente el estado de su caso.'
    ]
      .filter(linea => linea)
      .join('\n');

    const archivo =
      new Blob(
        [contenido],
        {
          type:
            'text/plain;charset=utf-8'
        }
      );

    const url =
      URL.createObjectURL(
        archivo
      );

    const enlace =
      document.createElement(
        'a'
      );

    enlace.href =
      url;

    enlace.download =
      `comprobante-${this.created.code}.txt`;

    enlace.click();

    URL.revokeObjectURL(
      url
    );
  }

  // =========================================================
  // FORMATEAR TAMAÑO
  // =========================================================

  formatBytes(
    tamano: number
  ): string {

    return tamano <
      1024 * 1024

      ? `${(
        tamano / 1024
      ).toFixed(0)} KB`

      : `${(
        tamano /
        1024 /
        1024
      ).toFixed(2)} MB`;
  }

  nombreInvalido(): boolean {
    return !this.form.anonymous &&
      (
        !this.form.fullName ||
        this.form.fullName.trim().split(/\s+/).length < 2
      );
  }
  
  correoInvalido(): boolean {
    if (this.form.anonymous) {
      return false;
    }
  
    const patron =
      /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
  
    return !this.form.email ||
      !patron.test(this.form.email.trim());
  }
  
  telefonoInvalido(): boolean {
    if (
      this.form.anonymous ||
      !this.form.phone
    ) {
      return false;
    }
  
    return !/^\d{8,15}$/.test(
      this.form.phone.trim()
    );
  }
  
  descripcionInvalida(): boolean {
    const descripcion =
      this.form.description?.trim() ?? '';
  
    return descripcion.length < 20 ||
      descripcion.length > 3000;
  }
}