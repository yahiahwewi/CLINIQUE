import { Component, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule } from '@angular/forms';
import { ProfileService, DoctorProfile } from '../../services/profile.service';
import { Department, DepartmentService } from '../../services/department.service';
import { forkJoin } from 'rxjs';

@Component({
  selector: 'app-doctor-profile',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './doctor-profile.component.html'
})
export class DoctorProfileComponent implements OnInit {
  loading = signal(true);
  saving = signal(false);
  saved = signal(false);
  error = signal('');
  form!: FormGroup;
  base = signal<DoctorProfile | null>(null);
  departments = signal<Department[]>([]);
  selectedDepartmentIds = signal<Set<number>>(new Set());

  constructor(
    private fb: FormBuilder,
    private svc: ProfileService,
    private deptSvc: DepartmentService
  ) {}

  ngOnInit(): void {
    this.form = this.fb.group({
      specialty: [''],
      licenseNumber: [''],
      bio: [''],
      languages: [''],
      consultationFeeCents: [0],
      yearsExperience: [0],
      photoUrl: ['']
    });

    forkJoin({
      profile: this.svc.getMyDoctorProfile(),
      departments: this.deptSvc.list()
    }).subscribe({
      next: ({ profile, departments }) => {
        this.base.set(profile);
        this.departments.set(departments);
        this.form.patchValue(profile);
        this.selectedDepartmentIds.set(new Set(profile.departmentIds ?? []));
        this.loading.set(false);
      },
      error: () => { this.error.set('Failed to load profile.'); this.loading.set(false); }
    });
  }

  toggleDepartment(d: Department): void {
    const next = new Set(this.selectedDepartmentIds());
    if (next.has(d.id)) next.delete(d.id); else next.add(d.id);
    this.selectedDepartmentIds.set(next);
  }

  isSelected(d: Department): boolean {
    return this.selectedDepartmentIds().has(d.id);
  }

  feeEuros(cents: number | null | undefined): string {
    if (!cents) return '0.00';
    return (cents / 100).toFixed(2);
  }

  onFeeChange(value: string): void {
    const euros = parseFloat(value) || 0;
    this.form.patchValue({ consultationFeeCents: Math.round(euros * 100) });
  }

  save(): void {
    this.saving.set(true);
    this.saved.set(false);
    this.error.set('');
    const v = this.form.value;
    const payload: DoctorProfile = {
      ...(this.base() ?? { userId: 0 }),
      specialty: v.specialty,
      licenseNumber: v.licenseNumber,
      bio: v.bio,
      languages: v.languages,
      consultationFeeCents: Number(v.consultationFeeCents) || 0,
      yearsExperience: Number(v.yearsExperience) || 0,
      photoUrl: v.photoUrl,
      departmentIds: Array.from(this.selectedDepartmentIds())
    };
    this.svc.saveMyDoctorProfile(payload).subscribe({
      next: p => {
        this.base.set(p);
        this.saving.set(false);
        this.saved.set(true);
        setTimeout(() => this.saved.set(false), 2500);
      },
      error: e => {
        this.saving.set(false);
        this.error.set(e?.error?.message || 'Failed to save.');
      }
    });
  }
}
