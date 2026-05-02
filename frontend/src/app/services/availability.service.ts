import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';

export type DayOfWeek = 'MONDAY' | 'TUESDAY' | 'WEDNESDAY' | 'THURSDAY' | 'FRIDAY' | 'SATURDAY' | 'SUNDAY';

export interface DoctorAvailability {
  id: number;
  doctorId: number;
  doctorName: string;
  dayOfWeek: DayOfWeek;
  startTime: string;
  endTime: string;
  slotDurationMinutes: number;
  active: boolean;
}

export interface DoctorAvailabilityRequest {
  dayOfWeek: DayOfWeek;
  startTime: string;
  endTime: string;
  slotDurationMinutes: number;
  active?: boolean;
}

export interface TimeSlot {
  start: string;
  end: string;
  available: boolean;
}

@Injectable({ providedIn: 'root' })
export class AvailabilityService {
  private apiUrl = '/api/availability';

  constructor(private http: HttpClient) {}

  getMine(): Observable<DoctorAvailability[]> {
    return this.http.get<DoctorAvailability[]>(`${this.apiUrl}/me`);
  }

  add(req: DoctorAvailabilityRequest): Observable<DoctorAvailability> {
    return this.http.post<DoctorAvailability>(`${this.apiUrl}/me`, req);
  }

  update(id: number, req: DoctorAvailabilityRequest): Observable<DoctorAvailability> {
    return this.http.put<DoctorAvailability>(`${this.apiUrl}/me/${id}`, req);
  }

  remove(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/me/${id}`);
  }

  getDoctorAvailability(doctorId: number): Observable<DoctorAvailability[]> {
    return this.http.get<DoctorAvailability[]>(`${this.apiUrl}/doctors/${doctorId}`);
  }

  getSlots(doctorId: number, date: string): Observable<TimeSlot[]> {
    const params = new HttpParams().set('date', date);
    return this.http.get<TimeSlot[]>(`${this.apiUrl}/doctors/${doctorId}/slots`, { params });
  }
}
