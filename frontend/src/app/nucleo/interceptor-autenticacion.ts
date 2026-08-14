import { HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { catchError, throwError } from 'rxjs';
import { ServicioAutenticacion } from './servicio-autenticacion';

export const interceptorAutenticacion:HttpInterceptorFn=(req,next)=>{
  const auth=inject(ServicioAutenticacion);const token=auth.token;const cloned=token?req.clone({setHeaders:{Authorization:`Bearer ${token}`}}):req;
  return next(cloned).pipe(catchError(err=>{if(err.status===401&&auth.logged)auth.logout(false);return throwError(()=>err);}));
};
