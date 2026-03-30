import { CommonModule } from '@angular/common';
import { Component, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { RouterModule } from '@angular/router';
import { AuthService } from '../../../services/auth.service';

@Component({
  selector: 'app-forgot-password',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterModule],
  templateUrl: './forgot-password.component.html'
})
export class ForgotPasswordComponent implements OnInit {
  form!: FormGroup;
  submitted = false;
  loading = false;
  successMessage = '';
  error = '';

  constructor(
    private fb: FormBuilder,
    private authService: AuthService
  ) {}

  ngOnInit(): void {
    this.form = this.fb.group({
      email: ['', [Validators.required, Validators.email]]
    });
  }

  get f() {
    return this.form.controls;
  }

  onSubmit(): void {
    this.submitted = true;
    this.error = '';
    this.successMessage = '';

    if (this.form.invalid) {
      return;
    }

    this.loading = true;
    this.authService.forgotPassword(this.form.getRawValue()).subscribe({
      next: (response) => {
        this.loading = false;
        this.successMessage = response.message;
      },
      error: (error) => {
        this.loading = false;
        this.error = error.error?.message || 'Unable to start password reset.';
      }
    });
  }
}
