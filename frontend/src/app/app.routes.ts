import { Routes } from '@angular/router';
import { AuthGuard } from './guards/auth.guard';
import { AdminGuard } from './guards/admin.guard';
export const routes: Routes = [
  {
    path: '',
    redirectTo: 'login',
    pathMatch: 'full'
  },
  {
    path: 'login',
    loadComponent: () => import('./components/auth/login/login.component').then(m => m.LoginComponent)
  },
  {
    path: 'register',
    loadComponent: () => import('./components/auth/register/register.component').then(m => m.RegisterComponent)
  },
  {
    path: 'forgot-password',
    loadComponent: () => import('./components/auth/forgot-password/forgot-password.component').then(m => m.ForgotPasswordComponent)
  },
  {
    path: 'reset-password',
    loadComponent: () => import('./components/auth/reset-password/reset-password.component').then(m => m.ResetPasswordComponent)
  },
  {
    path: 'dashboard',
    canActivate: [AuthGuard],
    loadComponent: () => import('./components/layout/layout.component').then(m => m.LayoutComponent),
    children: [
      {
        path: '',
        pathMatch: 'full',
        redirectTo: 'overview'
      },
      {
        path: 'appointments',
        loadComponent: () => import('./components/appointments/appointments.component').then(m => m.AppointmentsComponent)
      },
      {
        path: 'overview',
        loadComponent: () => import('./components/dashboard/dashboard.component').then(m => m.DashboardComponent)
      },
      {
        path: 'admin/dashboard',
        canActivate: [AdminGuard],
        loadComponent: () => import('./components/admin/dashboard/dashboard.component').then(m => m.AdminDashboardComponent)
      },
      {
        path: 'admin/users',
        canActivate: [AdminGuard],
        loadComponent: () => import('./components/admin/user-management/user-management.component').then(m => m.UserManagementComponent)
      }
    ]
  },
  {
    path: '**',
    redirectTo: 'login'
  }
];
