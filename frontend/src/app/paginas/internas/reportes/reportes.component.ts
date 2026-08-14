import { CommonModule } from '@angular/common';
import { Component, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';

import { Sucursal } from '../../../nucleo/modelos';
import { ServicioAutenticacion } from '../../../nucleo/servicio-autenticacion';
import { ServicioReportes } from './reportes.service';

@Component({
  selector: 'app-reportes',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './reportes.component.html',
  styleUrl: './reportes.component.css'
})
export class ComponenteReportes implements OnInit {
  branches: Sucursal[] = [];
  data: any = null;
  error = '';

  reportTypes = ['CASOS_REGISTRADOS', 'TIEMPOS_ATENCION', 'CASOS_RESPONSABLE', 'CASOS_SUCURSAL', 'SATISFACCION', 'AUDITORIA'];
  types = ['QUEJA', 'RECLAMO', 'DENUNCIA', 'SUGERENCIA'];
  statuses = ['REGISTRADO', 'PENDIENTE_ASIGNACION', 'ASIGNADO', 'EN_PROCESO', 'EN_ESPERA_CLIENTE', 'RESUELTO', 'CERRADO', 'RECHAZADO', 'CANCELADO', 'REABIERTO'];
  priorities = ['BAJA', 'MEDIA', 'ALTA', 'CRITICA'];
  categories = ['PRODUCTO', 'ATENCION', 'ENTREGA', 'COBRO', 'HIGIENE', 'INSTALACIONES', 'OTRA'];

  f: any = {
    reportType: 'CASOS_REGISTRADOS', from: '', to: '', type: null,
    status: null, priority: null, branchId: null, responsibleId: null, category: null
  };

  constructor(
    private servicioReportes: ServicioReportes,
    public auth: ServicioAutenticacion
  ) {}

  ngOnInit(): void {
    const hoy = new Date();
    const inicioMes = new Date(hoy.getFullYear(), hoy.getMonth(), 1);
    this.f.to = this.iso(hoy);
    this.f.from = this.iso(inicioMes);

    this.servicioReportes.listarSucursales().subscribe({
      next: sucursales => this.branches = sucursales,
      error: () => this.branches = []
    });

    if (this.auth.session?.role === 'AUDITOR') this.f.reportType = 'AUDITORIA';
  }

  iso(fecha: Date): string {
    return fecha.toISOString().slice(0, 10);
  }

  label(valor: string): string {
    return String(valor).replaceAll('_', ' ');
  }

  entries(objeto: any): [string, unknown][] {
    return Object.entries(objeto ?? {}) as [string, unknown][];
  }

  keys(objeto: any): string[] {
    return Object.keys(objeto ?? {});
  }

  preview(): void {
    this.error = '';
    this.servicioReportes.generarVistaPrevia(this.f).subscribe({
      next: respuesta => this.data = respuesta,
      error: e => this.error = e?.error?.message ?? 'No se pudo generar el reporte.'
    });
  }

  export(formato: string): void {
    this.servicioReportes.exportar(formato, this.f).subscribe({
      next: archivo => {
        const url = URL.createObjectURL(archivo);
        const enlace = document.createElement('a');
        enlace.href = url;
        enlace.download = `reporte-${this.f.reportType.toLowerCase()}.${formato === 'xlsx' ? 'xlsx' : 'pdf'}`;
        enlace.click();
        URL.revokeObjectURL(url);
      },
      error: () => this.error = 'No se pudo exportar el reporte.'
    });
  }
}
