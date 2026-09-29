import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  ApiResponse,
  AppointmentResponse,
  InsuranceRenewalResponse,
  LoanResponse,
  MonthlySummaryResponse,
  PendingAction,
  TripResponse
} from '../models/api.models';

@Injectable({
  providedIn: 'root'
})
export class DashboardService {
  private http = inject(HttpClient);

  getMonthlySummary(month: number, year: number): Observable<ApiResponse<MonthlySummaryResponse>> {
    return this.http.get<ApiResponse<MonthlySummaryResponse>>(`/api/v1/finance/analytics/monthly-summary?month=${month}&year=${year}`);
  }

  getActiveLoans(): Observable<ApiResponse<LoanResponse[]>> {
    return this.http.get<ApiResponse<LoanResponse[]>>('/api/v1/loans');
  }

  getUpcomingInsurance(windowDays: number = 60): Observable<ApiResponse<InsuranceRenewalResponse[]>> {
    return this.http.get<ApiResponse<InsuranceRenewalResponse[]>>(`/api/v1/insurance/renewals/upcoming?windowDays=${windowDays}`);
  }

  getUpcomingAppointments(windowDays: number = 30): Observable<ApiResponse<AppointmentResponse[]>> {
    return this.http.get<ApiResponse<AppointmentResponse[]>>(`/api/v1/healthcare/appointments/upcoming?windowDays=${windowDays}`);
  }

  getUpcomingTrips(windowDays: number = 30): Observable<ApiResponse<TripResponse[]>> {
    return this.http.get<ApiResponse<TripResponse[]>>(`/api/v1/travel/trips/upcoming?windowDays=${windowDays}`);
  }

  getPendingActions(): Observable<ApiResponse<PendingAction[]>> {
    return this.http.get<ApiResponse<PendingAction[]>>('/api/v1/assistant/actions/pending');
  }
}
