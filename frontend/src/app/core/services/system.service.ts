import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  ApiResponse,
  MonthlyObligationSummaryResponse,
  SystemHealthResponse,
  SystemMetricsResponse
} from '../models/api.models';

@Injectable({
  providedIn: 'root'
})
export class SystemService {
  private http = inject(HttpClient);

  getMonthlyObligations(month?: number, year?: number): Observable<ApiResponse<MonthlyObligationSummaryResponse>> {
    let params = new HttpParams();
    if (month) params = params.set('month', month);
    if (year) params = params.set('year', year);

    return this.http.get<ApiResponse<MonthlyObligationSummaryResponse>>('/api/v1/finance/obligations/monthly', { params });
  }

  getSystemHealth(): Observable<ApiResponse<SystemHealthResponse>> {
    return this.http.get<ApiResponse<SystemHealthResponse>>('/api/v1/system/health');
  }

  getSystemMetrics(): Observable<ApiResponse<SystemMetricsResponse>> {
    return this.http.get<ApiResponse<SystemMetricsResponse>>('/api/v1/system/metrics');
  }
}
