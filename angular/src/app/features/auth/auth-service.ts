import { inject, Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

interface UserData {
  username: string,
}

interface GeneralResponse {
  message: string,
}

@Injectable({
  providedIn: 'root',
})
export class AuthService {
  private http = inject(HttpClient)

  /**
   * Check if user is already signed in
   * @returns Observable
   */
  authenticateUser(): Observable<UserData> {
    return this.http.get<UserData>(
      '/api/auth/authenticateUser', 
      { withCredentials: true } 
    );
  }

  /**
   * User login
   * 
   * @param username 
   * @param password 
   * @returns Observable
   */
  login(username: string, password: string): Observable<GeneralResponse> {
    return this.http.post<GeneralResponse>(
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
  register(username: string, email: string, password: string, passwordAgain: string): Observable<GeneralResponse> {
    return this.http.post<GeneralResponse>(
      '/api/auth/register', 
      { username, email, password, passwordAgain }
    )
  }

  /**
   * User logout
   * 
   * @returns Observable
   */
  logout(): Observable<GeneralResponse> {
    return this.http.post<GeneralResponse>(
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
