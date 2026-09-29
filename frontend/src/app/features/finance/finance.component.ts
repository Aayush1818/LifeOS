import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { FinanceService } from '../../core/services/finance.service';
import { SystemService } from '../../core/services/system.service';
import {
  BudgetStatusResponse,
  CreateTransactionRequest,
  MonthlyObligationItem,
  MonthlyObligationSummaryResponse,
  MonthlySummaryResponse,
  RecurringTransaction,
  Transaction
} from '../../core/models/api.models';

@Component({
  selector: 'app-finance',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <div class="finance-container">
      <header class="page-header">
        <div>
          <h1 class="page-title">Personal Finance & Budgeting</h1>
          <p class="page-subtitle">Deterministic monetary ledger with SQL aggregation pushdown and threshold alerts</p>
        </div>
        <button (click)="showTransactionModal.set(true)" class="btn btn-primary">
          <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <line x1="12" y1="5" x2="12" y2="19"/>
            <line x1="5" y1="12" x2="19" y2="12"/>
          </svg>
          <span>Add Transaction</span>
        </button>
      </header>

      <!-- Financial Metrics Grid -->
      <section class="metric-grid">
        <div class="glass-card metric-card">
          <div class="metric-label">Monthly Income</div>
          <div class="metric-value text-emerald">
            {{ formatCurrency(summary()?.totalIncome || 0) }}
          </div>
          <div class="metric-subtext">Earned this calendar month</div>
        </div>

        <div class="glass-card metric-card">
          <div class="metric-label">Monthly Expenses</div>
          <div class="metric-value text-rose">
            {{ formatCurrency(summary()?.totalExpenses || 0) }}
          </div>
          <div class="metric-subtext">Net after refund deductions</div>
        </div>

        <div class="glass-card metric-card">
          <div class="metric-label">Net Savings</div>
          <div class="metric-value text-cyan">
            {{ formatCurrency(summary()?.netSavings || 0) }}
          </div>
          <div class="metric-subtext">Surplus cash flow</div>
        </div>

        <div class="glass-card metric-card">
          <div class="metric-label">Savings Rate</div>
          <div class="metric-value">
            {{ (summary()?.savingsRate || 0) | number:'1.1-1' }}%
          </div>
          <div class="metric-subtext">Target: > 20%</div>
        </div>
      </section>

      <!-- Monthly Obligations & Cash Flow Synthesis (Phase 18) -->
      <section class="section-block obligations-section">
        <div class="section-header">
          <div>
            <h2>Monthly Obligations & Cash Flow Synthesis</h2>
            <span class="subtext">Real-time aggregation across Loan EMIs, Insurance, Subscriptions & Travel</span>
          </div>
          <div class="month-selector glass-card">
            <button class="btn-month-nav" (click)="changeObligationMonth(-1)" title="Previous month">&larr;</button>
            <span class="month-label">{{ getMonthName(targetMonth()) }} {{ targetYear() }}</span>
            <button class="btn-month-nav" (click)="changeObligationMonth(1)" title="Next month">&rarr;</button>
          </div>
        </div>

        @if (obligationsLoading()) {
          <div class="loading-state">Synthesizing obligations across domains...</div>
        } @else {
          <div class="obligation-metrics-grid">
            <div class="glass-card ob-metric-card">
              <div class="metric-label">Total Obligations</div>
              <div class="metric-value text-rose">
                {{ formatCurrency(obligationsSummary()?.totalObligationAmount || 0) }}
              </div>
              <div class="metric-subtext">Due across all domains</div>
            </div>

            <div class="glass-card ob-metric-card">
              <div class="metric-label">Projected Income</div>
              <div class="metric-value text-emerald">
                {{ formatCurrency(obligationsSummary()?.projectedIncome || 0) }}
              </div>
              <div class="metric-subtext">Recurring salary & deposits</div>
            </div>

            <div class="glass-card ob-metric-card">
              <div class="metric-label">Net Projected Cash Flow</div>
              <div class="metric-value" [ngClass]="(obligationsSummary()?.netSurplusOrDeficit || 0) >= 0 ? 'text-emerald' : 'text-rose'">
                {{ formatCurrency(obligationsSummary()?.netSurplusOrDeficit || 0) }}
              </div>
              <div class="metric-subtext">
                <span class="surplus-pill" [ngClass]="(obligationsSummary()?.netSurplusOrDeficit || 0) >= 0 ? 'pill-surplus' : 'pill-deficit'">
                  {{ (obligationsSummary()?.netSurplusOrDeficit || 0) >= 0 ? 'Surplus' : 'Deficit' }}
                </span>
              </div>
            </div>
          </div>

          <!-- Domain Breakdown Mini-Pills -->
          <div class="domain-breakdown-row">
            <div class="breakdown-chip">
              <span class="chip-label">Loans:</span>
              <span class="chip-val">{{ formatCurrency(obligationsSummary()?.loanEmisTotal || 0) }}</span>
            </div>
            <div class="breakdown-chip">
              <span class="chip-label">Insurance:</span>
              <span class="chip-val">{{ formatCurrency(obligationsSummary()?.insurancePremiumsTotal || 0) }}</span>
            </div>
            <div class="breakdown-chip">
              <span class="chip-label">Bills / Subs:</span>
              <span class="chip-val">{{ formatCurrency(obligationsSummary()?.recurringBillsTotal || 0) }}</span>
            </div>
            <div class="breakdown-chip">
              <span class="chip-label">Travel Budgets:</span>
              <span class="chip-val">{{ formatCurrency(obligationsSummary()?.tripAllocationsTotal || 0) }}</span>
            </div>
          </div>

          <!-- Timeline Table -->
          @if (!obligationsSummary()?.items || obligationsSummary()!.items.length === 0) {
            <div class="empty-state-mini glass-card">
              <span>No upcoming obligations found for {{ getMonthName(targetMonth()) }} {{ targetYear() }}.</span>
            </div>
          } @else {
            <div class="glass-card table-container">
              <table class="data-table">
                <thead>
                  <tr>
                    <th>Due Date</th>
                    <th>Obligation</th>
                    <th>Category</th>
                    <th>Counterparty</th>
                    <th class="text-right">Amount</th>
                    <th class="text-right">Status</th>
                  </tr>
                </thead>
                <tbody>
                  @for (item of obligationsSummary()!.items; track item.id) {
                    <tr>
                      <td class="date-cell">
                        <span class="timeline-day">{{ item.dueDate | date:'d' }}</span>
                        <span class="timeline-month">{{ item.dueDate | date:'MMM' }}</span>
                      </td>
                      <td>
                        <span class="item-title">{{ item.title }}</span>
                      </td>
                      <td>
                        <span class="category-badge" [ngClass]="getObligationCategoryClass(item.category)">
                          {{ item.category }}
                        </span>
                      </td>
                      <td class="text-muted">{{ item.providerOrLender || '-' }}</td>
                      <td class="text-right amount-cell">
                        {{ item.amount | currency:(item.currency || 'USD') }}
                      </td>
                      <td class="text-right">
                        <span class="badge badge-subtle">{{ item.status }}</span>
                      </td>
                    </tr>
                  }
                </tbody>
              </table>
            </div>
          }
        }
      </section>

      <!-- Monthly Budget Tracking -->
      <section class="section-block">
        <div class="section-header">
          <h2>Monthly Category Budgets</h2>
          <span class="subtext">Real-time spend vs configurable thresholds</span>
        </div>

        @if (budgets().length === 0) {
          <div class="empty-state-mini glass-card">
            <span>No category budgets configured for this month.</span>
          </div>
        } @else {
          <div class="budget-grid">
            @for (b of budgets(); track b.category) {
              <div class="glass-card budget-card">
                <div class="budget-card-header">
                  <div class="budget-category">{{ b.category }}</div>
                  <span class="budget-status" [ngClass]="getBudgetStatusClass(b.status)">
                    {{ b.status }}
                  </span>
                </div>
                <div class="budget-amounts">
                  <span class="spent">{{ formatCurrency(b.currentSpent) }}</span>
                  <span class="limit">/ {{ formatCurrency(b.budgetLimit) }}</span>
                </div>

                <!-- Progress Bar -->
                <div class="progress-track">
                  <div
                    class="progress-fill"
                    [ngClass]="getProgressBarClass(b.percentageUsed)"
                    [style.width.%]="b.percentageUsed > 100 ? 100 : b.percentageUsed">
                  </div>
                </div>

                <div class="budget-footer">
                  <span>{{ b.percentageUsed | number:'1.0-0' }}% utilized</span>
                  <span class="subtext">Projected: {{ formatCurrency(b.projectedSpent) }}</span>
                </div>
              </div>
            }
          </div>
        }
      </section>

      <!-- Transactions Ledger -->
      <section class="section-block">
        <div class="section-header">
          <h2>Transaction Ledger</h2>
          <div class="filter-group">
            <button
              [class.active]="txFilter() === ''"
              (click)="txFilter.set('')"
              class="filter-pill">
              All
            </button>
            <button
              [class.active]="txFilter() === 'EXPENSE'"
              (click)="txFilter.set('EXPENSE')"
              class="filter-pill">
              Expenses
            </button>
            <button
              [class.active]="txFilter() === 'INCOME'"
              (click)="txFilter.set('INCOME')"
              class="filter-pill">
              Income
            </button>
          </div>
        </div>

        @if (loading()) {
          <div class="loading-state">Loading transactions...</div>
        } @else if (filteredTransactions().length === 0) {
          <div class="empty-state-mini glass-card">
            <span>No transactions recorded for this filter.</span>
          </div>
        } @else {
          <div class="glass-card table-container">
            <table class="data-table">
              <thead>
                <tr>
                  <th>Date</th>
                  <th>Description</th>
                  <th>Category</th>
                  <th>Type</th>
                  <th class="text-right">Amount</th>
                  <th class="text-right">Action</th>
                </tr>
              </thead>
              <tbody>
                @for (tx of filteredTransactions(); track tx.id) {
                  <tr>
                    <td>{{ tx.transactionDate | date:'mediumDate' }}</td>
                    <td>
                      <div class="tx-desc">
                        <span class="desc-main">{{ tx.description }}</span>
                        @if (tx.isRefund) {
                          <span class="badge badge-accent">Refund</span>
                        }
                        @if (tx.isRecurring) {
                          <span class="badge badge-subtle">Recurring</span>
                        }
                      </div>
                    </td>
                    <td>
                      <span class="badge badge-subtle">{{ tx.category }}</span>
                    </td>
                    <td>
                      <span class="type-badge" [class.income]="tx.transactionType === 'INCOME'">
                        {{ tx.transactionType }}
                      </span>
                    </td>
                    <td class="text-right amount-cell" [class.income]="tx.transactionType === 'INCOME'">
                      {{ tx.transactionType === 'INCOME' ? '+' : '-' }}{{ tx.amount | currency:(tx.currency || 'USD') }}
                    </td>
                    <td class="text-right">
                      <button (click)="deleteTx(tx.id)" class="btn-icon text-danger" title="Delete">
                        <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                          <path d="M3 6h18"/>
                          <path d="M19 6v14c0 1-1 2-2 2H7c-1 0-2-1-2-2V6"/>
                        </svg>
                      </button>
                    </td>
                  </tr>
                }
              </tbody>
            </table>
          </div>
        }
      </section>

      <!-- Recurring Subscriptions & Bills -->
      <section class="section-block">
        <div class="section-header">
          <h2>Active Recurring Subscriptions & Bills</h2>
        </div>
        <div class="recurring-grid">
          @for (rec of recurring(); track rec.id) {
            <div class="glass-card recurring-card">
              <div class="rec-header">
                <span class="rec-name">{{ rec.name }}</span>
                <span class="rec-amount">{{ rec.amount | currency:(rec.currency || 'USD') }}</span>
              </div>
              <div class="rec-meta">
                <span>{{ rec.frequency }}</span>
                <span class="subtext">Next due: {{ rec.nextDueDate | date:'mediumDate' }}</span>
              </div>
            </div>
          }
        </div>
      </section>

      <!-- Add Transaction Modal -->
      @if (showTransactionModal()) {
        <div class="modal-backdrop" (click)="showTransactionModal.set(false)">
          <div class="modal-card glass-card" (click)="$event.stopPropagation()">
            <div class="modal-header">
              <h3>Record New Transaction</h3>
              <button (click)="showTransactionModal.set(false)" class="btn-icon">
                <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <line x1="18" y1="6" x2="6" y2="18"/>
                  <line x1="6" y1="6" x2="18" y2="18"/>
                </svg>
              </button>
            </div>

            <form (ngSubmit)="submitTransaction()" class="modal-body">
              <div class="form-row">
                <div class="form-group flex-1">
                  <label class="form-label">Type</label>
                  <select [(ngModel)]="newTxType" name="newTxType" class="form-select">
                    <option value="EXPENSE">Expense</option>
                    <option value="INCOME">Income</option>
                  </select>
                </div>
                <div class="form-group flex-1">
                  <label class="form-label">Amount</label>
                  <input
                    type="number"
                    step="0.01"
                    [(ngModel)]="newTxAmount"
                    name="newTxAmount"
                    required
                    class="form-input"
                    placeholder="0.00"
                  />
                </div>
              </div>

              <div class="form-row">
                <div class="form-group flex-1">
                  <label class="form-label">Category</label>
                  <select [(ngModel)]="newTxCategory" name="newTxCategory" class="form-select">
                    <option value="FOOD_DINING">Food & Groceries</option>
                    <option value="HOUSING">Housing / Rent</option>
                    <option value="UTILITIES">Utilities & Bills</option>
                    <option value="TRANSPORTATION">Transportation</option>
                    <option value="HEALTHCARE">Healthcare & Medical</option>
                    <option value="ENTERTAINMENT">Entertainment</option>
                    <option value="SHOPPING">Shopping</option>
                    <option value="FINANCIAL_OBLIGATIONS">Debt & Loans</option>
                    <option value="PERSONAL_CARE">Personal Care</option>
                    <option value="EDUCATION">Education</option>
                    <option value="TRAVEL">Travel</option>
                    <option value="INCOME_SALARY">Salary / Income</option>
                    <option value="INCOME_INVESTMENT">Investments</option>
                    <option value="INCOME_FREELANCE">Freelance</option>
                    <option value="OTHER">Other</option>
                  </select>
                </div>
                <div class="form-group flex-1">
                  <label class="form-label">Payment Method</label>
                  <select [(ngModel)]="newTxPaymentMethod" name="newTxPaymentMethod" class="form-select">
                    <option value="UPI">UPI / Instant Pay</option>
                    <option value="BANK_TRANSFER">Bank Transfer / NetBanking</option>
                    <option value="CREDIT_CARD">Credit Card</option>
                    <option value="DEBIT_CARD">Debit Card</option>
                    <option value="CASH">Cash</option>
                    <option value="OTHER">Other</option>
                  </select>
                </div>
              </div>

              <div class="form-group">
                <label class="form-label">Description</label>
                <input
                  type="text"
                  [(ngModel)]="newTxDesc"
                  name="newTxDesc"
                  required
                  class="form-input"
                  placeholder="e.g. Monthly Salary or Supermarket groceries"
                />
              </div>

              <div class="form-group">
                <label class="form-label">Date</label>
                <input
                  type="date"
                  [(ngModel)]="newTxDate"
                  name="newTxDate"
                  required
                  class="form-input"
                />
              </div>

              <div class="form-checkbox-row">
                <label class="checkbox-label">
                  <input type="checkbox" [(ngModel)]="newTxIsRefund" name="newTxIsRefund"/>
                  <span>Is Refund (deduct from net expenses)</span>
                </label>
              </div>

              @if (txError()) {
                <div class="error-banner">{{ txError() }}</div>
              }

              <div class="modal-footer">
                <button type="button" (click)="showTransactionModal.set(false)" class="btn btn-secondary">Cancel</button>
                <button
                  type="button"
                  (click)="submitTransaction()"
                  class="btn btn-primary modal-btn-submit"
                  id="save-tx-btn"
                  style="display: inline-flex !important; align-items: center !important; justify-content: center !important; gap: 8px !important; min-width: 170px !important; height: 42px !important; color: #ffffff !important; background: linear-gradient(135deg, #6366f1 0%, #a855f7 100%) !important; font-size: 15px !important; font-weight: 700 !important; border-radius: 8px !important; border: none !important; cursor: pointer !important; box-shadow: 0 4px 15px rgba(99, 102, 241, 0.4) !important;">
                  @if (submittingTx()) {
                    <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="#ffffff" stroke-width="2.5" class="spin-icon">
                      <circle cx="12" cy="12" r="10" stroke-opacity="0.25"/>
                      <path d="M12 2a10 10 0 0 1 10 10"/>
                    </svg>
                    <span style="color: #ffffff !important; font-weight: 700 !important;">Saving...</span>
                  } @else {
                    <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="#ffffff" stroke-width="2.5">
                      <polyline points="20 6 9 17 4 12"/>
                    </svg>
                    <span style="color: #ffffff !important; font-weight: 700 !important;">Save Transaction</span>
                  }
                </button>
              </div>
            </form>
          </div>
        </div>
      }
    </div>
  `,
  styles: [`
    .finance-container {
      padding: 2rem;
      max-width: 1400px;
      margin: 0 auto;
      height: 100%;
      overflow-y: auto;
    }

    .page-header {
      display: flex;
      align-items: center;
      justify-content: space-between;
      margin-bottom: 2rem;
    }

    .metric-grid {
      display: grid;
      grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
      gap: 1.25rem;
      margin-bottom: 2.5rem;
    }

    .metric-card {
      padding: 1.5rem;
    }

    .metric-label {
      font-size: 0.8rem;
      color: var(--text-muted);
      text-transform: uppercase;
      letter-spacing: 0.05em;
      font-weight: 600;
      margin-bottom: 0.5rem;
    }

    .metric-value {
      font-size: 1.85rem;
      font-weight: 700;
      font-family: 'Outfit', sans-serif;
      margin-bottom: 0.35rem;
    }

    .metric-subtext {
      font-size: 0.75rem;
      color: var(--text-dim);
    }

    .text-emerald { color: var(--accent-emerald); }
    .text-rose { color: var(--accent-rose); }
    .text-cyan { color: var(--accent-cyan); }

    .month-selector {
      display: flex;
      align-items: center;
      gap: 0.5rem;
      padding: 0.35rem 0.75rem;
      border-radius: var(--radius-full);
      border: 1px solid var(--border-subtle);
    }

    .btn-month-nav {
      background: transparent;
      border: none;
      color: var(--text-muted);
      cursor: pointer;
      font-size: 1rem;
      padding: 0.2rem 0.5rem;
      border-radius: 4px;
      transition: color var(--transition-fast);
    }

    .btn-month-nav:hover {
      color: var(--text-main);
    }

    .month-label {
      font-size: 0.85rem;
      font-weight: 600;
      color: var(--text-main);
      min-width: 120px;
      text-align: center;
    }

    .obligation-metrics-grid {
      display: grid;
      grid-template-columns: repeat(auto-fit, minmax(240px, 1fr));
      gap: 1.25rem;
      margin-bottom: 1.25rem;
    }

    .ob-metric-card {
      padding: 1.25rem 1.5rem;
    }

    .surplus-pill {
      font-size: 0.7rem;
      font-weight: 700;
      padding: 0.15rem 0.5rem;
      border-radius: 9999px;
      text-transform: uppercase;
    }

    .pill-surplus {
      background: rgba(16, 185, 129, 0.15);
      color: var(--accent-emerald);
    }

    .pill-deficit {
      background: rgba(244, 63, 94, 0.15);
      color: var(--accent-rose);
    }

    .domain-breakdown-row {
      display: flex;
      flex-wrap: wrap;
      gap: 0.75rem;
      margin-bottom: 1.25rem;
    }

    .breakdown-chip {
      background: rgba(255, 255, 255, 0.03);
      border: 1px solid var(--border-subtle);
      border-radius: var(--radius-sm);
      padding: 0.4rem 0.85rem;
      display: flex;
      align-items: center;
      gap: 0.4rem;
      font-size: 0.8rem;
    }

    .chip-label {
      color: var(--text-muted);
    }

    .chip-val {
      font-weight: 700;
      color: var(--text-main);
      font-family: 'Outfit', sans-serif;
    }

    .date-cell {
      display: flex;
      align-items: baseline;
      gap: 0.35rem;
    }

    .timeline-day {
      font-size: 1.1rem;
      font-weight: 700;
      color: var(--text-main);
      font-family: 'Outfit', sans-serif;
    }

    .timeline-month {
      font-size: 0.75rem;
      color: var(--text-muted);
      text-transform: uppercase;
    }

    .item-title {
      font-weight: 500;
      color: var(--text-main);
    }

    .category-badge {
      font-size: 0.7rem;
      font-weight: 700;
      padding: 0.15rem 0.45rem;
      border-radius: 4px;
      text-transform: uppercase;
    }

    .cat-loan { background: rgba(245, 158, 11, 0.15); color: var(--accent-amber); }
    .cat-insurance { background: rgba(6, 182, 212, 0.15); color: var(--accent-cyan); }
    .cat-travel { background: rgba(168, 85, 247, 0.15); color: #c084fc; }
    .cat-util { background: rgba(99, 102, 241, 0.15); color: #a5b4fc; }

    .section-block {
      margin-bottom: 2.5rem;
    }

    .section-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin-bottom: 1.25rem;
    }

    .section-header h2 {
      font-size: 1.25rem;
      margin: 0;
    }

    .budget-grid {
      display: grid;
      grid-template-columns: repeat(auto-fit, minmax(280px, 1fr));
      gap: 1.25rem;
    }

    .budget-card {
      padding: 1.25rem 1.5rem;
    }

    .budget-card-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin-bottom: 0.75rem;
    }

    .budget-category {
      font-weight: 600;
      font-size: 0.95rem;
    }

    .budget-status {
      font-size: 0.7rem;
      font-weight: 700;
      padding: 0.15rem 0.45rem;
      border-radius: 4px;
      text-transform: uppercase;
    }

    .status-normal { background: rgba(16, 185, 129, 0.15); color: var(--accent-emerald); }
    .status-warning { background: rgba(245, 158, 11, 0.15); color: var(--accent-amber); }
    .status-critical { background: rgba(244, 63, 94, 0.15); color: var(--accent-rose); }

    .budget-amounts {
      margin-bottom: 0.75rem;
    }

    .budget-amounts .spent {
      font-size: 1.35rem;
      font-weight: 700;
      font-family: 'Outfit', sans-serif;
    }

    .budget-amounts .limit {
      font-size: 0.9rem;
      color: var(--text-muted);
      margin-left: 0.35rem;
    }

    .progress-track {
      height: 8px;
      background: var(--bg-input);
      border-radius: var(--radius-full);
      overflow: hidden;
      margin-bottom: 0.75rem;
    }

    .progress-fill {
      height: 100%;
      border-radius: var(--radius-full);
      transition: width 0.3s ease;
    }

    .bar-green { background: var(--accent-emerald); }
    .bar-amber { background: var(--accent-amber); }
    .bar-red { background: var(--accent-rose); }

    .budget-footer {
      display: flex;
      justify-content: space-between;
      font-size: 0.75rem;
      color: var(--text-muted);
    }

    .table-container {
      overflow-x: auto;
    }

    .data-table {
      width: 100%;
      border-collapse: collapse;
      text-align: left;
    }

    .data-table th, .data-table td {
      padding: 1rem 1.25rem;
      border-bottom: 1px solid var(--border-subtle);
    }

    .data-table th {
      font-size: 0.75rem;
      text-transform: uppercase;
      letter-spacing: 0.05em;
      color: var(--text-muted);
      font-weight: 600;
    }

    .tx-desc {
      display: flex;
      align-items: center;
      gap: 0.5rem;
    }

    .desc-main {
      font-weight: 500;
    }

    .type-badge {
      font-size: 0.7rem;
      font-weight: 700;
      padding: 0.15rem 0.45rem;
      border-radius: 4px;
      background: rgba(244, 63, 94, 0.15);
      color: var(--accent-rose);
    }

    .type-badge.income {
      background: rgba(16, 185, 129, 0.15);
      color: var(--accent-emerald);
    }

    .amount-cell {
      font-weight: 700;
      font-family: 'Outfit', sans-serif;
      color: var(--text-main);
    }

    .amount-cell.income {
      color: var(--accent-emerald);
    }

    .text-right { text-align: right; }
    .text-danger { color: var(--accent-rose); }

    .recurring-grid {
      display: grid;
      grid-template-columns: repeat(auto-fit, minmax(260px, 1fr));
      gap: 1rem;
    }

    .recurring-card {
      padding: 1.25rem;
    }

    .rec-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin-bottom: 0.5rem;
    }

    .rec-name {
      font-weight: 600;
    }

    .rec-amount {
      font-weight: 700;
      color: var(--accent-cyan);
    }

    .rec-meta {
      display: flex;
      justify-content: space-between;
      font-size: 0.75rem;
      color: var(--text-muted);
    }

    .filter-group {
      display: flex;
      gap: 0.5rem;
    }

    .filter-pill {
      background: var(--bg-surface);
      border: 1px solid var(--border-subtle);
      color: var(--text-secondary);
      padding: 0.35rem 0.85rem;
      border-radius: var(--radius-full);
      font-size: 0.8rem;
      cursor: pointer;
    }

    .filter-pill.active {
      background: rgba(99, 102, 241, 0.15);
      color: var(--primary);
      border-color: var(--primary);
    }

    .empty-state-mini {
      padding: 2rem;
      text-align: center;
      color: var(--text-muted);
      font-size: 0.85rem;
    }

    .modal-backdrop {
      position: fixed;
      inset: 0;
      background: rgba(0, 0, 0, 0.7);
      backdrop-filter: blur(8px);
      display: flex;
      align-items: center;
      justify-content: center;
      z-index: 100;
    }

    .modal-card {
      width: 100%;
      max-width: 480px;
      padding: 2rem;
      background: var(--bg-surface);
    }

    .modal-header {
      display: flex;
      align-items: center;
      justify-content: space-between;
      margin-bottom: 1.5rem;
    }

    .modal-body {
      display: flex;
      flex-direction: column;
      gap: 1.25rem;
    }

    .form-row {
      display: flex;
      gap: 1rem;
    }

    .flex-1 { flex: 1; }

    .checkbox-label {
      display: flex;
      align-items: center;
      gap: 0.5rem;
      font-size: 0.85rem;
      color: var(--text-secondary);
      cursor: pointer;
    }

    .modal-footer {
      display: flex;
      justify-content: flex-end;
      gap: 0.75rem;
      margin-top: 1rem;
    }

    .modal-btn-submit {
      min-width: 160px !important;
      justify-content: center !important;
      color: #ffffff !important;
      font-weight: 700 !important;
      font-size: 0.95rem !important;
      background: var(--gradient-brand) !important;
      box-shadow: 0 4px 15px var(--primary-glow) !important;
      cursor: pointer !important;
    }

    .error-banner {
      background: rgba(244, 63, 94, 0.15);
      border: 1px solid var(--accent-rose);
      color: var(--accent-rose);
      padding: 0.75rem 1rem;
      border-radius: var(--radius-sm);
      font-size: 0.85rem;
    }

    .spin-icon {
      animation: spin 1s linear infinite;
    }

    @keyframes spin {
      from { transform: rotate(0deg); }
      to { transform: rotate(360deg); }
    }
  `]
})
export class FinanceComponent implements OnInit {
  financeService = inject(FinanceService);
  systemService = inject(SystemService);

  summary = signal<MonthlySummaryResponse | null>(null);
  budgets = signal<BudgetStatusResponse[]>([]);
  transactions = signal<Transaction[]>([]);
  recurring = signal<RecurringTransaction[]>([]);
  loading = signal<boolean>(false);
  txFilter = signal<string>('');

  // Phase 18: Monthly Obligations Synthesis
  obligationsSummary = signal<MonthlyObligationSummaryResponse | null>(null);
  obligationsLoading = signal<boolean>(false);
  targetMonth = signal<number>(new Date().getMonth() + 1);
  targetYear = signal<number>(new Date().getFullYear());

  showTransactionModal = signal<boolean>(false);
  newTxType: 'EXPENSE' | 'INCOME' = 'EXPENSE';
  newTxAmount: number | null = null;
  newTxCategory: string = 'FOOD_DINING';
  newTxPaymentMethod: string = 'UPI';
  newTxDesc: string = '';
  newTxDate: string = new Date().toISOString().split('T')[0];
  newTxIsRefund: boolean = false;
  submittingTx = signal<boolean>(false);
  txError = signal<string>('');

  ngOnInit() {
    this.loadFinanceData();
    this.loadObligations();
  }

  loadObligations() {
    this.obligationsLoading.set(true);
    this.systemService.getMonthlyObligations(this.targetMonth(), this.targetYear()).subscribe({
      next: (res) => {
        if (res.success) {
          this.obligationsSummary.set(res.data);
        }
        this.obligationsLoading.set(false);
      },
      error: () => this.obligationsLoading.set(false)
    });
  }

  changeObligationMonth(delta: number) {
    let m = this.targetMonth() + delta;
    let y = this.targetYear();
    if (m < 1) {
      m = 12;
      y -= 1;
    } else if (m > 12) {
      m = 1;
      y += 1;
    }
    this.targetMonth.set(m);
    this.targetYear.set(y);
    this.loadObligations();
  }

  getMonthName(monthNum: number): string {
    const names = [
      'January', 'February', 'March', 'April', 'May', 'June',
      'July', 'August', 'September', 'October', 'November', 'December'
    ];
    return names[monthNum - 1] || 'Month ' + monthNum;
  }

  getObligationCategoryClass(category: string): string {
    if (category.includes('LOAN')) return 'cat-loan';
    if (category.includes('INSURANCE')) return 'cat-insurance';
    if (category.includes('TRAVEL')) return 'cat-travel';
    return 'cat-util';
  }

  loadFinanceData() {
    this.loading.set(true);
    const now = new Date();
    const month = now.getMonth() + 1;
    const year = now.getFullYear();

    this.financeService.getMonthlySummary(month, year).subscribe({
      next: (res) => { if (res.success) this.summary.set(res.data); }
    });

    this.financeService.getBudgetStatuses(month, year).subscribe({
      next: (res) => { if (res.success) this.budgets.set(res.data || []); }
    });

    this.financeService.getTransactions(0, 50).subscribe({
      next: (res) => {
        if (res.success && res.data) {
          this.transactions.set(res.data.content);
        }
        this.loading.set(false);
      },
      error: () => this.loading.set(false)
    });

    this.financeService.getRecurring().subscribe({
      next: (res) => { if (res.success) this.recurring.set(res.data || []); }
    });
  }

  filteredTransactions(): Transaction[] {
    const filter = this.txFilter();
    if (!filter) return this.transactions();
    return this.transactions().filter(t => t.transactionType === filter);
  }

  submitTransaction() {
    if (!this.newTxAmount) {
      this.txError.set('Please enter a valid transaction amount.');
      return;
    }
    if (!this.newTxDesc || !this.newTxDesc.trim()) {
      this.txError.set('Please enter a transaction description.');
      return;
    }

    this.submittingTx.set(true);
    this.txError.set('');
    const req: CreateTransactionRequest = {
      amount: this.newTxAmount,
      currency: 'USD',
      transactionType: this.newTxType,
      category: this.newTxCategory,
      paymentMethod: this.newTxPaymentMethod || 'OTHER',
      transactionDate: this.newTxDate,
      description: this.newTxDesc,
      isRefund: this.newTxIsRefund
    };

    this.financeService.createTransaction(req).subscribe({
      next: (res) => {
        this.submittingTx.set(false);
        if (res.success && res.data) {
          this.transactions.update(list => [res.data, ...list]);
          this.showTransactionModal.set(false);
          this.newTxAmount = null;
          this.newTxDesc = '';
          this.newTxIsRefund = false;
          this.txError.set('');
          // Refresh monthly summary
          const now = new Date();
          this.financeService.getMonthlySummary(now.getMonth() + 1, now.getFullYear()).subscribe(s => {
            if (s.success) this.summary.set(s.data);
          });
        }
      },
      error: (err) => {
        this.submittingTx.set(false);
        this.txError.set(err.error?.detail || err.error?.message || 'Failed to record transaction');
      }
    });
  }

  deleteTx(id: string) {
    if (confirm('Delete this transaction?')) {
      this.financeService.deleteTransaction(id).subscribe(() => {
        this.transactions.update(list => list.filter(t => t.id !== id));
      });
    }
  }

  formatCurrency(val: number): string {
    return new Intl.NumberFormat('en-US', { style: 'currency', currency: 'USD' }).format(val || 0);
  }

  getBudgetStatusClass(status: string): string {
    switch (status) {
      case 'NORMAL': return 'status-normal';
      case 'WARNING': return 'status-warning';
      default: return 'status-critical';
    }
  }

  getProgressBarClass(percent: number): string {
    if (percent < 75) return 'bar-green';
    if (percent <= 90) return 'bar-amber';
    return 'bar-red';
  }
}
