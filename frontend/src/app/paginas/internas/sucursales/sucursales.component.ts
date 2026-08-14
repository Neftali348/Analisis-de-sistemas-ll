import { CommonModule } from '@angular/common';
import { Component, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';

import { ServicioAvisos } from '../../../nucleo/servicio-avisos';
import { ServicioSucursales } from './sucursales.service';

@Component({
  selector: 'app-sucursales',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './sucursales.component.html',
  styleUrl: './sucursales.component.css'
})
export class ComponenteSucursales implements OnInit {
  branches: any[] = [];
  supervisors: any[] = [];
  editing = false;
  form: any = {};
  error = '';

  constructor(
    private servicioSucursales: ServicioSucursales,
    private toast: ServicioAvisos
  ) {}

  ngOnInit(): void {
    this.load();
    this.servicioSucursales.listarUsuarios().subscribe({
      next: usuarios => this.supervisors = usuarios.filter(u => u.role === 'SUPERVISOR' && u.status === 'ACTIVO'),
      error: () => this.supervisors = []
    });
  }

  load(): void {
    this.servicioSucursales.listarSucursales().subscribe({
      next: sucursales => this.branches = sucursales,
      error: e => this.error = e?.error?.message ?? 'No se pudieron cargar las sucursales.'
    });
  }

  newBranch(): void {
    this.form = {
      id: null, code: '', name: '', address: '', department: '', municipality: '',
      locationReference: '', phone: '', email: '', businessHours: '', observations: '',
      status: 'ACTIVO', supervisorId: null
    };
    this.editing = true;
  }

  edit(sucursal: any): void {
    this.form = { ...sucursal };
    this.editing = true;
    window.scrollTo({ top: 0, behavior: 'smooth' });
  }

  save(): void {
    const datos = {
      code: this.form.code,
      name: this.form.name,
      address: this.form.address,
      department: this.form.department,
      municipality: this.form.municipality,
      locationReference: this.form.locationReference || null,
      phone: this.form.phone || '',
      email: this.form.email || '',
      businessHours: this.form.businessHours || null,
      observations: this.form.observations || null,
      status: this.form.status,
      supervisorId: this.form.supervisorId
    };

    const solicitud = this.form.id
      ? this.servicioSucursales.actualizarSucursal(this.form.id, datos)
      : this.servicioSucursales.crearSucursal(datos);

    solicitud.subscribe({
      next: () => {
        this.editing = false;
        this.load();
        this.toast.show('Sucursal guardada.');
      },
      error: e => this.error = e?.error?.message ?? 'No se pudo guardar la sucursal.'
    });
  }
}
