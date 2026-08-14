import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { ServicioAutenticacion } from './servicio-autenticacion';

export const guardiaAutenticacion:CanActivateFn=()=>{const a=inject(ServicioAutenticacion);return a.logged?true:inject(Router).createUrlTree(['/login']);};
export const guardiaPermiso=(permission:string):CanActivateFn=>()=>{const a=inject(ServicioAutenticacion);return a.has(permission)?true:inject(Router).createUrlTree(['/app']);};
