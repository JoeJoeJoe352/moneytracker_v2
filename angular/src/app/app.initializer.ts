import { APP_INITIALIZER } from '@angular/core';
import { AuthService } from './features/auth/auth-service';

export function initApp(authService: AuthService) {
  return () => authService.loadUser();
}

export const appInitializerProvider = {
  provide: APP_INITIALIZER,
  useFactory: initApp,
  deps: [AuthService],
  multi: true
};