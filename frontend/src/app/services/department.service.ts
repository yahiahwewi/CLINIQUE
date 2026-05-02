import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface Department {
  id: number;
  name: string;
  description?: string;
  color?: string;
  icon?: string;
}

@Injectable({ providedIn: 'root' })
export class DepartmentService {
  private apiUrl = '/api/departments';

  constructor(private http: HttpClient) {}

  list(): Observable<Department[]>           { return this.http.get<Department[]>(this.apiUrl); }
  create(d: Department): Observable<Department> { return this.http.post<Department>(this.apiUrl, d); }
  update(id: number, d: Department)             { return this.http.put<Department>(`${this.apiUrl}/${id}`, d); }
  remove(id: number): Observable<void>           { return this.http.delete<void>(`${this.apiUrl}/${id}`); }
}
