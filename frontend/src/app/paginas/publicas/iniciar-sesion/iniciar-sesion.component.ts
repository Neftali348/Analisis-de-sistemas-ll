import { CommonModule } from '@angular/common';
import { Component } from '@angular/core';
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
export class ComponenteInicioSesion {
  username = '';
  password = '';
  loading = false;
  error = '';

  constructor(
    private servicioInicioSesion: ServicioInicioSesion,
    private router: Router
  ) {}

  login(): void {

    // Limpiar mensaje anterior
    this.error = '';
  
    // Validar campos obligatorios
    if (!this.username.trim() || !this.password) {
      this.error = 'Complete todos los campos obligatorios.';
      return;
    }
  
    // Solo mostrar "Ingresando..." después de validar
    this.loading = true;
  
    this.servicioInicioSesion
      .iniciarSesion(this.username.trim(), this.password)
      .subscribe({
  
        next: sesion => {
  
          this.loading = false;
  
          this.router.navigateByUrl(
            sesion.mustChangePassword
              ? '/cambiar-contrasena'
              : '/app/dashboard'
          );
        },
  
        error: e => {
  
          this.error =
            e?.error?.message ??
            'No fue posible iniciar sesión.';
  
          this.loading = false;
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
