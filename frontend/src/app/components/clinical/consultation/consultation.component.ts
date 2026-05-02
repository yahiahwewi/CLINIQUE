import { Component, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators, FormArray, FormsModule } from '@angular/forms';
import { ActivatedRoute, RouterModule } from '@angular/router';
import { forkJoin, of } from 'rxjs';
import { catchError } from 'rxjs/operators';

import { MedicalRecord, MedicalRecordService } from '../../../services/medical-record.service';
import { Prescription, PrescriptionService, PrescriptionStatus } from '../../../services/prescription.service';
import { LabOrder, LabOrderStatus, LabService } from '../../../services/lab.service';
import { Referral, ReferralService } from '../../../services/referral.service';
import { DataService, UserOption } from '../../../services/data.service';
import { ProfileService, PatientProfile } from '../../../services/profile.service';
import { AiService } from '../../../services/ai.service';

@Component({
  selector: 'app-consultation',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, FormsModule, RouterModule],
  templateUrl: './consultation.component.html'
})
export class ConsultationComponent implements OnInit {
  appointmentId!: number;
  loading = signal(true);
  error = signal('');

  // Patient context
  patientId = signal<number | null>(null);
  patient = signal<PatientProfile | null>(null);
  appointmentTitle = signal('');
  appointmentDateTime = signal('');

  // Medical record
  recordForm!: FormGroup;
  recordSaving = signal(false);
  recordSaved = signal(false);
  patientSummary = signal('');

  // Prescriptions
  prescriptions = signal<Prescription[]>([]);
  prescriptionForm!: FormGroup;
  showPrescriptionForm = signal(false);
  rxSaving = signal(false);

  // Lab orders
  labOrders = signal<LabOrder[]>([]);
  labForm!: FormGroup;
  showLabForm = signal(false);
  labSaving = signal(false);

  // Referrals
  referrals = signal<Referral[]>([]);
  doctors = signal<UserOption[]>([]);
  referralForm!: FormGroup;
  showReferralForm = signal(false);
  referralSaving = signal(false);

  // AI summary
  aiAvailable = signal(false);
  aiBusy = signal(false);
  aiPreview = signal<string>('');

  constructor(
    private route: ActivatedRoute,
    private fb: FormBuilder,
    private records: MedicalRecordService,
    private rxs: PrescriptionService,
    private labs: LabService,
    private referralsSvc: ReferralService,
    private dataSvc: DataService,
    private profileSvc: ProfileService,
    private aiSvc: AiService
  ) {}

  ngOnInit(): void {
    this.appointmentId = Number(this.route.snapshot.paramMap.get('appointmentId'));
    this.initForms();
    this.loadAll();
    this.aiSvc.status().subscribe(s => this.aiAvailable.set(s.enabled));
  }

  initForms(): void {
    this.recordForm = this.fb.group({
      chiefComplaint: [''],
      diagnosis: [''],
      plan: [''],
      privateNotes: ['']
    });
    this.prescriptionForm = this.fb.group({
      instructions: [''],
      items: this.fb.array([this.newItemGroup()])
    });
    this.labForm = this.fb.group({
      testName: ['', Validators.required],
      instructions: ['']
    });
    this.referralForm = this.fb.group({
      toDoctorId: [null, Validators.required],
      reason: ['', Validators.required]
    });
  }

