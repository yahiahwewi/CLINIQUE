import { ChangeDetectorRef, Component, NgZone, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators, AbstractControl } from '@angular/forms';
import { Router, RouterModule } from '@angular/router';
import { AuthService, RequestedRole } from '../../../services/auth.service';
import { NotificationService } from '../../../services/notification.service';

@Component({
  selector: 'app-register',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterModule],
  templateUrl: './register.component.html',
  styleUrls: ['./register.component.css']
})
export class RegisterComponent implements OnInit {
  registerForm!: FormGroup;
  loading = false;
  submitted = false;
  error = '';

  readonly roleOptions: { value: RequestedRole; label: string; description: string }[] = [
    { value: 'PATIENT', label: 'Patient', description: 'Book appointments with our doctors.' },
    { value: 'DOCTOR',  label: 'Doctor',  description: 'Manage availability and accept patient bookings.' },
    { value: 'NURSE',   label: 'Nurse',   description: 'See appointments assigned to you.' }
  ];

  constructor(
    private formBuilder: FormBuilder,
    private authService: AuthService,
    private router: Router,
    private notifications: NotificationService,
    private zone: NgZone,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    this.registerForm = this.formBuilder.group({
      firstName: ['', Validators.required],
      lastName: ['', Validators.required],
      email: ['', [Validators.required, Validators.email]],
      password: ['', [Validators.required, Validators.minLength(6)]],
      confirmPassword: ['', Validators.required],
      requestedRole: ['PATIENT' as RequestedRole, Validators.required]
    }, { validators: this.passwordMatchValidator });
  }

  passwordMatchValidator(control: AbstractControl): { [key: string]: any } | null {
    const password = control.get('password');
    const confirmPassword = control.get('confirmPassword');

    if (!password || !confirmPassword) {
      return null;
    }

    if (!confirmPassword.value) {
      return null;
    }

    if (password.value !== confirmPassword.value) {
      confirmPassword.setErrors({
        ...(confirmPassword.errors ?? {}),
        passwordMismatch: true
      });
      return { passwordMismatch: true };
    }

    if (confirmPassword.errors?.['passwordMismatch']) {
      const { passwordMismatch, ...remainingErrors } = confirmPassword.errors;
      confirmPassword.setErrors(Object.keys(remainingErrors).length ? remainingErrors : null);
    }

    return null;
  }

  get f() {
    return this.registerForm.controls;
  }

  onSubmit(): void {
    this.submitted = true;
    this.error = '';

    if (this.registerForm.invalid) {
      return;
    }

    this.loading = true;
    this.registerForm.patchValue({
      firstName: this.f['firstName'].value.trim(),
      lastName: this.f['lastName'].value.trim(),
      email: this.f['email'].value.trim().toLowerCase()
    }, { emitEvent: false });

    this.authService.register(this.registerForm.value).subscribe({
      next: (response) => {
        // Run inside the Angular zone so CD definitely fires, then hand off
        // to the router. Belt + suspenders: explicit markForCheck and a fresh
        // microtask before navigating, so the loading flag actually unbinds
        // from the button before the route changes.
        this.zone.run(() => {
          this.loading = false;
          this.cdr.markForCheck();

          const isPending = response?.approvalStatus === 'PENDING';
          const isPatient = response?.roles?.includes('ROLE_USER');
          const message = isPending
            ? (response?.message || 'Account created — pending administrator approval.')
            : 'Account created — please sign in.';

          this.notifications.success(message);
          // If a clinical staff account just registered we always go to /login
          // (they can't sign in until approved). Patients also go to /login —
          // simpler and consistent than auto-login from registration.
          this.router.navigate(['/login'], { queryParams: { registered: '1' } });
        });
      },
      error: (error) => {
        this.zone.run(() => {
          this.loading = false;
          this.cdr.markForCheck();
          this.error = error.error?.errors
            ? Object.values(error.error.errors)[0] as string
            : error.error?.message || 'Registration failed. Please try again.';
        });
      }
    });
  }
}
