import { ChangeDetectorRef, Component, NgZone, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { Router, ActivatedRoute, RouterModule } from '@angular/router';
import { AuthService } from '../../../services/auth.service';

type LoginIssue =
  | { kind: 'none' }
  | { kind: 'pending'; message: string }
  | { kind: 'rejected'; message: string }
  | { kind: 'credentials'; message: string }
  | { kind: 'other'; message: string };

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterModule],
  templateUrl: './login.component.html',
  styleUrls: ['./login.component.css']
})
export class LoginComponent implements OnInit {
  loginForm!: FormGroup;
  loading = false;
  submitted = false;
  issue: LoginIssue = { kind: 'none' };

  /** Set when the user arrives here from /register?registered=1 — shows a green banner. */
  justRegistered = false;

  readonly demoAccounts = [
    { label: 'Admin',   name: 'Admin User',  email: 'admin@example.com',  password: 'admin123',    icon: 'shield' },
    { label: 'Patient', name: 'John Doe',    email: 'john@example.com',   password: 'password123', icon: 'user' },
    { label: 'Doctor',  name: 'Dr. Stone',   email: 'doctor@example.com', password: 'password123', icon: 'stethoscope' },
    { label: 'Nurse',   name: 'Nina Brooks', email: 'nurse@example.com',  password: 'password123', icon: 'heart' }
  ];

  constructor(
    private formBuilder: FormBuilder,
    private authService: AuthService,
    private router: Router,
    private route: ActivatedRoute,
    private zone: NgZone,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    this.loginForm = this.formBuilder.group({
      email: ['', [Validators.required, Validators.email]],
      password: ['', [Validators.required, Validators.minLength(6)]]
    });

    this.justRegistered = this.route.snapshot.queryParamMap.get('registered') === '1';
  }

  get f() {
    return this.loginForm.controls;
  }

  /** Backwards-compat helper used by the template error pill. */
  get error(): string {
    return this.issue.kind === 'none' ? '' : this.issue.message;
  }

  onSubmit(): void {
    this.submitted = true;
    this.issue = { kind: 'none' };

    if (this.loginForm.invalid) {
      return;
    }

    this.loading = true;
    this.loginForm.patchValue({
      email: this.f['email'].value.trim().toLowerCase()
    }, { emitEvent: false });

    this.authService.login(this.loginForm.value).subscribe({
      next: (response) => {
        this.zone.run(() => {
          this.loading = false;
          this.cdr.markForCheck();
          const returnUrl = this.route.snapshot.queryParams['returnUrl'];
          this.router.navigateByUrl(returnUrl || this.authService.getDefaultRoute(response));
        });
      },
      error: (err) => {
        this.zone.run(() => {
          this.loading = false;
          this.issue = this.classifyError(err);
          this.cdr.markForCheck();
        });
      }
    });
  }

  private classifyError(err: any): LoginIssue {
    const status = err?.status;
    const backendMsg: string | undefined = err?.error?.message;

    if (status === 403 && backendMsg) {
      const lower = backendMsg.toLowerCase();
      if (lower.includes('pending')) {
        return { kind: 'pending', message: backendMsg };
      }
      if (lower.includes('not approved') || lower.includes('rejected')) {
        return { kind: 'rejected', message: backendMsg };
      }
    }

    if (status === 401) {
      return { kind: 'credentials', message: 'Email or password is incorrect.' };
    }
    if (status === 0 || err?.name === 'TimeoutError') {
      return { kind: 'other', message: 'The server is unreachable. Please check it\'s running and try again.' };
    }
    return { kind: 'other', message: backendMsg || 'Sign-in failed. Please try again.' };
  }

  fillDemoAccount(email: string, password: string): void {
    this.loginForm.patchValue({ email, password });
    this.issue = { kind: 'none' };
  }

  /** One-click sign-in: fill the form with a demo account and submit immediately. */
  loginAs(email: string, password: string): void {
    this.loginForm.patchValue({ email, password });
    this.submitted = false;
    this.issue = { kind: 'none' };
    this.onSubmit();
  }
}
