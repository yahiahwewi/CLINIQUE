import { Component, ElementRef, HostListener, OnDestroy, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { InboxService, InboxNotification } from '../../services/inbox.service';
import { Subject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';

@Component({
  selector: 'lumen-notification-bell',
  standalone: true,
  imports: [CommonModule, RouterModule],
  template: `
    <div class="relative" #host>
      <button
        type="button"
        (click)="togglePanel()"
        class="relative inline-flex h-10 w-10 items-center justify-center rounded-full border border-lumen-700/12 bg-white text-lumen-700 transition hover:bg-lumen-50"
        aria-label="Notifications"
      >
        <svg class="h-5 w-5" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">
          <path d="M6 8a6 6 0 0 1 12 0c0 7 3 8 3 8H3s3-1 3-8" />
          <path d="M10.3 21a1.94 1.94 0 0 0 3.4 0" />
        </svg>
        <span *ngIf="unread() > 0"
              class="absolute -top-0.5 -right-0.5 inline-flex h-4 min-w-4 items-center justify-center rounded-full bg-coral-500 px-1 text-[10px] font-semibold text-white">
          {{ unread() > 99 ? '99+' : unread() }}
        </span>
      </button>

      <div *ngIf="open()"
           class="absolute right-0 z-30 mt-2 w-80 overflow-hidden rounded-xl border border-lumen-700/8 bg-white shadow-xl">
        <div class="flex items-center justify-between border-b border-lumen-700/8 px-4 py-3">
          <p class="font-display text-sm font-semibold text-lumen-900">Notifications</p>
          <button *ngIf="unread() > 0" (click)="markAllRead()" class="text-xs font-medium text-lumen-700 hover:text-lumen-900">Mark all read</button>
        </div>

        <div class="max-h-80 overflow-y-auto">
          <div *ngIf="loading()" class="space-y-3 p-4">
            <div class="lumen-skeleton h-4 w-1/2"></div>
            <div class="lumen-skeleton h-3 w-3/4"></div>
            <div class="lumen-skeleton h-3 w-2/3"></div>
          </div>

          <div *ngIf="!loading() && items().length === 0" class="px-4 py-8 text-center text-sm text-ink-500">
            You're all caught up.
          </div>

          <ul class="lumen-hairline">
            <li *ngFor="let n of items()"
                class="px-4 py-3 transition hover:bg-canvas-50"
                [class.bg-lumen-50]="!n.isRead">
              <a [routerLink]="n.link || '/dashboard/overview'" (click)="open.set(false)" class="block">
                <p class="text-sm font-semibold text-lumen-900">{{ n.title }}</p>
                <p class="mt-0.5 text-sm text-ink-700">{{ n.body }}</p>
                <p class="mt-1 text-[11px] uppercase tracking-wide text-ink-500">{{ formatTime(n.createdAt) }}</p>
              </a>
            </li>
          </ul>
        </div>
      </div>
    </div>
  `
})
export class NotificationBellComponent implements OnInit, OnDestroy {
  open = signal(false);
  loading = signal(false);
  items = signal<InboxNotification[]>([]);
  unread = signal(0);
  private destroy$ = new Subject<void>();

  constructor(private inboxService: InboxService, private host: ElementRef<HTMLElement>) {}

  ngOnInit(): void {
    this.inboxService.unreadCount$.pipe(takeUntil(this.destroy$)).subscribe(c => this.unread.set(c));
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  togglePanel(): void {
    const next = !this.open();
    this.open.set(next);
    if (next) this.fetch();
  }

  fetch(): void {
    this.loading.set(true);
    this.inboxService.list().subscribe({
      next: list => { this.items.set(list); this.loading.set(false); },
      error: () => { this.items.set([]); this.loading.set(false); }
    });
  }

  markAllRead(): void {
    this.inboxService.markAllRead().subscribe({
      next: () => this.fetch()
    });
  }

  formatTime(iso: string): string {
    const d = new Date(iso);
    const diffMs = Date.now() - d.getTime();
    const diffMin = Math.round(diffMs / 60000);
    if (diffMin < 1) return 'just now';
    if (diffMin < 60) return `${diffMin}m ago`;
    if (diffMin < 60 * 24) return `${Math.round(diffMin / 60)}h ago`;
    return d.toLocaleDateString(undefined, { month: 'short', day: 'numeric' });
  }

  @HostListener('document:click', ['$event'])
  onDocClick(ev: MouseEvent): void {
    if (this.open() && !this.host.nativeElement.contains(ev.target as Node)) {
      this.open.set(false);
    }
  }
}
