import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Department } from './department.service';

export interface DoctorProfile {
  id?: number;
  userId: number;
  fullName?: string;
  email?: string;
  departmentIds?: number[];
  departments?: Department[];
  specialty?: string;
  licenseNumber?: string;
  bio?: string;
  languages?: string;
  consultationFeeCents?: number;
  yearsExperience?: number;
  photoUrl?: string;
}

export interface PatientProfile {
  id?: number;
  userId: number;
  fullName?: string;
  email?: string;
  dateOfBirth?: string;
  gender?: string;
  bloodType?: string;
  allergies?: string;
  chronicConditions?: string;
  emergencyContactName?: string;
  emergencyContactPhone?: string;
}

@Injectable({ providedIn: 'root' })
export class ProfileService {
  private doctorUrl = '/api/doctor-profiles';
  private patientUrl = '/api/patient-profiles';

  constructor(private http: HttpClient) {}

  listDoctors(): Observable<DoctorProfile[]>     { return this.http.get<DoctorProfile[]>(this.doctorUrl); }
  getMyDoctorProfile(): Observable<DoctorProfile> { return this.http.get<DoctorProfile>(`${this.doctorUrl}/me`); }
  saveMyDoctorProfile(p: DoctorProfile)           { return this.http.put<DoctorProfile>(`${this.doctorUrl}/me`, p); }
  getDoctorProfile(userId: number)                { return this.http.get<DoctorProfile>(`${this.doctorUrl}/${userId}`); }

  getMyPatientProfile(): Observable<PatientProfile> { return this.http.get<PatientProfile>(`${this.patientUrl}/me`); }
  saveMyPatientProfile(p: PatientProfile)             { return this.http.put<PatientProfile>(`${this.patientUrl}/me`, p); }
  getPatientProfile(userId: number)                   { return this.http.get<PatientProfile>(`${this.patientUrl}/${userId}`); }
}
