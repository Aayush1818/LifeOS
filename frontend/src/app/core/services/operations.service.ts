import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  ApiResponse,
  AppointmentResponse,
  BiometricRecord,
  TrackedAsset,
  TripResponse,
  WarrantyRecord
} from '../models/api.models';

@Injectable({
  providedIn: 'root'
})
export class OperationsService {
  private http = inject(HttpClient);

  // Healthcare
  getAppointments(): Observable<ApiResponse<AppointmentResponse[]>> {
    return this.http.get<ApiResponse<AppointmentResponse[]>>('/api/v1/healthcare/appointments');
  }

  getUpcomingAppointments(windowDays: number = 30): Observable<ApiResponse<AppointmentResponse[]>> {
    return this.http.get<ApiResponse<AppointmentResponse[]>>(
      `/api/v1/healthcare/appointments/upcoming?windowDays=${windowDays}`
    );
  }

  getVitals(): Observable<ApiResponse<BiometricRecord[]>> {
    return this.http.get<ApiResponse<BiometricRecord[]>>('/api/v1/healthcare/vitals');
  }

  // Travel
  getTrips(): Observable<ApiResponse<TripResponse[]>> {
    return this.http.get<ApiResponse<TripResponse[]>>('/api/v1/travel/trips');
  }

  getUpcomingTrips(windowDays: number = 30): Observable<ApiResponse<TripResponse[]>> {
    return this.http.get<ApiResponse<TripResponse[]>>(
      `/api/v1/travel/trips/upcoming?windowDays=${windowDays}`
    );
  }

  getTrip(id: string): Observable<ApiResponse<TripResponse>> {
    return this.http.get<ApiResponse<TripResponse>>(`/api/v1/travel/trips/${id}`);
  }

  // Assets & Warranties
  getAssets(): Observable<ApiResponse<TrackedAsset[]>> {
    return this.http.get<ApiResponse<TrackedAsset[]>>('/api/v1/assets');
  }

  getWarranties(): Observable<ApiResponse<WarrantyRecord[]>> {
    return this.http.get<ApiResponse<WarrantyRecord[]>>('/api/v1/warranties');
  }

  getExpiringWarranties(windowDays: number = 60): Observable<ApiResponse<WarrantyRecord[]>> {
    return this.http.get<ApiResponse<WarrantyRecord[]>>(
      `/api/v1/warranties/expiring?windowDays=${windowDays}`
    );
  }
}
