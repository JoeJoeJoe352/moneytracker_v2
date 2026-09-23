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
     * Ellenőrzi, hogy a user be van-e már jelentkezve
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
     * User regisztráció
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
     * Ellenőrzi, hogy a felhasználónév foglalt-e
     */
    checkNameUniqueness(username: string): Observable<boolean> {
        return this.http.post<boolean>('/api/auth/isUsernameExists', { username });
    }

    /**
     * Ellenőrzi, hogy az email cím foglalt-e
     */
    checkEmailUniqueness(email: string): Observable<boolean> {
        return this.http.post<boolean>('/api/auth/isEmailExists', { email });
    }
}
