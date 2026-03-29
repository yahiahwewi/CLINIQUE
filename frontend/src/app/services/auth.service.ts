import { Injectable, inject, PLATFORM_ID } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, BehaviorSubject, throwError } from 'rxjs';
import { catchError, tap, timeout } from 'rxjs/operators';
import { isPlatformBrowser } from '@angular/common';

export interface LoginRequest {
  email: string;
  password: string;
}

export interface RegisterRequest {
  firstName: string;
  lastName: string;
  email: string;
  password: string;
  confirmPassword: string;
}

export interface AuthResponse {
  token: string;
  type: string;
  id: number;
  email: string;
  firstName: string;
  lastName: string;
  roles: string[];
}

@Injectable({
  providedIn: 'root'
})
export class AuthService {
  private apiUrl = '/api';
  private requestTimeoutMs = 15000;
  private platformId = inject(PLATFORM_ID);
  private currentUserSubject = new BehaviorSubject<AuthResponse | null>(null);
  public currentUser$ = this.currentUserSubject.asObservable();

  constructor(private http: HttpClient) {
    if (this.isBrowser()) {
      const user = this.getUserFromStorage();
      if (user) {
        this.currentUserSubject.next(user);
      }
    }
  }

  login(loginRequest: LoginRequest): Observable<AuthResponse> {
    return this.http.post<AuthResponse>(`${this.apiUrl}/auth/login`, {
      email: loginRequest.email.trim().toLowerCase(),
      password: loginRequest.password
    })
      .pipe(
        timeout(this.requestTimeoutMs),
        tap(response => {
          this.saveToken(response.token);
          this.saveUser(response);
          this.currentUserSubject.next(response);
        }),
        catchError(error => this.handleAuthRequestError(error))
      );
  }

  register(registerRequest: RegisterRequest): Observable<AuthResponse> {
    return this.http.post<AuthResponse>(`${this.apiUrl}/auth/register`, {
      firstName: registerRequest.firstName.trim(),
      lastName: registerRequest.lastName.trim(),
      email: registerRequest.email.trim().toLowerCase(),
      password: registerRequest.password,
      confirmPassword: registerRequest.confirmPassword
    })
      .pipe(
        timeout(this.requestTimeoutMs),
        tap(response => {
          this.saveToken(response.token);
          this.saveUser(response);
          this.currentUserSubject.next(response);
        }),
        catchError(error => this.handleAuthRequestError(error))
      );
  }

  logout(): void {
    this.clearStoredAuth();
    this.currentUserSubject.next(null);
  }

  clearSession(): void {
    this.clearStoredAuth();
    this.currentUserSubject.next(null);
  }

  private clearStoredAuth(): void {
    if (this.isBrowser()) {
      localStorage.removeItem('auth_token');
      localStorage.removeItem('auth_user');
    }
  }

  saveToken(token: string): void {
    if (this.isBrowser()) {
      localStorage.setItem('auth_token', token);
    }
  }

  private saveUser(user: AuthResponse): void {
    if (this.isBrowser()) {
      localStorage.setItem('auth_user', JSON.stringify(user));
    }
  }

  getToken(): string | null {
    if (this.isBrowser()) {
      return localStorage.getItem('auth_token');
    }
    return null;
  }

  isAuthenticated(): boolean {
    return !!this.getToken();
  }

  hasRole(role: string): boolean {
    const user = this.getCurrentUser();
    return !!user && user.roles.includes(role);
  }

  hasAnyRole(roles: string[]): boolean {
    const user = this.getCurrentUser();
    return !!user && roles.some(role => user.roles.includes(role));
  }

  getCurrentUser(): AuthResponse | null {
    return this.currentUserSubject.value;
  }

  getDefaultRoute(user: AuthResponse | null = this.getCurrentUser()): string {
    if (!user) {
      return '/login';
    }

    if (user.roles.includes('ROLE_ADMIN')) {
      return '/dashboard/admin/dashboard';
    }

    return '/dashboard/overview';
  }

  private isBrowser(): boolean {
    return isPlatformBrowser(this.platformId);
  }

  private getUserFromStorage(): AuthResponse | null {
    if (!this.isBrowser()) {
      return null;
    }
    const userJson = localStorage.getItem('auth_user');
    if (userJson) {
      try {
        return JSON.parse(userJson);
      } catch (e) {
        console.error('Error parsing user from storage', e);
        return null;
      }
    }
    return null;
  }

  private handleAuthRequestError(error: any): Observable<never> {
    const normalizedMessage = error?.name === 'TimeoutError'
      ? 'The server took too long to respond. Please make sure the backend is running and try again.'
      : error?.status === 0
        ? 'Unable to reach the server. Please check that the app is running and try again.'
        : error?.error?.message;

    return throwError(() => ({
      ...error,
      error: {
        ...(error?.error ?? {}),
        message: normalizedMessage ?? 'Authentication request failed. Please try again.'
      }
    }));
  }
}
