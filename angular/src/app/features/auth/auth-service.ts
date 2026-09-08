import { inject, Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { LoginRequestData, RegisterRequestData, UserData } from './interfaces';

export interface GeneralResponse {
    message: string;
}

@Injectable({
    providedIn: 'root',
})
export class AuthService {
    private http = inject(HttpClient);

    /**
     * Check if user is already signed in
     */
    authenticateUser(): Observable<UserData> {
        return this.http.post<UserData>('/api/auth/authenticateUser', {});
    }

    /**
     * User login
     */
    login(requestData: LoginRequestData): Observable<GeneralResponse> {
        return this.http.post<GeneralResponse>('/api/auth/login', requestData);
    }

    /**
     * register user
     */
    register(params: RegisterRequestData): Observable<GeneralResponse> {
        return this.http.post<GeneralResponse>('/api/auth/register', params);
    }

    /**
     * User logout
     */
    logout(): Observable<GeneralResponse> {
        return this.http.post<GeneralResponse>('/api/auth/logout', {});
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
