import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface Holiday {
  id?: number;
  date: string;
  name: string;
}

@Injectable({ providedIn: 'root' })
export class HolidayService {
  private apiUrl = '/api/holidays';

  constructor(private http: HttpClient) {}

  list(): Observable<Holiday[]> { return this.http.get<Holiday[]>(this.apiUrl); }
  create(h: Holiday): Observable<Holiday> { return this.http.post<Holiday>(this.apiUrl, h); }
  update(id: number, h: Holiday): Observable<Holiday> { return this.http.put<Holiday>(`${this.apiUrl}/${id}`, h); }
  remove(id: number): Observable<void> { return this.http.delete<void>(`${this.apiUrl}/${id}`); }
}
