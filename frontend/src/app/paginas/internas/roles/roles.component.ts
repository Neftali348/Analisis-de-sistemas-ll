import { CommonModule } from '@angular/common';
import { Component, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';

import { CodigoPermiso, CodigoRol } from '../../../nucleo/modelos';
import { ServicioAvisos } from '../../../nucleo/servicio-avisos';
import { ServicioRoles, VistaRol } from './roles.service';

@Component({
  selector: 'app-roles',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './roles.component.html',
  styleUrl: './roles.component.css'
})
export class ComponenteRoles implements OnInit {
  roles: VistaRol[] = [];
  error = '';

  allowed: Record<CodigoRol, CodigoPermiso[]> = {
    AGENTE_ATENCION: ['CASE_VIEW', 'CASE_FOLLOWUP', 'CASE_RESOLVE', 'EVIDENCE_UPLOAD', 'EVIDENCE_DOWNLOAD'],
    SUPERVISOR: [
      'CASE_VIEW', 'CASE_UPDATE', 'CASE_ASSIGN', 'CASE_FOLLOWUP', 'CASE_CLOSE',
      'CASE_REOPEN', 'CASE_PRIORITY', 'EVIDENCE_UPLOAD', 'EVIDENCE_DOWNLOAD',
      'REPORT_VIEW', 'REPORT_EXPORT'
    ],
    ADMINISTRADOR: [
      'CASE_VIEW', 'CASE_UPDATE', 'CASE_ASSIGN', 'CASE_FOLLOWUP', 'CASE_RESOLVE',
      'CASE_CLOSE', 'CASE_REOPEN', 'CASE_PRIORITY', 'EVIDENCE_UPLOAD',
      'EVIDENCE_DOWNLOAD', 'USER_ADMIN', 'ROLE_ADMIN', 'BRANCH_ADMIN',
      'REPORT_VIEW', 'REPORT_EXPORT', 'AUDIT_VIEW', 'AUDIT_EXPORT', 'NOTIFICATION_ADMIN'
    ],
    AUDITOR: ['REPORT_VIEW', 'REPORT_EXPORT', 'AUDIT_VIEW', 'AUDIT_EXPORT']
  };

  constructor(
    private servicioRoles: ServicioRoles,
    private toast: ServicioAvisos
  ) {}

  ngOnInit(): void {
    this.load();
  }

  label(valor: string): string {
    return valor.replaceAll('_', ' ');
  }

  load(): void {
    this.servicioRoles.listarRoles().subscribe({
      next: roles => this.roles = roles,
      error: e => this.error = e?.error?.message ?? 'No se pudieron cargar los roles.'
    });
  }

  has(rol: VistaRol, permiso: CodigoPermiso): boolean {
    return (rol.permissions ?? []).includes(permiso);
  }

  toggle(rol: VistaRol, permiso: CodigoPermiso, evento: Event): void {
    const marcado = (evento.target as HTMLInputElement).checked;
    const permisos = new Set<CodigoPermiso>(rol.permissions ?? []);
    marcado ? permisos.add(permiso) : permisos.delete(permiso);
    rol.permissions = [...permisos];
  }

  save(rol: VistaRol): void {
    this.servicioRoles.actualizarPermisos(rol.code, rol.permissions).subscribe({
      next: respuesta => {
        Object.assign(rol, respuesta);
        this.toast.show('Permisos actualizados.');
      },
      error: e => this.error = e?.error?.message ?? 'No se pudieron actualizar los permisos.'
    });
  }

  setActive(rol: VistaRol, activo: boolean): void {
    this.servicioRoles.cambiarEstado(rol.code, activo).subscribe({
      next: respuesta => Object.assign(rol, respuesta),
      error: e => this.error = e?.error?.message ?? 'No se pudo cambiar el estado.'
    });
  }
}
