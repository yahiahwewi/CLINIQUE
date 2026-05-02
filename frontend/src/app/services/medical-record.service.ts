import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface MedicalRecord {
  id?: number;
  appointmentId: number;
  patientName?: string;
  doctorName?: string;
  appointmentDateTime?: string;
  chiefComplaint?: string;
  diagnosis?: string;
  plan?: string;
  privateNotes?: string;
  patientSummary?: string;
  createdAt?: string;
  updatedAt?: string;
}

@Injectable({ providedIn: 'root' })
export class MedicalRecordService {
  private apiUrl = '/api/medical-records';

  constructor(private http: HttpClient) {}

  getByAppointment(appointmentId: number): Observable<MedicalRecord> {
    return this.http.get<MedicalRecord>(`${this.apiUrl}/by-appointment/${appointmentId}`);
  }

  upsert(appointmentId: number, dto: MedicalRecord): Observable<MedicalRecord> {
    return this.http.put<MedicalRecord>(`${this.apiUrl}/by-appointment/${appointmentId}`, dto);
  }

  forPatient(patientId: number): Observable<MedicalRecord[]> {
    return this.http.get<MedicalRecord[]>(`${this.apiUrl}/patient/${patientId}`);
  }
}
