import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { Router } from '@angular/router';
import { App } from './app';
import { NotificationService } from './services/notification.service';
import { AuthService } from './services/auth.service';

describe('App', () => {
  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [App],
      providers: [
        provideRouter([]),
        NotificationService,
        {
          provide: AuthService,
          useValue: {
            currentUser$: { subscribe: () => ({ unsubscribe: () => undefined }) },
            logout: vi.fn(),
            hasRole: vi.fn().mockReturnValue(false),
            isAuthenticated: vi.fn().mockReturnValue(false)
          }
        }
      ]
    }).compileComponents();
  });

  it('creates the root app shell', () => {
    const fixture = TestBed.createComponent(App);
    expect(fixture.componentInstance).toBeTruthy();
  });

  it('navigates to login on logout', () => {
    const fixture = TestBed.createComponent(App);
    const app = fixture.componentInstance;
    const router = TestBed.inject(Router);
    const navigateSpy = vi.spyOn(router, 'navigate').mockResolvedValue(true);

    app.logout();

    expect(navigateSpy).toHaveBeenCalledWith(['/login']);
  });
});
