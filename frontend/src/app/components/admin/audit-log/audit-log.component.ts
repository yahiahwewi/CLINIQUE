import { Component, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { AuditLog, AuditService } from '../../../services/audit.service';

@Component({
  selector: 'app-audit-log',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './audit-log.component.html'
})
export class AuditLogComponent implements OnInit {
  loading = signal(true);
  error = signal('');
  logs = signal<AuditLog[]>([]);
  filter = signal('');

  constructor(private auditService: AuditService) {}

  ngOnInit(): void {
    this.refresh();
  }

  refresh(): void {
    this.loading.set(true);
    this.error.set('');
    this.auditService.recent(200).subscribe({
      next: list => { this.logs.set(list); this.loading.set(false); },
      error: () => { this.error.set('Failed to load audit log.'); this.loading.set(false); }
    });
  }

  filtered(): AuditLog[] {
    const q = this.filter().trim().toLowerCase();
    if (!q) return this.logs();
    return this.logs().filter(l =>
      l.action.toLowerCase().includes(q)
      || (l.summary || '').toLowerCase().includes(q)
      || (l.actorName || '').toLowerCase().includes(q)
      || (l.entityType || '').toLowerCase().includes(q)
    );
  }

  formatDate(iso: string): string {
    const d = new Date(iso);
    return d.toLocaleString(undefined, { month: 'short', day: 'numeric', hour: '2-digit', minute: '2-digit', second: '2-digit' });
  }

  actionTone(action: string): string {
    if (action.endsWith('_DELETED') || action.includes('CANCELLED')) return 'text-coral-600';
    if (action.endsWith('_CREATED')) return 'text-sage-600';
    if (action.endsWith('_UPDATED') || action.includes('CHANGED')) return 'text-lumen-700';
    return 'text-ink-700';
  }
}
