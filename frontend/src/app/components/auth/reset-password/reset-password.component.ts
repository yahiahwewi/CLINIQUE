import { CommonModule } from '@angular/common';
import { Component, OnInit } from '@angular/core';
import { AbstractControl, FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterModule } from '@angular/router';
import { AuthService } from '../../../services/auth.service';

@Component({
  selector: 'app-reset-password',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterModule],
  templateUrl: './reset-password.component.html'
})
export class ResetPasswordComponent implements OnInit {
  form!: FormGroup;
  submitted = false;
  loading = false;
  successMessage = '';
  error = '';

  constructor(
    private fb: FormBuilder,
    private authService: AuthService,
    private router: Router
  ) {}

  ngOnInit(): void {
    this.form = this.fb.group({
      token: ['', Validators.required],
      password: ['', [Validators.required, Validators.minLength(6)]],
      confirmPassword: ['', Validators.required]
    }, { validators: this.passwordMatchValidator });
  }

  get f() {
    return this.form.controls;
  }

  passwordMatchValidator(control: AbstractControl): { [key: string]: boolean } | null {
    const password = control.get('password')?.value;
    const confirmPassword = control.get('confirmPassword')?.value;

    if (!confirmPassword) {
      return null;
    }

    return password === confirmPassword ? null : { passwordMismatch: true };
  }

  onSubmit(): void {
    this.submitted = true;
    this.error = '';
    this.successMessage = '';

    if (this.form.invalid) {
      return;
    }

    this.loading = true;
    this.authService.resetPassword(this.form.getRawValue()).subscribe({
      next: (response) => {
        this.loading = false;
        this.successMessage = response.message;
        setTimeout(() => this.router.navigate(['/login']), 1500);
      },
      error: (error) => {
        this.loading = false;
        this.error = error.error?.message || 'Unable to reset password.';
      }
    });
  }
}
