import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  ApiResponse,
  CreateReminderRequest,
  NotificationCount,
  NotificationItem,
  PageResponse,
  Reminder,
  UpdateReminderRequest
} from '../models/api.models';

@Injectable({
  providedIn: 'root'
})
export class NotificationService {
  private http = inject(HttpClient);

  // ----------------------------------------------------
  // Notifications
  // ----------------------------------------------------
  getNotifications(
    page: number = 0,
    size: number = 20,
    unreadOnly: boolean = false
  ): Observable<ApiResponse<PageResponse<NotificationItem>>> {
    let params = new HttpParams()
      .set('page', page)
      .set('size', size)
      .set('unreadOnly', unreadOnly);
    return this.http.get<ApiResponse<PageResponse<NotificationItem>>>('/api/v1/notifications', { params });
  }

  getUnreadCount(): Observable<ApiResponse<NotificationCount>> {
    return this.http.get<ApiResponse<NotificationCount>>('/api/v1/notifications/unread-count');
  }

  markAsRead(id: string): Observable<ApiResponse<NotificationItem>> {
    return this.http.patch<ApiResponse<NotificationItem>>(`/api/v1/notifications/${id}/read`, {});
  }

  markAllAsRead(): Observable<ApiResponse<void>> {
    return this.http.post<ApiResponse<void>>('/api/v1/notifications/mark-all-read', {});
  }

  deleteNotification(id: string): Observable<ApiResponse<void>> {
    return this.http.delete<ApiResponse<void>>(`/api/v1/notifications/${id}`);
  }

  // ----------------------------------------------------
  // Reminders
  // ----------------------------------------------------
  getReminders(
    status?: string,
    page: number = 0,
    size: number = 20
  ): Observable<ApiResponse<PageResponse<Reminder>>> {
    let params = new HttpParams().set('page', page).set('size', size);
    if (status) {
      params = params.set('status', status);
    }
    return this.http.get<ApiResponse<PageResponse<Reminder>>>('/api/v1/reminders', { params });
  }

  getUpcomingReminders(days: number = 7): Observable<ApiResponse<Reminder[]>> {
    const params = new HttpParams().set('days', days);
    return this.http.get<ApiResponse<Reminder[]>>('/api/v1/reminders/upcoming', { params });
  }

  getOverdueReminders(): Observable<ApiResponse<Reminder[]>> {
    return this.http.get<ApiResponse<Reminder[]>>('/api/v1/reminders/overdue');
  }

  createReminder(req: CreateReminderRequest): Observable<ApiResponse<Reminder>> {
    return this.http.post<ApiResponse<Reminder>>('/api/v1/reminders', req);
  }

  updateReminder(id: string, req: UpdateReminderRequest): Observable<ApiResponse<Reminder>> {
    return this.http.put<ApiResponse<Reminder>>(`/api/v1/reminders/${id}`, req);
  }

  completeReminder(id: string): Observable<ApiResponse<Reminder>> {
    return this.http.patch<ApiResponse<Reminder>>(`/api/v1/reminders/${id}/complete`, {});
  }

  dismissReminder(id: string): Observable<ApiResponse<Reminder>> {
    return this.http.patch<ApiResponse<Reminder>>(`/api/v1/reminders/${id}/dismiss`, {});
  }

  deleteReminder(id: string): Observable<ApiResponse<void>> {
    return this.http.delete<ApiResponse<void>>(`/api/v1/reminders/${id}`);
  }
}
