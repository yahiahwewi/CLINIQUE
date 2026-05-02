import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export type LabOrderStatus = 'ORDERED' | 'IN_PROGRESS' | 'COMPLETED' | 'CANCELLED';

export interface LabOrder {
  id?: number;
  appointmentId: number;
  patientId?: number;
  patientName?: string;
  doctorId?: number;
  doctorName?: string;
  testName: string;
  instructions?: string;
  status?: LabOrderStatus;
  resultText?: string | null;
  abnormal?: boolean | null;
  completedByName?: string | null;
  completedAt?: string | null;
  createdAt?: string;
}

@Injectable({ providedIn: 'root' })
export class LabService {
  private apiUrl = '/api/lab-orders';

  constructor(private http: HttpClient) {}

  byAppointment(id: number): Observable<LabOrder[]> { return this.http.get<LabOrder[]>(`${this.apiUrl}/by-appointment/${id}`); }
  byPatient(id: number): Observable<LabOrder[]>     { return this.http.get<LabOrder[]>(`${this.apiUrl}/patient/${id}`); }
  queue(): Observable<LabOrder[]>                   { return this.http.get<LabOrder[]>(`${this.apiUrl}/queue`); }

  create(o: LabOrder): Observable<LabOrder>          { return this.http.post<LabOrder>(this.apiUrl, o); }
  start(id: number): Observable<LabOrder>           { return this.http.post<LabOrder>(`${this.apiUrl}/${id}/start`, {}); }
  uploadResult(id: number, resultText: string, abnormal: boolean): Observable<LabOrder> {
    return this.http.post<LabOrder>(`${this.apiUrl}/${id}/result`, { resultText, abnormal });
  }
}
