import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting, HttpTestingController } from '@angular/common/http/testing';
import { AuthService } from './auth.service';

describe('AuthService', () => {
  let service: AuthService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    localStorage.clear();

    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        AuthService
      ]
    });

    service = TestBed.inject(AuthService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
    localStorage.clear();
  });

  it('stores auth state after login', () => {
    const response = {
      token: 'jwt-token',
      type: 'Bearer',
      id: 1,
      email: 'admin@example.com',
      firstName: 'Admin',
      lastName: 'User',
      roles: ['ROLE_ADMIN']
    };

    service.login({ email: 'Admin@Example.com ', password: 'admin123' }).subscribe(result => {
      expect(result.token).toBe('jwt-token');
      expect(service.getCurrentUser()?.email).toBe('admin@example.com');
      expect(localStorage.getItem('auth_token')).toBe('jwt-token');
    });

    const request = httpMock.expectOne('/api/auth/login');
    expect(request.request.body.email).toBe('admin@example.com');
    request.flush(response);
  });

  it('calls forgot-password endpoint', () => {
    service.forgotPassword({ email: 'john@example.com' }).subscribe(result => {
      expect(result.message).toContain('reset link');
    });

    const request = httpMock.expectOne('/api/auth/forgot-password');
    expect(request.request.method).toBe('POST');
    request.flush({ message: 'If the account exists, a reset link has been generated.' });
  });

  it('routes admins to the admin overview', () => {
    expect(service.getDefaultRoute({
      token: 'token',
      type: 'Bearer',
      id: 1,
      email: 'admin@example.com',
      firstName: 'Admin',
      lastName: 'User',
      roles: ['ROLE_ADMIN']
    })).toBe('/dashboard/admin/dashboard');
  });
});
