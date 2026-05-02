import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface AuditLog {
  id: number;
  actorName: string;
  actorEmail: string | null;
  action: string;
  entityType: string | null;
  entityId: number | null;
  summary: string;
  createdAt: string;
}

@Injectable({ providedIn: 'root' })
export class AuditService {
  private apiUrl = '/api/admin/audit';

  constructor(private http: HttpClient) {}

  recent(limit = 100): Observable<AuditLog[]> {
    const params = new HttpParams().set('limit', String(limit));
    return this.http.get<AuditLog[]>(this.apiUrl, { params });
  }
}
