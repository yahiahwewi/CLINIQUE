import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ActivatedRoute, Router } from '@angular/router';
import { of, throwError } from 'rxjs';
import { LoginComponent } from './login.component';
import { AuthService } from '../../../services/auth.service';

describe('LoginComponent', () => {
  let fixture: ComponentFixture<LoginComponent>;
  let component: LoginComponent;
  let authService: {
    login: ReturnType<typeof vi.fn>;
    getDefaultRoute: ReturnType<typeof vi.fn>;
  };
  let router: { navigateByUrl: ReturnType<typeof vi.fn> };

  beforeEach(async () => {
    authService = {
      login: vi.fn(),
      getDefaultRoute: vi.fn().mockReturnValue('/dashboard/overview')
    };
    router = {
      navigateByUrl: vi.fn()
    };

    await TestBed.configureTestingModule({
      imports: [LoginComponent],
      providers: [
        { provide: AuthService, useValue: authService },
        { provide: Router, useValue: router },
        { provide: ActivatedRoute, useValue: { snapshot: { queryParams: {} } } }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(LoginComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('submits valid credentials and redirects', () => {
    authService.login.mockReturnValue(of({
      token: 'token',
      type: 'Bearer',
      id: 1,
      email: 'admin@example.com',
      firstName: 'Admin',
      lastName: 'User',
      roles: ['ROLE_ADMIN']
    }));

    component.loginForm.setValue({ email: 'admin@example.com', password: 'admin123' });
    component.onSubmit();

    expect(authService.login).toHaveBeenCalled();
    expect(router.navigateByUrl).toHaveBeenCalledWith('/dashboard/overview');
  });

  it('shows API error messages when login fails', () => {
    authService.login.mockReturnValue(throwError(() => ({
      error: { message: 'Invalid email or password' }
    })));

    component.loginForm.setValue({ email: 'admin@example.com', password: 'wrongpass' });
    component.onSubmit();

    expect(component.error).toBe('Invalid email or password');
    expect(component.loading).toBe(false);
  });
});
