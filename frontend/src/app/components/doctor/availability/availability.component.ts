import { Component, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import {
  AvailabilityService,
  DoctorAvailability,
  DoctorAvailabilityRequest,
  DayOfWeek
} from '../../../services/availability.service';

@Component({
  selector: 'app-availability',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './availability.component.html'
})
export class AvailabilityComponent implements OnInit {
  loading = signal(true);
  error = signal('');
  windows = signal<DoctorAvailability[]>([]);
  showForm = signal(false);
  editingId = signal<number | null>(null);
  saving = signal(false);
  formError = signal('');

  form!: FormGroup;
  readonly days: DayOfWeek[] = ['MONDAY','TUESDAY','WEDNESDAY','THURSDAY','FRIDAY','SATURDAY','SUNDAY'];
  readonly durations = [15, 20, 30, 45, 60, 90];

  constructor(private fb: FormBuilder, private svc: AvailabilityService) {}

  ngOnInit(): void {
    this.form = this.fb.group({
      dayOfWeek: ['MONDAY' as DayOfWeek, Validators.required],
      startTime: ['09:00', Validators.required],
      endTime:   ['12:00', Validators.required],
      slotDurationMinutes: [30, Validators.required],
      active: [true]
    });
    this.refresh();
  }

  refresh(): void {
    this.loading.set(true);
    this.error.set('');
    this.svc.getMine().subscribe({
      next: list => { this.windows.set(list); this.loading.set(false); },
      error: () => { this.error.set('Failed to load availability.'); this.loading.set(false); }
    });
  }

  openAdd(): void {
    this.editingId.set(null);
    this.form.reset({ dayOfWeek: 'MONDAY', startTime: '09:00', endTime: '12:00', slotDurationMinutes: 30, active: true });
    this.formError.set('');
    this.showForm.set(true);
  }

  openEdit(window: DoctorAvailability): void {
    this.editingId.set(window.id);
    this.form.patchValue({
      dayOfWeek: window.dayOfWeek,
      startTime: window.startTime.slice(0, 5),
      endTime: window.endTime.slice(0, 5),
      slotDurationMinutes: window.slotDurationMinutes,
      active: window.active
    });
    this.formError.set('');
    this.showForm.set(true);
  }

  closeForm(): void {
    this.showForm.set(false);
    this.editingId.set(null);
  }

  save(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      this.formError.set('Please fill all fields.');
      return;
    }
    const v = this.form.value;
    if (v.startTime >= v.endTime) {
      this.formError.set('Start time must be before end time.');
      return;
    }
    const payload: DoctorAvailabilityRequest = {
      dayOfWeek: v.dayOfWeek,
      startTime: v.startTime + ':00',
      endTime: v.endTime + ':00',
      slotDurationMinutes: Number(v.slotDurationMinutes),
      active: !!v.active
    };
    this.saving.set(true);
    const obs = this.editingId()
      ? this.svc.update(this.editingId()!, payload)
      : this.svc.add(payload);
    obs.subscribe({
      next: () => {
        this.saving.set(false);
        this.closeForm();
        this.refresh();
      },
      error: e => {
        this.saving.set(false);
        this.formError.set(e?.error?.message || 'Failed to save availability.');
      }
    });
  }

  remove(window: DoctorAvailability): void {
    if (!confirm(`Remove ${this.dayLabel(window.dayOfWeek)} ${window.startTime}–${window.endTime}?`)) return;
    this.svc.remove(window.id).subscribe({ next: () => this.refresh() });
  }

  windowsByDay(): { day: DayOfWeek; items: DoctorAvailability[] }[] {
    return this.days.map(day => ({
      day,
      items: this.windows()
        .filter(w => w.dayOfWeek === day)
        .sort((a, b) => a.startTime.localeCompare(b.startTime))
    }));
  }

  slotsCount(w: DoctorAvailability): number {
    const [sh, sm] = w.startTime.slice(0, 5).split(':').map(Number);
    const [eh, em] = w.endTime.slice(0, 5).split(':').map(Number);
    const minutes = (eh * 60 + em) - (sh * 60 + sm);
    return Math.floor(minutes / w.slotDurationMinutes);
  }

  dayLabel(day: DayOfWeek): string {
    return day.charAt(0) + day.slice(1).toLowerCase();
  }

  fmtTime(t: string): string {
    return t.slice(0, 5);
  }
}
