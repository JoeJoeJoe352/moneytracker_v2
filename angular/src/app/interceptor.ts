import { HttpInterceptorFn } from '@angular/common/http';

export const CredentialsInterceptor: HttpInterceptorFn = (req, next) => {
  const modified = req.clone({
    withCredentials: true
  });

  return next(modified);
};