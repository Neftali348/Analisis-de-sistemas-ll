import { CommonModule } from '@angular/common';
import { Component, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';

import { ServicioAvisos } from '../../../nucleo/servicio-avisos';
import { ServicioNotificaciones } from './notificaciones.service';

@Component({
  selector: 'app-notificaciones',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './notificaciones.component.html',
  styleUrl: './notificaciones.component.css'
})
export class ComponenteNotificaciones implements OnInit {
  tab = 'history';
  history: any[] = [];
  templates: any[] = [];
  error = '';

  constructor(
    private servicioNotificaciones: ServicioNotificaciones,
    private toast: ServicioAvisos
  ) {}

  ngOnInit(): void {
    this.load();
  }

  label(valor: string): string {
    return String(valor).replaceAll('_', ' ');
  }

  load(): void {
    this.servicioNotificaciones.listarHistorial().subscribe({
      next: historial => this.history = historial,
      error: e => this.error = e?.error?.message ?? 'No se pudo cargar el historial.'
    });

    this.servicioNotificaciones.listarPlantillas().subscribe({
      next: plantillas => this.templates = plantillas,
      error: e => this.error = e?.error?.message ?? 'No se pudieron cargar las plantillas.'
    });
  }

  saveTemplate(plantilla: any): void {
    const datos = {
      name: plantilla.name,
      subject: plantilla.subject,
      content: plantilla.content,
      active: plantilla.active
    };
    this.servicioNotificaciones.actualizarPlantilla(plantilla.id, datos).subscribe({
      next: respuesta => {
        Object.assign(plantilla, respuesta);
        this.toast.show('Plantilla actualizada.');
      },
      error: e => this.error = e?.error?.message ?? 'No se pudo actualizar la plantilla.'
    });
  }

  resend(notificacion: any): void {
    const motivo = prompt('Motivo del reintento (opcional):') || '';
    this.servicioNotificaciones.reintentar(notificacion.id, motivo).subscribe({
      next: () => {
        this.load();
        this.toast.show('Reintento solicitado.');
      },
      error: e => this.error = e?.error?.message ?? 'No se pudo reintentar.'
    });
  }

  cancel(notificacion: any): void {
    const motivo = prompt('Motivo de cancelación:') || '';
    if (!motivo) return;
    this.servicioNotificaciones.cancelar(notificacion.id, motivo).subscribe({
      next: () => {
        this.load();
        this.toast.show('Notificación cancelada.');
      },
      error: e => this.error = e?.error?.message ?? 'No se pudo cancelar.'
    });
  }
}
