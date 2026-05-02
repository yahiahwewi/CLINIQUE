import { Component, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { Department, DepartmentService } from '../../../services/department.service';

@Component({
  selector: 'app-departments',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './departments.component.html'
})
export class DepartmentsComponent implements OnInit {
  loading = signal(true);
  saving = signal(false);
  error = signal('');
  list = signal<Department[]>([]);
  showForm = signal(false);
  editingId = signal<number | null>(null);
  form!: FormGroup;

  readonly suggestedColors = ['#0F4C5C', '#266B73', '#7BAE7F', '#E36F6F', '#B45309', '#7C3AED', '#0891B2'];
  readonly iconOptions = [
    { name: 'heart', label: 'Heart' },
    { name: 'baby', label: 'Baby' },
    { name: 'sun', label: 'Sun' },
    { name: 'stethoscope', label: 'Stethoscope' },
    { name: 'bone', label: 'Bone' },
    { name: 'brain', label: 'Brain' },
    { name: 'eye', label: 'Eye' },
    { name: 'tooth', label: 'Tooth' }
  ];

  constructor(private fb: FormBuilder, private svc: DepartmentService) {}

  ngOnInit(): void {
    this.form = this.fb.group({
      name: ['', Validators.required],
      description: [''],
      color: ['#0F4C5C'],
      icon: ['stethoscope']
    });
    this.refresh();
  }

  refresh(): void {
    this.loading.set(true);
    this.svc.list().subscribe({
      next: l => { this.list.set(l); this.loading.set(false); },
      error: () => { this.error.set('Failed to load departments.'); this.loading.set(false); }
    });
  }

  openCreate(): void {
    this.editingId.set(null);
    this.form.reset({ name: '', description: '', color: '#0F4C5C', icon: 'stethoscope' });
    this.showForm.set(true);
  }

  openEdit(d: Department): void {
    this.editingId.set(d.id);
    this.form.patchValue(d);
    this.showForm.set(true);
  }

  close(): void { this.showForm.set(false); }

  save(): void {
    if (this.form.invalid) { this.form.markAllAsTouched(); return; }
    this.saving.set(true);
    const v = this.form.value as Department;
    const obs = this.editingId() ? this.svc.update(this.editingId()!, v) : this.svc.create(v);
    obs.subscribe({
      next: () => { this.saving.set(false); this.close(); this.refresh(); },
      error: e => { this.saving.set(false); this.error.set(e?.error?.message || 'Failed to save.'); }
    });
  }

  remove(d: Department): void {
    if (!confirm(`Delete department "${d.name}"?`)) return;
    this.svc.remove(d.id).subscribe({ next: () => this.refresh() });
  }
}
