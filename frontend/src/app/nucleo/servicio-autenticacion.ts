import { Injectable, NgZone } from '@angular/core';
import { Router } from '@angular/router';
import { BehaviorSubject, interval, tap } from 'rxjs';
import { ServicioApi } from './servicio-api';
import { Sesion } from './modelos';

@Injectable({providedIn:'root'})
export class ServicioAutenticacion {
  private readonly key='sgq_session'; private readonly subject=new BehaviorSubject<Sesion|null>(this.read()); readonly session$=this.subject.asObservable();
  private lastActivity=Date.now();
  constructor(private api:ServicioApi,private router:Router,zone:NgZone){
    zone.runOutsideAngular(()=>{['click','keydown','mousemove','touchstart','scroll'].forEach(e=>window.addEventListener(e,()=>this.lastActivity=Date.now(),{passive:true}));interval(60000).subscribe(()=>this.checkActivity());});
  }
  login(username:string,password:string,remember=false){return this.api.post<Sesion>('/api/auth/login',{username,password}).pipe(tap(s=>{this.subject.next(s);this.lastActivity=Date.now();this.persist(s,remember);}));}
  requestRecovery(usernameOrEmail:string){return this.api.post<{message:string}>('/api/auth/forgot-password',{usernameOrEmail});}
  logout(callApi=true){if(callApi&&this.subject.value)this.api.post('/api/auth/logout',{}).subscribe({error:()=>{}});this.subject.next(null);localStorage.removeItem(this.key);sessionStorage.removeItem(this.key);this.router.navigateByUrl('/login');}
  get session(){return this.subject.value;} get token(){return this.subject.value?.token??null;} get logged(){return !!this.subject.value;}
  has(permission:string){return this.subject.value?.permissions.includes(permission)??false;}
  private checkActivity(){if(!this.logged)return;const idle=Date.now()-this.lastActivity;if(idle>=15*60_000){this.router.navigateByUrl('/login').then(()=>this.logout(false));return;}if(idle<5*60_000)this.api.post('/api/auth/touch',{}).subscribe({error:()=>{}});}
  private persist(s:Sesion,remember:boolean){(remember?localStorage:sessionStorage).setItem(this.key,JSON.stringify(s));if(remember)sessionStorage.removeItem(this.key);else localStorage.removeItem(this.key);}
  private read():Sesion|null{try{const raw=sessionStorage.getItem(this.key)||localStorage.getItem(this.key);return raw?JSON.parse(raw) as Sesion:null;}catch{return null;}}
}
