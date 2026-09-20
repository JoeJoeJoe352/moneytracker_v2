import { HttpInterceptorFn } from '@angular/common/http';

const API_URL_PREFIX = '/api/';

export const CredentialsInterceptor: HttpInterceptorFn = (req, next) => {
  if (!req.url.startsWith(API_URL_PREFIX)) {
    return next(req);
  }

  const modified = req.clone({
    withCredentials: true
  });

  return next(modified);
};
