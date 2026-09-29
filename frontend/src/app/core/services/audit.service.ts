import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  ApiResponse,
  AuditLog,
  PageResponse,
  PolicyComparisonRequest,
  PolicyComparisonResponse
} from '../models/api.models';

@Injectable({
  providedIn: 'root'
})
export class AuditService {
  private http = inject(HttpClient);

  getAuditLogs(eventType?: string, page: number = 0, size: number = 20): Observable<ApiResponse<PageResponse<AuditLog>>> {
    let params = new HttpParams()
      .set('page', page)
      .set('size', size);
    if (eventType) {
      params = params.set('eventType', eventType);
    }
    return this.http.get<ApiResponse<PageResponse<AuditLog>>>('/api/v1/audit/logs', { params });
  }

  downloadUserDataExport(): Observable<Blob> {
    return this.http.get('/api/v1/users/me/export', { responseType: 'blob' });
  }

  comparePolicies(request: PolicyComparisonRequest): Observable<ApiResponse<PolicyComparisonResponse>> {
    return this.http.post<ApiResponse<PolicyComparisonResponse>>('/api/v1/insurance/policies/compare', request);
  }
}
