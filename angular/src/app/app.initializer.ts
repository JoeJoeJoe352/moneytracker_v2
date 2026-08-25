import { AuthService } from './features/auth/auth-service';
import { UserDataStore } from './shared/services/user-data-store';

export function initApp(authService: AuthService, userDataStore: UserDataStore) {
    return new Promise<void>((resolve) => {
        authService.authenticateUser().subscribe({
            next: (userData) => {
                userDataStore.loadUser(userData.username);
                resolve();
            },
            error: (error) => {
                userDataStore.resetData();
                if (error.status !== 401) {
                    console.error('unknown error during authcheck!', error);
                }
                resolve();
            },
        });
    });
}