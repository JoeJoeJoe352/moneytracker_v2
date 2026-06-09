import { inject, Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

@Injectable({
  providedIn: 'root',
})
export class AuthService {
  private http = inject(HttpClient)

  /**
   * User login
   * 
   * @param username 
   * @param password 
   * @returns Observable Observable
   */
  login(username: string, password: string): Observable<{message: string}> {
    return this.http.post<{message: string}>(
      '/api/auth/login', 
      { username, password }, 
    );
  }

  /**
   * register user
   * 
   * @param username 
   * @param email 
   * @param password 
   * @param passwordAgain
   * @returns Observable
   */
  register(username: string, email: string, password: string, passwordAgain: string): Observable<{message: string}> {
    return this.http.post<{message: string}>(
      '/api/auth/register', 
      { username, email, password, passwordAgain }
    )
  }

  /**
   * User logout
   * 
   * @returns Observable
   */
  logout(): Observable<{message: string}> {
    return this.http.post<{message: string}>(
      '/api/auth/logout',
      {},
      { withCredentials: true }
    );
  }

  /**
   * Check if username is taken
   * 
   * @param username 
   * @returns Observable
   */
  checkNameUniqueness(username: string): Observable<boolean> {
    return this.http.post<boolean>(
      '/api/auth/isUsernameExists',
      { username }
    )
  }

  /**
   * Check if email is taken
   * 
   * @param email 
   * @returns Observable
   */
  checkEmailUniqueness(email: string): Observable<boolean> {
    return this.http.post<boolean>(
      '/api/auth/isEmailExists',
      { email }
    )
  }
}
