import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterLink } from '@angular/router';
import { DashboardService } from '../../core/services/dashboard.service';
import { AssistantService } from '../../core/services/assistant.service';
import { InsightService } from '../../core/services/insight.service';
import {
  AppointmentResponse,
  InsightItem,
  InsightSummaryResponse,
  InsuranceRenewalResponse,
  LoanResponse,
  MonthlySummaryResponse,
  PendingAction,
  TripResponse
} from '../../core/models/api.models';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [CommonModule, RouterLink],
  template: `
    <div class="dashboard-container">
      <!-- Top Header -->
      <header class="dashboard-header">
        <div>
          <h1 class="page-title">Executive Dashboard</h1>
          <p class="page-subtitle">Real-time financial obligations, upcoming deadlines, and proactive life insights</p>
        </div>
        <div class="header-actions">
          <button (click)="triggerAnalysis()" class="btn btn-secondary" [disabled]="analyzing()">
            <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" [class.spin]="analyzing()">
              <path d="M21.5 2v6h-6M21.34 15.57a10 10 0 1 1-.57-8.38l5.67-5.67"/>
            </svg>
            <span>{{ analyzing() ? 'Analyzing...' : 'Scan Anomalies' }}</span>
          </button>
          <a routerLink="/assistant" class="btn btn-primary">
            <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path d="m12 3-1.9 5.8a2 2 0 0 1-1.3 1.3L3 12l5.8 1.9a2 2 0 0 1 1.3 1.3L12 21l1.9-5.8a2 2 0 0 1 1.3-1.3L21 12l-5.8-1.9a2 2 0 0 1-1.3-1.3Z"/>
            </svg>
            <span>Ask Assistant</span>
          </a>
          <a routerLink="/documents" class="btn btn-secondary">
            <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"/>
              <polyline points="17 8 12 3 7 8"/>
              <line x1="12" x2="12" y1="3" y2="15"/>
            </svg>
            <span>Upload Document</span>
          </a>
        </div>
      </header>

      <!-- Pending Actions Alert Banner -->
      @if (pendingActions().length > 0) {
        <div class="pending-alert-card glass-card">
          <div class="alert-icon">
            <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <circle cx="12" cy="12" r="10"/>
              <line x1="12" y1="8" x2="12" y2="12"/>
              <line x1="12" y1="16" x2="12.01" y2="16"/>
            </svg>
          </div>
          <div class="alert-content">
            <div class="alert-title">{{ pendingActions().length }} Action(s) Awaiting Confirmation</div>
            <div class="alert-desc">The AI Assistant prepared scheduled reminders or payments that require your explicit approval before mutating state.</div>
          </div>
          <a routerLink="/assistant" class="btn btn-sm btn-primary">Review & Confirm</a>
        </div>
      }

      <!-- Metric Grid -->
      <section class="metric-grid">
        <div class="glass-card metric-card">
          <div class="metric-header">
            <span class="metric-label">Monthly Expenses</span>
            <span class="metric-icon expense-icon">
              <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <line x1="12" x2="12" y1="2" y2="22"/>
                <path d="M17 5H9.5a3.5 3.5 0 0 0 0 7h5a3.5 3.5 0 0 1 0 7H6"/>
              </svg>
            </span>
          </div>
          <div class="metric-value">{{ formatCurrency(monthlySummary()?.totalExpenses || 0) }}</div>
          <div class="metric-footer">
            <span class="subtext">Savings Rate: {{ (monthlySummary()?.savingsRate || 0) | number:'1.1-1' }}%</span>
          </div>
        </div>

        <div class="glass-card metric-card">
          <div class="metric-header">
            <span class="metric-label">Active Loans Balance</span>
            <span class="metric-icon loan-icon">
              <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <rect width="20" height="14" x="2" y="5" rx="2"/>
                <line x1="2" x2="22" y1="10" y2="10"/>
              </svg>
            </span>
          </div>
          <div class="metric-value">{{ formatCurrency(totalLoanBalance()) }}</div>
          <div class="metric-footer">
            <span class="subtext">{{ activeLoans().length }} Active Loan(s) · Monthly EMI: {{ formatCurrency(totalMonthlyEmi()) }}</span>
          </div>
        </div>

        <div class="glass-card metric-card">
          <div class="metric-header">
            <span class="metric-label">Upcoming Renewals</span>
            <span class="metric-icon ins-icon">
              <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10"/>
              </svg>
            </span>
          </div>
          <div class="metric-value">{{ upcomingInsurance().length }}</div>
          <div class="metric-footer">
            <span class="subtext">Next 60 Days Policy Expirations</span>
          </div>
        </div>

        <div class="glass-card metric-card">
          <div class="metric-header">
            <span class="metric-label">Healthcare & Travel</span>
            <span class="metric-icon life-icon">
              <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <path d="M19 14c1.49-1.46 3-3.21 3-5.5A5.5 5.5 0 0 0 16.5 3c-1.76 0-3 .5-4.5 2-1.5-1.5-2.74-2-4.5-2A5.5 5.5 0 0 0 2 8.5c0 2.3 1.5 4.05 3 5.5l7 7Z"/>
              </svg>
            </span>
          </div>
          <div class="metric-value">{{ upcomingAppointments().length + upcomingTrips().length }}</div>
          <div class="metric-footer">
            <span class="subtext">{{ upcomingAppointments().length }} Doctor Visits · {{ upcomingTrips().length }} Trip(s)</span>
          </div>
        </div>
      </section>

      <!-- Proactive Insights & Anomaly Detection Section (Phase 19) -->
      <section class="insights-section glass-card">
        <div class="insights-header">
          <div class="insights-title-block">
            <div class="insights-badge-row">
              <span class="pulse-indicator"></span>
              <h2 class="section-title">Autonomous Optimization & Anomaly Engine</h2>
              @if (insightSummary()?.criticalCount; as cc) {
                @if (cc > 0) {
                  <span class="badge badge-critical">{{ cc }} Critical</span>
                }
              }
              @if (insightSummary()?.warningCount; as wc) {
                @if (wc > 0) {
                  <span class="badge badge-warning">{{ wc }} Warning</span>
                }
              }
              @if (insightSummary()?.infoCount; as ic) {
                @if (ic > 0) {
                  <span class="badge badge-info">{{ ic }} Optimization</span>
                }
              }
            </div>
            <p class="section-subtitle">Real-time cross-domain detection of spending spikes, budget exhaustion, debt payoff savings, warranty expiries, and schedule conflicts.</p>
          </div>
          <div class="insights-controls">
            <button (click)="loadInsights()" class="btn btn-sm btn-outline" title="Refresh active insights">
              <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <path d="M21.5 2v6h-6M21.34 15.57a10 10 0 1 1-.57-8.38l5.67-5.67"/>
              </svg>
              <span>Refresh</span>
            </button>
          </div>
        </div>

        @if (activeInsights().length === 0) {
          <div class="insights-clean-state">
            <div class="clean-icon">
              <svg width="32" height="32" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <path d="M22 11.08V12a10 10 0 1 1-5.93-9.14"/>
                <polyline points="22 4 12 14.01 9 11.01"/>
              </svg>
            </div>
            <div class="clean-text">
              <h4>All Systems Optimized</h4>
              <p>No financial surges, budget exhaustion risks, high-rate debt anomalies, or travel schedule conflicts detected.</p>
            </div>
          </div>
        } @else {
          <div class="insights-grid">
            @for (item of activeInsights(); track item.id) {
              <div class="insight-card" [ngClass]="'severity-' + item.severity.toLowerCase()">
                <div class="insight-card-header">
                  <span class="severity-tag" [ngClass]="'tag-' + item.severity.toLowerCase()">
                    {{ item.severity }}
                  </span>
                  <span class="type-tag">{{ formatType(item.insightType) }}</span>
                  <button (click)="dismiss(item.id)" class="dismiss-btn" title="Dismiss Alert">
                    <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                      <line x1="18" y1="6" x2="6" y2="18"/>
                      <line x1="6" y1="6" x2="18" y2="18"/>
                    </svg>
                  </button>
                </div>
                <h4 class="insight-title">{{ item.title }}</h4>
                <p class="insight-desc">{{ item.description }}</p>
                <div class="insight-card-footer">
                  <button (click)="handleAction(item)" class="btn btn-sm btn-action">
                    <span>{{ getActionLabel(item.actionType) }}</span>
                    <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                      <polyline points="9 18 15 12 9 6"/>
                    </svg>
                  </button>
                  <span class="insight-time">{{ item.createdAt | date:'shortDate' }}</span>
                </div>
              </div>
            }
          </div>
        }
      </section>

      <!-- Main Operational Columns -->
      <div class="dashboard-columns">
        <!-- Left: Upcoming Deadlines & Commitments -->
        <div class="glass-card column-card">
          <div class="card-title-row">
            <h3>Upcoming Obligations & Renewals</h3>
            <span class="badge badge-info">Verified Sources</span>
          </div>

          <div class="items-list">
            @if (upcomingInsurance().length === 0 && activeLoans().length === 0) {
              <div class="empty-state">No upcoming obligations recorded in this cycle.</div>
            }

            @for (ins of upcomingInsurance(); track ins.id) {
              <div class="obligation-item">
                <div class="item-icon ins-badge">INS</div>
                <div class="item-info">
                  <div class="item-title">{{ ins.policyName }} ({{ ins.providerName }})</div>
                  <div class="item-sub">Renews on {{ ins.nextRenewalDate | date:'mediumDate' }} · Premium: {{ formatCurrency(ins.premiumAmount) }}</div>
                </div>
                <span class="badge badge-pending">Renewal Due</span>
              </div>
            }

            @for (loan of activeLoans(); track loan.id) {
              <div class="obligation-item">
                <div class="item-icon loan-badge">EMI</div>
                <div class="item-info">
                  <div class="item-title">{{ loan.lenderName }} ({{ loan.loanType }})</div>
                  <div class="item-sub">Monthly EMI: {{ formatCurrency(loan.monthlyEmi) }} · Due Day: {{ loan.emiDueDay }}th</div>
                </div>
                <span class="badge badge-info">Active EMI</span>
              </div>
            }
          </div>
        </div>

        <!-- Right: Doctor Appointments & Travel -->
        <div class="glass-card column-card">
          <div class="card-title-row">
            <h3>Life Operations & Schedules</h3>
            <span class="badge badge-success">Live Schedules</span>
          </div>

          <div class="items-list">
            @if (upcomingAppointments().length === 0 && upcomingTrips().length === 0) {
              <div class="empty-state">No doctor visits or itineraries scheduled.</div>
            }

            @for (app of upcomingAppointments(); track app.id) {
              <div class="obligation-item">
                <div class="item-icon health-badge">DOC</div>
                <div class="item-info">
                  <div class="item-title">{{ app.doctorName }} — {{ app.medicalSpecialty }}</div>
                  <div class="item-sub">{{ app.clinicName }} · {{ app.appointmentTime | date:'medium' }}</div>
                </div>
                <span class="badge badge-success">{{ app.status }}</span>
              </div>
            }

            @for (trip of upcomingTrips(); track trip.id) {
              <div class="obligation-item">
                <div class="item-icon trip-badge">TRIP</div>
                <div class="item-info">
                  <div class="item-title">{{ trip.tripTitle }} · {{ trip.destination }}</div>
                  <div class="item-sub">{{ trip.startDate | date:'mediumDate' }} – {{ trip.endDate | date:'mediumDate' }}</div>
                </div>
                <span class="badge badge-info">{{ trip.status }}</span>
              </div>
            }
          </div>
        </div>
      </div>
    </div>
  `,
  styles: [`
    .dashboard-container {
      padding: 2rem 2.5rem;
      display: flex;
      flex-direction: column;
      gap: 1.75rem;
      max-width: 1440px;
      margin: 0 auto;
    }

    .dashboard-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      flex-wrap: wrap;
      gap: 1rem;
    }

    .page-title {
      font-size: 1.85rem;
      margin-bottom: 0.25rem;
    }

    .page-subtitle {
      font-size: 0.875rem;
      color: var(--text-muted);
    }

    .header-actions {
      display: flex;
      gap: 0.75rem;
    }

    /* Pending Actions Alert */
    .pending-alert-card {
      display: flex;
      align-items: center;
      gap: 1rem;
      padding: 1rem 1.25rem;
      background: rgba(245, 158, 11, 0.1);
      border: 1px solid rgba(245, 158, 11, 0.35);
      border-radius: var(--radius-md);
    }

    .alert-icon {
      color: var(--accent-amber);
      display: flex;
      align-items: center;
    }

    .alert-content {
      flex: 1;
    }

    .alert-title {
      font-weight: 700;
      color: var(--accent-amber);
      font-size: 0.925rem;
    }

    .alert-desc {
      font-size: 0.8rem;
      color: var(--text-secondary);
    }

    /* Metrics Grid */
    .metric-grid {
      display: grid;
      grid-template-columns: repeat(auto-fit, minmax(240px, 1fr));
      gap: 1.25rem;
    }

    .metric-card {
      padding: 1.35rem 1.25rem;
    }

    .metric-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin-bottom: 0.75rem;
    }

    .metric-label {
      font-size: 0.825rem;
      font-weight: 600;
      color: var(--text-secondary);
      text-transform: uppercase;
      letter-spacing: 0.04em;
    }

    .metric-icon {
      width: 32px;
      height: 32px;
      border-radius: var(--radius-sm);
      display: flex;
      align-items: center;
      justify-content: center;
    }

    .expense-icon { background: rgba(244, 63, 94, 0.15); color: var(--accent-rose); }
    .loan-icon { background: rgba(99, 102, 241, 0.15); color: var(--primary); }
    .ins-icon { background: rgba(245, 158, 11, 0.15); color: var(--accent-amber); }
    .life-icon { background: rgba(16, 185, 129, 0.15); color: var(--accent-emerald); }

    .metric-value {
      font-family: 'Outfit', sans-serif;
      font-size: 1.85rem;
      font-weight: 800;
      letter-spacing: -0.02em;
      margin-bottom: 0.35rem;
      color: var(--text-main);
    }

    .metric-footer .subtext {
      font-size: 0.75rem;
      color: var(--text-muted);
    }

    /* Proactive Insights Section */
    .insights-section {
      padding: 1.5rem;
      border: 1px solid rgba(99, 102, 241, 0.25);
      background: linear-gradient(135deg, rgba(30, 41, 59, 0.4) 0%, rgba(15, 23, 42, 0.6) 100%);
      display: flex;
      flex-direction: column;
      gap: 1.25rem;
    }

    .insights-header {
      display: flex;
      justify-content: space-between;
      align-items: flex-start;
      flex-wrap: wrap;
      gap: 1rem;
    }

    .insights-badge-row {
      display: flex;
      align-items: center;
      gap: 0.75rem;
      flex-wrap: wrap;
    }

    .pulse-indicator {
      width: 10px;
      height: 10px;
      border-radius: 50%;
      background: var(--primary);
      box-shadow: 0 0 10px var(--primary);
      animation: pulse 2s infinite;
    }

    @keyframes pulse {
      0% { transform: scale(0.95); opacity: 0.8; }
      50% { transform: scale(1.3); opacity: 1; }
      100% { transform: scale(0.95); opacity: 0.8; }
    }

    .section-title {
      font-size: 1.15rem;
      font-weight: 700;
      color: var(--text-main);
      margin: 0;
    }

    .section-subtitle {
      font-size: 0.825rem;
      color: var(--text-secondary);
      margin-top: 0.35rem;
      max-width: 900px;
    }

    .insights-controls {
      display: flex;
      align-items: center;
      gap: 0.5rem;
    }

    .badge-critical {
      background: rgba(244, 63, 94, 0.2);
      color: #fda4af;
      border: 1px solid rgba(244, 63, 94, 0.4);
      padding: 0.2rem 0.5rem;
      border-radius: 4px;
      font-size: 0.75rem;
      font-weight: 700;
    }

    .badge-warning {
      background: rgba(245, 158, 11, 0.2);
      color: #fde68a;
      border: 1px solid rgba(245, 158, 11, 0.4);
      padding: 0.2rem 0.5rem;
      border-radius: 4px;
      font-size: 0.75rem;
      font-weight: 700;
    }

    .insights-clean-state {
      display: flex;
      align-items: center;
      gap: 1.25rem;
      padding: 1.25rem 1.5rem;
      background: rgba(16, 185, 129, 0.08);
      border: 1px solid rgba(16, 185, 129, 0.25);
      border-radius: var(--radius-md);
    }

    .clean-icon {
      color: var(--accent-emerald);
      flex-shrink: 0;
    }

    .clean-text h4 {
      font-size: 0.95rem;
      color: var(--accent-emerald);
      margin: 0 0 0.25rem 0;
      font-weight: 600;
    }

    .clean-text p {
      font-size: 0.825rem;
      color: var(--text-secondary);
      margin: 0;
    }

    .insights-grid {
      display: grid;
      grid-template-columns: repeat(auto-fit, minmax(320px, 1fr));
      gap: 1rem;
    }

    .insight-card {
      background: rgba(15, 23, 42, 0.75);
      border-radius: var(--radius-md);
      padding: 1.15rem 1.25rem;
      display: flex;
      flex-direction: column;
      gap: 0.75rem;
      border: 1px solid var(--border-subtle);
      transition: transform var(--transition-fast), border-color var(--transition-fast);
    }

    .insight-card:hover {
      transform: translateY(-2px);
    }

    .severity-critical {
      border-color: rgba(244, 63, 94, 0.4);
      border-left: 4px solid var(--accent-rose);
    }

    .severity-warning {
      border-color: rgba(245, 158, 11, 0.4);
      border-left: 4px solid var(--accent-amber);
    }

    .severity-info {
      border-color: rgba(6, 182, 212, 0.4);
      border-left: 4px solid var(--accent-cyan);
    }

    .insight-card-header {
      display: flex;
      align-items: center;
      justify-content: space-between;
      gap: 0.5rem;
    }

    .severity-tag {
      font-size: 0.65rem;
      font-weight: 800;
      letter-spacing: 0.05em;
      padding: 0.15rem 0.45rem;
      border-radius: 3px;
    }

    .tag-critical { background: rgba(244, 63, 94, 0.2); color: #fda4af; }
    .tag-warning { background: rgba(245, 158, 11, 0.2); color: #fde68a; }
    .tag-info { background: rgba(6, 182, 212, 0.2); color: #a5f3fc; }

    .type-tag {
      font-size: 0.7rem;
      font-weight: 600;
      color: var(--text-muted);
      flex: 1;
      text-transform: uppercase;
      letter-spacing: 0.04em;
    }

    .dismiss-btn {
      background: none;
      border: none;
      color: var(--text-muted);
      cursor: pointer;
      padding: 0.2rem;
      border-radius: 4px;
      display: flex;
      align-items: center;
      justify-content: center;
      transition: color var(--transition-fast);
    }

    .dismiss-btn:hover {
      color: var(--text-main);
      background: rgba(255, 255, 255, 0.05);
    }

    .insight-title {
      font-size: 0.95rem;
      font-weight: 700;
      color: var(--text-main);
      margin: 0;
      line-height: 1.35;
    }

    .insight-desc {
      font-size: 0.8rem;
      color: var(--text-secondary);
      line-height: 1.5;
      margin: 0;
      flex: 1;
    }

    .insight-card-footer {
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin-top: 0.25rem;
      padding-top: 0.5rem;
      border-top: 1px solid rgba(255, 255, 255, 0.05);
    }

    .btn-action {
      background: rgba(99, 102, 241, 0.15);
      color: #a5b4fc;
      border: 1px solid rgba(99, 102, 241, 0.3);
      font-size: 0.75rem;
      font-weight: 600;
      display: inline-flex;
      align-items: center;
      gap: 0.35rem;
      padding: 0.35rem 0.65rem;
      border-radius: var(--radius-sm);
      cursor: pointer;
      transition: all var(--transition-fast);
    }

    .btn-action:hover {
      background: rgba(99, 102, 241, 0.3);
      color: #ffffff;
    }

    .insight-time {
      font-size: 0.7rem;
      color: var(--text-muted);
    }

    .spin {
      animation: spin 1s linear infinite;
    }

    @keyframes spin {
      from { transform: rotate(0deg); }
      to { transform: rotate(360deg); }
    }

    /* Columns */
    .dashboard-columns {
      display: grid;
      grid-template-columns: 1fr 1fr;
      gap: 1.5rem;
    }

    @media (max-width: 900px) {
      .dashboard-columns {
        grid-template-columns: 1fr;
      }
    }

    .column-card {
      padding: 1.5rem;
      display: flex;
      flex-direction: column;
      gap: 1.25rem;
    }

    .card-title-row {
      display: flex;
      justify-content: space-between;
      align-items: center;
    }

    .items-list {
      display: flex;
      flex-direction: column;
      gap: 0.75rem;
    }

    .empty-state {
      padding: 2rem 1rem;
      text-align: center;
      color: var(--text-muted);
      font-size: 0.85rem;
    }

    .obligation-item {
      display: flex;
      align-items: center;
      gap: 0.85rem;
      padding: 0.75rem 0.85rem;
      background: rgba(255, 255, 255, 0.02);
      border: 1px solid var(--border-subtle);
      border-radius: var(--radius-sm);
      transition: background var(--transition-fast);
    }

    .obligation-item:hover {
      background: rgba(255, 255, 255, 0.04);
    }

    .item-icon {
      font-size: 0.7rem;
      font-weight: 800;
      padding: 0.25rem 0.45rem;
      border-radius: 4px;
      letter-spacing: 0.05em;
    }

    .ins-badge { background: rgba(245, 158, 11, 0.2); color: var(--accent-amber); }
    .loan-badge { background: rgba(99, 102, 241, 0.2); color: #a5b4fc; }
    .health-badge { background: rgba(16, 185, 129, 0.2); color: var(--accent-emerald); }
    .trip-badge { background: rgba(6, 182, 212, 0.2); color: var(--accent-cyan); }

    .item-info {
      flex: 1;
    }

    .item-title {
      font-weight: 600;
      font-size: 0.875rem;
      color: var(--text-main);
    }

    .item-sub {
      font-size: 0.75rem;
      color: var(--text-muted);
    }
  `]
})
export class DashboardComponent implements OnInit {
  private dashboardService = inject(DashboardService);
  private assistantService = inject(AssistantService);
  private insightService = inject(InsightService);
  private router = inject(Router);

