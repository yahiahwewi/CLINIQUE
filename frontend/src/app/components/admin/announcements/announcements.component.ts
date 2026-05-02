import { Component, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { Announcement, AnnouncementService } from '../../../services/announcement.service';

@Component({
  selector: 'app-announcements',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './announcements.component.html'
})
export class AnnouncementsComponent implements OnInit {
  loading = signal(true);
  saving = signal(false);
  list = signal<Announcement[]>([]);
  showForm = signal(false);
  editingId = signal<number | null>(null);
  form!: FormGroup;
  error = signal('');

  constructor(private fb: FormBuilder, private svc: AnnouncementService) {}

  ngOnInit(): void {
    this.form = this.fb.group({
      title: ['', Validators.required],
      body: ['', Validators.required],
      audience: ['ALL', Validators.required],
      tone: ['INFO', Validators.required],
      active: [true],
      expiresAt: ['']
    });
    this.refresh();
  }

  refresh(): void {
    this.loading.set(true);
    this.svc.list().subscribe({
      next: l => { this.list.set(l); this.loading.set(false); },
      error: () => { this.error.set('Failed to load announcements.'); this.loading.set(false); }
    });
  }

  openCreate(): void {
    this.editingId.set(null);
    this.form.reset({ title: '', body: '', audience: 'ALL', tone: 'INFO', active: true, expiresAt: '' });
    this.showForm.set(true);
  }

  openEdit(a: Announcement): void {
    this.editingId.set(a.id!);
    this.form.patchValue({
      ...a,
      expiresAt: a.expiresAt ? a.expiresAt.slice(0, 16) : ''
    });
    this.showForm.set(true);
  }

  close(): void { this.showForm.set(false); }

  save(): void {
    if (this.form.invalid) { this.form.markAllAsTouched(); return; }
    this.saving.set(true);
    const v = this.form.value;
    const payload: Announcement = {
      title: v.title,
      body: v.body,
      audience: v.audience,
      tone: v.tone,
      active: !!v.active,
      expiresAt: v.expiresAt ? v.expiresAt + ':00' : null
    };
    const obs = this.editingId() ? this.svc.update(this.editingId()!, payload) : this.svc.create(payload);
    obs.subscribe({
      next: () => { this.saving.set(false); this.close(); this.refresh(); },
      error: e => { this.saving.set(false); this.error.set(e?.error?.message || 'Failed to save.'); }
    });
  }

  remove(a: Announcement): void {
    if (!confirm(`Delete "${a.title}"?`)) return;
    this.svc.remove(a.id!).subscribe({ next: () => this.refresh() });
  }

  toneClass(tone: string | undefined): string {
    if (tone === 'WARNING') return 'border-l-amber-500 text-amber-700';
    if (tone === 'SUCCESS') return 'border-l-sage-500 text-sage-700';
    return 'border-l-lumen-500 text-lumen-700';
  }

  formatDate(iso?: string): string {
    if (!iso) return '—';
    return new Date(iso).toLocaleString(undefined, { month: 'short', day: 'numeric', year: 'numeric', hour: '2-digit', minute: '2-digit' });
  }
}
