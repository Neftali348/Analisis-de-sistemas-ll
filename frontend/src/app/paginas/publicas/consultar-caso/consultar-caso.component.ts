import { CommonModule } from '@angular/common';
import { Component } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';

import { VistaCaso } from '../../../nucleo/modelos';
import { ServicioConsultarCaso } from './consultar-caso.service';

@Component({
  selector: 'app-consultar-caso',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './consultar-caso.component.html',
  styleUrl: './consultar-caso.component.css'
})
export class ComponenteConsultarCaso {
  code = '';
  email = '';
  trackingKey = '';
  loading = false;
  error = '';
  caseData: VistaCaso | null = null;
  response = '';
  responseFiles: File[] = [];
  reason = '';
  reopenReason = '';
  rating = 5;
  ratingComment = '';

  constructor(private servicioConsultarCaso: ServicioConsultarCaso) {}

  get verifyParams(): Record<string, unknown> {
    return {
      email: this.email || undefined,
      trackingKey: this.trackingKey || undefined
    };
  }

  get canCancel(): boolean {
    return !!this.caseData && ['REGISTRADO', 'PENDIENTE_ASIGNACION', 'ASIGNADO', 'EN_PROCESO'].includes(this.caseData.status);
  }

  lookup(): void {
    this.error = '';
    if (!this.code || (!this.email && !this.trackingKey)) {
      this.error = 'Ingrese código y correo, o código y clave temporal.';
      return;
    }

    this.loading = true;
    this.servicioConsultarCaso.consultar({
      code: this.code.trim().toUpperCase(),
      email: this.email || null,
      trackingKey: this.trackingKey || null
    }).subscribe({
      next: caso => {
        this.caseData = caso;
        this.code = caso.code;
        this.loading = false;
      },
      error: e => {
        this.error = e?.error?.message ?? 'No fue posible consultar el caso.';
        this.caseData = null;
        this.loading = false;
      }
    });
  }

  pickResponseFiles(e: Event): void {
    const input = e.target as HTMLInputElement;
    const archivos = Array.from(input.files ?? []);
    if (archivos.length > 5 || archivos.some(archivo => archivo.size > 2 * 1024 * 1024)) {
      this.error = 'Puede adjuntar hasta 5 archivos de máximo 2 MB cada uno.';
      input.value = '';
      return;
    }
    this.responseFiles = archivos;
  }

  respond(): void {
    if (!this.caseData || this.response.trim().length < 10) {
      this.error = 'La respuesta debe tener al menos 10 caracteres.';
      return;
    }

    const datos = new FormData();
    datos.append('data', new Blob([JSON.stringify({ response: this.response })], { type: 'application/json' }));
    this.responseFiles.forEach(archivo => datos.append('files', archivo));

    this.servicioConsultarCaso.responder(this.caseData.code, datos, this.cleanParams()).subscribe({
      next: caso => {
        this.caseData = caso;
        this.response = '';
        this.responseFiles = [];
        this.error = '';
      },
      error: e => this.error = e?.error?.message ?? 'No se pudo registrar la respuesta.'
    });
  }

  cancel(): void {
    if (!this.caseData || !this.reason.trim()) {
      this.error = 'Indique el motivo de cancelación.';
      return;
    }
    this.servicioConsultarCaso.cancelar(this.caseData.code, this.reason, this.cleanParams()).subscribe({
      next: caso => this.caseData = caso,
      error: e => this.error = e?.error?.message ?? 'No se pudo cancelar.'
    });
  }

  requestReopen(): void {
    if (!this.caseData || !this.reopenReason.trim()) {
      this.error = 'Indique el motivo de reapertura.';
      return;
    }
    this.servicioConsultarCaso.solicitarReapertura(this.caseData.code, this.reopenReason, this.cleanParams()).subscribe({
      next: respuesta => {
        this.error = '';
        alert(respuesta.message);
        this.reopenReason = '';
      },
      error: e => this.error = e?.error?.message ?? 'No se pudo solicitar la reapertura.'
    });
  }

  rate(): void {
    if (!this.caseData) return;
    this.servicioConsultarCaso.calificar(
      this.caseData.code,
      this.rating,
      this.ratingComment || null,
      this.cleanParams()
    ).subscribe({
      next: respuesta => alert(respuesta.message),
      error: e => this.error = e?.error?.message ?? 'No se pudo registrar la calificación.'
    });
  }

  download(id: number, name: string): void {
    if (!this.caseData) return;
    this.servicioConsultarCaso.descargarEvidencia(this.caseData.code, id, this.verifyParams).subscribe({
      next: archivo => this.saveBlob(archivo, name),
      error: () => this.error = 'No se pudo descargar la evidencia.'
    });
  }

  cleanParams(): Record<string, unknown> {
    const parametros: Record<string, unknown> = {};
    if (this.email) parametros['email'] = this.email;
    if (this.trackingKey) parametros['trackingKey'] = this.trackingKey;
    return parametros;
  }

  saveBlob(blob: Blob, name: string): void {
    const url = URL.createObjectURL(blob);
    const enlace = document.createElement('a');
    enlace.href = url;
    enlace.download = name;
    enlace.click();
    URL.revokeObjectURL(url);
  }
}
