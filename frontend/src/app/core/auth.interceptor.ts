import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { catchError, throwError } from 'rxjs';

import { SessionService } from './session.service';

export const authExpiredInterceptor: HttpInterceptorFn = (request, next) => {
  const session = inject(SessionService);
  return next(request).pipe(
    catchError((error: unknown) => {
      if (
        error instanceof HttpErrorResponse &&
        error.status === 401 &&
        !request.url.startsWith('/api/auth/')
      ) {
        session.handleExpired();
      }
      return throwError(() => error);
    })
  );
};
