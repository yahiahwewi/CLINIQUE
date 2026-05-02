import { Component, OnDestroy, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { AuthService, AuthResponse } from '../../services/auth.service';
import { Appointment, DataService, UserStatistics } from '../../services/data.service';
import { Subject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './dashboard.component.html'
})
export class DashboardComponent implements OnInit, OnDestroy {
  currentUser: AuthResponse | null = null;
  appointments = signal<Appointment[]>([]);
  loadingAppointments = signal(true);
  appointmentsError = signal('');
  stats = signal<UserStatistics | null>(null);
  loadingStats = signal(false);
  private destroy$ = new Subject<void>();

  constructor(
    private authService: AuthService,
    private dataService: DataService
  ) {}

  ngOnInit(): void {
    this.authService.currentUser$.pipe(takeUntil(this.destroy$)).subscribe(user => {
      this.currentUser = user;
    });

    this.dataService.getAppointments().pipe(takeUntil(this.destroy$)).subscribe({
      next: (appointments) => {
        this.appointments.set(appointments);
        this.loadingAppointments.set(false);
      },
      error: () => {
        this.appointmentsError.set('Failed to load appointments.');
        this.loadingAppointments.set(false);
      }
    });

    if (this.hasRole('ROLE_ADMIN')) {
      this.loadingStats.set(true);
      this.dataService.adminGetUserStats().pipe(takeUntil(this.destroy$)).subscribe({
        next: s => { this.stats.set(s); this.loadingStats.set(false); },
        error: () => this.loadingStats.set(false)
      });
    }
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  hasRole(role: string): boolean {
    return this.authService.hasRole(role);
  }

  /* ----- patient view helpers ----- */
  nextAppointment(): Appointment | null {
    return this.upcoming()[0] ?? null;
  }

  upcoming(): Appointment[] {
    const now = Date.now();
    return this.appointments()
      .filter(a => new Date(a.appointmentDateTime).getTime() > now && a.status !== 'CANCELLED')
      .sort((a, b) => new Date(a.appointmentDateTime).getTime() - new Date(b.appointmentDateTime).getTime());
  }

  past(): Appointment[] {
    const now = Date.now();
    return this.appointments()
      .filter(a => new Date(a.appointmentDateTime).getTime() <= now)
      .sort((a, b) => new Date(b.appointmentDateTime).getTime() - new Date(a.appointmentDateTime).getTime());
  }

  /* ----- doctor view helpers ----- */
  todayAppointments(): Appointment[] {
    const today = new Date();
    today.setHours(0, 0, 0, 0);
    const end = new Date(today); end.setDate(end.getDate() + 1);
    return this.appointments()
      .filter(a => {
        const t = new Date(a.appointmentDateTime).getTime();
        return t >= today.getTime() && t < end.getTime();
      })
      .sort((a, b) => new Date(a.appointmentDateTime).getTime() - new Date(b.appointmentDateTime).getTime());
  }

  pendingForMe(): number {
    const me = this.currentUser?.id;
    return this.appointments().filter(a => a.status === 'PENDING' && (this.hasRole('ROLE_DOCTOR') ? a.doctor.id === me : true)).length;
  }

  acceptedForMe(): number {
    const me = this.currentUser?.id;
    return this.appointments().filter(a => a.status === 'ACCEPTED' && (this.hasRole('ROLE_DOCTOR') ? a.doctor.id === me : true)).length;
  }

  thisWeekCount(): number {
    const now = new Date();
    const weekEnd = new Date(now); weekEnd.setDate(now.getDate() + 7);
    return this.appointments().filter(a => {
      const t = new Date(a.appointmentDateTime).getTime();
      return t >= now.getTime() && t <= weekEnd.getTime();
    }).length;
  }

  /* ----- formatting ----- */
  formatDate(date: string): string {
    return new Date(date).toLocaleString(undefined, { month: 'short', day: 'numeric', hour: 'numeric', minute: '2-digit' });
  }

  formatTime(date: string): string {
    return new Date(date).toLocaleTimeString(undefined, { hour: 'numeric', minute: '2-digit' });
  }

  countdown(date: string): string {
    const diffMs = new Date(date).getTime() - Date.now();
    if (diffMs <= 0) return 'now';
    const minutes = Math.floor(diffMs / 60000);
    if (minutes < 60) return `in ${minutes} min`;
    const hours = Math.floor(minutes / 60);
    if (hours < 24) return `in ${hours}h ${minutes % 60}m`;
    const days = Math.floor(hours / 24);
    return `in ${days}d ${hours % 24}h`;
  }

  statusPillClass(status: string): string {
    if (status === 'ACCEPTED') return 'lumen-pill lumen-pill-accepted';
    if (status === 'CANCELLED') return 'lumen-pill lumen-pill-cancelled';
    return 'lumen-pill lumen-pill-pending';
  }
}
