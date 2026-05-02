import { Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'lumen-brand-mark',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="flex items-center gap-2.5" [class.justify-center]="iconOnly">
      <span class="relative inline-flex h-9 w-9 items-center justify-center rounded-xl bg-lumen-700">
        <svg viewBox="0 0 32 32" class="h-5 w-5" fill="none" stroke="#F4EFE6" stroke-width="2.5" stroke-linecap="round">
          <path d="M11 16a5 5 0 0 1 10 0" />
          <path d="M16 11a5 5 0 0 1 0 10" />
        </svg>
      </span>
      <div *ngIf="!iconOnly" class="leading-tight">
        <p class="font-display text-[1.05rem] font-semibold text-lumen-900 tracking-tight">Lumen Health</p>
        <p class="text-[0.6875rem] uppercase tracking-[0.18em] text-ink-500">Care, coordinated.</p>
      </div>
    </div>
  `
})
export class BrandMarkComponent {
  @Input() iconOnly = false;
}
