export type CodigoRol =
  | 'AGENTE_ATENCION'
  | 'SUPERVISOR'
  | 'ADMINISTRADOR'
  | 'AUDITOR';

export type CodigoPermiso =
  | 'CASE_VIEW'
  | 'CASE_UPDATE'
  | 'CASE_ASSIGN'
  | 'CASE_FOLLOWUP'
  | 'CASE_RESOLVE'
  | 'CASE_CLOSE'
  | 'CASE_REOPEN'
  | 'CASE_PRIORITY'
  | 'EVIDENCE_UPLOAD'
  | 'EVIDENCE_DOWNLOAD'
  | 'USER_ADMIN'
  | 'ROLE_ADMIN'
  | 'BRANCH_ADMIN'
  | 'REPORT_VIEW'
  | 'REPORT_EXPORT'
  | 'AUDIT_VIEW'
  | 'AUDIT_EXPORT'
  | 'NOTIFICATION_ADMIN';

export interface Sesion {
  token: string;
  userId: number;
  fullName: string;
  username: string;
  role: CodigoRol;
  branchId: number | null;
  permissions: string[];
  mustChangePassword: boolean;
  message: string;
}

export interface Sucursal {
  id: number;
  code: string;
  name: string;
  address: string;
  department?: string;
  municipality?: string;
  locationReference?: string;
  phone?: string;
  email?: string;
  businessHours?: string;
  observations?: string;
  status?: 'ACTIVO' | 'INACTIVO';
  supervisorId?: number | null;
  supervisor?: string | null;
  activeUsers?: number;
  activeCases?: number;
  createdAt?: string;
  updatedAt?: string;
  version?: number;
}

export interface Evidencia {
  id: number;
  originalName: string;
  contentType: string;
  sizeBytes: number;
  status: string;
  description?: string | null;
  visibleToClient: boolean;
  uploadedAt: string;
}

export interface Seguimiento {
    id: number;
    type: string;
    description: string;
    visibleToClient: boolean;
    resultingStatus: string;
    createdAt: string;
    author: string | null;
  }

// =========================================================
// CU-03 - VISTA PÚBLICA
// =========================================================

export interface VistaCasoPublico {
  code: string;

  // En denuncia confidencial el backend puede devolver null.
  type: string | null;

  status: string;

  priority: string | null;

  category: string | null;

  branch: string | null;

  description: string;

  incidentAt: string | null;

  createdAt: string;

  updatedAt: string | null;

  resolution: string | null;

  resolutionAt: string | null;

  closeReason: string | null;

  closeComment: string | null;

  closedAt: string | null;

  confidential: boolean;

  canRequestReopen: boolean;

  reopenRequested: boolean;

  followUps: Seguimiento[];

  evidences: Evidencia[];
}

// =========================================================
// VISTA INTERNA
// =========================================================

export interface VistaCaso {
  id?: number;
  code: string;
  type: string;
  status: string;
  priority: string;
  category?: string;
  anonymous?: boolean;
  confidential?: boolean;
  fullName?: string | null;
  email?: string | null;
  phone?: string | null;
  branch: string;
  branchId?: number;
  orderNumber?: string | null;
  administrativeObservation?: string | null;
  incidentAt: string;
  description: string;
  contactAuthorized?: boolean;
  responsibleId?: number | null;
  responsible?: string | null;
  resolution?: string | null;
  resolutionAt?: string | null;
  closeReason?: string | null;
  closeComment?: string | null;
  closedAt?: string | null;
  createdAt: string;
  updatedAt?: string;
  firstResponseAt?: string | null;
  slaDeadlineAt?: string | null;
  slaWarningSent?: boolean;
  slaBreached?: boolean;
  followUps: Seguimiento[];
  evidences: Evidencia[];
  version?: number;
}

export interface PaginaCasos {
  content: VistaCaso[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}