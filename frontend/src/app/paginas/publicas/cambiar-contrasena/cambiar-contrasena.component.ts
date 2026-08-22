import { CommonModule } from '@angular/common';
import { Component } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import Swal from 'sweetalert2';

import { ServicioAutenticacion } from '../../../nucleo/servicio-autenticacion';
import { ServicioCambiarContrasena } from './cambiar-contrasena.service';

@Component({
  selector: 'app-cambiar-contrasena',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule
  ],
  templateUrl: './cambiar-contrasena.component.html',
  styleUrl: './cambiar-contrasena.component.css'
})
export class ComponenteCambiarContrasena {

  currentPassword = '';
  newPassword = '';
  confirmPassword = '';

  error = '';
  loading = false;

  intentoGuardar = false;

  mostrarActual = false;
  mostrarNueva = false;
  mostrarConfirmacion = false;


  constructor(
    private servicioCambiarContrasena: ServicioCambiarContrasena,
    private auth: ServicioAutenticacion,
    private router: Router
  ) {}


  save(): void {

    this.intentoGuardar = true;
    this.error = '';
  
    // =====================================================
    // FA01 - CAMPOS OBLIGATORIOS
    // =====================================================
  
    if (
      !this.currentPassword ||
      !this.newPassword ||
      !this.confirmPassword
    ) {
  
      this.error =
        'Complete todos los campos obligatorios.';
  
      return;
    }
  
  
    // =====================================================
    // FA06 - REQUISITOS DE SEGURIDAD
    // RN AN02: No. 26
    // =====================================================
  
    const patronContrasena =
      /^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[^A-Za-z0-9]).{8,}$/;
  
    if (!patronContrasena.test(this.newPassword)) {
  
      this.error =
        'La contraseña no cumple con los requisitos de seguridad.';
  
      // Conservar contraseña actual
      // this.currentPassword NO se limpia
  
      // Limpiar nueva contraseña
      this.newPassword = '';
  
      // Limpiar confirmación
      this.confirmPassword = '';
  
      return;
    }
  
  
    // =====================================================
    // VALIDAR COINCIDENCIA
    // =====================================================
  
    if (this.newPassword !== this.confirmPassword) {
  
      this.error =
        'La nueva contraseña y su confirmación no coinciden.';
  
      return;
    }
  
  
    // Guardar la nueva contraseña antes
    // de limpiar cualquier información.
    const nuevaContrasena = this.newPassword;
  
    const usuario =
      this.auth.session?.username ?? '';
  
    this.loading = true;
  
  
    // =====================================================
    // CAMBIAR CONTRASEÑA
    // =====================================================
  
    this.servicioCambiarContrasena
      .cambiar(
        this.currentPassword,
        this.newPassword,
        this.confirmPassword
      )
      .subscribe({
  
        next: () => {
  
          /*
           * Después de cambiar la contraseña,
           * el JWT anterior puede dejar de ser válido.
           *
           * Volvemos a autenticarnos utilizando
           * la nueva contraseña.
           */
  
          this.auth
            .login(
              usuario,
              nuevaContrasena
            )
            .subscribe({
  
              next: () => {
  
                this.loading = false;
  
  
                // =================================================
                // OPERACIÓN EXITOSA
                // =================================================
  
                Swal.fire({
  
                  toast: true,
  
                  position: 'top-end',
  
                  icon: 'success',
  
                  title: 'Operación exitosa',
  
                  text:
                    'La operación se realizó con éxito.',
  
                  showConfirmButton: false,
  
                  showCloseButton: true,
  
                  timer: 3000,
  
                  timerProgressBar: true
  
                });
  
  
                this.router.navigateByUrl(
                  '/app/dashboard'
                );
  
              },
  
  
              error: () => {
  
                this.loading = false;
  
                this.auth.logout(false);
  
  
                Swal.fire({
  
                  icon: 'success',
  
                  title:
                    'Contraseña actualizada',
  
                  text:
                    'La contraseña fue actualizada correctamente. Inicie sesión nuevamente.',
  
                  confirmButtonText:
                    'Ir al inicio de sesión',
  
                  confirmButtonColor:
                    '#12263a',
  
                  allowOutsideClick: false
  
                }).then(() => {
  
                  this.router.navigateByUrl(
                    '/login'
                  );
  
                });
  
              }
  
            });
  
        },
  
  
        error: e => {
  
          this.loading = false;
  
  
          // =================================================
          // FA06 DESDE BACKEND
          // =================================================
          // Por seguridad, también verificamos si el backend
          // rechazó la contraseña por complejidad.
  
          const mensajeBackend =
            e?.error?.message ?? '';
  
  
          if (
            mensajeBackend
              .toLowerCase()
              .includes('requisitos') ||
  
            mensajeBackend
              .toLowerCase()
              .includes('complejidad') ||
  
            mensajeBackend
              .toLowerCase()
              .includes('contraseña')
          ) {
  
            this.error =
              'La contraseña no cumple con los requisitos de seguridad.';
  
            // FA06:
            // Conservar contraseña actual
            // Limpiar nueva y confirmación
  
            this.newPassword = '';
            this.confirmPassword = '';
  
            return;
          }
  
  
          // OTROS ERRORES
  
          this.error =
            e?.error?.message ??
            'No fue posible cambiar la contraseña.';
  
          this.newPassword = '';
          this.confirmPassword = '';
  
        }
  
      });
  
  }

}