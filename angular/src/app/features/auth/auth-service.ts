import { inject, Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { LoginRequestData, RegisterRequestData, UserData } from './interfaces';

@Injectable({
    providedIn: 'root',
})
export class AuthService {
    private readonly http = inject(HttpClient);

    /**
     * Check if user is already signed in
     */
    authenticateUser(): Observable<UserData> {
        return this.http.post<UserData>('/api/auth/authenticateUser', {});
    }

    /**
     * User login. A backend üres body-val válaszol, a token egy HttpOnly cookie-ban érkezik
     */
    login(requestData: LoginRequestData): Observable<void> {
        return this.http.post<void>('/api/auth/login', requestData);
    }

    /**
     * register user
     */
    register(params: RegisterRequestData): Observable<void> {
        return this.http.post<void>('/api/auth/register', params);
    }

    /**
     * User logout
     */
    logout(): Observable<void> {
        return this.http.post<void>('/api/auth/logout', {});
    }

    /**
     * Check if username is taken
     */
    checkNameUniqueness(username: string): Observable<boolean> {
        return this.http.post<boolean>('/api/auth/isUsernameExists', { username });
    }

    /**
     * Check if email is taken
     */
    checkEmailUniqueness(email: string): Observable<boolean> {
        return this.http.post<boolean>('/api/auth/isEmailExists', { email });
    }
}
