import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { LoanInsuranceService } from '../../core/services/loan-insurance.service';
import { AuditService } from '../../core/services/audit.service';
import { DocumentService } from '../../core/services/document.service';
import {
  AmortizationEntry,
  DocumentResponse,
  InsurancePolicyResponse,
  LoanResponse,
  PolicyComparisonResponse,
  PrepaymentSimulationRequest,
  PrepaymentSimulationResult
} from '../../core/models/api.models';

@Component({
  selector: 'app-loans-insurance',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <div class="loans-container">
      <header class="page-header">
        <div>
          <h1 class="page-title">Loans & Insurance Portfolio</h1>
          <p class="page-subtitle">Exact loan amortization, prepayment impact simulation, and policy renewal tracking</p>
        </div>
      </header>

      <!-- Section: Active Loans -->
      <section class="section-block">
        <div class="section-header">
          <h2>Active Loans Portfolio</h2>
          <span class="subtext">Deterministic amortization & balance calculations</span>
        </div>

        @if (loadingLoans()) {
          <div class="loading-state">Loading active loans...</div>
        } @else if (loans().length === 0) {
          <div class="empty-state-mini glass-card">No active loans registered in LifeOS.</div>
        } @else {
          <div class="loans-grid">
            @for (loan of loans(); track loan.id) {
              <div class="glass-card loan-card">
                <div class="loan-card-top">
                  <div>
                    <h3 class="lender-name">{{ loan.lenderName }}</h3>
                    <span class="loan-type-tag">{{ loan.loanType }}</span>
                  </div>
                  <div class="loan-status">{{ loan.status }}</div>
                </div>

                <div class="loan-metrics-row">
                  <div>
                    <div class="metric-mini-label">Outstanding Balance</div>
                    <div class="metric-mini-val text-amber">{{ formatCurrency(loan.outstandingBalance) }}</div>
                  </div>
                  <div>
                    <div class="metric-mini-label">Monthly EMI</div>
                    <div class="metric-mini-val">{{ formatCurrency(loan.monthlyEmi) }}</div>
                  </div>
                </div>

                <div class="loan-specs">
                  <div class="spec-item">
                    <span class="spec-label">Interest Rate:</span>
                    <span class="spec-val">{{ loan.interestRate }}%</span>
                  </div>
                  <div class="spec-item">
                    <span class="spec-label">Tenure:</span>
                    <span class="spec-val">{{ loan.tenureMonths }} mos</span>
                  </div>
                  <div class="spec-item">
                    <span class="spec-label">Due Day:</span>
                    <span class="spec-val">{{ loan.emiDueDay }}th of month</span>
                  </div>
                </div>

                <div class="loan-actions">
                  <button (click)="openSimulator(loan)" class="btn btn-sm btn-primary">
                    <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                      <line x1="12" y1="1" x2="12" y2="23"/>
                      <path d="M17 5H9.5a3.5 3.5 0 0 0 0 7h5a3.5 3.5 0 0 1 0 7H6"/>
                    </svg>
                    <span>Simulate Prepayment</span>
                  </button>
                  <button (click)="viewSchedule(loan)" class="btn btn-sm btn-secondary">
                    <span>Amortization Schedule</span>
                  </button>
                </div>
              </div>
            }
          </div>
        }
      </section>

      <!-- Section: Insurance Policies -->
      <section class="section-block">
        <div class="section-header">
          <div>
            <h2>Insurance Policies & Renewals</h2>
            <span class="subtext">Active life, health, vehicle, and home coverage</span>
          </div>
          <button (click)="openComparisonModal()" class="btn btn-sm btn-secondary comp-trigger-btn">
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M16 3h5v5"/>
              <path d="M8 21H3v-5"/>
              <path d="M21 3l-7 7"/>
              <path d="M3 21l7-7"/>
            </svg>
            <span>Compare Policies</span>
          </button>
        </div>

        @if (loadingInsurance()) {
          <div class="loading-state">Loading insurance policies...</div>
        } @else if (policies().length === 0) {
          <div class="empty-state-mini glass-card">No insurance policies registered.</div>
        } @else {
          <div class="insurance-grid">
            @for (pol of policies(); track pol.id) {
              <div class="glass-card policy-card">
                <div class="policy-header">
                  <div>
                    <h3 class="policy-name">{{ pol.policyName }}</h3>
                    <div class="provider-name">{{ pol.providerName }} · {{ pol.policyType }}</div>
                  </div>
                  <span class="policy-status">{{ pol.status }}</span>
                </div>

                <div class="policy-body">
                  <div class="policy-stat">
                    <span class="stat-label">Coverage Amount</span>
                    <span class="stat-val text-emerald">{{ formatCurrency(pol.coverageAmount) }}</span>
                  </div>
                  <div class="policy-stat">
                    <span class="stat-label">Premium</span>
                    <span class="stat-val">{{ formatCurrency(pol.premiumAmount) }} / {{ pol.premiumFrequency }}</span>
                  </div>
                </div>

                <div class="policy-footer">
                  <div class="renewal-info">
                    <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                      <circle cx="12" cy="12" r="10"/>
                      <polyline points="12 6 12 12 16 14"/>
                    </svg>
                    <span>Next Renewal: {{ pol.nextRenewalDate | date:'mediumDate' }}</span>
                  </div>
                </div>
              </div>
            }
          </div>
        }
      </section>

      <!-- Prepayment Simulator Modal -->
      @if (activeLoanForSim()) {
        <div class="modal-backdrop" (click)="activeLoanForSim.set(null)">
          <div class="modal-card glass-card" (click)="$event.stopPropagation()">
            <div class="modal-header">
              <div>
                <h3>Prepayment Impact Simulator</h3>
                <div class="subtext">{{ activeLoanForSim()?.lenderName }} (Balance: {{ formatCurrency(activeLoanForSim()?.outstandingBalance || 0) }})</div>
              </div>
              <button (click)="activeLoanForSim.set(null)" class="btn-icon">
                <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <line x1="18" y1="6" x2="6" y2="18"/>
                  <line x1="6" y1="6" x2="18" y2="18"/>
                </svg>
              </button>
            </div>

            <form (ngSubmit)="runSimulation()" class="modal-body">
              <div class="form-group">
                <label class="form-label">Lump-Sum Prepayment Amount ($)</label>
                <input
                  type="number"
                  [(ngModel)]="simAmount"
                  name="simAmount"
                  required
                  class="form-input"
                  placeholder="e.g. 5000"
                />
              </div>

              <div class="form-group">
                <label class="form-label">Strategy</label>
                <select [(ngModel)]="simType" name="simType" class="form-select">
                  <option value="REDUCE_TENURE">Reduce Tenure (Pay off loan sooner)</option>
                  <option value="REDUCE_EMI">Reduce Monthly EMI (Lower monthly cash outflow)</option>
                </select>
              </div>

              <button type="submit" [disabled]="simulating() || !simAmount" class="btn btn-primary sim-btn">
                {{ simulating() ? 'Calculating...' : 'Run Simulation' }}
              </button>

              @if (simResult()) {
                <div class="sim-results glass-card">
                  <div class="result-highlight">
                    <div class="highlight-label">Total Interest Saved</div>
                    <div class="highlight-value text-emerald">
                      {{ formatCurrency(simResult()?.interestSaved || 0) }}
                    </div>
                  </div>

                  <div class="sim-details-grid">
                    @if (simType === 'REDUCE_TENURE') {
                      <div>
                        <span class="subtext">Tenure Reduction:</span>
                        <div class="bold-val">{{ simResult()?.monthsSaved }} months saved</div>
                      </div>
                      <div>
                        <span class="subtext">New Tenure:</span>
                        <div class="bold-val">{{ simResult()?.newTenureMonths }} months remaining</div>
                      </div>
                    } @else {
                      <div>
                        <span class="subtext">New Monthly EMI:</span>
                        <div class="bold-val text-cyan">{{ formatCurrency(simResult()?.newMonthlyEmi || 0) }}</div>
                      </div>
                    }
                  </div>
                </div>
              }
            </form>
          </div>
        </div>
      }

      <!-- Amortization Schedule Drawer -->
      @if (activeLoanForSchedule()) {
        <aside class="schedule-drawer glass-panel">
          <div class="drawer-header">
            <div>
              <h3>Amortization Schedule</h3>
              <div class="subtext">{{ activeLoanForSchedule()?.lenderName }} ({{ activeLoanForSchedule()?.loanType }})</div>
            </div>
            <button (click)="activeLoanForSchedule.set(null)" class="btn-icon">
              <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <line x1="18" y1="6" x2="6" y2="18"/>
                <line x1="6" y1="6" x2="18" y2="18"/>
              </svg>
            </button>
          </div>

          <div class="drawer-table-wrap">
            @if (loadingSchedule()) {
              <div class="loading-state">Computing amortization schedule...</div>
            } @else {
              <table class="data-table">
                <thead>
                  <tr>
                    <th>#</th>
                    <th>Date</th>
                    <th>EMI</th>
                    <th>Principal</th>
                    <th>Interest</th>
                    <th>Balance</th>
                  </tr>
                </thead>
                <tbody>
                  @for (entry of schedule(); track entry.installmentNumber) {
                    <tr>
                      <td>{{ entry.installmentNumber }}</td>
                      <td>{{ entry.paymentDate | date:'mediumDate' }}</td>
                      <td>{{ formatCurrency(entry.emiAmount) }}</td>
                      <td class="text-emerald">{{ formatCurrency(entry.principalComponent) }}</td>
                      <td class="text-rose">{{ formatCurrency(entry.interestComponent) }}</td>
                      <td>{{ formatCurrency(entry.remainingBalance) }}</td>
                    </tr>
                  }
                </tbody>
              </table>
            }
          </div>
        </aside>
      }

      <!-- Phase 17: Policy Comparison Modal -->
      @if (showComparisonModal()) {
        <div class="modal-backdrop" (click)="closeComparisonModal()">
          <div class="modal-card comp-modal glass-card" (click)="$event.stopPropagation()">
            <div class="modal-header">
              <div>
                <h3>Insurance Policy Clause Comparison</h3>
                <div class="subtext">Clause diffing, coverage limits, exclusions & recommendation synthesis</div>
              </div>
              <button (click)="closeComparisonModal()" class="btn-icon">
                <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <line x1="18" y1="6" x2="6" y2="18"/>
                  <line x1="6" y1="6" x2="18" y2="18"/>
                </svg>
              </button>
            </div>

            <div class="modal-body comp-modal-body">
              <div class="selection-grid">
                <div class="form-group">
                  <label class="form-label">Base Policy Document (Doc 1)</label>
                  @if (insuranceDocuments().length > 0) {
                    <select [(ngModel)]="selectedDoc1" class="form-select">
                      <option value="">-- Select Base Policy --</option>
                      @for (doc of insuranceDocuments(); track doc.id) {
                        <option [value]="doc.id">{{ doc.title }} (v{{ doc.version }})</option>
                      }
                    </select>
                  } @else {
                    <input type="text" [(ngModel)]="selectedDoc1" placeholder="Enter Policy Document UUID" class="form-input"/>
                  }
                </div>

                <div class="form-group">
                  <label class="form-label">Renewal / Target Policy (Doc 2)</label>
                  @if (insuranceDocuments().length > 0) {
                    <select [(ngModel)]="selectedDoc2" class="form-select">
                      <option value="">-- Select Target Policy --</option>
                      @for (doc of insuranceDocuments(); track doc.id) {
                        <option [value]="doc.id">{{ doc.title }} (v{{ doc.version }})</option>
                      }
                    </select>
                  } @else {
                    <input type="text" [(ngModel)]="selectedDoc2" placeholder="Enter Policy Document UUID" class="form-input"/>
                  }
                </div>
              </div>

              <div class="comp-action-bar">
                <button (click)="executeComparison()" [disabled]="comparing() || !selectedDoc1 || !selectedDoc2" class="btn btn-primary">
                  {{ comparing() ? 'Analyzing & Comparing Clauses...' : 'Compare Clauses' }}
                </button>
                @if (comparisonError()) {
                  <span class="comp-error-msg">{{ comparisonError() }}</span>
                }
              </div>

              <!-- Results Display -->
              @if (comparisonResult()) {
                <div class="comp-results-container">
                  <div class="comp-doc-header">
                    <div class="comp-doc-badge">
                      <span class="doc-label">DOC 1</span>
                      <span class="doc-title">{{ comparisonResult()?.documentTitle1 }}</span>
                    </div>
                    <div class="vs-divider">VS</div>
                    <div class="comp-doc-badge">
                      <span class="doc-label">DOC 2</span>
                      <span class="doc-title">{{ comparisonResult()?.documentTitle2 }}</span>
                    </div>
                  </div>

                  <div class="comp-summary-box">
                    <h4>Executive Summary</h4>
                    <p>{{ comparisonResult()?.summary }}</p>
                  </div>

                  <div class="benefits-grid">
                    <div class="benefit-box added">
                      <h5>Added Benefits & Enhancements</h5>
                      <ul>
                        @for (item of comparisonResult()?.addedBenefits; track item) {
                          <li><span class="symbol-add">+</span> {{ item }}</li>
                        }
                      </ul>
                    </div>
                    <div class="benefit-box removed">
                      <h5>Removed Benefits & Deductions</h5>
                      <ul>
                        @for (item of comparisonResult()?.removedBenefits; track item) {
                          <li><span class="symbol-remove">&times;</span> {{ item }}</li>
                        }
                      </ul>
                    </div>
                  </div>

                  <div class="diff-section">
                    <h5>Key Clause Analysis</h5>
                    <table class="data-table">
                      <thead>
                        <tr>
                          <th>Category</th>
                          <th>Clause</th>
                          <th>Doc 1 Value</th>
                          <th>Doc 2 Value</th>
                          <th>Impact</th>
                        </tr>
                      </thead>
                      <tbody>
                        @for (diff of comparisonResult()?.clauseDifferences; track diff.clauseName) {
                          <tr>
                            <td class="font-medium">{{ diff.category }}</td>
                            <td>{{ diff.clauseName }}</td>
                            <td class="text-muted">{{ diff.document1Value }}</td>
                            <td class="font-medium text-main">{{ diff.document2Value }}</td>
                            <td>
                              <span class="impact-tag" [class.positive]="diff.impact === 'POSITIVE'" [class.negative]="diff.impact === 'NEGATIVE'">
                                {{ diff.impact }}
                              </span>
                            </td>
                          </tr>
                        }
                      </tbody>
                    </table>
                  </div>

                  <div class="recommendation-box">
                    <h5>Audit Recommendation</h5>
                    <p class="rec-text">{{ comparisonResult()?.overallRecommendation }}</p>
                    <p class="premium-text">{{ comparisonResult()?.premiumAnalysis }}</p>
                  </div>
                </div>
              }
            </div>
          </div>
        </div>
      }
    </div>
  `,
  styles: [`
    .loans-container {
      padding: 2rem;
      max-width: 1400px;
      margin: 0 auto;
      height: 100%;
      overflow-y: auto;
    }

    .page-header {
      margin-bottom: 2rem;
    }

    .section-block {
      margin-bottom: 3rem;
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

    .loans-grid {
      display: grid;
      grid-template-columns: repeat(auto-fit, minmax(340px, 1fr));
      gap: 1.5rem;
    }

    .loan-card {
      padding: 1.5rem;
      display: flex;
      flex-direction: column;
    }

    .loan-card-top {
      display: flex;
      justify-content: space-between;
      align-items: flex-start;
      margin-bottom: 1.25rem;
    }

    .lender-name {
      font-size: 1.15rem;
      margin: 0 0 0.25rem;
    }

    .loan-type-tag {
      font-size: 0.75rem;
      background: rgba(99, 102, 241, 0.15);
      color: var(--primary);
      padding: 0.15rem 0.5rem;
      border-radius: 4px;
    }

    .loan-status {
      font-size: 0.75rem;
      font-weight: 600;
      color: var(--accent-emerald);
      background: rgba(16, 185, 129, 0.1);
      padding: 0.2rem 0.5rem;
      border-radius: 4px;
    }

    .loan-metrics-row {
      display: grid;
      grid-template-columns: 1fr 1fr;
      gap: 1rem;
      padding: 1rem 0;
      border-top: 1px solid var(--border-subtle);
      border-bottom: 1px solid var(--border-subtle);
      margin-bottom: 1rem;
    }

    .metric-mini-label {
      font-size: 0.75rem;
      color: var(--text-muted);
      text-transform: uppercase;
      font-weight: 600;
      margin-bottom: 0.25rem;
    }

    .metric-mini-val {
      font-size: 1.35rem;
      font-weight: 700;
      font-family: 'Outfit', sans-serif;
    }

    .text-amber { color: var(--accent-amber); }
    .text-emerald { color: var(--accent-emerald); }
    .text-rose { color: var(--accent-rose); }
    .text-cyan { color: var(--accent-cyan); }

    .loan-specs {
      display: flex;
      justify-content: space-between;
      font-size: 0.8rem;
      color: var(--text-secondary);
      margin-bottom: 1.25rem;
    }

    .spec-item {
      display: flex;
      gap: 0.35rem;
    }

    .spec-label {
      color: var(--text-muted);
    }

    .spec-val {
      font-weight: 600;
      color: var(--text-main);
    }

    .loan-actions {
      display: flex;
      gap: 0.75rem;
      margin-top: auto;
    }

    .insurance-grid {
      display: grid;
      grid-template-columns: repeat(auto-fit, minmax(320px, 1fr));
      gap: 1.5rem;
    }

    .policy-card {
      padding: 1.5rem;
    }

    .policy-header {
      display: flex;
      justify-content: space-between;
      align-items: flex-start;
      margin-bottom: 1rem;
    }

    .policy-name {
      font-size: 1.1rem;
      margin: 0 0 0.25rem;
    }

    .provider-name {
      font-size: 0.8rem;
      color: var(--text-muted);
    }

    .policy-status {
      font-size: 0.7rem;
      font-weight: 700;
      color: var(--accent-cyan);
      background: rgba(6, 182, 212, 0.1);
      padding: 0.15rem 0.5rem;
      border-radius: 4px;
    }

    .policy-body {
      display: grid;
      grid-template-columns: 1fr 1fr;
      gap: 1rem;
      margin-bottom: 1rem;
      padding: 0.75rem 0;
      border-top: 1px solid var(--border-subtle);
      border-bottom: 1px solid var(--border-subtle);
    }

    .stat-label {
      font-size: 0.75rem;
      color: var(--text-muted);
      display: block;
      margin-bottom: 0.25rem;
    }

    .stat-val {
      font-size: 1.15rem;
      font-weight: 700;
      font-family: 'Outfit', sans-serif;
    }

    .policy-footer {
      font-size: 0.8rem;
      color: var(--text-secondary);
    }

    .renewal-info {
      display: flex;
      align-items: center;
      gap: 0.4rem;
      color: var(--accent-amber);
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
      justify-content: space-between;
      align-items: flex-start;
      margin-bottom: 1.5rem;
    }

    .sim-btn {
      width: 100%;
      justify-content: center;
      margin-top: 0.5rem;
    }

    .sim-results {
      margin-top: 1.25rem;
      padding: 1.25rem;
      background: rgba(16, 185, 129, 0.05);
      border-color: rgba(16, 185, 129, 0.2);
    }

    .result-highlight {
      text-align: center;
      margin-bottom: 1rem;
      padding-bottom: 0.75rem;
      border-bottom: 1px solid var(--border-subtle);
    }

    .highlight-label {
      font-size: 0.8rem;
      color: var(--text-muted);
      text-transform: uppercase;
      font-weight: 600;
    }

    .highlight-value {
      font-size: 2rem;
      font-weight: 700;
      font-family: 'Outfit', sans-serif;
    }

    .sim-details-grid {
      display: flex;
      justify-content: space-around;
      font-size: 0.85rem;
    }

    .bold-val {
      font-weight: 700;
      color: var(--text-main);
      margin-top: 0.2rem;
    }

    .schedule-drawer {
      position: fixed;
      top: 0;
      right: 0;
      width: 580px;
      height: 100vh;
      background: rgba(11, 16, 30, 0.95);
      border-left: 1px solid var(--border-light);
      z-index: 90;
      display: flex;
      flex-direction: column;
      box-shadow: -10px 0 30px rgba(0, 0, 0, 0.5);
    }

    .drawer-header {
      padding: 1.5rem;
      border-bottom: 1px solid var(--border-subtle);
      display: flex;
      justify-content: space-between;
      align-items: flex-start;
    }

    .drawer-table-wrap {
      flex: 1;
      overflow-y: auto;
      padding: 1rem;
    }

    .data-table {
      width: 100%;
      border-collapse: collapse;
      text-align: left;
      font-size: 0.85rem;
    }

    .data-table th, .data-table td {
      padding: 0.75rem 0.85rem;
      border-bottom: 1px solid var(--border-subtle);
    }

    .data-table th {
      font-size: 0.75rem;
      color: var(--text-muted);
      text-transform: uppercase;
    }

    .loading-state, .empty-state-mini {
      padding: 2rem;
      text-align: center;
      color: var(--text-muted);
      font-size: 0.9rem;
    }

    /* Phase 17: Comparison Modal & Diff Results */
    .comp-trigger-btn {
      display: flex;
      align-items: center;
      gap: 0.4rem;
    }

    .comp-modal {
      width: 950px;
      max-width: 95vw;
      max-height: 90vh;
      display: flex;
      flex-direction: column;
      background: rgba(15, 23, 42, 0.96);
      border: 1px solid var(--border-subtle);
      border-radius: var(--radius-md);
      box-shadow: 0 20px 40px rgba(0, 0, 0, 0.6);
    }

    .comp-modal-body {
      flex: 1;
      overflow-y: auto;
      padding: 1.5rem;
    }

    .selection-grid {
      display: grid;
      grid-template-columns: 1fr 1fr;
      gap: 1.25rem;
      margin-bottom: 1rem;
    }

    .comp-action-bar {
      display: flex;
      align-items: center;
      gap: 1rem;
      margin-bottom: 1.5rem;
      padding-bottom: 1rem;
      border-bottom: 1px solid var(--border-subtle);
    }

    .comp-error-msg {
      color: var(--accent-rose);
      font-size: 0.8rem;
    }

    .comp-results-container {
      display: flex;
      flex-direction: column;
      gap: 1.25rem;
    }

    .comp-doc-header {
      display: flex;
      align-items: center;
      justify-content: space-between;
      background: rgba(255, 255, 255, 0.03);
      border: 1px solid var(--border-subtle);
      border-radius: var(--radius-sm);
      padding: 0.75rem 1.25rem;
    }

    .comp-doc-badge {
      display: flex;
      flex-direction: column;
    }

    .doc-label {
      font-size: 0.65rem;
      color: var(--text-muted);
      font-weight: 700;
      letter-spacing: 0.05em;
    }

    .doc-title {
      font-size: 0.95rem;
      font-weight: 600;
      color: var(--text-main);
    }

    .vs-divider {
      font-size: 0.75rem;
      font-weight: 800;
      color: #a5b4fc;
      background: rgba(99, 102, 241, 0.2);
      padding: 0.25rem 0.6rem;
      border-radius: 9999px;
    }

    .comp-summary-box {
      background: rgba(99, 102, 241, 0.08);
      border-left: 3px solid var(--primary);
      padding: 1rem 1.25rem;
      border-radius: 0 var(--radius-sm) var(--radius-sm) 0;
    }

    .comp-summary-box h4 {
      margin: 0 0 0.35rem;
      font-size: 0.9rem;
      color: #a5b4fc;
    }

    .comp-summary-box p {
      margin: 0;
      font-size: 0.85rem;
      line-height: 1.45;
      color: var(--text-main);
    }

    .benefits-grid {
      display: grid;
      grid-template-columns: 1fr 1fr;
      gap: 1rem;
    }

    .benefit-box {
      padding: 1rem;
      border-radius: var(--radius-sm);
      border: 1px solid var(--border-subtle);
    }

    .benefit-box.added {
      background: rgba(16, 185, 129, 0.06);
      border-color: rgba(16, 185, 129, 0.25);
    }

    .benefit-box.removed {
      background: rgba(244, 63, 94, 0.06);
      border-color: rgba(244, 63, 94, 0.25);
    }

    .benefit-box h5 {
      margin: 0 0 0.5rem;
      font-size: 0.85rem;
    }

    .benefit-box.added h5 {
      color: var(--accent-emerald);
    }

    .benefit-box.removed h5 {
      color: var(--accent-rose);
    }

    .benefit-box ul {
      margin: 0;
      padding: 0;
      list-style: none;
      font-size: 0.8rem;
    }

    .benefit-box li {
      margin-bottom: 0.35rem;
      display: flex;
      align-items: flex-start;
      gap: 0.4rem;
    }

    .symbol-add {
      color: var(--accent-emerald);
      font-weight: 700;
    }

    .symbol-remove {
      color: var(--accent-rose);
      font-weight: 700;
    }

    .diff-section h5 {
      margin: 0 0 0.75rem;
      font-size: 0.85rem;
      color: var(--text-secondary);
    }

    .impact-tag {
      font-size: 0.7rem;
      font-weight: 700;
      padding: 0.15rem 0.45rem;
      border-radius: 4px;
      text-transform: uppercase;
    }

    .impact-tag.positive {
      background: rgba(16, 185, 129, 0.15);
      color: var(--accent-emerald);
    }

    .impact-tag.negative {
      background: rgba(244, 63, 94, 0.15);
      color: var(--accent-rose);
    }

    .recommendation-box {
      background: rgba(255, 255, 255, 0.03);
      border: 1px solid var(--border-subtle);
      border-radius: var(--radius-sm);
      padding: 1rem 1.25rem;
    }

    .recommendation-box h5 {
      margin: 0 0 0.4rem;
      color: var(--accent-amber);
      font-size: 0.85rem;
    }

    .rec-text {
      margin: 0 0 0.5rem;
      font-size: 0.85rem;
      line-height: 1.45;
    }

    .premium-text {
      margin: 0;
      font-size: 0.8rem;
      color: var(--text-muted);
    }
  `]
})
export class LoansInsuranceComponent implements OnInit {
  loanInsuranceService = inject(LoanInsuranceService);
  auditService = inject(AuditService);
  documentService = inject(DocumentService);

  loans = signal<LoanResponse[]>([]);
  policies = signal<InsurancePolicyResponse[]>([]);
  loadingLoans = signal<boolean>(false);
  loadingInsurance = signal<boolean>(false);

  // Phase 17: Policy Comparison
  showComparisonModal = signal<boolean>(false);
  comparing = signal<boolean>(false);
  comparisonResult = signal<PolicyComparisonResponse | null>(null);
  comparisonError = signal<string | null>(null);
  insuranceDocuments = signal<DocumentResponse[]>([]);
  selectedDoc1: string = '';
  selectedDoc2: string = '';

  openComparisonModal() {
    this.showComparisonModal.set(true);
    this.comparisonError.set(null);
    if (this.insuranceDocuments().length === 0) {
      this.documentService.getDocuments(0, 50, 'INSURANCE').subscribe({
        next: (res) => {
          if (res.success && res.data) {
            this.insuranceDocuments.set(res.data.content || []);
          }
        }
      });
    }
  }

  closeComparisonModal() {
    this.showComparisonModal.set(false);
  }

  executeComparison() {
    if (!this.selectedDoc1 || !this.selectedDoc2) return;
    this.comparing.set(true);
    this.comparisonError.set(null);

    this.auditService.comparePolicies({
      documentId1: this.selectedDoc1,
      documentId2: this.selectedDoc2
    }).subscribe({
      next: (res) => {
        if (res.success && res.data) {
          this.comparisonResult.set(res.data);
        } else {
          this.comparisonError.set(res.message || 'Comparison failed');
        }
        this.comparing.set(false);
      },
      error: (err) => {
        this.comparisonError.set(err.error?.detail || 'Comparison failed. Verify document access.');
        this.comparing.set(false);
      }
    });
  }

  // Prepayment Simulator
  activeLoanForSim = signal<LoanResponse | null>(null);
  simAmount: number | null = 5000;
  simType: 'REDUCE_TENURE' | 'REDUCE_EMI' = 'REDUCE_TENURE';
  simulating = signal<boolean>(false);
  simResult = signal<PrepaymentSimulationResult | null>(null);

  // Amortization Schedule
  activeLoanForSchedule = signal<LoanResponse | null>(null);
  schedule = signal<AmortizationEntry[]>([]);
  loadingSchedule = signal<boolean>(false);

  ngOnInit() {
    this.loadData();
  }

  loadData() {
    this.loadingLoans.set(true);
    this.loanInsuranceService.getLoans().subscribe({
      next: (res) => {
        if (res.success && res.data) {
          this.loans.set(res.data);
        }
        this.loadingLoans.set(false);
      },
      error: () => this.loadingLoans.set(false)
    });

    this.loadingInsurance.set(true);
    this.loanInsuranceService.getInsurancePolicies().subscribe({
      next: (res) => {
        if (res.success && res.data) {
          this.policies.set(res.data);
        }
        this.loadingInsurance.set(false);
      },
      error: () => this.loadingInsurance.set(false)
    });
  }

  openSimulator(loan: LoanResponse) {
    this.activeLoanForSim.set(loan);
    this.simResult.set(null);
    this.simAmount = 5000;
  }

  runSimulation() {
    const loan = this.activeLoanForSim();
    if (!loan || !this.simAmount) return;

    this.simulating.set(true);
    const req: PrepaymentSimulationRequest = {
      prepaymentAmount: this.simAmount,
      prepaymentMonth: 1,
      prepaymentType: this.simType
    };

    this.loanInsuranceService.simulatePrepayment(loan.id, req).subscribe({
      next: (res) => {
        if (res.success) {
          this.simResult.set(res.data);
        }
        this.simulating.set(false);
      },
      error: () => this.simulating.set(false)
    });
  }

  viewSchedule(loan: LoanResponse) {
    this.activeLoanForSchedule.set(loan);
    this.loadingSchedule.set(true);
    this.loanInsuranceService.getAmortizationSchedule(loan.id).subscribe({
      next: (res) => {
        if (res.success && res.data) {
          this.schedule.set(res.data);
        }
        this.loadingSchedule.set(false);
      },
      error: () => this.loadingSchedule.set(false)
    });
  }

  formatCurrency(val: number): string {
    return new Intl.NumberFormat('en-US', { style: 'currency', currency: 'USD' }).format(val || 0);
  }
}
