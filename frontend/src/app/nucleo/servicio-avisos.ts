import { Injectable } from '@angular/core';
import { BehaviorSubject } from 'rxjs';
@Injectable({providedIn:'root'})
export class ServicioAvisos {
  readonly message$=new BehaviorSubject<string|null>(null); private timer?:ReturnType<typeof setTimeout>;
  show(message:string){this.message$.next(message);if(this.timer)clearTimeout(this.timer);this.timer=setTimeout(()=>this.message$.next(null),4000);}
}
