import { CommonModule } from '@angular/common';
import { Component, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';

import { CodigoRol, Sucursal } from '../../../nucleo/modelos';
import { ServicioAvisos } from '../../../nucleo/servicio-avisos';
import { ServicioUsuarios } from './usuarios.service';

@Component({
  selector: 'app-usuarios',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './usuarios.component.html',
  styleUrl: './usuarios.component.css'
})
export class ComponenteUsuarios implements OnInit {
  users: any[] = [];
  branches: Sucursal[] = [];
  filters: any = { q: '', role: '', branchId: '', state: '' };
  roles: CodigoRol[] = ['AGENTE_ATENCION', 'SUPERVISOR', 'ADMINISTRADOR', 'AUDITOR'];
  editing = false;
  error = '';
  form: any = {};

  constructor(
    private servicioUsuarios: ServicioUsuarios,
    private toast: ServicioAvisos
  ) {}

  get filteredUsers(): any[] {
    const q = String(this.filters.q || '').toLowerCase();
    return this.users.filter(usuario =>
      (!q || [usuario.fullName, usuario.username, usuario.email]
        .some((valor: string) => String(valor).toLowerCase().includes(q))) &&
      (!this.filters.role || usuario.role === this.filters.role) &&
      (!this.filters.branchId || String(usuario.branchId) === String(this.filters.branchId)) &&
      (!this.filters.state || (this.filters.state === 'BLOQUEADO' ? !!usuario.lockedUntil : usuario.status === this.filters.state))
    );
  }

  ngOnInit(): void {
    this.load();
    this.servicioUsuarios.listarSucursales().subscribe({
      next: sucursales => this.branches = sucursales,
      error: () => this.branches = []
    });
  }

  label(valor: string): string {
    return valor.replaceAll('_', ' ');
  }

  load(): void {
    this.servicioUsuarios.listarUsuarios().subscribe({
      next: usuarios => this.users = usuarios,
      error: e => this.error = e?.error?.message ?? 'No se pudieron cargar los usuarios.'
    });
  }

  newUser(): void {
    this.form = {
      id: null, fullName: '', username: '', email: '', role: 'AGENTE_ATENCION',
      branchId: null, status: 'ACTIVO', temporaryPassword: ''
    };
    this.editing = true;
  }

  edit(usuario: any): void {
    this.form = { ...usuario, temporaryPassword: null };
    this.editing = true;
    window.scrollTo({ top: 0, behavior: 'smooth' });
  }

  save(): void {
    const datos = {
      fullName: this.form.fullName,
      username: this.form.username,
      email: this.form.email,
      role: this.form.role,
      branchId: this.form.branchId,
      status: this.form.status,
      temporaryPassword: this.form.id ? null : (this.form.temporaryPassword || null)
    };

    const solicitud = this.form.id
      ? this.servicioUsuarios.actualizarUsuario(this.form.id, datos)
      : this.servicioUsuarios.crearUsuario(datos);

    solicitud.subscribe({
      next: respuesta => {
        this.editing = false;
        this.load();
        if (respuesta.temporaryPassword) {
          alert(`Contraseña temporal: ${respuesta.temporaryPassword}\nGuárdala y entrégala por un canal seguro.`);
        }
        this.toast.show('Usuario guardado.');
      },
      error: e => this.error = e?.error?.message ?? 'No se pudo guardar el usuario.'
    });
  }

  status(usuario: any, estado: string): void {
    this.servicioUsuarios.cambiarEstado(usuario.id, estado).subscribe({
      next: () => {
        this.load();
        this.toast.show('Estado actualizado.');
      },
      error: e => this.error = e?.error?.message ?? 'No se pudo cambiar el estado.'
    });
  }

  unlock(usuario: any): void {
    this.servicioUsuarios.desbloquear(usuario.id).subscribe({
      next: () => {
        this.load();
        this.toast.show('Usuario desbloqueado.');
      },
      error: e => this.error = e?.error?.message ?? 'No se pudo desbloquear.'
    });
  }

  reset(usuario: any): void {
    if (!confirm(`¿Generar nueva contraseña temporal para ${usuario.username}?`)) return;
    this.servicioUsuarios.restablecerContrasena(usuario.id).subscribe({
      next: respuesta => alert(`${respuesta.message}\n\nContraseña temporal: ${respuesta.temporaryPassword}`),
      error: e => this.error = e?.error?.message ?? 'No se pudo restablecer.'
    });
  }
}
