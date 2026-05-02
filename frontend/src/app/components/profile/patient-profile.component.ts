import { Component, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule } from '@angular/forms';
import { ProfileService, PatientProfile } from '../../services/profile.service';

@Component({
  selector: 'app-patient-profile',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './patient-profile.component.html'
})
export class PatientProfileComponent implements OnInit {
  loading = signal(true);
  saving = signal(false);
  saved = signal(false);
  error = signal('');
  form!: FormGroup;
  base = signal<PatientProfile | null>(null);

  readonly bloodTypes = ['A+','A-','B+','B-','AB+','AB-','O+','O-','Unknown'];
  readonly genders = ['Female', 'Male', 'Non-binary', 'Prefer not to say'];

  constructor(private fb: FormBuilder, private svc: ProfileService) {}

  ngOnInit(): void {
    this.form = this.fb.group({
      dateOfBirth: [''],
      gender: [''],
      bloodType: [''],
      allergies: [''],
      chronicConditions: [''],
      emergencyContactName: [''],
      emergencyContactPhone: ['']
    });

    this.svc.getMyPatientProfile().subscribe({
      next: p => {
        this.base.set(p);
        this.form.patchValue(p);
        this.loading.set(false);
      },
      error: () => { this.error.set('Failed to load profile.'); this.loading.set(false); }
    });
  }

  save(): void {
    this.saving.set(true);
    this.saved.set(false);
    this.error.set('');
    const payload: PatientProfile = { ...(this.base() ?? { userId: 0 }), ...this.form.value };
    this.svc.saveMyPatientProfile(payload).subscribe({
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
