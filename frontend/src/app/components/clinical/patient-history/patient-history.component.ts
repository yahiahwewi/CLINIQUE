import { Component, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { forkJoin, of } from 'rxjs';
import { catchError } from 'rxjs/operators';

import { AuthService } from '../../../services/auth.service';
import { MedicalRecord, MedicalRecordService } from '../../../services/medical-record.service';
import { Prescription, PrescriptionService } from '../../../services/prescription.service';
import { LabOrder, LabService } from '../../../services/lab.service';

interface TimelineEvent {
  date: string;
  kind: 'visit' | 'prescription' | 'lab';
  title: string;
  body?: string;
  ref?: any;
}

@Component({
  selector: 'app-patient-history',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './patient-history.component.html'
})
export class PatientHistoryComponent implements OnInit {
  loading = signal(true);
  error = signal('');
  events = signal<TimelineEvent[]>([]);

  constructor(
    private auth: AuthService,
    private records: MedicalRecordService,
    private rxs: PrescriptionService,
    private labs: LabService
  ) {}

  ngOnInit(): void {
    const me = this.auth.getCurrentUser();
    if (!me) { this.loading.set(false); return; }

    forkJoin({
      records: this.records.forPatient(me.id).pipe(catchError(() => of([] as MedicalRecord[]))),
      prescriptions: this.rxs.byPatient(me.id).pipe(catchError(() => of([] as Prescription[]))),
      labs: this.labs.byPatient(me.id).pipe(catchError(() => of([] as LabOrder[])))
    }).subscribe({
      next: ({ records, prescriptions, labs }) => {
        const events: TimelineEvent[] = [];

        records.forEach(r => events.push({
          date: r.appointmentDateTime || r.createdAt || '',
          kind: 'visit',
          title: 'Visit with Dr. ' + (r.doctorName || ''),
          body: r.patientSummary || r.diagnosis || r.chiefComplaint,
          ref: r
        }));

        prescriptions.forEach(rx => events.push({
          date: rx.createdAt || '',
          kind: 'prescription',
          title: 'Prescription from Dr. ' + (rx.doctorName || ''),
          body: rx.items.map(i => i.drugName + (i.dose ? ' ' + i.dose : '')).join(', '),
          ref: rx
        }));

        labs.forEach(o => events.push({
          date: o.completedAt || o.createdAt || '',
          kind: 'lab',
          title: o.testName,
          body: o.resultText
              ? (o.abnormal ? '⚠ Abnormal: ' : '') + o.resultText
              : 'Status: ' + (o.status || 'ordered').toLowerCase(),
          ref: o
        }));

        events.sort((a, b) => new Date(b.date).getTime() - new Date(a.date).getTime());
        this.events.set(events);
        this.loading.set(false);
      },
      error: () => { this.error.set('Failed to load history.'); this.loading.set(false); }
    });
  }

  iconFor(k: TimelineEvent['kind']): string {
    switch (k) {
      case 'visit': return 'M9 5h6a2 2 0 0 1 2 2v13l-5-3-5 3V7a2 2 0 0 1 2-2z';
      case 'prescription': return 'M3 12h6m6 0h6M3 18h6m6 0h6M3 6h18';
      case 'lab': return 'M9 3h6v3l4 13a2 2 0 0 1-2 3H7a2 2 0 0 1-2-3l4-13V3z';
    }
  }

  toneClass(k: TimelineEvent['kind']): string {
    if (k === 'prescription') return 'bg-lumen-100 text-lumen-700';
    if (k === 'lab') return 'bg-amber-100 text-amber-700';
    return 'bg-sage-500/15 text-sage-600';
  }

  pdfHref(rx: Prescription): string { return this.rxs.pdfUrl(rx.id!); }
  isRx(e: TimelineEvent): boolean { return e.kind === 'prescription'; }

  fmt(iso?: string): string {
    if (!iso) return '—';
    const d = new Date(iso);
    return d.toLocaleString(undefined, { weekday: 'short', month: 'short', day: 'numeric', hour: '2-digit', minute: '2-digit' });
  }
}
