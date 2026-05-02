import { Component, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { Holiday, HolidayService } from '../../../services/holiday.service';

@Component({
  selector: 'app-holidays',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './holidays.component.html'
})
export class HolidaysComponent implements OnInit {
  loading = signal(true);
  saving = signal(false);
  error = signal('');
  list = signal<Holiday[]>([]);
  showForm = signal(false);
  editingId = signal<number | null>(null);
  form!: FormGroup;

  constructor(private fb: FormBuilder, private svc: HolidayService) {}

  ngOnInit(): void {
    this.form = this.fb.group({
      date: ['', Validators.required],
      name: ['', Validators.required]
    });
    this.refresh();
  }

  refresh(): void {
    this.loading.set(true);
    this.svc.list().subscribe({
      next: l => { this.list.set(l); this.loading.set(false); },
      error: () => { this.error.set('Failed to load holidays.'); this.loading.set(false); }
    });
  }

  openCreate(): void {
    this.editingId.set(null);
    this.form.reset({ date: '', name: '' });
    this.showForm.set(true);
  }

  openEdit(h: Holiday): void {
    this.editingId.set(h.id!);
    this.form.patchValue(h);
    this.showForm.set(true);
  }

  close(): void { this.showForm.set(false); }

  save(): void {
    if (this.form.invalid) { this.form.markAllAsTouched(); return; }
    this.saving.set(true);
    const v = this.form.value as Holiday;
    const obs = this.editingId() ? this.svc.update(this.editingId()!, v) : this.svc.create(v);
    obs.subscribe({
      next: () => { this.saving.set(false); this.close(); this.refresh(); },
      error: e => { this.saving.set(false); this.error.set(e?.error?.message || 'Failed to save.'); }
    });
  }

  remove(h: Holiday): void {
    if (!confirm(`Remove holiday "${h.name}" on ${h.date}?`)) return;
    this.svc.remove(h.id!).subscribe({ next: () => this.refresh() });
  }

  formatDate(iso: string): string {
    return new Date(iso).toLocaleDateString(undefined, { weekday: 'short', month: 'long', day: 'numeric', year: 'numeric' });
  }

  isPast(iso: string): boolean {
    return new Date(iso).getTime() < Date.now() - 24 * 60 * 60 * 1000;
  }
}
