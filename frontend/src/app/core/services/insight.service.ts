import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { ApiResponse, InsightItem, InsightSummaryResponse } from '../models/api.models';

@Injectable({
  providedIn: 'root'
})
export class InsightService {
  private http = inject(HttpClient);
  private readonly baseUrl = '/api/v1/insights';

  getInsights(severity?: string): Observable<ApiResponse<InsightSummaryResponse>> {
    let params = new HttpParams();
    if (severity) {
      params = params.set('severity', severity);
    }
    return this.http.get<ApiResponse<InsightSummaryResponse>>(this.baseUrl, { params });
  }

  generateInsights(): Observable<ApiResponse<InsightSummaryResponse>> {
    return this.http.post<ApiResponse<InsightSummaryResponse>>(`${this.baseUrl}/generate`, {});
  }

  dismissInsight(id: string): Observable<ApiResponse<InsightItem>> {
    return this.http.post<ApiResponse<InsightItem>>(`${this.baseUrl}/${id}/dismiss`, {});
  }

  actionInsight(id: string): Observable<ApiResponse<InsightItem>> {
    return this.http.post<ApiResponse<InsightItem>>(`${this.baseUrl}/${id}/action`, {});
  }
}
