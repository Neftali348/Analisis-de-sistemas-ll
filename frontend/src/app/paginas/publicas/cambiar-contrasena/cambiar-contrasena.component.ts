import { CommonModule } from '@angular/common';
import { Component } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';

import { ServicioAutenticacion } from '../../../nucleo/servicio-autenticacion';
import { ServicioCambiarContrasena } from './cambiar-contrasena.service';

@Component({
  selector: 'app-cambiar-contrasena',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './cambiar-contrasena.component.html',
  styleUrl: './cambiar-contrasena.component.css'
})
export class ComponenteCambiarContrasena {
  currentPassword = '';
  newPassword = '';
  confirmPassword = '';
  error = '';

  constructor(
    private servicioCambiarContrasena: ServicioCambiarContrasena,
    private auth: ServicioAutenticacion,
    private router: Router
  ) {}

  save(): void {
    this.error = '';
    this.servicioCambiarContrasena.cambiar(
      this.currentPassword,
      this.newPassword,
      this.confirmPassword
    ).subscribe({
      next: () => {
        this.auth.logout(false);
        alert('Contraseña actualizada. Inicie sesión nuevamente.');
        this.router.navigateByUrl('/login');
      },
      error: e => this.error = e?.error?.message ?? 'No fue posible cambiar la contraseña.'
    });
  }
}
