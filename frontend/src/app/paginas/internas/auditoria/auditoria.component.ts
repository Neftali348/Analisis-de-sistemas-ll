import { CommonModule } from '@angular/common';
import { Component, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';

import { ServicioAuditoria } from './auditoria.service';

@Component({
  selector: 'app-auditoria',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './auditoria.component.html',
  styleUrl: './auditoria.component.css'
})
export class ComponenteAuditoria implements OnInit {
  page: any = null;
  selected: any = null;
  integrityResult: any = null;
  error = '';
  f: any = { from: '', to: '', username: '', module: '', action: '', ip: '' };

  constructor(private servicioAuditoria: ServicioAuditoria) {}

  ngOnInit(): void {
    const hoy = new Date();
    const haceTreintaDias = new Date(Date.now() - 30 * 86400000);
    this.f.from = haceTreintaDias.toISOString().slice(0, 10);
    this.f.to = hoy.toISOString().slice(0, 10);
    this.load(0);
  }

  load(pagina: number): void {
    this.servicioAuditoria.listarEventos({ ...this.f, page: pagina, size: 25 }).subscribe({
      next: respuesta => this.page = respuesta,
      error: e => this.error = e?.error?.message ?? 'No se pudo consultar la bitácora.'
    });
  }

  detail(id: number): void {
    this.servicioAuditoria.obtenerDetalle(id).subscribe({
      next: respuesta => this.selected = respuesta,
      error: e => this.error = e?.error?.message ?? 'No se pudo abrir el evento.'
    });
  }

  integrity(): void {
    this.servicioAuditoria.verificarIntegridad().subscribe({
      next: respuesta => this.integrityResult = respuesta,
      error: e => this.error = e?.error?.message ?? 'No se pudo verificar la integridad.'
    });
  }
}
