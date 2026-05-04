import { Routes } from '@angular/router';
import { AuthGuard } from './guards/auth.guard';
import { AdminGuard } from './guards/admin.guard';

export const routes: Routes = [
  { path: '', redirectTo: 'login', pathMatch: 'full' },
  { path: 'login', loadComponent: () => import('./components/auth/login/login.component').then(m => m.LoginComponent) },
  { path: 'register', loadComponent: () => import('./components/auth/register/register.component').then(m => m.RegisterComponent) },
  { path: 'forgot-password', loadComponent: () => import('./components/auth/forgot-password/forgot-password.component').then(m => m.ForgotPasswordComponent) },
  { path: 'reset-password', loadComponent: () => import('./components/auth/reset-password/reset-password.component').then(m => m.ResetPasswordComponent) },
  {
    path: 'dashboard',
    canActivate: [AuthGuard],
    loadComponent: () => import('./components/layout/layout.component').then(m => m.LayoutComponent),
    children: [
      { path: '', pathMatch: 'full', redirectTo: 'overview' },
      { path: 'overview', loadComponent: () => import('./components/dashboard/dashboard.component').then(m => m.DashboardComponent) },
      { path: 'appointments', loadComponent: () => import('./components/appointments/appointments.component').then(m => m.AppointmentsComponent) },
      { path: 'availability', loadComponent: () => import('./components/doctor/availability/availability.component').then(m => m.AvailabilityComponent) },
      { path: 'time-off', loadComponent: () => import('./components/doctor/time-off/time-off.component').then(m => m.DoctorTimeOffComponent) },
      { path: 'profile/patient', loadComponent: () => import('./components/profile/patient-profile.component').then(m => m.PatientProfileComponent) },
      { path: 'profile/doctor',  loadComponent: () => import('./components/profile/doctor-profile.component').then(m => m.DoctorProfileComponent) },
      { path: 'history', loadComponent: () => import('./components/clinical/patient-history/patient-history.component').then(m => m.PatientHistoryComponent) },
      { path: 'referrals', loadComponent: () => import('./components/clinical/referrals/referrals.component').then(m => m.ReferralsComponent) },
      { path: 'lab-analyzer', loadComponent: () => import('./components/clinical/lab-analyzer/lab-analyzer.component').then(m => m.LabAnalyzerComponent) },
      { path: 'appointments/:appointmentId/consultation', loadComponent: () => import('./components/clinical/consultation/consultation.component').then(m => m.ConsultationComponent) },
      { path: 'admin/dashboard', canActivate: [AdminGuard], loadComponent: () => import('./components/admin/dashboard/dashboard.component').then(m => m.AdminDashboardComponent) },
      { path: 'admin/users', canActivate: [AdminGuard], loadComponent: () => import('./components/admin/user-management/user-management.component').then(m => m.UserManagementComponent) },
      { path: 'admin/approvals', canActivate: [AdminGuard], loadComponent: () => import('./components/admin/approvals/approvals.component').then(m => m.ApprovalsComponent) },
      { path: 'admin/departments', canActivate: [AdminGuard], loadComponent: () => import('./components/admin/departments/departments.component').then(m => m.DepartmentsComponent) },
      { path: 'admin/announcements', canActivate: [AdminGuard], loadComponent: () => import('./components/admin/announcements/announcements.component').then(m => m.AnnouncementsComponent) },
      { path: 'admin/holidays', canActivate: [AdminGuard], loadComponent: () => import('./components/admin/holidays/holidays.component').then(m => m.HolidaysComponent) },
      { path: 'admin/time-off', canActivate: [AdminGuard], loadComponent: () => import('./components/admin/time-off/time-off-approvals.component').then(m => m.TimeOffApprovalsComponent) },
      { path: 'admin/audit', canActivate: [AdminGuard], loadComponent: () => import('./components/admin/audit-log/audit-log.component').then(m => m.AuditLogComponent) }
    ]
  },
  { path: '**', redirectTo: 'login' }
];
