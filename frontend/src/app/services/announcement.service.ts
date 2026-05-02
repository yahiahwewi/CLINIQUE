import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export type AnnouncementAudience = 'ALL' | 'STAFF' | 'PATIENTS';
export type AnnouncementTone = 'INFO' | 'SUCCESS' | 'WARNING';

export interface Announcement {
  id?: number;
  title: string;
  body: string;
  audience: AnnouncementAudience;
  tone: AnnouncementTone;
  active: boolean;
  createdAt?: string;
  expiresAt?: string | null;
}

@Injectable({ providedIn: 'root' })
export class AnnouncementService {
  private apiUrl = '/api/announcements';

  constructor(private http: HttpClient) {}

  active(): Observable<Announcement[]>           { return this.http.get<Announcement[]>(`${this.apiUrl}/active`); }
  list(): Observable<Announcement[]>             { return this.http.get<Announcement[]>(this.apiUrl); }
  create(a: Announcement): Observable<Announcement> { return this.http.post<Announcement>(this.apiUrl, a); }
  update(id: number, a: Announcement): Observable<Announcement> { return this.http.put<Announcement>(`${this.apiUrl}/${id}`, a); }
  remove(id: number): Observable<void>           { return this.http.delete<void>(`${this.apiUrl}/${id}`); }
}
