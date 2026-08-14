import { CommonModule } from '@angular/common';
import { Component, OnInit } from '@angular/core';
import { RouterLink } from '@angular/router';

import { ServicioAutenticacion } from '../../../nucleo/servicio-autenticacion';
import { ServicioPanel } from './panel.service';

@Component({
  selector: 'app-panel',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './panel.component.html',
  styleUrl: './panel.component.css'
})
export class ComponentePanel implements OnInit {
  data: any = null;
  error = '';

  constructor(
    private servicioPanel: ServicioPanel,
    public auth: ServicioAutenticacion
  ) {}

  ngOnInit(): void {
    this.servicioPanel.obtenerResumen().subscribe({
      next: respuesta => this.data = respuesta,
      error: e => this.error = e?.error?.message ?? 'No se pudo cargar el resumen.'
    });
  }

  entries(objeto: any): [string, unknown][] {
    return Object.entries(objeto ?? {}) as [string, unknown][];
  }
}
