import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';

export type TimeOffStatus = 'PENDING' | 'APPROVED' | 'REJECTED';

export interface TimeOff {
  id?: number;
  doctorId?: number;
  doctorName?: string;
  doctorEmail?: string;
  startDate: string;
  endDate: string;
  reason?: string;
  status?: TimeOffStatus;
  createdAt?: string;
  decidedAt?: string | null;
  decidedByName?: string | null;
  decisionNote?: string | null;
}

@Injectable({ providedIn: 'root' })
export class TimeOffService {
  private apiUrl = '/api/time-off';

  constructor(private http: HttpClient) {}

  getMine(): Observable<TimeOff[]>   { return this.http.get<TimeOff[]>(`${this.apiUrl}/me`); }
  request(t: TimeOff): Observable<TimeOff> { return this.http.post<TimeOff>(`${this.apiUrl}/me`, t); }

  byStatus(status: TimeOffStatus): Observable<TimeOff[]> {
    const params = new HttpParams().set('status', status);
    return this.http.get<TimeOff[]>(this.apiUrl, { params });
  }

  pendingCount(): Observable<{ count: number }> {
    return this.http.get<{ count: number }>(`${this.apiUrl}/pending-count`);
  }

  approve(id: number, note?: string): Observable<TimeOff> {
    return this.http.post<TimeOff>(`${this.apiUrl}/${id}/approve`, note ? { note } : {});
  }

  reject(id: number, note?: string): Observable<TimeOff> {
    return this.http.post<TimeOff>(`${this.apiUrl}/${id}/reject`, note ? { note } : {});
  }
}
