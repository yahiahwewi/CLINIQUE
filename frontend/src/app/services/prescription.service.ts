import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export type PrescriptionStatus = 'DRAFT' | 'ACTIVE' | 'DISPENSED' | 'CANCELLED';

export interface PrescriptionItem {
  id?: number;
  drugName: string;
  dose?: string;
  frequency?: string;
  durationDays?: number;
  notes?: string;
}

export interface Prescription {
  id?: number;
  appointmentId: number;
  patientId?: number;
  patientName?: string;
  doctorId?: number;
  doctorName?: string;
  instructions?: string;
  status?: PrescriptionStatus;
  dispensedByName?: string | null;
  dispensedAt?: string | null;
  items: PrescriptionItem[];
  createdAt?: string;
}

@Injectable({ providedIn: 'root' })
export class PrescriptionService {
  private apiUrl = '/api/prescriptions';

  constructor(private http: HttpClient) {}

  byAppointment(id: number): Observable<Prescription[]> {
    return this.http.get<Prescription[]>(`${this.apiUrl}/by-appointment/${id}`);
  }

  byPatient(id: number): Observable<Prescription[]> {
    return this.http.get<Prescription[]>(`${this.apiUrl}/patient/${id}`);
  }

  active(): Observable<Prescription[]> {
    return this.http.get<Prescription[]>(`${this.apiUrl}/active`);
  }

  create(rx: Prescription): Observable<Prescription> {
    return this.http.post<Prescription>(this.apiUrl, rx);
  }

  dispense(id: number): Observable<Prescription> {
    return this.http.post<Prescription>(`${this.apiUrl}/${id}/dispense`, {});
  }

  cancel(id: number): Observable<void> {
    return this.http.post<void>(`${this.apiUrl}/${id}/cancel`, {});
  }

  pdfUrl(id: number): string {
    return `${this.apiUrl}/${id}/pdf`;
  }
}
