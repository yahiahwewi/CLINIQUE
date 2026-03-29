import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { shareReplay, tap } from 'rxjs/operators';

export interface Role {
  id?: number;
  name: string;
  description?: string;
}

export interface User {
  id: number;
  email: string;
  firstName: string;
  lastName: string;
  enabled: boolean;
  roles: Role[];
}

export interface UserCreateRequest {
  firstName: string;
  lastName: string;
  email: string;
  password: string;
  roles: string[];
  enabled: boolean;
}

export interface UserStatistics {
  totalUsers: number;
  activeUsers: number;
  inactiveUsers: number;
  totalAppointments: number;
  pendingAppointments: number;
  acceptedAppointments: number;
}

export interface UserOption {
  id: number;
  fullName: string;
  email: string;
}

export type AppointmentStatus = 'PENDING' | 'ACCEPTED' | 'CANCELLED';

export interface Appointment {
  id: number;
  title: string;
  description: string;
  appointmentDateTime: string;
  status: AppointmentStatus;
  patient: UserOption;
  doctor: UserOption;
  nurse: UserOption | null;
  createdAt: string;
  updatedAt: string;
}

export interface AppointmentRequest {
  title: string;
  description: string;
  appointmentDateTime: string;
  doctorId: number;
  nurseId: number | null;
}

export interface AppointmentMeta {
  doctors: UserOption[];
  nurses: UserOption[];
}

@Injectable({
  providedIn: 'root'
})
export class DataService {
  private apiUrl = '/api';
  private appointments$?: Observable<Appointment[]>;
  private appointmentMeta$?: Observable<AppointmentMeta>;
  private adminUsers$?: Observable<User[]>;
  private userStats$?: Observable<UserStatistics>;

  constructor(private http: HttpClient) {}

  getCurrentUser(): Observable<User> {
    return this.http.get<User>(`${this.apiUrl}/users/me`);
  }

  adminGetAllUsers(): Observable<User[]> {
    if (!this.adminUsers$) {
      this.adminUsers$ = this.http.get<User[]>(`${this.apiUrl}/admin/users`).pipe(shareReplay(1));
    }
    return this.adminUsers$;
  }

  adminCreateUser(user: UserCreateRequest): Observable<User> {
    return this.http.post<User>(`${this.apiUrl}/admin/users`, user).pipe(
      tap(() => this.invalidateAdminCaches())
    );
  }

  adminGetUserStats(): Observable<UserStatistics> {
    if (!this.userStats$) {
      this.userStats$ = this.http.get<UserStatistics>(`${this.apiUrl}/admin/stats`).pipe(shareReplay(1));
    }
    return this.userStats$;
  }

  adminUpdateUser(id: number, user: Partial<User>): Observable<User> {
    return this.http.put<User>(`${this.apiUrl}/admin/users/${id}`, user).pipe(
      tap(() => this.invalidateAdminCaches())
    );
  }

  adminToggleUserStatus(id: number): Observable<void> {
    return this.http.patch<void>(`${this.apiUrl}/admin/users/${id}/toggle-status`, {}).pipe(
      tap(() => this.invalidateAdminCaches())
    );
  }

  adminChangePassword(id: number, password: string): Observable<void> {
    return this.http.patch<void>(`${this.apiUrl}/admin/users/${id}/change-password`, { password });
  }

  adminDeleteUser(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/admin/users/${id}`).pipe(
      tap(() => this.invalidateAdminCaches())
    );
  }

  getAppointments(): Observable<Appointment[]> {
    if (!this.appointments$) {
      this.appointments$ = this.http.get<Appointment[]>(`${this.apiUrl}/appointments`).pipe(shareReplay(1));
    }
    return this.appointments$;
  }

  getAppointmentMeta(): Observable<AppointmentMeta> {
    if (!this.appointmentMeta$) {
      this.appointmentMeta$ = this.http.get<AppointmentMeta>(`${this.apiUrl}/appointments/meta`).pipe(shareReplay(1));
    }
    return this.appointmentMeta$;
  }

  createAppointment(request: AppointmentRequest): Observable<Appointment> {
    return this.http.post<Appointment>(`${this.apiUrl}/appointments`, request).pipe(
      tap(() => this.invalidateAppointmentCaches())
    );
  }

  updateAppointment(id: number, request: AppointmentRequest): Observable<Appointment> {
    return this.http.put<Appointment>(`${this.apiUrl}/appointments/${id}`, request).pipe(
      tap(() => this.invalidateAppointmentCaches())
    );
  }

  updateAppointmentStatus(id: number, status: AppointmentStatus): Observable<Appointment> {
    return this.http.patch<Appointment>(`${this.apiUrl}/appointments/${id}/status`, { status }).pipe(
      tap(() => this.invalidateAppointmentCaches())
    );
  }

  deleteAppointment(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/appointments/${id}`).pipe(
      tap(() => this.invalidateAppointmentCaches())
    );
  }

  invalidateAppointmentCaches(): void {
    this.appointments$ = undefined;
    this.appointmentMeta$ = undefined;
    this.userStats$ = undefined;
  }

  invalidateAdminCaches(): void {
    this.adminUsers$ = undefined;
    this.userStats$ = undefined;
  }
}
