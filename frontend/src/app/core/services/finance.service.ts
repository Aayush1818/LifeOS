import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  ApiResponse,
  Budget,
  BudgetStatusResponse,
  CreateTransactionRequest,
  MonthlySummaryResponse,
  PageResponse,
  RecurringTransaction,
  Transaction
} from '../models/api.models';

@Injectable({
  providedIn: 'root'
})
export class FinanceService {
  private http = inject(HttpClient);

  getTransactions(
    page: number = 0,
    size: number = 20,
    type?: string,
    category?: string
  ): Observable<ApiResponse<PageResponse<Transaction>>> {
    let params = new HttpParams().set('page', page).set('size', size);
    if (type) params = params.set('type', type);
    if (category) params = params.set('category', category);

    return this.http.get<ApiResponse<PageResponse<Transaction>>>('/api/v1/finance/transactions', { params });
  }

  createTransaction(req: CreateTransactionRequest): Observable<ApiResponse<Transaction>> {
    return this.http.post<ApiResponse<Transaction>>('/api/v1/finance/transactions', req);
  }

  deleteTransaction(id: string): Observable<ApiResponse<void>> {
    return this.http.delete<ApiResponse<void>>(`/api/v1/finance/transactions/${id}`);
  }

  getMonthlySummary(month: number, year: number): Observable<ApiResponse<MonthlySummaryResponse>> {
    return this.http.get<ApiResponse<MonthlySummaryResponse>>(
      `/api/v1/finance/analytics/monthly-summary?month=${month}&year=${year}`
    );
  }

  getBudgets(): Observable<ApiResponse<Budget[]>> {
    return this.http.get<ApiResponse<Budget[]>>('/api/v1/budgets');
  }

  getBudgetStatuses(month: number, year: number): Observable<ApiResponse<BudgetStatusResponse[]>> {
    return this.http.get<ApiResponse<BudgetStatusResponse[]>>(
      `/api/v1/budgets/status?month=${month}&year=${year}`
    );
  }

  createBudget(budget: Partial<Budget>): Observable<ApiResponse<Budget>> {
    return this.http.post<ApiResponse<Budget>>('/api/v1/budgets', budget);
  }

  getRecurring(): Observable<ApiResponse<RecurringTransaction[]>> {
    return this.http.get<ApiResponse<RecurringTransaction[]>>('/api/v1/finance/recurring');
  }
}
