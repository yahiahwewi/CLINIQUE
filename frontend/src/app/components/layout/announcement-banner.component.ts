import { Component, OnInit, OnDestroy, signal } from '@angular/core';
import { CommonModule, isPlatformBrowser } from '@angular/common';
import { inject, PLATFORM_ID } from '@angular/core';
import { Announcement, AnnouncementService } from '../../services/announcement.service';

@Component({
  selector: 'lumen-announcement-banner',
  standalone: true,
  imports: [CommonModule],
  template: `
    <ng-container *ngIf="visible().length > 0">
      <div *ngFor="let a of visible()"
           class="mb-4 flex items-start gap-3 rounded-lg border-l-4 px-4 py-3 text-sm"
           [ngClass]="toneClasses(a.tone)">
        <span class="mt-0.5 inline-flex h-5 w-5 items-center justify-center">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" class="h-4 w-4">
            <path d="M3 11l18-8-3 18-7-7-8-3z" />
          </svg>
        </span>
        <div class="min-w-0 flex-1">
          <p class="font-semibold">{{ a.title }}</p>
          <p class="text-ink-700">{{ a.body }}</p>
        </div>
        <button (click)="dismiss(a)" class="text-ink-500 hover:text-ink-900" aria-label="Dismiss">
          <svg class="h-4 w-4" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M6 18L18 6M6 6l12 12"/></svg>
        </button>
      </div>
    </ng-container>
  `
})
export class AnnouncementBannerComponent implements OnInit, OnDestroy {
  private static readonly DISMISSED_KEY = 'lumen.dismissedAnnouncements';
  private platformId = inject(PLATFORM_ID);

  active = signal<Announcement[]>([]);
  dismissed = signal<Set<number>>(new Set());
  private intervalRef: any;

  constructor(private svc: AnnouncementService) {}

  ngOnInit(): void {
    this.dismissed.set(this.loadDismissed());
    this.fetch();
    this.intervalRef = setInterval(() => this.fetch(), 5 * 60 * 1000);
  }

  ngOnDestroy(): void {
    if (this.intervalRef) clearInterval(this.intervalRef);
  }

  visible(): Announcement[] {
    const dismissed = this.dismissed();
    return this.active().filter(a => !dismissed.has(a.id!));
  }

  dismiss(a: Announcement): void {
    if (a.id == null) return;
    const next = new Set(this.dismissed());
    next.add(a.id);
    this.dismissed.set(next);
    this.persistDismissed(next);
  }

  toneClasses(tone: string | undefined): string {
    if (tone === 'WARNING') return 'bg-amber-50 border-l-amber-500 text-amber-700';
    if (tone === 'SUCCESS') return 'bg-sage-500/8 border-l-sage-500 text-sage-700';
    return 'bg-lumen-50 border-l-lumen-500 text-lumen-700';
  }

  private fetch(): void {
    this.svc.active().subscribe({
      next: list => this.active.set(list),
      error: () => {}
    });
  }

  private loadDismissed(): Set<number> {
    if (!isPlatformBrowser(this.platformId)) return new Set();
    try {
      const raw = localStorage.getItem(AnnouncementBannerComponent.DISMISSED_KEY);
      if (!raw) return new Set();
      return new Set<number>(JSON.parse(raw) as number[]);
    } catch {
      return new Set();
    }
  }

  private persistDismissed(set: Set<number>): void {
    if (!isPlatformBrowser(this.platformId)) return;
    localStorage.setItem(AnnouncementBannerComponent.DISMISSED_KEY, JSON.stringify(Array.from(set)));
  }
}
