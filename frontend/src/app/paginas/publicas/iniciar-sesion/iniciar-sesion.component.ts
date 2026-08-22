import { CommonModule } from '@angular/common';
import { Component, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';


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

    const mensaje =
      sessionStorage.getItem(
        'sgq_mensaje_login'
      );
  
    if (mensaje) {
      this.error = mensaje;
  
      sessionStorage.removeItem(
        'sgq_mensaje_login'
      );
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
  
          // Mensaje del flujo normal.
          if (!sesion.mustChangePassword) {
            sessionStorage.setItem(
              'sgq_mensaje',
              sesion.message ||
              'Inicio de sesión realizado con éxito.'
            );
          }
  
          this.router.navigateByUrl(
            sesion.mustChangePassword
              ? '/cambiar-contrasena'
              : '/app/dashboard'
          );
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
  
          // Errores controlados: FA02, FA03, FA04
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
    const valor = prompt('Ingrese su usuario o correo institucional:');
    if (!valor) return;
    this.servicioInicioSesion.solicitarRecuperacion(valor).subscribe({
      next: respuesta => alert(respuesta.message),
      error: e => alert(e?.error?.message ?? 'No fue posible procesar la solicitud.')
    });
  }
}
