import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { UserDataStore } from './shared/services/user-data-store';

export const authGuardGuard: CanActivateFn = () => {
  const router = inject(Router)
  const userData = inject(UserDataStore);
  if (userData.isUserLogged()) {
    return true
  } else {
    return router.parseUrl('/welcome') 
  }
};
