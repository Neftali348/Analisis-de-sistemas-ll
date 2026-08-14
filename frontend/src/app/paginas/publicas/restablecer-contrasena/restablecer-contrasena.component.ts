import { CommonModule } from '@angular/common';
import { Component, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';

import { ServicioRestablecerContrasena } from './restablecer-contrasena.service';

@Component({
  selector: 'app-restablecer-contrasena',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './restablecer-contrasena.component.html',
  styleUrl: './restablecer-contrasena.component.css'
})
export class ComponenteRestablecerContrasena implements OnInit {
  token = '';
  newPassword = '';
  confirmPassword = '';
  error = '';

  constructor(
    private route: ActivatedRoute,
    private servicioRestablecer: ServicioRestablecerContrasena,
    private router: Router
  ) {}

  ngOnInit(): void {
    this.token = this.route.snapshot.queryParamMap.get('token') ?? '';
    if (!this.token) this.error = 'El enlace no contiene un token de recuperación.';
  }

  save(): void {
    if (!this.token) return;
    this.servicioRestablecer.restablecer(this.token, this.newPassword, this.confirmPassword).subscribe({
      next: respuesta => {
        alert(respuesta.message);
        this.router.navigateByUrl('/login');
      },
      error: e => this.error = e?.error?.message ?? 'No fue posible restablecer la contraseña.'
    });
  }
}
