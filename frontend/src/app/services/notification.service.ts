import { Injectable } from '@angular/core';
import { BehaviorSubject, Observable } from 'rxjs';

export interface Notification {
  id: string;
  type: 'success' | 'error' | 'info' | 'warning';
  message: string;
}

@Injectable({
  providedIn: 'root'
})
export class NotificationService {
  private notificationsSubject = new BehaviorSubject<Notification[]>([]);
  public notifications$ = this.notificationsSubject.asObservable();

  constructor() {}

  success(message: string): void {
    this.addNotification('success', message);
  }

  error(message: string): void {
    this.addNotification('error', message);
  }

  info(message: string): void {
    this.addNotification('info', message);
  }

  warning(message: string): void {
    this.addNotification('warning', message);
  }

  private addNotification(type: 'success' | 'error' | 'info' | 'warning', message: string): void {
    const id = Math.random().toString(36).substr(2, 9);
    const notification: Notification = { id, type, message };

    const current = this.notificationsSubject.value;
    this.notificationsSubject.next([...current, notification]);

    // Auto-remove after 5 seconds
    setTimeout(() => {
      this.removeNotification(id);
    }, 5000);
  }

  removeNotification(id: string): void {
    const current = this.notificationsSubject.value;
    this.notificationsSubject.next(current.filter(n => n.id !== id));
  }
}

