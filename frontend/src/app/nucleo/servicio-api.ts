import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';

@Injectable({providedIn:'root'})
export class ServicioApi {
  constructor(private http: HttpClient) {}
  get<T>(url:string, params?:Record<string,unknown>):Observable<T>{
    let p=new HttpParams(); Object.entries(params??{}).forEach(([k,v])=>{if(v!==undefined&&v!==null&&v!==''){if(Array.isArray(v))v.forEach(x=>p=p.append(k,String(x)));else p=p.set(k,String(v));}});
    return this.http.get<T>(url,{params:p});
  }
  post<T>(url:string,body:unknown,options?:object):Observable<T>{return this.http.post<T>(url,body,options);}
  put<T>(url:string,body:unknown):Observable<T>{return this.http.put<T>(url,body);}
  patch<T>(url:string,body:unknown):Observable<T>{return this.http.patch<T>(url,body);}
  delete<T>(url:string):Observable<T>{return this.http.delete<T>(url);}
  postBlob(url:string,body:unknown):Observable<Blob>{return this.http.post(url,body,{responseType:'blob'});}
  download(url:string, params?:Record<string,unknown>):Observable<Blob>{
    let p=new HttpParams(); Object.entries(params??{}).forEach(([k,v])=>{if(v!==undefined&&v!==null&&v!=='')p=p.set(k,String(v));});
    return this.http.get(url,{params:p,responseType:'blob'});
  }
}
