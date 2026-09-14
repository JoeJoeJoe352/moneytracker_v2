import { AuthService } from './features/auth/auth-service';
import { UserDataStore } from './shared/services/user-data-store';

export function initApp(authService: AuthService, userDataStore: UserDataStore) {
    return new Promise<void>((resolve) => {
        authService.authenticateUser().subscribe({
            next: (userData) => {
                userDataStore.loadUserData(userData);
                resolve();
            },
            error: (error) => {
                userDataStore.resetData();
                // 403 a válasz, ha nem jó a jwt token
                if (error.status !== 403) {
                    console.error('unknown error during authcheck!', error);
                }
                resolve();
            },
        });
    });
}
