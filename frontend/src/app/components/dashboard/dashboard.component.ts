import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { AuthService, AuthResponse } from '../../services/auth.service';
import { Appointment, DataService } from '../../services/data.service';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './dashboard.component.html',
  styleUrls: ['./dashboard.component.css']
})
export class DashboardComponent implements OnInit {
  currentUser: AuthResponse | null = null;
  appointments: Appointment[] = [];
  loadingAppointments = true;
  appointmentsError = '';

  constructor(
    private authService: AuthService,
    private dataService: DataService
  ) {}

  ngOnInit(): void {
    this.authService.currentUser$.subscribe(user => {
      this.currentUser = user;
    });

    this.dataService.getAppointments().subscribe({
      next: (appointments) => {
        this.appointments = appointments;
        this.loadingAppointments = false;
      },
      error: () => {
        this.appointmentsError = 'Failed to load assigned appointments.';
        this.loadingAppointments = false;
      }
    });
  }

  hasRole(role: string): boolean {
    return this.authService.hasRole(role);
  }

  roleSummary(): string {
    if (this.hasRole('ROLE_ADMIN')) {
      return 'You can manage accounts and appointments across the whole hospital.';
    }
    if (this.hasRole('ROLE_DOCTOR')) {
      return 'You can review assigned appointments and accept or cancel them.';
    }
    if (this.hasRole('ROLE_NURSE')) {
      return 'You can view the appointments that have been assigned to you.';
    }
    return 'You can create, edit, and delete your own appointments.';
  }

  appointmentHeading(): string {
    if (this.hasRole('ROLE_DOCTOR')) {
      return 'Assigned to You';
    }
    if (this.hasRole('ROLE_NURSE')) {
      return 'Nursing Assignments';
    }
    if (this.hasRole('ROLE_ADMIN')) {
      return 'Recent Appointment Activity';
    }
    return 'Your Upcoming Appointments';
  }

  formatDate(date: string): string {
    return new Date(date).toLocaleString();
  }
}