  loadAll(): void {
    this.loading.set(true);
    this.error.set('');

    forkJoin({
      record: this.records.getByAppointment(this.appointmentId).pipe(catchError(() => of(null as any))),
      rxList: this.rxs.byAppointment(this.appointmentId).pipe(catchError(() => of([] as Prescription[]))),
      labList: this.labs.byAppointment(this.appointmentId).pipe(catchError(() => of([] as LabOrder[]))),
      meta: this.dataSvc.getAppointmentMeta().pipe(catchError(() => of({ doctors: [], nurses: [] })))
    }).subscribe({
      next: ({ record, rxList, labList, meta }) => {
        if (record) {
          this.recordForm.patchValue({
            chiefComplaint: record.chiefComplaint || '',
            diagnosis: record.diagnosis || '',
            plan: record.plan || '',
            privateNotes: record.privateNotes || ''
          });
          this.patientSummary.set(record.patientSummary || '');
          this.appointmentTitle.set(this.titleFromRecord(record));
          this.appointmentDateTime.set(record.appointmentDateTime || '');
          // patientId comes from the rx list or appointment meta — we hydrate below
        }
        this.prescriptions.set(rxList);
        this.labOrders.set(labList);
        this.doctors.set(meta.doctors || []);

        // Patient ID: pick from any rx, lab, or call /appointments/meta to get from appointment list
        const inferredPatientId = rxList[0]?.patientId ?? labList[0]?.patientId ?? null;
        if (inferredPatientId != null) {
          this.patientId.set(inferredPatientId);
          this.profileSvc.getPatientProfile(inferredPatientId).subscribe({
            next: p => this.patient.set(p),
            error: () => {}
          });
          this.referralsSvc.forPatient(inferredPatientId).subscribe(refs => this.referrals.set(refs));
        }

        this.loading.set(false);
      },
      error: () => { this.error.set('Failed to load consultation.'); this.loading.set(false); }
    });
  }

  private titleFromRecord(record: MedicalRecord): string {
    return ''; // backend doesn't put title on record DTO; appointment page already shows it
  }

  /* --------------------- Medical record --------------------- */
  saveRecord(): void {
    this.recordSaving.set(true);
    this.recordSaved.set(false);
    this.records.upsert(this.appointmentId, {
      ...this.recordForm.value,
      appointmentId: this.appointmentId
    }).subscribe({
      next: r => {
        this.recordSaving.set(false);
        this.recordSaved.set(true);
        this.patientSummary.set(r.patientSummary || this.patientSummary());
        setTimeout(() => this.recordSaved.set(false), 2500);
      },
      error: e => { this.recordSaving.set(false); this.error.set(e?.error?.message || 'Failed to save record.'); }
    });
  }

  /* --------------------- Prescriptions --------------------- */
  get prescriptionItems(): FormArray { return this.prescriptionForm.get('items') as FormArray; }

  newItemGroup(): FormGroup {
    return this.fb.group({
      drugName: ['', Validators.required],
      dose: [''],
      frequency: [''],
      durationDays: [null],
      notes: ['']
    });
  }

  addRxItem(): void { this.prescriptionItems.push(this.newItemGroup()); }
  removeRxItem(i: number): void { if (this.prescriptionItems.length > 1) this.prescriptionItems.removeAt(i); }

  openRxForm(): void {
    this.prescriptionForm.reset({ instructions: '' });
    while (this.prescriptionItems.length > 0) this.prescriptionItems.removeAt(0);
    this.prescriptionItems.push(this.newItemGroup());
    this.showPrescriptionForm.set(true);
  }
  closeRxForm(): void { this.showPrescriptionForm.set(false); }

  saveRx(): void {
    if (this.prescriptionForm.invalid) { this.prescriptionForm.markAllAsTouched(); return; }
    this.rxSaving.set(true);
    const v = this.prescriptionForm.value;
    this.rxs.create({
      appointmentId: this.appointmentId,
      instructions: v.instructions,
      items: (v.items || []).map((i: any) => ({
        drugName: i.drugName,
        dose: i.dose || undefined,
        frequency: i.frequency || undefined,
        durationDays: i.durationDays ? Number(i.durationDays) : undefined,
        notes: i.notes || undefined
      }))
    }).subscribe({
      next: rx => {
        this.rxSaving.set(false);
        this.prescriptions.set([rx, ...this.prescriptions()]);
        this.closeRxForm();
      },
      error: e => { this.rxSaving.set(false); this.error.set(e?.error?.message || 'Failed to save prescription.'); }
    });
  }