  monthlySummary = signal<MonthlySummaryResponse | null>(null);
  activeLoans = signal<LoanResponse[]>([]);
  upcomingInsurance = signal<InsuranceRenewalResponse[]>([]);
  upcomingAppointments = signal<AppointmentResponse[]>([]);
  upcomingTrips = signal<TripResponse[]>([]);
  pendingActions = this.assistantService.pendingActions;

  // Phase 19: Proactive Life Insights
  activeInsights = signal<InsightItem[]>([]);
  insightSummary = signal<InsightSummaryResponse | null>(null);
  analyzing = signal<boolean>(false);

  totalLoanBalance = signal<number>(0);
  totalMonthlyEmi = signal<number>(0);

  ngOnInit() {
    const now = new Date();
    this.dashboardService.getMonthlySummary(now.getMonth() + 1, now.getFullYear()).subscribe({
      next: res => { if (res.success) this.monthlySummary.set(res.data); }
    });

    this.dashboardService.getActiveLoans().subscribe({
      next: res => {
        if (res.success && res.data) {
          this.activeLoans.set(res.data);
          const totalBal = res.data.reduce((acc, l) => acc + (l.outstandingBalance || 0), 0);
          const totalEmi = res.data.reduce((acc, l) => acc + (l.monthlyEmi || 0), 0);
          this.totalLoanBalance.set(totalBal);
          this.totalMonthlyEmi.set(totalEmi);
        }
      }
    });

    this.dashboardService.getUpcomingInsurance(60).subscribe({
      next: res => { if (res.success && res.data) this.upcomingInsurance.set(res.data); }
    });

    this.dashboardService.getUpcomingAppointments(30).subscribe({
      next: res => { if (res.success && res.data) this.upcomingAppointments.set(res.data); }
    });

    this.dashboardService.getUpcomingTrips(30).subscribe({
      next: res => { if (res.success && res.data) this.upcomingTrips.set(res.data); }
    });

    this.assistantService.loadPendingActions().subscribe();

    this.loadInsights();
  }

