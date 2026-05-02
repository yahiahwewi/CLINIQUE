import { Component, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { TimeOff, TimeOffService, TimeOffStatus } from '../../../services/time-off.service';

@Component({
  selector: 'app-time-off-approvals',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './time-off-approvals.component.html'
})
export class TimeOffApprovalsComponent implements OnInit {
  loading = signal(true);
  error = signal('');
  status = signal<TimeOffStatus>('PENDING');
  list = signal<TimeOff[]>([]);
  busyId = signal<number | null>(null);

  constructor(private svc: TimeOffService) {}

  ngOnInit(): void { this.refresh(); }

  setStatus(s: TimeOffStatus): void {
    this.status.set(s);
    this.refresh();
  }

  refresh(): void {
    this.loading.set(true);
    this.svc.byStatus(this.status()).subscribe({
      next: l => { this.list.set(l); this.loading.set(false); },
      error: () => { this.error.set('Failed to load time-off requests.'); this.loading.set(false); }
    });
  }

  approve(t: TimeOff): void {
    this.busyId.set(t.id!);
    this.svc.approve(t.id!).subscribe({
      next: () => { this.busyId.set(null); this.refresh(); },
      error: () => { this.busyId.set(null); this.error.set('Failed to approve.'); }
    });
  }

  reject(t: TimeOff): void {
    const note = prompt('Reason for rejecting (optional):') || undefined;
    this.busyId.set(t.id!);
    this.svc.reject(t.id!, note).subscribe({
      next: () => { this.busyId.set(null); this.refresh(); },
      error: () => { this.busyId.set(null); this.error.set('Failed to reject.'); }
    });
  }

  formatRange(t: TimeOff): string {
    return `${this.fmt(t.startDate)} → ${this.fmt(t.endDate)}`;
  }

  private fmt(iso: string): string {
    return new Date(iso).toLocaleDateString(undefined, { month: 'short', day: 'numeric', year: 'numeric' });
  }

  daysSpanned(t: TimeOff): number {
    const ms = new Date(t.endDate).getTime() - new Date(t.startDate).getTime();
    return Math.round(ms / (24 * 60 * 60 * 1000)) + 1;
  }

  pillClass(s?: TimeOffStatus): string {
    if (s === 'APPROVED') return 'lumen-pill lumen-pill-accepted';
    if (s === 'REJECTED') return 'lumen-pill lumen-pill-cancelled';
    return 'lumen-pill lumen-pill-pending';
  }
}
