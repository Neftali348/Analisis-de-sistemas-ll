import { CommonModule } from '@angular/common';
import { Component, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import Swal from 'sweetalert2';

import { ServicioInicioSesion } from './iniciar-sesion.service';

@Component({
  selector: 'app-iniciar-sesion',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './iniciar-sesion.component.html',
  styleUrl: './iniciar-sesion.component.css'
})
export class ComponenteInicioSesion implements OnInit {

  username = '';
  password = '';
  loading = false;
  error = '';
  intentoIngresar = false;
  mostrarContrasena = false;

  constructor(
    private servicioInicioSesion: ServicioInicioSesion,
    private router: Router
  ) {}


  ngOnInit(): void {

    const mensaje = sessionStorage.getItem('sgq_mensaje_login');

    if (mensaje) {
      this.error = mensaje;
      sessionStorage.removeItem('sgq_mensaje_login');
    }

  }


  login(): void {

    this.intentoIngresar = true;
    this.error = '';

    // FA01
    if (!this.username.trim() || !this.password) {
      this.error = 'Complete todos los campos obligatorios.';
      return;
    }

    this.loading = true;

    this.servicioInicioSesion
      .iniciarSesion(
        this.username.trim(),
        this.password
      )
      .subscribe({

        next: sesion => {

          this.loading = false;

          // Si debe cambiar contraseña
          if (sesion.mustChangePassword) {

            this.router.navigateByUrl('/cambiar-contrasena');

            return;
          }


          // LOGIN EXITOSO
          Swal.fire({
            toast: true,
            position: 'top-end',

            icon: 'success',

            title: '¡Bienvenido!',
            text: sesion.message || 'Inicio de sesión realizado con éxito.',

            showConfirmButton: false,

            timer: 3000,
            timerProgressBar: true,

            showCloseButton: true
          });


          // Ir al dashboard
          this.router.navigateByUrl('/app/dashboard');

        },


        error: e => {

          // FA02: siempre limpiar contraseña
          this.password = '';
          this.loading = false;


          // FA12
          if (e.status === 0 || e.status === 503) {

            this.error =
              'No fue posible comunicarse con el servidor. Intente nuevamente.';

            return;
          }


          // FA02, FA03, FA04
          if (e.status === 400 || e.status === 403) {

            this.error =
              e?.error?.message ??
              'No fue posible iniciar sesión.';

            return;
          }


          // FA13
          this.error =
            'Error interno del sistema. Intente nuevamente.';

        }

      });

  }


  forgot(): void {

    Swal.fire({
      title: 'Recuperar contraseña',

      text: 'Ingrese su usuario o correo institucional.',

      input: 'text',

      inputPlaceholder: 'Usuario o correo',

      showCancelButton: true,

      confirmButtonText: 'Continuar',

      cancelButtonText: 'Cancelar',

      confirmButtonColor: '#12263a',

      showLoaderOnConfirm: true,

      preConfirm: valor => {

        if (!valor?.trim()) {

          Swal.showValidationMessage(
            'Debe ingresar su usuario o correo.'
          );

          return false;
        }

        return new Promise(resolve => {

          this.servicioInicioSesion
            .solicitarRecuperacion(valor.trim())
            .subscribe({

              next: respuesta => {
                resolve(respuesta);
              },

              error: e => {

                Swal.showValidationMessage(
                  e?.error?.message ??
                  'No fue posible procesar la solicitud.'
                );

              }

            });

        });

      },

      allowOutsideClick: () =>
        !Swal.isLoading()

    }).then(resultado => {

      if (resultado.isConfirmed) {

        const respuesta: any = resultado.value;

        Swal.fire({
          icon: 'success',

          title: 'Solicitud procesada',

          text:
            respuesta?.message ??
            'Se procesó la solicitud correctamente.',

          confirmButtonText: 'Aceptar',

          confirmButtonColor: '#12263a'
        });

      }

    });

  }

}