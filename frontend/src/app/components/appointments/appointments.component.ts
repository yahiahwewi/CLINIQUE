import { Component, OnDestroy, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import {
  Appointment,
  AppointmentMeta,
  AppointmentRequest,
  AppointmentStatus,
  DataService,
  UserOption
} from '../../services/data.service';
import { AuthService, AuthResponse } from '../../services/auth.service';
import { Subject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';

@Component({
  selector: 'app-appointments',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './appointments.component.html',
  styleUrl: './appointments.component.css'
})
export class AppointmentsComponent implements OnInit, OnDestroy {
  appointments: Appointment[] = [];
  meta: AppointmentMeta = { doctors: [], nurses: [] };
  appointmentForm!: FormGroup;
  loadingAppointments = true;
  loadingMeta = false;
  saving = false;
  error = '';
  success = '';
  showForm = false;
  editingAppointment: Appointment | null = null;
  currentUser: AuthResponse | null = null;
  formError = '';
  private destroy$ = new Subject<void>();

  constructor(
    private dataService: DataService,
    private authService: AuthService,
    private formBuilder: FormBuilder
  ) {}

  ngOnInit(): void {
    this.currentUser = this.authService.getCurrentUser();
    this.initForm();
    this.loadData();
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  initForm(): void {
    this.appointmentForm = this.formBuilder.group({
      title: ['', Validators.required],
      description: ['', Validators.required],
      appointmentDateTime: ['', Validators.required],
      doctorId: [null, Validators.required],
      nurseId: [null]
    });
  }

  loadData(): void {
    this.error = '';
    this.loadingAppointments = true;

    this.dataService.getAppointments()
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (appointments) => {
          this.appointments = appointments;
          this.loadingAppointments = false;
        },
        error: () => {
          this.error = 'Failed to load appointments';
          this.loadingAppointments = false;
        }
      });

    if (this.canCreateAppointments()) {
      this.loadAppointmentMeta();
    }
  }

  openCreateForm(): void {
    this.editingAppointment = null;
    this.formError = '';
    if (this.canCreateAppointments() && this.meta.doctors.length === 0 && !this.loadingMeta) {
      this.loadAppointmentMeta();
    }
    this.appointmentForm.reset({
      nurseId: null,
      appointmentDateTime: this.defaultDateTime()
    });
    this.showForm = true;
  }

  openEditForm(appointment: Appointment): void {
    this.editingAppointment = appointment;
    this.formError = '';
    if (this.canCreateAppointments() && this.meta.doctors.length === 0 && !this.loadingMeta) {
      this.loadAppointmentMeta();
    }
    this.appointmentForm.patchValue({
      title: appointment.title,
      description: appointment.description,
      appointmentDateTime: appointment.appointmentDateTime.slice(0, 16),
      doctorId: appointment.doctor.id,
      nurseId: appointment.nurse?.id ?? null
    });
    this.showForm = true;
  }

  closeForm(): void {
    this.showForm = false;
    this.editingAppointment = null;
    this.formError = '';
    this.appointmentForm.reset({ nurseId: null });
  }

  saveAppointment(): void {
    if (this.appointmentForm.invalid) {
      this.appointmentForm.markAllAsTouched();
      this.formError = 'Please complete all required fields before saving.';
      return;
    }

    this.saving = true;
    this.error = '';
    this.success = '';
    this.formError = '';

    const value = this.appointmentForm.getRawValue();
    const payload: AppointmentRequest = {
      title: value.title,
      description: value.description,
      appointmentDateTime: value.appointmentDateTime,
      doctorId: Number(value.doctorId),
      nurseId: value.nurseId ? Number(value.nurseId) : null
    };

    const request$ = this.editingAppointment
      ? this.dataService.updateAppointment(this.editingAppointment.id, payload)
      : this.dataService.createAppointment(payload);

    request$.pipe(takeUntil(this.destroy$)).subscribe({
      next: (appointment) => {
        this.saving = false;
        if (this.editingAppointment) {
          this.appointments = this.appointments.map(currentAppointment =>
            currentAppointment.id === appointment.id ? appointment : currentAppointment
          );
        } else {
          this.appointments = [appointment, ...this.appointments].sort(
            (left, right) =>
              new Date(left.appointmentDateTime).getTime() - new Date(right.appointmentDateTime).getTime()
          );
        }
        this.success = this.editingAppointment
          ? 'Appointment updated successfully'
          : 'Appointment created successfully';
        this.closeForm();
      },
      error: (error) => {
        this.saving = false;
        const validationErrors = error?.error?.errors;
        this.formError = validationErrors?.appointmentDateTime
          || error?.error?.message
          || 'Failed to save appointment';
      }
    });
  }

  updateStatus(appointment: Appointment, status: AppointmentStatus): void {
    this.dataService.updateAppointmentStatus(appointment.id, status)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (updatedAppointment) => {
          this.appointments = this.appointments.map(currentAppointment =>
            currentAppointment.id === updatedAppointment.id ? updatedAppointment : currentAppointment
          );
          this.success = `Appointment marked as ${status.toLowerCase()}`;
        },
        error: (error) => {
          this.error = error?.error?.message || 'Failed to update appointment status';
        }
      });
  }

  deleteAppointment(appointment: Appointment): void {
    const confirmed = confirm(`Delete appointment "${appointment.title}"?`);
    if (!confirmed) {
      return;
    }

    this.dataService.deleteAppointment(appointment.id)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: () => {
          this.appointments = this.appointments.filter(currentAppointment => currentAppointment.id !== appointment.id);
          this.success = 'Appointment deleted successfully';
        },
        error: (error) => {
          this.error = error?.error?.message || 'Failed to delete appointment';
        }
      });
  }

  canCreateAppointments(): boolean {
    return this.authService.hasAnyRole(['ROLE_USER', 'ROLE_ADMIN']);
  }

  canEditAppointment(appointment: Appointment): boolean {
    return this.authService.hasRole('ROLE_ADMIN') || appointment.patient.id === this.currentUser?.id;
  }

  canDeleteAppointment(appointment: Appointment): boolean {
    return this.authService.hasRole('ROLE_ADMIN')
      || appointment.patient.id === this.currentUser?.id
      || (this.authService.hasRole('ROLE_DOCTOR') && appointment.doctor.id === this.currentUser?.id);
  }

  canManageStatus(appointment: Appointment): boolean {
    return this.authService.hasRole('ROLE_ADMIN')
      || (this.authService.hasRole('ROLE_DOCTOR') && appointment.doctor.id === this.currentUser?.id);
  }

  heading(): string {
    if (this.authService.hasRole('ROLE_ADMIN')) {
      return 'All Appointments';
    }
    if (this.authService.hasRole('ROLE_DOCTOR')) {
      return 'Doctor Appointment Queue';
    }
    if (this.authService.hasRole('ROLE_NURSE')) {
      return 'Assigned Nursing Schedule';
    }
    return 'My Appointments';
  }

  roleHint(): string {
    if (this.authService.hasRole('ROLE_ADMIN')) {
      return 'Manage every appointment and coordinate staff assignments.';
    }
    if (this.authService.hasRole('ROLE_DOCTOR')) {
      return 'Review appointments assigned to you and accept or cancel them.';
    }
    if (this.authService.hasRole('ROLE_NURSE')) {
      return 'View appointments assigned to you by patients, doctors, or admin staff.';
    }
    return 'Create, edit, and cancel your own appointments.';
  }

  statusClass(status: AppointmentStatus): string {
    switch (status) {
      case 'ACCEPTED':
        return 'bg-green-100 text-green-800';
      case 'CANCELLED':
        return 'bg-red-100 text-red-800';
      default:
        return 'bg-amber-100 text-amber-800';
    }
  }

  formatDate(date: string): string {
    return new Date(date).toLocaleString();
  }

  appointmentCount(): number {
    return this.appointments.length;
  }

  pendingCount(): number {
    return this.appointments.filter(appointment => appointment.status === 'PENDING').length;
  }

  acceptedCount(): number {
    return this.appointments.filter(appointment => appointment.status === 'ACCEPTED').length;
  }

  nextAppointmentLabel(): string {
    const nextAppointment = this.appointments
      .filter(appointment => new Date(appointment.appointmentDateTime).getTime() > Date.now())
      .sort((left, right) => new Date(left.appointmentDateTime).getTime() - new Date(right.appointmentDateTime).getTime())[0];

    return nextAppointment ? this.formatDate(nextAppointment.appointmentDateTime) : 'No upcoming appointment';
  }

  minDateTime(): string {
    const now = new Date();
    now.setMinutes(now.getMinutes() - now.getTimezoneOffset());
    return now.toISOString().slice(0, 16);
  }

  defaultDateTime(): string {
    const nextHour = new Date(Date.now() + 60 * 60 * 1000);
    nextHour.setMinutes(0, 0, 0);
    nextHour.setMinutes(nextHour.getMinutes() - nextHour.getTimezoneOffset());
    return nextHour.toISOString().slice(0, 16);
  }

  private loadAppointmentMeta(): void {
    this.loadingMeta = true;
    this.dataService.getAppointmentMeta()
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (meta) => {
          this.meta = meta;
          this.loadingMeta = false;
        },
        error: () => {
          this.error = 'Failed to load appointment staff options';
          this.loadingMeta = false;
        }
      });
  }

  shouldShowError(controlName: string): boolean {
    const control = this.appointmentForm.get(controlName);
    return !!control && control.invalid && (control.touched || control.dirty);
  }

  trackByAppointment(_: number, appointment: Appointment): number {
    return appointment.id;
  }

  compareUsers(first: UserOption | null, second: UserOption | null): boolean {
    return first?.id === second?.id;
  }

  loading(): boolean {
    return this.loadingAppointments;
  }
}
