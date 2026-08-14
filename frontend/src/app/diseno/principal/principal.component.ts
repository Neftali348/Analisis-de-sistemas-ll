import { CommonModule } from '@angular/common';
import { Component } from '@angular/core';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';

import { ServicioAutenticacion } from '../../nucleo/servicio-autenticacion';
import { ServicioAvisos } from '../../nucleo/servicio-avisos';
import { ServicioPrincipal } from './principal.service';

@Component({
  selector: 'app-principal',
  standalone: true,
  imports: [CommonModule, RouterOutlet, RouterLink, RouterLinkActive],
  templateUrl: './principal.component.html',
  styleUrl: './principal.component.css'
})
export class ComponentePrincipal {
  open = false;

  constructor(
    public auth: ServicioAutenticacion,
    public toast: ServicioAvisos,
    private servicioPrincipal: ServicioPrincipal
  ) {}

  get showAdmin(): boolean {
    return this.servicioPrincipal.mostrarAdministracion();
  }

  get initials(): string {
    return this.servicioPrincipal.obtenerIniciales();
  }

  get roleLabel(): string {
    return this.servicioPrincipal.obtenerNombreRol();
  }

  logout(): void {
    this.servicioPrincipal.cerrarSesion();
  }
}
