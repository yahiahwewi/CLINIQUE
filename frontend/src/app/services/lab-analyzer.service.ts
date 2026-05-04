import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, of } from 'rxjs';
import { catchError } from 'rxjs/operators';

export type LabUrgency = 'ROUTINE' | 'PROMPT' | 'URGENT';
export type LabValueStatus = 'low' | 'normal' | 'high' | 'unknown';
export type LabSeverity = 'normal' | 'mild' | 'moderate' | 'severe' | 'unknown';

export interface LabFinding {
  test: string;
  testId: string;
  value: number | null;
  unit: string;
  expectedLow: number;
  expectedHigh: number;
  status: LabValueStatus;
  severity: LabSeverity;
}

export interface LabAnalysis {
  extractedValues: Record<string, number>;
  abnormalFindings: LabFinding[];
  normalFindings: LabFinding[];
  missingTests: string[];
  predictedCondition: string;
  predictedConditionLabel: string;
  confidence: number;
  classProbabilities: Record<string, number>;
  summary: string;
  explanation: string;
  nextSteps: string;
  urgency: LabUrgency;
  disclaimer: string;
  filename?: string;
  rawText?: string;
}

export interface LabSample {
  name: string;
  title: string;
  url: string;
  size: number;
}

export interface LabHealth {
  ok: boolean;
  service: string;
  modelLoaded: boolean;
  modelMetrics: {
    accuracy: number;
    labels: string[];
    nTrain: number;
    nTest: number;
  } | null;
}

/**
 * All requests go through the Angular dev-server's `/lab-ai/*` proxy entry,
 * which forwards to the Flask service on :5001. The proxy strips the
 * `/lab-ai` prefix so the Flask side sees its native paths (`/health`,
 * `/api/analyze`, etc.).
 */
@Injectable({ providedIn: 'root' })
export class LabAnalyzerService {
  private readonly base = '/lab-ai';

  constructor(private http: HttpClient) {}

  health(): Observable<LabHealth | null> {
    return this.http.get<LabHealth>(`${this.base}/health`).pipe(
      catchError(() => of(null))
    );
  }

  listSamples(): Observable<LabSample[]> {
    return this.http.get<LabSample[]>(`${this.base}/api/samples`).pipe(
      catchError(() => of([]))
    );
  }

  /** Returns the absolute (proxied) URL for a sample PDF — used to fetch its
   *  bytes so we can re-POST it as if the user had uploaded it themselves. */
  sampleUrl(name: string): string {
    return `${this.base}/api/samples/${name}`;
  }

  fetchSampleAsFile(name: string): Promise<File> {
    return fetch(this.sampleUrl(name))
      .then(r => r.blob())
      .then(blob => new File([blob], name, { type: 'application/pdf' }));
  }

  analyze(file: File): Observable<LabAnalysis> {
    const fd = new FormData();
    fd.append('file', file);
    return this.http.post<LabAnalysis>(`${this.base}/api/analyze`, fd);
  }
}
