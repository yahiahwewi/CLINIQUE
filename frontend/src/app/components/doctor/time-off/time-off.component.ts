import { Component, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { TimeOff, TimeOffService, TimeOffStatus } from '../../../services/time-off.service';

@Component({
  selector: 'app-doctor-time-off',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './time-off.component.html'
})
export class DoctorTimeOffComponent implements OnInit {
  loading = signal(true);
  saving = signal(false);
  error = signal('');
  list = signal<TimeOff[]>([]);
  showForm = signal(false);
  form!: FormGroup;

  constructor(private fb: FormBuilder, private svc: TimeOffService) {}

  ngOnInit(): void {
    const today = new Date().toISOString().slice(0, 10);
    this.form = this.fb.group({
      startDate: [today, Validators.required],
      endDate: [today, Validators.required],
      reason: ['']
    });
    this.refresh();
  }

  refresh(): void {
    this.loading.set(true);
    this.svc.getMine().subscribe({
      next: l => { this.list.set(l); this.loading.set(false); },
      error: () => { this.error.set('Failed to load requests.'); this.loading.set(false); }
    });
  }

  open(): void {
    const today = new Date().toISOString().slice(0, 10);
    this.form.reset({ startDate: today, endDate: today, reason: '' });
    this.showForm.set(true);
  }

  close(): void { this.showForm.set(false); }

  submit(): void {
    if (this.form.invalid) { this.form.markAllAsTouched(); return; }
    this.saving.set(true);
    const v = this.form.value as TimeOff;
    this.svc.request(v).subscribe({
      next: () => { this.saving.set(false); this.close(); this.refresh(); },
      error: e => { this.saving.set(false); this.error.set(e?.error?.message || 'Failed to submit.'); }
    });
  }

  pillClass(s?: TimeOffStatus): string {
    if (s === 'APPROVED') return 'lumen-pill lumen-pill-accepted';
    if (s === 'REJECTED') return 'lumen-pill lumen-pill-cancelled';
    return 'lumen-pill lumen-pill-pending';
  }

  fmt(iso: string): string {
    return new Date(iso).toLocaleDateString(undefined, { month: 'short', day: 'numeric', year: 'numeric' });
  }

  daysSpanned(t: TimeOff): number {
    const ms = new Date(t.endDate).getTime() - new Date(t.startDate).getTime();
    return Math.round(ms / (24 * 60 * 60 * 1000)) + 1;
  }
}
