import { Injectable } from '@angular/core';
import { HttpInterceptor, HttpRequest, HttpHandler, HttpEvent, HttpErrorResponse } from '@angular/common/http';
import { Observable, throwError } from 'rxjs';
import { catchError } from 'rxjs/operators';
import { Router } from '@angular/router';
import { NotificationService } from '../services/notification.service';
import { AuthService } from '../services/auth.service';

@Injectable()
export class ErrorInterceptor implements HttpInterceptor {
  constructor(
    private router: Router,
    private notificationService: NotificationService,
    private authService: AuthService
  ) {}

  intercept(request: HttpRequest<any>, next: HttpHandler): Observable<HttpEvent<any>> {
    return next.handle(request).pipe(
      catchError((error: HttpErrorResponse) => {
        const isAuthRequest = request.url.includes('/auth/login') || request.url.includes('/auth/register');

        if (error.status === 401) {
          if (isAuthRequest) {
            return throwError(() => error);
          }

          this.authService.clearSession();
          this.router.navigate(['/login']);
          this.notificationService.error('Your session has expired. Please login again.');
        } else if (error.status === 403) {
          this.notificationService.error('You do not have permission to perform this action.');
        } else if (error.status === 404) {
          this.notificationService.error('The requested resource was not found.');
        } else if (error.status === 500) {
          this.notificationService.error('An internal server error occurred. Please try again later.');
        } else if (error.error?.message) {
          this.notificationService.error(error.error.message);
        } else {
          this.notificationService.error('An error occurred. Please try again.');
        }

        return throwError(() => error);
      })
    );
  }
}

