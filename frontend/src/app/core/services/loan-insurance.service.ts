import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  AmortizationEntry,
  ApiResponse,
  InsurancePolicyResponse,
  InsuranceRenewalResponse,
  LoanResponse,
  PrepaymentSimulationRequest,
  PrepaymentSimulationResult
} from '../models/api.models';

@Injectable({
  providedIn: 'root'
})
export class LoanInsuranceService {
  private http = inject(HttpClient);

  getLoans(): Observable<ApiResponse<LoanResponse[]>> {
    return this.http.get<ApiResponse<LoanResponse[]>>('/api/v1/loans');
  }

  getLoan(id: string): Observable<ApiResponse<LoanResponse>> {
    return this.http.get<ApiResponse<LoanResponse>>(`/api/v1/loans/${id}`);
  }

  getAmortizationSchedule(loanId: string): Observable<ApiResponse<AmortizationEntry[]>> {
    return this.http.get<ApiResponse<AmortizationEntry[]>>(`/api/v1/loans/${loanId}/schedule`);
  }

  simulatePrepayment(
    loanId: string,
    req: PrepaymentSimulationRequest
  ): Observable<ApiResponse<PrepaymentSimulationResult>> {
    return this.http.post<ApiResponse<PrepaymentSimulationResult>>(
      `/api/v1/loans/${loanId}/prepayment-impact`,
      req
    );
  }

  getInsurancePolicies(): Observable<ApiResponse<InsurancePolicyResponse[]>> {
    return this.http.get<ApiResponse<InsurancePolicyResponse[]>>('/api/v1/insurance');
  }

  getUpcomingRenewals(windowDays: number = 60): Observable<ApiResponse<InsuranceRenewalResponse[]>> {
    return this.http.get<ApiResponse<InsuranceRenewalResponse[]>>(
      `/api/v1/insurance/renewals/upcoming?windowDays=${windowDays}`
    );
  }
}
