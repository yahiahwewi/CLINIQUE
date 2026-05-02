import { Injectable, inject, PLATFORM_ID } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { BehaviorSubject, Observable, Subscription, interval } from 'rxjs';
import { switchMap, catchError } from 'rxjs/operators';
import { isPlatformBrowser } from '@angular/common';
import { of } from 'rxjs';

export interface InboxNotification {
  id: number;
  title: string;
  body: string;
  link: string | null;
  isRead: boolean;
  createdAt: string;
}

@Injectable({ providedIn: 'root' })
export class InboxService {
  private apiUrl = '/api';
  private platformId = inject(PLATFORM_ID);
  private unreadSubject = new BehaviorSubject<number>(0);
  public unreadCount$ = this.unreadSubject.asObservable();
  private pollSub: Subscription | null = null;

  constructor(private http: HttpClient) {}

  startPolling(): void {
    if (!isPlatformBrowser(this.platformId) || this.pollSub) return;
    this.refreshUnread();
    this.pollSub = interval(30000)
      .pipe(switchMap(() => this.http.get<{ count: number }>(`${this.apiUrl}/notifications/unread-count`).pipe(catchError(() => of({ count: this.unreadSubject.value })))))
      .subscribe(res => this.unreadSubject.next(res.count ?? 0));
  }

  stopPolling(): void {
    this.pollSub?.unsubscribe();
    this.pollSub = null;
  }

  refreshUnread(): void {
    this.http.get<{ count: number }>(`${this.apiUrl}/notifications/unread-count`)
      .pipe(catchError(() => of({ count: 0 })))
      .subscribe(res => this.unreadSubject.next(res.count ?? 0));
  }

  list(): Observable<InboxNotification[]> {
    return this.http.get<InboxNotification[]>(`${this.apiUrl}/notifications`);
  }

  markAllRead(): Observable<void> {
    return new Observable<void>(observer => {
      this.http.post<void>(`${this.apiUrl}/notifications/mark-all-read`, {}).subscribe({
        next: () => {
          this.unreadSubject.next(0);
          observer.next();
          observer.complete();
        },
        error: e => observer.error(e)
      });
    });
  }
}
