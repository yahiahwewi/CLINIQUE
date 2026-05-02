import { Component, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { DataService, User } from '../../../services/data.service';

@Component({
  selector: 'app-approvals',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './approvals.component.html'
})
export class ApprovalsComponent implements OnInit {
  loading = signal(true);
  error = signal('');
  pending = signal<User[]>([]);
  busyId = signal<number | null>(null);

  constructor(private data: DataService) {}

  ngOnInit(): void {
    this.refresh();
  }

  refresh(): void {
    this.loading.set(true);
    this.error.set('');
    this.data.adminGetPendingApprovals().subscribe({
      next: list => { this.pending.set(list); this.loading.set(false); },
      error: () => { this.error.set('Failed to load pending approvals.'); this.loading.set(false); }
    });
  }

  approve(user: User): void {
    if (this.busyId() !== null) return;
    this.busyId.set(user.id);
    this.data.adminApproveUser(user.id).subscribe({
      next: () => { this.busyId.set(null); this.removeFromList(user.id); },
      error: () => { this.busyId.set(null); this.error.set('Failed to approve account.'); }
    });
  }

  reject(user: User): void {
    if (this.busyId() !== null) return;
    if (!confirm(`Reject ${user.firstName} ${user.lastName}'s registration?`)) return;
    this.busyId.set(user.id);
    this.data.adminRejectUser(user.id).subscribe({
      next: () => { this.busyId.set(null); this.removeFromList(user.id); },
      error: () => { this.busyId.set(null); this.error.set('Failed to reject account.'); }
    });
  }

  private removeFromList(id: number): void {
    this.pending.set(this.pending().filter(u => u.id !== id));
  }

  prettyRole(requestedRole: string | undefined): string {
    if (!requestedRole) return 'Patient';
    const stripped = requestedRole.replace('ROLE_', '');
    if (stripped === 'USER') return 'Patient';
    return stripped.charAt(0) + stripped.slice(1).toLowerCase();
  }

  initials(user: User): string {
    return ((user.firstName?.[0] || '') + (user.lastName?.[0] || '')).toUpperCase() || '·';
  }
}