  cancelRx(rx: Prescription): void {
    if (!confirm(`Cancel this prescription?`)) return;
    this.rxs.cancel(rx.id!).subscribe({ next: () => { rx.status = 'CANCELLED'; this.prescriptions.set([...this.prescriptions()]); } });
  }

  pdfUrl(rx: Prescription): string { return this.rxs.pdfUrl(rx.id!); }

  rxStatusClass(s: PrescriptionStatus | undefined): string {
    if (s === 'DISPENSED') return 'lumen-pill lumen-pill-accepted';
    if (s === 'CANCELLED') return 'lumen-pill lumen-pill-cancelled';
    if (s === 'ACTIVE') return 'lumen-pill lumen-pill-pending';
    return 'lumen-pill';
  }

  /* --------------------- Lab orders --------------------- */
  openLabForm(): void {
    this.labForm.reset({ testName: '', instructions: '' });
    this.showLabForm.set(true);
  }
  closeLabForm(): void { this.showLabForm.set(false); }

  saveLab(): void {
    if (this.labForm.invalid) { this.labForm.markAllAsTouched(); return; }
    this.labSaving.set(true);
    this.labs.create({
      appointmentId: this.appointmentId,
      testName: this.labForm.value.testName,
      instructions: this.labForm.value.instructions
    }).subscribe({
      next: o => { this.labSaving.set(false); this.labOrders.set([o, ...this.labOrders()]); this.closeLabForm(); },
      error: e => { this.labSaving.set(false); this.error.set(e?.error?.message || 'Failed to save lab order.'); }
    });
  }

  labStatusClass(s: LabOrderStatus | undefined): string {
    if (s === 'COMPLETED') return 'lumen-pill lumen-pill-accepted';
    if (s === 'IN_PROGRESS') return 'lumen-pill lumen-pill-pending';
    if (s === 'CANCELLED') return 'lumen-pill lumen-pill-cancelled';
    return 'lumen-pill';
  }

  /* --------------------- Referrals --------------------- */
  openReferralForm(): void {
    this.referralForm.reset({ toDoctorId: null, reason: '' });
    this.showReferralForm.set(true);
  }
  closeReferralForm(): void { this.showReferralForm.set(false); }

  saveReferral(): void {
    if (this.referralForm.invalid || !this.patientId()) {
      this.referralForm.markAllAsTouched();
      return;
    }
    this.referralSaving.set(true);
    this.referralsSvc.create({
      toDoctorId: Number(this.referralForm.value.toDoctorId),
      patientId: this.patientId()!,
      appointmentId: this.appointmentId,
      reason: this.referralForm.value.reason
    }).subscribe({
      next: r => { this.referralSaving.set(false); this.referrals.set([r, ...this.referrals()]); this.closeReferralForm(); },
      error: e => { this.referralSaving.set(false); this.error.set(e?.error?.message || 'Failed to send referral.'); }
    });
  }

  /* --------------------- AI summary --------------------- */
  generateSummary(save: boolean): void {
    this.aiBusy.set(true);
    this.aiSvc.visitSummary({ appointmentId: this.appointmentId, save }).subscribe({
      next: r => {
        this.aiBusy.set(false);
        this.aiPreview.set(r.summary);
        if (r.saved) this.patientSummary.set(r.summary);
      },
      error: e => { this.aiBusy.set(false); this.error.set(e?.error?.message || 'AI summary failed.'); }
    });
  }

  /* --------------------- formatting --------------------- */
  fmt(iso?: string | null): string {
    if (!iso) return '—';
    const d = new Date(iso);
    return d.toLocaleString(undefined, { month: 'short', day: 'numeric', hour: '2-digit', minute: '2-digit' });
  }

  age(dob?: string): number | null {
    if (!dob) return null;
    const d = new Date(dob);
    const ageDiff = Date.now() - d.getTime();
    return Math.abs(new Date(ageDiff).getUTCFullYear() - 1970);
  }
}
