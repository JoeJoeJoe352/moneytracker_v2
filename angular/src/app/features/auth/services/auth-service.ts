import { Injectable, NgZone } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

@Injectable({
  providedIn: 'root',
})
export class AuthService {
  constructor(private http: HttpClient) {}

  /**
   * User bejelentkeztetése
   * 
   * @param username 
   * @param password 
   * @returns Observable
   */
  login(username: string, password: string): Observable<{message: string}> {
    return this.http.post<{message: string}>(
      '/api/auth/login', 
      { username, password }, 
      //{ withCredentials: true } // cookie-kat is küldje
    );
  }

  /**
   * User kijelentkeztetése
   * 
   * @returns Observable
   */
  logout(): Observable<{message: string}> {
    return this.http.post<{message: string}>(
      '/api/auth/logout',
      {}
    );
  }
}
