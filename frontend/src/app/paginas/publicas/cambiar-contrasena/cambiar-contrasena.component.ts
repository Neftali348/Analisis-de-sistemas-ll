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
  loading = false;

  constructor(
    private servicioCambiarContrasena: ServicioCambiarContrasena,
    private auth: ServicioAutenticacion,
    private router: Router
  ) {}

  save(): void {

    this.error = '';

    // Validar campos obligatorios
    if (
      !this.currentPassword ||
      !this.newPassword ||
      !this.confirmPassword
    ) {
      this.error = 'Complete todos los campos obligatorios.';
      return;
    }

    // Guardar antes de limpiar
    const nuevaContrasena = this.newPassword;

    const usuario =
      this.auth.session?.username ?? '';

    this.loading = true;

    this.servicioCambiarContrasena
      .cambiar(
        this.currentPassword,
        this.newPassword,
        this.confirmPassword
      )
      .subscribe({

        next: () => {

          // Después del cambio, el JWT anterior ya no sirve.
          // Volvemos a autenticar con la nueva contraseña.
          this.auth
            .login(
              usuario,
              nuevaContrasena
            )
            .subscribe({

              next: () => {

                this.loading = false;

                alert(
                  'Contraseña actualizada correctamente.'
                );

                this.router.navigateByUrl(
                  '/app/dashboard'
                );
              },

              error: () => {

                this.loading = false;

                this.auth.logout(false);

                alert(
                  'Contraseña actualizada. Inicie sesión nuevamente.'
                );

                this.router.navigateByUrl(
                  '/login'
                );
              }
            });
        },

        error: e => {

          this.error =
            e?.error?.message ??
            'No fue posible cambiar la contraseña.';

          // FA06:
          // conservar contraseña actual
          // limpiar nueva y confirmación
          this.newPassword = '';
          this.confirmPassword = '';

          this.loading = false;
        }
      });
  }
}