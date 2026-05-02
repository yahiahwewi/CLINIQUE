import { Component, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Referral, ReferralService, ReferralStatus } from '../../../services/referral.service';
import { forkJoin, of } from 'rxjs';
import { catchError } from 'rxjs/operators';

@Component({
  selector: 'app-referrals',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './referrals.component.html'
})
export class ReferralsComponent implements OnInit {
  loading = signal(true);
  error = signal('');
  incoming = signal<Referral[]>([]);
  outgoing = signal<Referral[]>([]);
  busyId = signal<number | null>(null);
  tab = signal<'incoming' | 'outgoing'>('incoming');

  constructor(private svc: ReferralService) {}

  ngOnInit(): void { this.refresh(); }

  refresh(): void {
    this.loading.set(true);
    forkJoin({
      inc: this.svc.incoming().pipe(catchError(() => of([] as Referral[]))),
      out: this.svc.outgoing().pipe(catchError(() => of([] as Referral[])))
    }).subscribe({
      next: ({ inc, out }) => {
        this.incoming.set(inc);
        this.outgoing.set(out);
        this.loading.set(false);
      },
      error: () => { this.error.set('Failed to load referrals.'); this.loading.set(false); }
    });
  }

  setStatus(r: Referral, status: ReferralStatus): void {
    this.busyId.set(r.id!);
    this.svc.setStatus(r.id!, status).subscribe({
      next: () => { this.busyId.set(null); this.refresh(); },
      error: () => this.busyId.set(null)
    });
  }

  pillClass(s?: ReferralStatus): string {
    if (s === 'PENDING') return 'lumen-pill lumen-pill-pending';
    if (s === 'ACCEPTED' || s === 'COMPLETED') return 'lumen-pill lumen-pill-accepted';
    if (s === 'DECLINED') return 'lumen-pill lumen-pill-cancelled';
    return 'lumen-pill';
  }

  fmt(iso?: string): string {
    if (!iso) return '—';
    return new Date(iso).toLocaleString(undefined, { month: 'short', day: 'numeric', hour: '2-digit', minute: '2-digit' });
  }
}