  loadInsights() {
    this.insightService.getInsights().subscribe({
      next: res => {
        if (res.success && res.data) {
          this.insightSummary.set(res.data);
          this.activeInsights.set(res.data.insights || []);
        }
      }
    });
  }

  triggerAnalysis() {
    this.analyzing.set(true);
    this.insightService.generateInsights().subscribe({
      next: res => {
        this.analyzing.set(false);
        if (res.success && res.data) {
          this.insightSummary.set(res.data);
          this.activeInsights.set(res.data.insights || []);
        }
      },
      error: () => this.analyzing.set(false)
    });
  }

  dismiss(id: string) {
    this.insightService.dismissInsight(id).subscribe({
      next: res => {
        if (res.success) {
          this.activeInsights.update(list => list.filter(item => item.id !== id));
          if (this.insightSummary()) {
            this.loadInsights();
          }
        }
      }
    });
  }

  handleAction(item: InsightItem) {
    this.insightService.actionInsight(item.id).subscribe({
      next: () => {
        // Route according to action type
        switch (item.actionType) {
          case 'VIEW_BUDGET':
            this.router.navigate(['/finance']);
            break;
          case 'SIMULATE_PREPAYMENT':
            this.router.navigate(['/loans-insurance']);
            break;
          case 'RENEW_POLICY':
            this.router.navigate(['/loans-insurance']);
            break;
          case 'VIEW_WARRANTY':
            this.router.navigate(['/life-operations']);
            break;
          case 'CHECK_ITINERARY':
            this.router.navigate(['/life-operations']);
            break;
          default:
            this.router.navigate(['/assistant']);
        }
      }
    });
  }

  formatType(type: string): string {
    return type.replace(/_/g, ' ');
  }

  getActionLabel(actionType: string): string {
    switch (actionType) {
      case 'VIEW_BUDGET': return 'Review Budget';
      case 'SIMULATE_PREPAYMENT': return 'Simulate Prepayment';
      case 'RENEW_POLICY': return 'Manage Policy';
      case 'VIEW_WARRANTY': return 'Inspect Warranty';
      case 'CHECK_ITINERARY': return 'Resolve Conflict';
      default: return 'Take Action';
    }
  }

  formatCurrency(value: number): string {
    return new Intl.NumberFormat('en-US', { style: 'currency', currency: 'USD' }).format(value);
  }
}
