import { CommonModule } from '@angular/common';
import { Component, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';

import { PaginaCasos, Sucursal } from '../../../nucleo/modelos';
import { ServicioCasos } from './casos.service';

@Component({
  selector: 'app-casos',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './casos.component.html',
  styleUrl: './casos.component.css'
})
export class ComponenteCasos implements OnInit {
  page: PaginaCasos | null = null;
  branches: Sucursal[] = [];
  error = '';

  f: any = {
    q: '', status: '', type: '', priority: '', branchId: '',
    category: '', sla: '', from: '', to: ''
  };

  statuses = [
    'REGISTRADO', 'PENDIENTE_ASIGNACION', 'ASIGNADO', 'EN_PROCESO',
    'EN_ESPERA_CLIENTE', 'RESUELTO', 'CERRADO', 'RECHAZADO',
    'CANCELADO', 'REABIERTO'
  ];
  types = ['QUEJA', 'RECLAMO', 'DENUNCIA', 'SUGERENCIA'];
  priorities = ['BAJA', 'MEDIA', 'ALTA', 'CRITICA'];
  categories = ['PRODUCTO', 'ATENCION', 'ENTREGA', 'COBRO', 'HIGIENE', 'INSTALACIONES', 'OTRA'];

  constructor(private servicioCasos: ServicioCasos) {}

  ngOnInit(): void {
    this.servicioCasos.listarSucursales().subscribe({
      next: sucursales => this.branches = sucursales,
      error: () => this.error = 'No se pudieron cargar las sucursales.'
    });
    this.load(0);
  }

  label(valor: string): string {
    return valor.replaceAll('_', ' ');
  }

  clear(): void {
    this.f = { q: '', status: '', type: '', priority: '', branchId: '', category: '', sla: '', from: '', to: '' };
    this.load(0);
  }

  load(pagina: number): void {
    this.error = '';
    this.servicioCasos.listarCasos({ ...this.f, page: Math.max(0, pagina), size: 20 }).subscribe({
      next: respuesta => this.page = respuesta,
      error: e => this.error = e?.error?.message ?? 'No se pudo cargar la bandeja.'
    });
  }
}
