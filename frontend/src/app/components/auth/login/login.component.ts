import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { Router, ActivatedRoute, RouterModule } from '@angular/router';
import { AuthService } from '../../../services/auth.service';
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
  error = '';
  readonly demoAccounts = [
    { label: 'Admin', email: 'admin@example.com', password: 'admin123' },
    { label: 'Patient', email: 'john@example.com', password: 'password123' },
    { label: 'Doctor', email: 'doctor@example.com', password: 'password123' },
    { label: 'Nurse', email: 'nurse@example.com', password: 'password123' }
  ];

  constructor(
    private formBuilder: FormBuilder,
    private authService: AuthService,
    private router: Router,
    private route: ActivatedRoute
  ) {}

  ngOnInit(): void {
    this.loginForm = this.formBuilder.group({
      email: ['', [Validators.required, Validators.email]],
      password: ['', [Validators.required, Validators.minLength(6)]]
    });
  }

  get f() {
    return this.loginForm.controls;
  }

  onSubmit(): void {
    this.submitted = true;
    this.error = '';

    if (this.loginForm.invalid) {
      return;
    }

    this.loading = true;
    this.loginForm.patchValue({
      email: this.f['email'].value.trim().toLowerCase()
    }, { emitEvent: false });

    this.authService.login(this.loginForm.value).subscribe({
      next: (response) => {
        this.loading = false;
        const returnUrl = this.route.snapshot.queryParams['returnUrl'];
        this.router.navigateByUrl(returnUrl || this.authService.getDefaultRoute(response));
      },
      error: (error) => {
        this.loading = false;
        this.error = error.error?.message || 'Login failed. Please check your credentials.';
      }
    });
  }

  fillDemoAccount(email: string, password: string): void {
    this.loginForm.patchValue({ email, password });
    this.error = '';
  }
}

