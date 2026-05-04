import { Component, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import {
  LabAnalysis, LabAnalyzerService, LabFinding, LabHealth, LabSample,
} from '../../../services/lab-analyzer.service';

@Component({
  selector: 'app-lab-analyzer',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './lab-analyzer.component.html'
})
export class LabAnalyzerComponent implements OnInit {
  health = signal<LabHealth | null>(null);
  samples = signal<LabSample[]>([]);
  serviceAvailable = signal(false);

  // upload state
  chosenFile = signal<File | null>(null);
  chosenLabel = signal<string>('');
  loadingSample = signal<string | null>(null);
  dragOver = signal(false);

  // analysis state
  analyzing = signal(false);
  result = signal<LabAnalysis | null>(null);
  error = signal<string>('');

  constructor(private svc: LabAnalyzerService) {}

  ngOnInit(): void {
    this.svc.health().subscribe(h => {
      this.health.set(h);
      this.serviceAvailable.set(!!h?.ok && !!h?.modelLoaded);
    });
    this.svc.listSamples().subscribe(list => this.samples.set(list));
  }

  /* ------------------- file selection ------------------- */
  onFileChange(ev: Event): void {
    const input = ev.target as HTMLInputElement;
    const file = input.files?.[0];
    if (file) this.setFile(file);
  }

  onDrop(ev: DragEvent): void {
    ev.preventDefault();
    this.dragOver.set(false);
    const file = ev.dataTransfer?.files?.[0];
    if (file) this.setFile(file);
  }

  onDragOver(ev: DragEvent): void {
    ev.preventDefault();
    this.dragOver.set(true);
  }

  onDragLeave(): void {
    this.dragOver.set(false);
  }

  async useSample(sample: LabSample): Promise<void> {
    this.loadingSample.set(sample.name);
    try {
      const file = await this.svc.fetchSampleAsFile(sample.name);
      this.setFile(file, sample.title);
    } catch {
      this.error.set('Could not load sample. Is the lab-ai service running?');
    } finally {
      this.loadingSample.set(null);
    }
  }

  private setFile(file: File, label?: string): void {
    this.chosenFile.set(file);
    this.chosenLabel.set(label || file.name);
    this.error.set('');
    this.result.set(null);
  }

  reset(): void {
    this.chosenFile.set(null);
    this.chosenLabel.set('');
    this.result.set(null);
    this.error.set('');
  }

  /* ------------------- analyze ------------------- */
  analyze(): void {
    const file = this.chosenFile();
    if (!file) return;
    this.analyzing.set(true);
    this.error.set('');
    this.result.set(null);
    this.svc.analyze(file).subscribe({
      next: r => { this.analyzing.set(false); this.result.set(r); },
      error: e => {
        this.analyzing.set(false);
        this.error.set(e?.error?.error || 'Analysis failed. Is the lab-ai service running on :5001?');
      }
    });
  }

  /* ------------------- view helpers ------------------- */
  urgencyClass(u: string | undefined): string {
    if (u === 'URGENT') return 'lumen-pill lumen-pill-cancelled';
    if (u === 'PROMPT') return 'lumen-pill lumen-pill-pending';
    return 'lumen-pill lumen-pill-accepted';
  }

  statusPill(f: LabFinding): string {
    if (f.status === 'low' || f.status === 'high') return 'lumen-pill lumen-pill-cancelled';
    if (f.status === 'normal') return 'lumen-pill lumen-pill-accepted';
    return 'lumen-pill';
  }

  sortedProbabilities(): { label: string; pct: number }[] {
    const r = this.result();
    if (!r) return [];
    return Object.entries(r.classProbabilities)
      .map(([label, p]) => ({ label, pct: p * 100 }))
      .sort((a, b) => b.pct - a.pct);
  }

  fileSizeKb(s: number): string {
    return (s / 1024).toFixed(1) + ' KB';
  }
}
