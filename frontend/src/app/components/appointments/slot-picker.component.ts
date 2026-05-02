import { Component, EventEmitter, Input, OnChanges, Output, SimpleChanges, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { AvailabilityService, TimeSlot } from '../../services/availability.service';

@Component({
  selector: 'lumen-slot-picker',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <div class="space-y-3">
      <div class="flex items-end gap-3">
        <button type="button" (click)="stepDay(-1)"
                class="lumen-btn lumen-btn-ghost !px-2.5 !py-2"
                aria-label="Previous day">
          <svg class="h-4 w-4" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round"><path d="M15 6l-6 6 6 6"/></svg>
        </button>
        <div class="flex-1">
          <label class="lumen-label">Date</label>
          <input
            type="date"
            [min]="minDate"
            [ngModel]="date()"
            (ngModelChange)="onDateChange($event)"
            class="lumen-input"
          />
          <p class="mt-1 text-[11px] uppercase tracking-wider text-ink-500">{{ humanDate() }}</p>
        </div>
        <button type="button" (click)="stepDay(1)"
                class="lumen-btn lumen-btn-ghost !px-2.5 !py-2"
                aria-label="Next day">
          <svg class="h-4 w-4" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round"><path d="M9 6l6 6-6 6"/></svg>
        </button>
      </div>

      <div *ngIf="selected()" class="rounded-lg bg-lumen-50 px-3 py-2 text-sm">
        <span class="text-[11px] uppercase tracking-wider text-ink-500">Selected · </span>
        <span class="font-display font-semibold text-lumen-900">{{ selectedDisplay() }}</span>
      </div>

      <div *ngIf="!doctorId" class="rounded-lg border border-dashed border-lumen-700/20 bg-canvas-50 px-4 py-6 text-center text-sm text-ink-500">
        Pick a doctor first to see open slots.
      </div>

      <div *ngIf="doctorId && loading()" class="grid grid-cols-3 gap-2 sm:grid-cols-4">
        <div *ngFor="let i of [1,2,3,4,5,6,7,8]" class="lumen-skeleton h-10"></div>
      </div>

      <div *ngIf="doctorId && !loading() && slots().length === 0"
           class="rounded-lg border border-dashed border-lumen-700/20 bg-canvas-50 px-4 py-6 text-center text-sm text-ink-500">
        No availability on {{ humanDate() }}.
        <button type="button" (click)="stepDay(1)" class="ml-1 font-medium text-lumen-700 underline">Try the next day →</button>
      </div>

      <div *ngIf="doctorId && !loading() && slots().length > 0" class="grid grid-cols-3 gap-2 sm:grid-cols-4">
        <button *ngFor="let slot of slots()"
                type="button"
                (click)="select(slot)"
                [disabled]="!slot.available"
                [class.lumen-btn-primary]="isSelected(slot)"
                class="lumen-btn lumen-btn-ghost !px-3 !py-2 !text-sm tabular-nums"
                [class.opacity-40]="!slot.available"
                [class.cursor-not-allowed]="!slot.available"
                [title]="slot.available ? 'Pick this slot' : 'Already booked'">
          {{ formatSlot(slot.start) }}
        </button>
      </div>
    </div>
  `
})
export class SlotPickerComponent implements OnChanges {
  @Input() doctorId: number | null = null;
  @Input() initialSlot: string | null = null;
  @Output() slotSelected = new EventEmitter<string>();

  date = signal(this.tomorrowIso());
  loading = signal(false);
  slots = signal<TimeSlot[]>([]);
  selected = signal<string | null>(null);

  readonly minDate = this.todayIso();

  constructor(private svc: AvailabilityService) {}

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['initialSlot'] && this.initialSlot) {
      const isoDate = this.initialSlot.slice(0, 10);
      this.date.set(isoDate);
      this.selected.set(this.initialSlot);
    }
    if (this.doctorId) this.fetch();
  }

  onDateChange(value: string): void {
    this.date.set(value);
    this.fetch();
  }

  stepDay(delta: number): void {
    const d = new Date(this.date() + 'T00:00:00');
    d.setDate(d.getDate() + delta);
    const today = new Date(this.minDate + 'T00:00:00');
    if (d < today) return;
    this.date.set(this.toIso(d));
    this.fetch();
  }

  humanDate(): string {
    const d = new Date(this.date() + 'T00:00:00');
    return d.toLocaleDateString(undefined, { weekday: 'long', month: 'long', day: 'numeric' });
  }

  fetch(): void {
    if (!this.doctorId) return;
    this.loading.set(true);
    this.svc.getSlots(this.doctorId, this.date()).subscribe({
      next: list => { this.slots.set(list); this.loading.set(false); },
      error: () => { this.slots.set([]); this.loading.set(false); }
    });
  }

  select(slot: TimeSlot): void {
    if (!slot.available) return;
    const value = slot.start;
    this.selected.set(value);
    this.slotSelected.emit(value);
  }

  isSelected(slot: TimeSlot): boolean {
    if (!this.selected()) return false;
    return this.selected()!.startsWith(slot.start.slice(0, 16));
  }

  selectedDisplay(): string {
    if (!this.selected()) return '';
    const d = new Date(this.selected()!);
    return d.toLocaleString(undefined, { weekday: 'short', month: 'short', day: 'numeric', hour: 'numeric', minute: '2-digit' });
  }

  formatSlot(iso: string): string {
    const d = new Date(iso);
    return d.toLocaleTimeString(undefined, { hour: 'numeric', minute: '2-digit' });
  }

  private todayIso(): string { return this.toIso(new Date()); }
  private tomorrowIso(): string { const d = new Date(); d.setDate(d.getDate() + 1); return this.toIso(d); }
  private toIso(d: Date): string {
    const y = d.getFullYear();
    const m = String(d.getMonth() + 1).padStart(2, '0');
    const day = String(d.getDate()).padStart(2, '0');
    return `${y}-${m}-${day}`;
  }
}
