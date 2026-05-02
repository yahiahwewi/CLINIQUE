import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export type ReferralStatus = 'PENDING' | 'ACCEPTED' | 'COMPLETED' | 'DECLINED';

export interface Referral {
  id?: number;
  fromDoctorId?: number;
  fromDoctorName?: string;
  toDoctorId: number;
  toDoctorName?: string;
  patientId: number;
  patientName?: string;
  appointmentId?: number | null;
  reason: string;
  status?: ReferralStatus;
  createdAt?: string;
}

@Injectable({ providedIn: 'root' })
export class ReferralService {
  private apiUrl = '/api/referrals';

  constructor(private http: HttpClient) {}

  incoming(): Observable<Referral[]>  { return this.http.get<Referral[]>(`${this.apiUrl}/incoming`); }
  outgoing(): Observable<Referral[]>  { return this.http.get<Referral[]>(`${this.apiUrl}/outgoing`); }
  forPatient(id: number): Observable<Referral[]> { return this.http.get<Referral[]>(`${this.apiUrl}/patient/${id}`); }

  create(r: Referral): Observable<Referral>      { return this.http.post<Referral>(this.apiUrl, r); }
  setStatus(id: number, status: ReferralStatus): Observable<Referral> {
    return this.http.post<Referral>(`${this.apiUrl}/${id}/status/${status}`, {});
  }
}
