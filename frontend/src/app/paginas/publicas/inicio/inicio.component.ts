import { Component } from '@angular/core';
import { RouterLink } from '@angular/router';

import { ServicioInicio } from './inicio.service';

@Component({
  selector: 'app-inicio',
  standalone: true,
  imports: [RouterLink],
  templateUrl: './inicio.component.html',
  styleUrl: './inicio.component.css'
})
export class ComponenteInicio {
  constructor(public servicioInicio: ServicioInicio) {}
}
