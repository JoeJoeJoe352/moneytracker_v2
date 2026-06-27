import { HttpInterceptorFn } from '@angular/common/http';

export const credentialsInterceptor: HttpInterceptorFn = (req, next) => {
  const modified = req.clone({
    withCredentials: true
  });

  return next(modified);
};