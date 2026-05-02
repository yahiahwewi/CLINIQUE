import { Component, EventEmitter, Input, OnInit, Output, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { AiService, TriageResponse, Urgency } from '../../services/ai.service';

export interface TriageOutcome {
  suggestedDepartmentId: number | null;
  suggestedDepartment: string;
  draftChiefComplaint: string;
}

@Component({
  selector: 'lumen-triage-widget',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './triage-widget.component.html'
})
export class TriageWidgetComponent implements OnInit {
  @Input() prefillSymptoms = '';
  @Output() outcome = new EventEmitter<TriageOutcome>();

  symptoms = '';
  age: number | null = null;
  gender = '';
  busy = signal(false);
  result = signal<TriageResponse | null>(null);
  error = signal('');
  enabled = signal(true);
  expanded = signal(false);

  constructor(private ai: AiService) {}

  ngOnInit(): void {
    this.symptoms = this.prefillSymptoms || '';
    this.ai.status().subscribe(s => this.enabled.set(s.enabled));
  }

  toggle(): void { this.expanded.update(v => !v); }

  run(): void {
    if (!this.symptoms.trim()) { this.error.set('Describe your symptoms first.'); return; }
    this.busy.set(true);
    this.error.set('');
    this.ai.triage({
      symptoms: this.symptoms.trim(),
      ageYears: this.age,
      gender: this.gender || null
    }).subscribe({
      next: r => { this.busy.set(false); this.result.set(r); },
      error: e => { this.busy.set(false); this.error.set(e?.error?.message || 'AI triage failed.'); }
    });
  }

  apply(): void {
    const r = this.result();
    if (!r) return;
    this.outcome.emit({
      suggestedDepartmentId: r.suggestedDepartmentId,
      suggestedDepartment: r.suggestedDepartment,
      draftChiefComplaint: r.draftChiefComplaint
    });
  }

  urgencyClass(u: Urgency | undefined): string {
    if (u === 'URGENT') return 'lumen-pill lumen-pill-cancelled';
    if (u === 'NORMAL') return 'lumen-pill lumen-pill-pending';
    return 'lumen-pill lumen-pill-accepted';
  }
}
