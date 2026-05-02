import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, of } from 'rxjs';
import { catchError } from 'rxjs/operators';

export type Urgency = 'LOW' | 'NORMAL' | 'URGENT';

export interface TriageRequest {
  symptoms: string;
  ageYears?: number | null;
  gender?: string | null;
}

export interface TriageResponse {
  suggestedDepartment: string;
  suggestedDepartmentId: number | null;
  urgency: Urgency;
  draftChiefComplaint: string;
  redFlags: string[];
  disclaimer: string;
}

export interface VisitSummaryRequest {
  appointmentId: number;
  save?: boolean;
}

export interface VisitSummaryResponse {
  summary: string;
  saved: boolean;
}

@Injectable({ providedIn: 'root' })
export class AiService {
  private apiUrl = '/api/ai';

  constructor(private http: HttpClient) {}

  status(): Observable<{ enabled: boolean }> {
    return this.http.get<{ enabled: boolean }>(`${this.apiUrl}/status`).pipe(
      catchError(() => of({ enabled: false }))
    );
  }

  triage(req: TriageRequest): Observable<TriageResponse> {
    return this.http.post<TriageResponse>(`${this.apiUrl}/triage`, req);
  }

  visitSummary(req: VisitSummaryRequest): Observable<VisitSummaryResponse> {
    return this.http.post<VisitSummaryResponse>(`${this.apiUrl}/visit-summary`, req);
  }
}
