import { CommonModule } from '@angular/common';
import { Component, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, RouterLink } from '@angular/router';
import Swal from 'sweetalert2';
import { Sucursal, VistaCaso } from '../../../nucleo/modelos';
import { ServicioAutenticacion } from '../../../nucleo/servicio-autenticacion';
import { AgenteDisponible, DisponibilidadEvidencia, FiltrosAgentes, ServicioDetalleCaso, TipoAsociacionEvidencia } from './detalle-caso.service';
type EstadoEspecial = 'RECHAZADO' | 'CANCELADO';
type PanelAccion = 'EDITAR' | 'ASIGNAR' | 'ESTADO' | 'SEGUIMIENTO' | 'EVIDENCIA' | 'RESOLUCION' | 'CERRAR' | 'REABRIR' | 'ESPECIAL' | null;
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
    branches: Sucursal[] = [];
    agents: AgenteDisponible[] = [];
    loading = false;
    operating = false;
    panelActivo: PanelAccion = null;
    error = '';
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
    readonly maxArchivos = 5;
    readonly maxArchivoBytes = 2 * 1024 * 1024;
    readonly extensionesPermitidas = ['jpg', 'jpeg', 'png', 'pdf'];
    readonly tiposMimePermitidos = [
        'image/jpeg',
        'image/png',
        'application/pdf'
    ];
    readonly types = ['QUEJA', 'RECLAMO', 'DENUNCIA', 'SUGERENCIA'];
    readonly categories = [
        'PRODUCTO',
        'ATENCION',
        'ENTREGA',
        'COBRO',
        'HIGIENE',
        'INSTALACIONES',
        'OTRA'
    ];
    readonly priorities = [
        'BAJA',
        'MEDIA',
        'ALTA',
        'CRITICA'
    ];
    readonly followTypes = [
        'COMENTARIO_INTERNO',
        'RESPUESTA_CLIENTE',
        'SOLICITUD_INFORMACION',
        'ACCION_CORRECTIVA'
    ];
    readonly closeReasons = [
        'SOLUCIONADO',
        'IMPROCEDENTE',
        'DUPLICADO',
        'CLIENTE_NO_RESPONDIO',
        'CANCELADO',
        'OTRO'
    ];
    assign = {
        responsibleId: null as number | null,
        reason: ''
    };
    agentFilters = {
        q: '',
        branchId: '' as number | '',
        category: '',
        available: '' as boolean | '',
        maxOpenCases: '' as number | ''
    };
    agentsLoading = false;
    edit = {
        type: '',
        branchId: null as number | null,
        category: '',
        priority: '',
        orderNumber: '',
        administrativeObservation: ''
    };
    newPriority = 'MEDIA';
    follow = {
        type: 'COMENTARIO_INTERNO',
        description: '',
        visibleToClient: false,
        newStatus: null as string | null
    };
    followFiles: File[] = [];
    evidenceDescription = '';
    evidenceAssociationType: TipoAsociacionEvidencia = 'CASO';
    evidenceFollowUpId: number | null = null;
    evidenceFile: File | null = null;
    evidenceVisible = false;
    evidenceAvailability: DisponibilidadEvidencia | null = null;
    evidenceAvailabilityLoading = false;
    resolution = '';
    close = {
        reason: 'SOLUCIONADO',
        summary: '',
        internalObservation: '',
        detailReason: '',
        duplicateCaseCode: '',
        notifyClient: true,
        sendSurvey: true,
        criticalReviewConfirmed: false
    };
    reopen = {
        reason: '',
        specialJustification: false
    };
    normalStatus = '';
    normalStatusReason = '';
    specialStatus: EstadoEspecial = 'RECHAZADO';
    specialReason = '';
    constructor(private route: ActivatedRoute, private servicioDetalle: ServicioDetalleCaso, public auth: ServicioAutenticacion) { }
    ngOnInit(): void {
        this.id = Number(this.route.snapshot.paramMap.get('id'));
        if (!Number.isFinite(this.id) || this.id <= 0) {
            this.mostrarError('Error interno del sistema. Intente nuevamente.');
            return;
        }
        this.cargarSucursales();
        this.load();
    }
    private cargarSucursales(): void {
        this.servicioDetalle.listarSucursales().subscribe({
            next: sucursales => this.branches = sucursales,
            error: e => this.mostrarErrorHttp(e)
        });
    }
    load(): void {
        if (this.loading)
            return;
        this.error = '';
        this.loading = true;
        this.servicioDetalle.obtenerCaso(this.id).subscribe({
            next: caso => {
                this.c = caso;
                this.loading = false;
                this.panelActivo = null;
                this.newPriority = caso.priority ?? 'MEDIA';
                this.edit = {
                    type: caso.type ?? '',
                    branchId: caso.branchId ?? null,
                    category: caso.category ?? '',
                    priority: caso.priority ?? 'MEDIA',
                    orderNumber: caso.orderNumber ?? '',
                    administrativeObservation: caso.administrativeObservation ?? ''
                };
                this.normalStatus = '';
                this.normalStatusReason = '';
                const tieneContactoAutorizado = caso.anonymous !== true &&
                    caso.contactAuthorized === true &&
                    (!!caso.email || !!caso.phone);
                this.close.notifyClient = tieneContactoAutorizado;
                this.close.sendSurvey = tieneContactoAutorizado;
                this.agents = [];
                this.assign = {
                    responsibleId: null,
                    reason: ''
                };
            },
            error: e => {
                this.loading = false;
                this.mostrarErrorHttp(e);
            }
        });
    }
    loadAgents(): void {
        if (!this.puedeAsignarResponsable || this.agentsLoading) {
            return;
        }
        const filtros: FiltrosAgentes = {};
        const q = this.agentFilters.q.trim();
        if (q) {
            filtros.q = q;
        }
        if (this.agentFilters.branchId !== '') {
            filtros.branchId = this.agentFilters.branchId;
        }
        if (this.agentFilters.category) {
            filtros.category = this.agentFilters.category;
        }
        if (this.agentFilters.available !== '') {
            filtros.available = this.agentFilters.available;
        }
        if (this.agentFilters.maxOpenCases !== '') {
            filtros.maxOpenCases = this.agentFilters.maxOpenCases;
        }
        this.agentsLoading = true;
        this.servicioDetalle.listarAgentes(this.id, filtros).subscribe({
            next: agentes => {
                this.agents = agentes;
                this.agentsLoading = false;
                if (this.assign.responsibleId != null &&
                    !this.agents.some(a => a.id === this.assign.responsibleId)) {
                    this.assign.responsibleId = null;
                }
            },
            error: e => {
                this.agents = [];
                this.agentsLoading = false;
                this.mostrarErrorHttp(e);
            }
        });
    }
    buscarAgentes(): void {
        this.loadAgents();
    }
    limpiarFiltrosAgentes(): void {
        this.agentFilters = {
            q: '',
            branchId: '',
            category: '',
            available: '',
            maxOpenCases: ''
        };
        this.assign.responsibleId = null;
        this.assign.reason = '';
        this.loadAgents();
    }
    seleccionarAgente(agente: AgenteDisponible): void {
        if (!agente.available) {
            this.mostrarValidacion('El responsable seleccionado no se encuentra disponible.');
            return;
        }
        if (this.c?.responsibleId != null &&
            this.c.responsibleId === agente.id) {
            this.mostrarValidacion('Seleccione un responsable diferente al actual');
            return;
        }
        this.assign.responsibleId = agente.id;
    }
    get agenteSeleccionado(): AgenteDisponible | null {
        if (this.assign.responsibleId == null) {
            return null;
        }
        return this.agents.find(a => a.id === this.assign.responsibleId) ?? null;
    }
    get esSoloLectura(): boolean {
        return this.c?.status === 'CERRADO';
    }
    get puedeEditar(): boolean {
        return !!this.c &&
            this.auth.has('CASE_UPDATE') &&
            !this.esSoloLectura;
    }
    get puedeAsignarResponsable(): boolean {
        return !!this.c &&
            this.auth.has('CASE_ASSIGN') &&
            !['CERRADO', 'CANCELADO', 'RECHAZADO'].includes(this.c.status);
    }
    get puedeRegistrarSeguimiento(): boolean {
        return !!this.c &&
            this.auth.has('CASE_FOLLOWUP') &&
            !['CERRADO', 'CANCELADO', 'RECHAZADO'].includes(this.c.status);
    }
    get puedeAdjuntarEvidencia(): boolean {
        return !!this.c &&
            this.auth.has('EVIDENCE_UPLOAD') &&
            !['CERRADO', 'CANCELADO', 'RECHAZADO'].includes(this.c.status);
    }
    get puedeResolver(): boolean {
        return !!this.c &&
            this.auth.has('CASE_RESOLVE') &&
            ['EN_PROCESO', 'EN_ESPERA_CLIENTE'].includes(this.c.status);
    }
    get motivosBloqueoCierre(): string[] {
        if (!this.c) return ['No existe información del caso.'];
        const motivos: string[] = [], d = this.datosCaso;
        if (this.c.status !== 'RESUELTO') motivos.push(`El caso se encuentra en estado ${this.label(this.c.status)} y no permite esta operación.`);
        if (this.c.responsibleId == null || d?.responsibleActive === false) motivos.push('El caso debe contar con un responsable activo antes de cerrarse.');
        if ((this.c.followUps?.length ?? 0) === 0) motivos.push('Debe registrar al menos un seguimiento antes de cerrar el caso.');
        if (!this.c.resolution?.trim()) motivos.push('Debe registrar una resolución antes de cerrar el caso.');
        if (d?.hasPendingCorrectiveActions === true || Number(d?.pendingCorrectiveActions ?? 0) > 0) motivos.push('El caso contiene acciones pendientes y no puede cerrarse.');
        if (d?.pendingReopenRequest === true || d?.hasPendingReopenRequest === true) motivos.push('Existe una solicitud de reapertura pendiente de revisión.');
        if (d?.caseScopeAllowed === false || d?.closeScopeAllowed === false) motivos.push('No posee permisos para realizar esta acción.');
        if (this.c.confidential === true && d?.confidentialCloseAllowed === false) motivos.push('No posee permisos para realizar esta acción.');
        return [...new Set(motivos)];
    }
    get puedeCerrar(): boolean {
        return !!this.c &&
            this.auth.has('CASE_CLOSE') &&
            this.motivosBloqueoCierre.length === 0;
    }
    get requiereRevisionCriticaCierre(): boolean {
        return !!this.c &&
            (this.c.priority === 'CRITICA' || this.c.confidential === true);
    }
    get puedeReabrir(): boolean {
        return !!this.c &&
            this.auth.has('CASE_REOPEN') &&
            ['RESUELTO', 'CERRADO', 'RECHAZADO', 'CANCELADO'].includes(this.c.status);
    }
    get puedeCambiarEstadoNormal(): boolean {
        return !!this.c &&
            this.auth.has('CASE_UPDATE') &&
            this.estadosCambioNormal.length > 0;
    }
    get puedeRechazarOCancelar(): boolean {
        return !!this.c &&
            this.auth.has('CASE_UPDATE') &&
            this.opcionesEstadoEspecial.length > 0;
    }
    get estadosCambioNormal(): string[] {
        if (!this.c)
            return [];
        switch (this.c.status) {
            case 'REGISTRADO':
                return ['PENDIENTE_ASIGNACION'];
            case 'PENDIENTE_ASIGNACION':
                return ['ASIGNADO'];
            case 'ASIGNADO':
                return ['EN_PROCESO'];
            case 'EN_PROCESO':
                return ['EN_ESPERA_CLIENTE'];
            case 'EN_ESPERA_CLIENTE':
                return ['EN_PROCESO'];
            case 'REABIERTO':
                return ['ASIGNADO', 'EN_PROCESO'];
            default:
                return [];
        }
    }
    get opcionesEstadoEspecial(): EstadoEspecial[] {
        if (!this.c)
            return [];
        switch (this.c.status) {
            case 'REGISTRADO':
                return ['CANCELADO'];
            case 'PENDIENTE_ASIGNACION':
            case 'ASIGNADO':
            case 'EN_PROCESO':
                return ['RECHAZADO', 'CANCELADO'];
            case 'EN_ESPERA_CLIENTE':
                return ['CANCELADO'];
            default:
                return [];
        }
    }
    get visibilidadSeguimientoEditable(): boolean {
        return this.follow.type === 'ACCION_CORRECTIVA';
    }
    get estadosSeguimientoPermitidos(): string[] {
        if (!this.c)
            return [];
        switch (this.follow.type) {
            case 'SOLICITUD_INFORMACION':
                return this.c.status === 'EN_PROCESO'
                    ? ['EN_ESPERA_CLIENTE']
                    : [];
            case 'RESPUESTA_CLIENTE':
                if (this.c.status === 'EN_ESPERA_CLIENTE') {
                    return ['EN_PROCESO', 'RESUELTO'];
                }
                return this.c.status === 'EN_PROCESO'
                    ? ['RESUELTO']
                    : [];
            case 'ACCION_CORRECTIVA':
            case 'COMENTARIO_INTERNO':
                return this.estadosCambioNormal;
            default:
                return [];
        }
    }
    private get datosCaso(): any { return this.c as any; }
    get puedeNotificarCliente(): boolean {
        return !!this.c && this.c.anonymous !== true && this.c.contactAuthorized === true && (!!this.c.email || !!this.c.phone);
    }
    get fechaCierre(): string | null { return this.datosCaso?.closedAt ?? this.datosCaso?.closureDate ?? null; }
    get motivoCierre(): string { return this.datosCaso?.closeReason ?? this.datosCaso?.closureReason ?? '—'; }
    get resumenCierre(): string { return this.datosCaso?.closeSummary ?? this.datosCaso?.closureSummary ?? '—'; }
    get usuarioCierre(): string { return this.datosCaso?.closedBy ?? this.datosCaso?.closedByName ?? this.datosCaso?.authorizedBy ?? '—'; }
    get tieneDatosCierre(): boolean { return !!this.c && this.c.status === 'CERRADO'; }
    get notificacionesCaso(): any[] {
        const d = this.datosCaso;
        if (Array.isArray(d?.notifications)) return d.notifications;
        if (Array.isArray(d?.notificationHistory)) return d.notificationHistory;
        if (Array.isArray(d?.sentNotifications)) return d.sentNotifications;
        return [];
    }
    get reaperturaFueraDePlazo(): boolean {
        if (!this.c?.closedAt)
            return false;
        const fechaCierre = new Date(this.c.closedAt).getTime();
        if (!Number.isFinite(fechaCierre))
            return false;
        const quinceDias = 15 * 24 * 60 * 60 * 1000;
        return Date.now() - fechaCierre > quinceDias;
    }
    abrirPanel(panel: PanelAccion): void {
        if (!panel || !this.c) {
            return;
        }
        if (this.esSoloLectura &&
            panel !== 'REABRIR') {
            this.mostrarValidacion('El caso se encuentra cerrado y no puede modificarse.');
            return;
        }
        this.panelActivo = panel;
        if (panel === 'ASIGNAR' && this.puedeAsignarResponsable) {
            this.agentFilters = {
                q: '',
                branchId: '',
                category: '',
                available: '',
                maxOpenCases: ''
            };
            this.assign = {
                responsibleId: null,
                reason: ''
            };
            this.loadAgents();
        }
        if (panel === 'EDITAR') {
            this.restaurarEdicion();
        }
        if (panel === 'EVIDENCIA') {
            this.prepararFormularioEvidencia();
        }
        setTimeout(() => {
            document
                .getElementById('panel-accion-activo')
                ?.scrollIntoView({
                behavior: 'smooth',
                block: 'start'
            });
        });
    }
    cerrarPanel(): void {
        this.panelActivo = null;
    }
    abrirAccionEspecial(estado: EstadoEspecial): void {
        if (!this.opcionesEstadoEspecial.includes(estado)) {
            this.mostrarValidacion('El cambio de estado solicitado no está permitido.');
            return;
        }
        this.specialStatus = estado;
        this.specialReason = '';
        this.abrirPanel('ESPECIAL');
    }
    get seguimientosInternos(): any[] {
        return (this.c?.followUps ?? [])
            .filter(f => f.visibleToClient === false);
    }
    slaTexto(): string {
        if (!this.c) {
            return '—';
        }
        if (this.c.slaBreached) {
            return 'Vencido';
        }
        if (this.c.slaWarningSent) {
            return 'Próximo a vencer';
        }
        return 'En tiempo';
    }
    tiempoConsumidoSla(): string {
        if (!this.c) {
            return '—';
        }
        const minutos = Number((this.c as any).slaConsumedMinutes);
        if (!Number.isFinite(minutos) || minutos < 0) {
            return '—';
        }
        const horas = Math.floor(minutos / 60);
        const mins = Math.floor(minutos % 60);
        if (horas <= 0) {
            return `${mins} min`;
        }
        return `${horas} h ${mins} min`;
    }
    label(valor: string | null | undefined): string {
        return valor ? valor.replace(/_/g, ' ') : '—';
    }
    bytes(tamano: number): string {
        if (tamano < 1024)
            return `${tamano} B`;
        if (tamano < 1048576) {
            return `${Math.round(tamano / 1024)} KB`;
        }
        return `${(tamano / 1048576).toFixed(2)} MB`;
    }
    onFollowTypeChange(): void {
        this.follow.newStatus = null;
        switch (this.follow.type) {
            case 'COMENTARIO_INTERNO':
                this.follow.visibleToClient = false;
                break;
            case 'RESPUESTA_CLIENTE':
                this.follow.visibleToClient = true;
                break;
            case 'SOLICITUD_INFORMACION':
                this.follow.visibleToClient = true;
                if (this.c?.status === 'EN_PROCESO') {
                    this.follow.newStatus = 'EN_ESPERA_CLIENTE';
                }
                break;
            case 'ACCION_CORRECTIVA':
                this.follow.visibleToClient = false;
                break;
        }
    }
    async cancelarAsignacion(): Promise<void> {
        if (this.assign.responsibleId == null &&
            !this.assign.reason.trim()) {
            this.cerrarPanel();
            return;
        }
        const r = await Swal.fire({
            icon: 'warning',
            title: 'Cancelar asignación',
            text: '¿Está seguro de descartar la operación?',
            showCancelButton: true,
            confirmButtonText: 'Descartar',
            cancelButtonText: 'Continuar',
            reverseButtons: true,
            confirmButtonColor: '#12263a',
            allowOutsideClick: false
        });
        if (!r.isConfirmed) {
            return;
        }
        this.assign = {
            responsibleId: null,
            reason: ''
        };
        this.panelActivo = null;
    }
    async doAssign(): Promise<void> {
        if (!this.c) {
            return;
        }
        const agente = this.agenteSeleccionado;
        if (!agente) {
            this.mostrarValidacion('Complete todos los campos obligatorios.');
            return;
        }
        if (!agente.available) {
            this.mostrarValidacion('El responsable seleccionado no se encuentra disponible.');
            return;
        }
        const reasignacion = this.c.responsibleId != null;
        if (reasignacion &&
            this.c.responsibleId === agente.id) {
            this.mostrarValidacion('Seleccione un responsable diferente al actual');
            return;
        }
        if (reasignacion &&
            !this.assign.reason.trim()) {
            this.mostrarValidacion('Complete todos los campos obligatorios.');
            return;
        }
        const carga = `${agente.openCases} caso(s) abierto(s), ` +
            `${agente.overdueCases} vencido(s)`;
        let textoConfirmacion = reasignacion
            ? `¿Está seguro de reasignar el caso a ${agente.fullName}?`
            : `¿Está seguro de asignar el caso a ${agente.fullName}?`;
        textoConfirmacion += ` Carga actual: ${carga}.`;
        if (this.c.priority === 'CRITICA') {
            textoConfirmacion +=
                ' El caso posee prioridad Crítica y está sujeto al escalamiento definido por RN20.';
        }
        const r = await Swal.fire({
            icon: 'question',
            title: reasignacion
                ? 'Reasignar Responsable'
                : 'Asignar Responsable',
            text: textoConfirmacion,
            showCancelButton: true,
            confirmButtonText: reasignacion
                ? 'Reasignar'
                : 'Asignar',
            cancelButtonText: 'Cancelar',
            reverseButtons: true,
            confirmButtonColor: '#12263a',
            allowOutsideClick: false
        });
        if (!r.isConfirmed) {
            return;
        }
        this.operating = true;
        this.servicioDetalle.asignarResponsable(this.id, {
            responsibleId: agente.id,
            reason: this.assign.reason.trim(),
            version: this.c.version
        }).subscribe({
            next: () => {
                this.operating = false;
                this.assign = {
                    responsibleId: null,
                    reason: ''
                };
                void this.operacionExitosa(reasignacion
                    ? 'El caso se reasignó con éxito.'
                    : `El caso se asignó con éxito a ${agente.fullName}.`);
            },
            error: e => {
                this.operating = false;
                this.mostrarErrorHttp(e, true);
            }
        });
    }
    saveEdit(): void {
        if (!this.c)
            return;
        if (this.c.status === 'CERRADO') {
            this.mostrarValidacion('El caso se encuentra cerrado y no puede modificarse.');
            return;
        }
        if (!this.edit.type ||
            !this.edit.branchId ||
            !this.edit.category ||
            !this.edit.priority) {
            this.mostrarValidacion('Complete todos los campos obligatorios.');
            return;
        }
        this.operating = true;
        this.servicioDetalle.actualizarCaso(this.id, {
            type: this.edit.type,
            branchId: this.edit.branchId as number,
            category: this.edit.category,
            priority: this.edit.priority,
            orderNumber: this.edit.orderNumber.trim(),
            administrativeObservation: this.edit.administrativeObservation.trim()
        }).subscribe({
            next: () => {
                this.operating = false;
                void this.operacionExitosa('La información del caso se actualizó con éxito.');
            },
            error: e => {
                this.operating = false;
                this.mostrarErrorHttp(e, true);
            }
        });
    }
    async cancelarEdicion(): Promise<void> {
        const r = await Swal.fire({
            icon: 'warning',
            title: 'Descartar cambios',
            text: 'Los cambios no guardados serán descartados.',
            showCancelButton: true,
            confirmButtonText: 'Descartar',
            cancelButtonText: 'Continuar editando',
            reverseButtons: true,
            confirmButtonColor: '#344553'
        });
        if (!r.isConfirmed)
            return;
        this.restaurarEdicion();
        this.panelActivo = null;
    }
    private restaurarEdicion(): void {
        if (!this.c)
            return;
        this.edit = {
            type: this.c.type ?? '',
            branchId: this.c.branchId ?? null,
            category: this.c.category ?? '',
            priority: this.c.priority ?? 'MEDIA',
            orderNumber: this.c.orderNumber ?? '',
            administrativeObservation: this.c.administrativeObservation ?? ''
        };
    }
    changePriority(): void {
        if (!this.c)
            return;
        if (this.c.status === 'CERRADO') {
            this.mostrarValidacion('El caso se encuentra cerrado y no puede modificarse.');
            return;
        }
        this.operating = true;
        this.servicioDetalle.cambiarPrioridad(this.id, this.newPriority).subscribe({
            next: () => {
                this.operating = false;
                void this.operacionExitosa('La información del caso se actualizó con éxito.');
            },
            error: e => {
                this.operating = false;
                this.mostrarErrorHttp(e, true);
            }
        });
    }
    changeNormalStatus(): void {
        if (!this.normalStatus) {
            this.mostrarValidacion('Complete todos los campos obligatorios.');
            return;
        }
        this.operating = true;
        this.servicioDetalle.cambiarEstado(this.id, this.normalStatus, this.normalStatusReason.trim() ||
            'Cambio de estado operativo').subscribe({
            next: () => {
                this.operating = false;
                this.normalStatus = '';
                this.normalStatusReason = '';
                void this.operacionExitosa('El estado del caso se actualizó con éxito.');
            },
            error: e => {
                this.operating = false;
                this.mostrarErrorHttp(e, true);
            }
        });
    }
    pickFollowFiles(event: Event): void {
        const input = event.target as HTMLInputElement;
        const nuevos = Array.from(input.files ?? []);
        if (this.followFiles.length + nuevos.length >
            this.maxArchivos) {
            this.mostrarValidacion('Se alcanzó la cantidad máxima de archivos permitidos.');
            input.value = '';
            return;
        }
        for (const archivo of nuevos) {
            const error = this.validarArchivo(archivo);
            if (error) {
                this.mostrarValidacion(error);
                input.value = '';
                return;
            }
        }
        this.followFiles = [
            ...this.followFiles,
            ...nuevos
        ];
        input.value = '';
    }
    retirarArchivoSeguimiento(indice: number): void {
        this.followFiles =
            this.followFiles.filter((_, i) => i !== indice);
    }
    addFollow(): void {
        const descripcion = this.follow.description.trim();
        if (!descripcion) {
            this.mostrarValidacion('Complete todos los campos obligatorios.');
            return;
        }
        if (descripcion.length < 10 ||
            descripcion.length > 3000) {
            this.mostrarValidacion('La descripción debe contener entre 10 y 3000 caracteres.');
            return;
        }
        if (this.follow.type ===
            'COMENTARIO_INTERNO') {
            this.follow.visibleToClient = false;
        }
        if (this.follow.type ===
            'RESPUESTA_CLIENTE' ||
            this.follow.type ===
                'SOLICITUD_INFORMACION') {
            this.follow.visibleToClient = true;
        }
        const tipoOriginal = this.follow.type;
        const data = new FormData();
        data.append('data', new Blob([
            JSON.stringify({
                ...this.follow,
                description: descripcion
            })
        ], {
            type: 'application/json'
        }));
        this.followFiles.forEach(archivo => data.append('files', archivo));
        this.operating = true;
        this.servicioDetalle.agregarSeguimiento(this.id, data).subscribe({
            next: () => {
                this.operating = false;
                this.follow = {
                    type: 'COMENTARIO_INTERNO',
                    description: '',
                    visibleToClient: false,
                    newStatus: null
                };
                this.followFiles = [];
                void this.operacionExitosa(tipoOriginal ===
                    'SOLICITUD_INFORMACION'
                    ? 'La solicitud de información se envió con éxito.'
                    : 'El seguimiento se registró con éxito.');
            },
            error: e => {
                this.operating = false;
                this.mostrarErrorHttp(e, true);
            }
        });
    }
    private prepararFormularioEvidencia(): void {
        this.evidenceDescription = '';
        this.evidenceAssociationType =
            'CASO';
        this.evidenceFollowUpId =
            null;
        this.evidenceFile =
            null;
        this.evidenceVisible =
            false;
        this.evidenceAvailability =
            null;
        this.cargarDisponibilidadEvidencia();
    }
    onEvidenceAssociationChange(): void {
        this.evidenceFile = null;
        if (this.evidenceAssociationType ===
            'CASO') {
            this.evidenceFollowUpId =
                null;
            this.evidenceVisible =
                false;
            this.cargarDisponibilidadEvidencia();
            return;
        }
        this.evidenceFollowUpId =
            null;
        this.evidenceVisible =
            false;
        this.evidenceAvailability =
            null;
    }
    onEvidenceFollowUpChange(): void {
        this.evidenceFile =
            null;
        this.cargarDisponibilidadEvidencia();
    }
    cargarDisponibilidadEvidencia(): void {
        if (!this.c) {
            return;
        }
        if (this.evidenceAssociationType ===
            'SEGUIMIENTO' &&
            this.evidenceFollowUpId == null) {
            this.evidenceAvailability =
                null;
            return;
        }
        this.evidenceAvailabilityLoading =
            true;
        this.servicioDetalle
            .consultarDisponibilidadEvidencia(this.id, this.evidenceAssociationType, this.evidenceFollowUpId)
            .subscribe({
            next: disponibilidad => {
                this.evidenceAvailabilityLoading =
                    false;
                this.evidenceAvailability =
                    disponibilidad;
                if (!disponibilidad
                    .visibilityEditable) {
                    this.evidenceVisible =
                        disponibilidad
                            .visibleToClient;
                }
            },
            error: e => {
                this.evidenceAvailabilityLoading =
                    false;
                this.evidenceAvailability =
                    null;
                this.mostrarErrorHttp(e);
            }
        });
    }
    get seguimientoEvidenciaSeleccionado() {
        if (this.evidenceFollowUpId == null ||
            !this.c) {
            return null;
        }
        return this.c.followUps.find(seguimiento => seguimiento.id ===
            this.evidenceFollowUpId) ?? null;
    }
    get extensionEvidenciaSeleccionada(): string {
        if (!this.evidenceFile) {
            return '—';
        }
        const partes = this.evidenceFile.name
            .split('.');
        if (partes.length < 2) {
            return '—';
        }
        return partes
            .pop()!
            .toUpperCase();
    }
    get puedeSeleccionarArchivoEvidencia(): boolean {
        if (this.evidenceAvailabilityLoading ||
            this.operating) {
            return false;
        }
        if (this.evidenceAssociationType ===
            'SEGUIMIENTO' &&
            this.evidenceFollowUpId == null) {
            return false;
        }
        return (this.evidenceAvailability != null &&
            this.evidenceAvailability.available > 0);
    }
    pickEvidence(event: Event): void {
        const input = event.target as HTMLInputElement;
        const archivo = input.files?.[0] ?? null;
        if (!archivo) {
            this.evidenceFile =
                null;
            return;
        }
        if (!this.evidenceAvailability ||
            this.evidenceAvailability
                .available <= 0) {
            this.mostrarValidacion('Se alcanzó la cantidad máxima de archivos permitidos.');
            this.evidenceFile =
                null;
            input.value = '';
            return;
        }
        const error = this.validarArchivo(archivo);
        if (error) {
            this.mostrarValidacion(error);
            this.evidenceFile =
                null;
            input.value = '';
            return;
        }
        this.evidenceFile =
            archivo;
    }
    retirarEvidenciaSeleccionada(): void {
        this.evidenceFile =
            null;
    }
    async cancelarEvidencia(): Promise<void> {
        const hayCambios = !!this.evidenceFile ||
            !!this.evidenceDescription.trim() ||
            this.evidenceAssociationType !==
                'CASO' ||
            this.evidenceFollowUpId != null ||
            this.evidenceVisible;
        if (!hayCambios) {
            this.cerrarPanel();
            return;
        }
        const r = await Swal.fire({
            icon: 'warning',
            title: 'Cancelar carga de evidencia',
            text: '¿Está seguro de cancelar? Los datos ingresados y el archivo seleccionado serán descartados.',
            showCancelButton: true,
            confirmButtonText: 'Sí, cancelar',
            cancelButtonText: 'Continuar',
            reverseButtons: true,
            confirmButtonColor: '#12263a',
            allowOutsideClick: false
        });
        if (!r.isConfirmed) {
            return;
        }
        this.prepararFormularioEvidencia();
        this.panelActivo =
            null;
    }
    async uploadEvidence(): Promise<void> {
        if (!this.c) {
            return;
        }
        if (!this.evidenceDescription.trim() ||
            !this.evidenceFile) {
            this.mostrarValidacion('Complete todos los campos obligatorios.');
            return;
        }
        if (this.evidenceDescription
            .trim()
            .length > 500) {
            this.mostrarValidacion('La descripción del archivo supera el límite permitido.');
            return;
        }
        if (this.evidenceAssociationType ===
            'SEGUIMIENTO' &&
            this.evidenceFollowUpId == null) {
            this.mostrarValidacion('Complete todos los campos obligatorios.');
            return;
        }
        if (!this.evidenceAvailability ||
            this.evidenceAvailability
                .available <= 0) {
            this.mostrarValidacion('Se alcanzó la cantidad máxima de archivos permitidos.');
            return;
        }
        const r = await Swal.fire({
            icon: 'question',
            title: 'Cargar evidencia',
            text: '¿Está seguro de cargar la evidencia seleccionada?',
            showCancelButton: true,
            confirmButtonText: 'Cargar Evidencia',
            cancelButtonText: 'Cancelar',
            reverseButtons: true,
            confirmButtonColor: '#12263a',
            allowOutsideClick: false
        });
        if (!r.isConfirmed) {
            return;
        }
        const data = new FormData();
        data.append('file', this.evidenceFile);
        this.operating =
            true;
        this.servicioDetalle
            .adjuntarEvidencia(this.id, data, {
            associationType: this.evidenceAssociationType,
            followUpId: this.evidenceFollowUpId,
            description: this.evidenceDescription
                .trim(),
            visibleToClient: this.evidenceVisible,
            version: this.c.version
        })
            .subscribe({
            next: () => {
                this.operating =
                    false;
                this.evidenceFile =
                    null;
                this.evidenceDescription =
                    '';
                this.evidenceAssociationType =
                    'CASO';
                this.evidenceFollowUpId =
                    null;
                this.evidenceVisible =
                    false;
                this.evidenceAvailability =
                    null;
                void this.operacionExitosa('La evidencia se cargó con éxito.');
            },
            error: e => {
                this.operating =
                    false;
                this.mostrarErrorHttp(e, true);
            }
        });
    }
    download(idEvidencia: number, nombre: string): void {
        this.servicioDetalle.descargarEvidencia(idEvidencia).subscribe({
            next: archivo => { this.guardarArchivo(archivo, nombre); this.mostrarExito('El archivo se descargó con éxito.'); },
            error: e => this.mostrarErrorHttp(e)
        });
    }
    downloadClosureCertificate(): void {
        if (!this.c || this.c.status !== 'CERRADO') return;
        this.servicioDetalle.descargarConstanciaCierre(this.id).subscribe({
            next: archivo => { this.guardarArchivo(archivo, `constancia-cierre-${this.c?.code ?? this.id}.pdf`); this.mostrarExito('El archivo se descargó con éxito.'); },
            error: e => this.mostrarErrorHttp(e)
        });
    }
    resolve(): void {
        const texto = this.resolution.trim();
        if (!texto) {
            this.mostrarValidacion('Complete todos los campos obligatorios.');
            return;
        }
        if (texto.length < 10 ||
            texto.length > 3000) {
            this.mostrarValidacion('La resolución debe contener entre 10 y 3000 caracteres.');
            return;
        }
        this.operating = true;
        this.servicioDetalle.resolverCaso(this.id, texto).subscribe({
            next: () => {
                this.operating = false;
                this.resolution = '';
                void this.operacionExitosa('La resolución se registró con éxito.');
            },
            error: e => {
                this.operating = false;
                this.mostrarErrorHttp(e, true);
            }
        });
    }
    onCloseReasonChange(): void {
        if (this.close.reason !== 'OTRO') {
            this.close.detailReason = '';
        }
        if (this.close.reason !== 'DUPLICADO') {
            this.close.duplicateCaseCode = '';
        }
    }
    async doClose(): Promise<void> {
        if (!this.c) return;
        if (!this.auth.has('CASE_CLOSE')) { this.mostrarValidacion('No posee permisos para realizar esta acción.'); return; }
        const bloqueos = this.motivosBloqueoCierre;
        if (bloqueos.length) { this.mostrarValidacion(bloqueos[0]); return; }
        const d = this.datosCaso, resumen = this.close.summary.trim();
        if (!this.close.reason || !resumen) { this.mostrarValidacion('Complete todos los campos obligatorios.'); return; }
        if (resumen.length > 3000) { this.mostrarValidacion('El resumen de cierre no puede superar 3000 caracteres.'); return; }
        const detalleMotivo = this.close.detailReason.trim();
        if (this.close.reason === 'OTRO' && detalleMotivo.length < 10) { this.mostrarValidacion('El detalle del motivo debe contener al menos 10 caracteres.'); return; }
        const codigoDuplicado = this.close.duplicateCaseCode.trim().toUpperCase();
        if (this.close.reason === 'DUPLICADO' && !codigoDuplicado) { this.mostrarValidacion('Complete todos los campos obligatorios.'); return; }
        if (this.close.reason === 'DUPLICADO' && codigoDuplicado === this.c.code.toUpperCase()) { this.mostrarValidacion('El código del caso principal no puede corresponder al mismo caso.'); return; }
        const solicitudPendiente = d?.pendingInformationRequest === true || d?.hasPendingInformationRequest === true;
        if (solicitudPendiente && this.close.reason !== 'CLIENTE_NO_RESPONDIO') {
            this.mostrarValidacion('Debe reanudar el caso o aplicar el motivo Cliente no respondió conforme a las reglas vigentes.'); return;
        }
        if (this.close.reason === 'CLIENTE_NO_RESPONDIO') {
            const sinSolicitud = d?.hasInformationRequest === false;
            const noEsperoCliente = d?.wasWaitingForClient === false;
            const intentosInsuficientes = d?.contactAttemptsSufficient === false || (Number.isFinite(Number(d?.requiredContactAttempts)) && Number(d?.contactAttempts ?? 0) < Number(d?.requiredContactAttempts));
            if (sinSolicitud || noEsperoCliente || intentosInsuficientes) { this.mostrarValidacion('No existen intentos de contacto suficientes para utilizar este motivo.'); return; }
        }
        if (this.requiereRevisionCriticaCierre) {
            if (d?.criticalReviewApproved === false) { this.mostrarValidacion('La revisión obligatoria del caso crítico se encuentra pendiente.'); return; }
            if (!this.close.criticalReviewConfirmed) { this.mostrarValidacion('La revisión obligatoria del caso crítico se encuentra pendiente.'); return; }
        }
        const notifyClient = this.puedeNotificarCliente && this.close.notifyClient;
        const sendSurvey = this.puedeNotificarCliente && this.close.sendSurvey;
        const r = await Swal.fire({
            icon: 'warning', title: 'Confirmar cierre',
            text: `¿Está seguro de cerrar el caso ${this.c.code}? Después del cierre no se permitirán modificaciones ordinarias`,
            showCancelButton: true, confirmButtonText: 'Confirmar Cierre', cancelButtonText: 'Cancelar',
            reverseButtons: true, confirmButtonColor: '#344553', allowOutsideClick: false
        });
        if (!r.isConfirmed) return;
        this.operating = true;
        this.servicioDetalle.cerrarCaso(this.id, {
            reason: this.close.reason, summary: resumen, internalObservation: this.close.internalObservation.trim(),
            detailReason: detalleMotivo, duplicateCaseCode: codigoDuplicado, notifyClient, sendSurvey,
            criticalReviewConfirmed: this.close.criticalReviewConfirmed, version: this.c.version
        }).subscribe({
            next: caso => {
                this.operating = false;
                this.close = { reason: 'SOLUCIONADO', summary: '', internalObservation: '', detailReason: '', duplicateCaseCode: '', notifyClient: this.puedeNotificarCliente, sendSurvey: this.puedeNotificarCliente, criticalReviewConfirmed: false };
                const aviso = (caso as any)?.closeWarning ?? (caso as any)?.notificationWarning ?? '';
                void this.operacionExitosa(aviso ? `El caso se cerró con éxito. ${aviso}` : 'El caso se cerró con éxito.');
            },
            error: e => {
                this.operating = false;
                if (e?.status === 409) {
                    const mensaje = this.mensajeServidor(e) ?? 'El caso fue actualizado por otro usuario. Revise la información vigente.';
                    void Swal.fire({ icon: 'warning', title: 'Información actualizada', text: mensaje, confirmButtonText: 'OK', confirmButtonColor: '#344553', allowOutsideClick: false }).then(() => this.load());
                    return;
                }
                this.mostrarErrorHttp(e, true);
            }
        });
    }
    async cancelarCierre(): Promise<void> {
        const r = await Swal.fire({
            icon: 'warning',
            title: 'Cancelar cierre',
            text: '¿Está seguro de cancelar el cierre?',
            showCancelButton: true,
            confirmButtonText: 'Sí, cancelar',
            cancelButtonText: 'No',
            reverseButtons: true,
            confirmButtonColor: '#344553'
        });
        if (!r.isConfirmed)
            return;
        this.close = {
            reason: 'SOLUCIONADO',
            summary: '',
            internalObservation: '',
            detailReason: '',
            duplicateCaseCode: '',
            notifyClient: this.puedeNotificarCliente,
            sendSurvey: this.puedeNotificarCliente,
            criticalReviewConfirmed: false
        };
        this.panelActivo = null;
    }
    async doReopen(): Promise<void> {
        if (!this.c)
            return;
        if (!this.reopen.reason.trim()) {
            this.mostrarValidacion('Complete todos los campos obligatorios.');
            return;
        }
        if (this.reaperturaFueraDePlazo &&
            !this.reopen.specialJustification) {
            this.mostrarValidacion('El plazo ordinario para reabrir el caso ha vencido.');
            return;
        }
        const r = await Swal.fire({
            icon: 'question',
            title: 'Reabrir caso',
            text: `¿Está seguro de reabrir el caso ${this.c.code}?`,
            showCancelButton: true,
            confirmButtonText: 'Reabrir',
            cancelButtonText: 'Cancelar',
            reverseButtons: true,
            confirmButtonColor: '#344553'
        });
        if (!r.isConfirmed)
            return;
        this.operating = true;
        this.servicioDetalle.reabrirCaso(this.id, {
            reason: this.reopen.reason.trim(),
            specialJustification: this.reopen.specialJustification
        }).subscribe({
            next: () => {
                this.operating = false;
                this.reopen = {
                    reason: '',
                    specialJustification: false
                };
                void this.operacionExitosa('El caso se reabrió con éxito.');
            },
            error: e => {
                this.operating = false;
                this.mostrarErrorHttp(e, true);
            }
        });
    }
    async changeStatus(): Promise<void> {
        if (!this.c)
            return;
        if (!this.opcionesEstadoEspecial
            .includes(this.specialStatus)) {
            this.mostrarValidacion('El cambio de estado solicitado no está permitido.');
            return;
        }
        if (!this.specialReason.trim()) {
            this.mostrarValidacion('Complete todos los campos obligatorios.');
            return;
        }
        const rechazo = this.specialStatus === 'RECHAZADO';
        const r = await Swal.fire({
            icon: 'warning',
            title: rechazo
                ? 'Rechazar caso'
                : 'Cancelar caso',
            text: rechazo
                ? `¿Está seguro de rechazar el caso ${this.c.code}?`
                : `¿Está seguro de cancelar el caso ${this.c.code}?`,
            showCancelButton: true,
            confirmButtonText: rechazo
                ? 'Rechazar'
                : 'Cancelar Caso',
            cancelButtonText: 'Regresar',
            reverseButtons: true,
            confirmButtonColor: rechazo
                ? '#7b6028'
                : '#8b3a3a',
            allowOutsideClick: false
        });
        if (!r.isConfirmed)
            return;
        this.operating = true;
        this.servicioDetalle.cambiarEstado(this.id, this.specialStatus, this.specialReason.trim()).subscribe({
            next: () => {
                this.operating = false;
                this.specialReason = '';
                void this.operacionExitosa('El estado del caso se actualizó con éxito.');
            },
            error: e => {
                this.operating = false;
                this.mostrarErrorHttp(e, true);
            }
        });
    }
    private validarArchivo(archivo: File): string | null {
        const extension = archivo.name
            .split('.')
            .pop()
            ?.toLowerCase() ?? '';
        if (!this.extensionesPermitidas
            .includes(extension) ||
            !this.tiposMimePermitidos
                .includes(archivo.type)) {
            return 'El formato del archivo no está permitido.';
        }
        if (archivo.size >
            this.maxArchivoBytes) {
            return 'El archivo supera el tamaño máximo permitido de 2 MB.';
        }
        return null;
    }
    private async operacionExitosa(mensaje: string): Promise<void> {
        this.panelActivo = null;
        await Swal.fire({
            icon: 'success',
            title: 'Operación exitosa',
            text: mensaje,
            confirmButtonText: 'OK',
            confirmButtonColor: '#344553',
            allowOutsideClick: true
        });
        this.load();
    }
    private mostrarExito(mensaje: string): void {
        void Swal.fire({
            icon: 'success',
            title: 'Operación exitosa',
            text: mensaje,
            confirmButtonText: 'OK',
            confirmButtonColor: '#344553'
        });
    }
    private mostrarValidacion(mensaje: string): void {
        void Swal.fire({
            icon: 'warning',
            title: 'Validación',
            text: mensaje,
            confirmButtonText: 'OK',
            confirmButtonColor: '#344553'
        });
    }
    private mostrarError(mensaje: string): void {
        this.error = mensaje;
        void Swal.fire({
            icon: 'error',
            title: 'Error',
            text: mensaje,
            confirmButtonText: 'OK',
            confirmButtonColor: '#344553'
        });
    }
    private mensajeServidor(e: any): string | null {
        if (typeof e?.error?.message === 'string' && e.error.message.trim()) return e.error.message.trim();
        if (typeof e?.error === 'string' && e.error.trim()) return e.error.trim();
        return null;
    }
    private mostrarErrorHttp(e: any, recargarSiCambio = false): void {
        let mensaje: string;
        if (e?.status === 0) {
            mensaje =
                'Error de conexión con el servidor.';
        }
        else if (e?.status === 401) {
            mensaje =
                'La sesión ha expirado. Inicie sesión nuevamente.';
        }
        else if (e?.status === 403) {
            mensaje =
                'No posee permisos para realizar esta acción.';
        }
        else if (e?.status === 409) {
            mensaje =
                'La operación fue rechazada porque la información cambió durante el proceso.';
        }
        else {
            mensaje = this.mensajeServidor(e) ?? 'Error interno del sistema. Intente nuevamente.';
        }
        this.error = mensaje;
        void Swal.fire({
            icon: 'error',
            title: 'Error',
            text: mensaje,
            confirmButtonText: 'OK',
            confirmButtonColor: '#344553'
        }).then(() => {
            if (recargarSiCambio &&
                e?.status === 409) {
                this.load();
            }
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

