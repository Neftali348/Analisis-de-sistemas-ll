import { CommonModule } from '@angular/common';
import { Component, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';

import { Sucursal } from '../../../nucleo/modelos';
import { ServicioRegistrarCaso } from './registrar-caso.service';

@Component({
  selector: 'app-registrar-caso',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
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
  categories = ['PRODUCTO', 'ATENCION', 'ENTREGA', 'COBRO', 'HIGIENE', 'INSTALACIONES', 'OTRA'];

  form: any = {
    type: 'QUEJA', category: 'PRODUCTO', branchId: null, incidentAt: '', anonymous: false,
    confidential: false, fullName: '', email: '', phone: '', orderNumber: '', description: '',
    contactAuthorized: false, privacyAccepted: false
  };

  constructor(private servicioRegistrarCaso: ServicioRegistrarCaso) {}

  ngOnInit(): void {
    this.maxNow = this.localNow();
    this.form.incidentAt = this.maxNow;
    this.servicioRegistrarCaso.listarSucursales().subscribe({
      next: sucursales => this.branches = sucursales,
      error: () => this.error = 'No se pudieron cargar las sucursales activas.'
    });
  }

  localNow(): string {
    const fecha = new Date();
    const dosDigitos = (numero: number) => String(numero).padStart(2, '0');
    return `${fecha.getFullYear()}-${dosDigitos(fecha.getMonth() + 1)}-${dosDigitos(fecha.getDate())}T${dosDigitos(fecha.getHours())}:${dosDigitos(fecha.getMinutes())}`;
  }

  label(valor: string): string {
    return valor.replaceAll('_', ' ');
  }

  pickFiles(e: Event): void {
    const input = e.target as HTMLInputElement;
    const lista = Array.from(input.files ?? []);
    this.error = '';

    if (lista.length > 5) {
      this.error = 'Solo puede adjuntar hasta 5 archivos.';
      input.value = '';
      return;
    }

    for (const archivo of lista) {
      if (archivo.size > 2 * 1024 * 1024) {
        this.error = `${archivo.name} supera 2 MB.`;
        input.value = '';
        return;
      }
      if (!/\.(jpe?g|png|pdf)$/i.test(archivo.name)) {
        this.error = `${archivo.name} tiene un formato no permitido.`;
        input.value = '';
        return;
      }
    }
    this.files = lista;
  }

  submit(): void {
    this.error = '';

    if (!this.form.branchId) {
      this.error = 'Seleccione una sucursal.';
      return;
    }

    if (!this.form.anonymous && this.form.fullName.trim().split(/\s+/).length < 2) {
      this.error = 'Ingrese nombre y apellido.';
      return;
    }

    if (!this.form.anonymous && !this.form.email) {
      this.error = 'El correo es obligatorio para un caso identificado.';
      return;
    }

    if (this.form.type !== 'DENUNCIA') this.form.confidential = false;

    const payload = {
      ...this.form,
      fullName: this.form.anonymous ? null : this.form.fullName,
      email: this.form.anonymous ? null : this.form.email,
      phone: this.form.anonymous ? null : this.form.phone || '',
      orderNumber: this.form.orderNumber || null,
      incidentAt: this.form.incidentAt + ':00'
    };

    const datos = new FormData();
    datos.append('data', new Blob([JSON.stringify(payload)], { type: 'application/json' }));
    this.files.forEach(archivo => datos.append('files', archivo));

    this.loading = true;
    this.servicioRegistrarCaso.registrar(datos).subscribe({
      next: respuesta => {
        this.created = respuesta;
        this.loading = false;
      },
      error: e => {
        this.error = e?.error?.message ?? 'No fue posible registrar el caso.';
        this.loading = false;
      }
    });
  }

  formatBytes(tamano: number): string {
    return tamano < 1024 * 1024
      ? `${(tamano / 1024).toFixed(0)} KB`
      : `${(tamano / 1024 / 1024).toFixed(2)} MB`;
  }
}
