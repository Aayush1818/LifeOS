import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { OperationsService } from '../../core/services/operations.service';
import {
  AppointmentResponse,
  BiometricRecord,
  TrackedAsset,
  TripResponse,
  WarrantyRecord
} from '../../core/models/api.models';

@Component({
  selector: 'app-life-operations',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <div class="operations-container">
      <header class="page-header">
        <div>
          <h1 class="page-title">Life Operations Management</h1>
          <p class="page-subtitle">Healthcare consultations, biometric tracking, travel itineraries, and warranties</p>
        </div>
      </header>

      <!-- Subsystem Navigation Tabs -->
      <div class="operation-tabs">
        <button
          [class.active]="activeTab() === 'HEALTHCARE'"
          (click)="activeTab.set('HEALTHCARE')"
          class="tab-btn">
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M19 14c1.49-1.46 3-3.21 3-5.5A5.5 5.5 0 0 0 16.5 3c-1.76 0-3 .5-4.5 2-1.5-1.5-2.74-2-4.5-2A5.5 5.5 0 0 0 2 8.5c0 2.3 1.5 4.05 3 5.5l7 7Z"/>
          </svg>
          <span>Healthcare & Vitals</span>
        </button>

        <button
          [class.active]="activeTab() === 'TRAVEL'"
          (click)="activeTab.set('TRAVEL')"
          class="tab-btn">
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M17.8 19.2 16 11l3.5-3.5C21 6 21.5 4 21 3c-1-.5-3 0-4.5 1.5L13 8 4.8 6.2c-.5-.1-.9.1-1.1.5l-.3.5c-.2.5-.1 1 .3 1.3L9 12l-2 3H4l-1 1 3 2 2 3 1-1v-3l3-2 3.5 5.3c.3.4.8.5 1.3.3l.5-.2c.4-.3.6-.7.5-1.2z"/>
          </svg>
          <span>Travel & Itineraries</span>
        </button>

        <button
          [class.active]="activeTab() === 'ASSETS'"
          (click)="activeTab.set('ASSETS')"
          class="tab-btn">
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M21 16V8a2 2 0 0 0-1-1.73l-7-4a2 2 0 0 0-2 0l-7 4A2 2 0 0 0 3 8v8a2 2 0 0 0 1 1.73l7 4a2 2 0 0 0 2 0l7-4A2 2 0 0 0 21 16z"/>
          </svg>
          <span>Assets & Warranties</span>
        </button>
      </div>

      <!-- Tab 1: Healthcare & Vitals -->
      @if (activeTab() === 'HEALTHCARE') {
        <div class="tab-content">
          <section class="section-block">
            <div class="section-header">
              <h2>Doctor Appointments</h2>
              <span class="subtext">Non-diagnostic consultation records</span>
            </div>

            @if (appointments().length === 0) {
              <div class="empty-state-mini glass-card">No healthcare appointments recorded.</div>
            } @else {
              <div class="cards-grid">
                @for (apt of appointments(); track apt.id) {
                  <div class="glass-card item-card">
                    <div class="item-card-top">
                      <div>
                        <h3 class="item-title">{{ apt.doctorName }}</h3>
                        <div class="item-subtitle">{{ apt.medicalSpecialty }} · {{ apt.clinicName }}</div>
                      </div>
                      <span class="badge badge-accent">{{ apt.status }}</span>
                    </div>

                    <div class="item-body">
                      <div class="schedule-time">
                        <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                          <circle cx="12" cy="12" r="10"/>
                          <polyline points="12 6 12 12 16 14"/>
                        </svg>
                        <span>{{ apt.appointmentTime | date:'medium' }}</span>
                      </div>
                      @if (apt.reasonForVisit) {
                        <p class="item-notes">{{ apt.reasonForVisit }}</p>
                      }
                    </div>
                  </div>
                }
              </div>
            }
          </section>

          <section class="section-block">
            <div class="section-header">
              <h2>Biometric Vitals History</h2>
            </div>
            @if (vitals().length === 0) {
              <div class="empty-state-mini glass-card">No biometric vitals logged.</div>
            } @else {
              <div class="glass-card table-container">
                <table class="data-table">
                  <thead>
                    <tr>
                      <th>Date</th>
                      <th>Metric Type</th>
                      <th>Value</th>
                      <th>Unit</th>
                    </tr>
                  </thead>
                  <tbody>
                    @for (v of vitals(); track v.id) {
                      <tr>
                        <td>{{ v.recordedAt | date:'mediumDate' }}</td>
                        <td><span class="badge badge-subtle">{{ v.metricType }}</span></td>
                        <td class="bold-val text-cyan">{{ v.metricValue }}</td>
                        <td class="subtext">{{ v.unit }}</td>
                      </tr>
                    }
                  </tbody>
                </table>
              </div>
            }
          </section>
        </div>
      }

      <!-- Tab 2: Travel & Itineraries -->
      @if (activeTab() === 'TRAVEL') {
        <div class="tab-content">
          <div class="section-header">
            <h2>Trips & Itineraries</h2>
            <span class="subtext">Multi-city bookings and itinerary items</span>
          </div>

          @if (trips().length === 0) {
            <div class="empty-state-mini glass-card">No upcoming or past trips planned.</div>
          } @else {
            <div class="cards-grid">
              @for (trip of trips(); track trip.id) {
                <div class="glass-card item-card">
                  <div class="item-card-top">
                    <div>
                      <h3 class="item-title">{{ trip.tripTitle }}</h3>
                      <div class="item-subtitle text-cyan">📍 {{ trip.destination }}</div>
                    </div>
                    <span class="badge badge-accent">{{ trip.status }}</span>
                  </div>

                  <div class="item-body">
                    <div class="schedule-time">
                      <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                        <rect width="18" height="18" x="3" y="4" rx="2" ry="2"/>
                        <line x1="16" y1="2" x2="16" y2="6"/>
                        <line x1="8" y1="2" x2="8" y2="6"/>
                        <line x1="3" y1="10" x2="21" y2="10"/>
                      </svg>
                      <span>{{ trip.startDate | date:'mediumDate' }} — {{ trip.endDate | date:'mediumDate' }}</span>
                    </div>
                    @if (trip.totalBudget) {
                      <div class="trip-budget">
                        Budget: {{ trip.totalBudget | currency:'USD' }}
                      </div>
                    }
                  </div>
                </div>
              }
            </div>
          }
        </div>
      }

      <!-- Tab 3: Assets & Warranties -->
      @if (activeTab() === 'ASSETS') {
        <div class="tab-content">
          <section class="section-block">
            <div class="section-header">
              <h2>Tracked Assets & Devices</h2>
            </div>
            @if (assets().length === 0) {
              <div class="empty-state-mini glass-card">No physical assets cataloged.</div>
            } @else {
              <div class="cards-grid">
                @for (asset of assets(); track asset.id) {
                  <div class="glass-card item-card">
                    <div class="item-card-top">
                      <div>
                        <h3 class="item-title">{{ asset.assetName }}</h3>
                        <div class="item-subtitle">{{ asset.category }}</div>
                      </div>
                      <span class="badge badge-subtle">{{ asset.status }}</span>
                    </div>

                    <div class="item-body">
                      <div class="spec-row">
                        <span class="spec-label">Purchase Price:</span>
                        <span class="bold-val">{{ asset.purchasePrice | currency:'USD' }}</span>
                      </div>
                      <div class="spec-row">
                        <span class="spec-label">Purchased:</span>
                        <span>{{ asset.purchaseDate | date:'mediumDate' }}</span>
                      </div>
                      @if (asset.serialNumber) {
                        <div class="spec-row">
                          <span class="spec-label">Serial:</span>
                          <span class="mono">{{ asset.serialNumber }}</span>
                        </div>
                      }
                    </div>
                  </div>
                }
              </div>
            }
          </section>

          <section class="section-block">
            <div class="section-header">
              <h2>Warranties & Protection Plans</h2>
            </div>
            @if (warranties().length === 0) {
              <div class="empty-state-mini glass-card">No active warranty policies tracked.</div>
            } @else {
              <div class="cards-grid">
                @for (w of warranties(); track w.id) {
                  <div class="glass-card item-card">
                    <div class="item-card-top">
                      <div>
                        <h3 class="item-title">{{ w.assetName || 'Asset Warranty' }}</h3>
                        <div class="item-subtitle">Provider: {{ w.provider }}</div>
                      </div>
                      <span class="badge badge-accent">{{ w.status }}</span>
                    </div>

                    <div class="item-body">
                      <div class="spec-row">
                        <span class="spec-label">Duration:</span>
                        <span>{{ w.durationMonths }} months</span>
                      </div>
                      <div class="spec-row">
                        <span class="spec-label">Expires At:</span>
                        <span class="text-amber">{{ w.expiryDate | date:'mediumDate' }}</span>
                      </div>
                    </div>
                  </div>
                }
              </div>
            }
          </section>
        </div>
      }
    </div>
  `,
  styles: [`
    .operations-container {
      padding: 2rem;
      max-width: 1400px;
      margin: 0 auto;
      height: 100%;
      overflow-y: auto;
    }

    .page-header {
      margin-bottom: 2rem;
    }

    .operation-tabs {
      display: flex;
      gap: 0.75rem;
      margin-bottom: 2rem;
      border-bottom: 1px solid var(--border-subtle);
      padding-bottom: 1rem;
    }

    .tab-btn {
      display: flex;
      align-items: center;
      gap: 0.5rem;
      background: var(--bg-surface);
      border: 1px solid var(--border-subtle);
      color: var(--text-secondary);
      padding: 0.65rem 1.25rem;
      border-radius: var(--radius-md);
      font-size: 0.9rem;
      font-weight: 500;
      cursor: pointer;
      transition: all var(--transition-fast);
    }

    .tab-btn:hover, .tab-btn.active {
      background: rgba(99, 102, 241, 0.15);
      color: var(--primary);
      border-color: var(--primary);
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

    .cards-grid {
      display: grid;
      grid-template-columns: repeat(auto-fit, minmax(320px, 1fr));
      gap: 1.5rem;
    }

    .item-card {
      padding: 1.5rem;
    }

    .item-card-top {
      display: flex;
      justify-content: space-between;
      align-items: flex-start;
      margin-bottom: 1rem;
    }

    .item-title {
      font-size: 1.1rem;
      margin: 0 0 0.25rem;
    }

    .item-subtitle {
      font-size: 0.8rem;
      color: var(--text-muted);
    }

    .schedule-time {
      display: flex;
      align-items: center;
      gap: 0.4rem;
      font-size: 0.85rem;
      color: var(--text-secondary);
      margin-bottom: 0.5rem;
    }

    .item-notes {
      font-size: 0.85rem;
      color: var(--text-secondary);
      margin: 0.5rem 0 0;
    }

    .trip-budget {
      font-size: 0.85rem;
      font-weight: 600;
      color: var(--accent-emerald);
      margin-top: 0.5rem;
    }

    .spec-row {
      display: flex;
      justify-content: space-between;
      font-size: 0.85rem;
      padding: 0.35rem 0;
      border-bottom: 1px solid var(--border-subtle);
    }

    .spec-label {
      color: var(--text-muted);
    }

    .bold-val {
      font-weight: 600;
      color: var(--text-main);
    }

    .text-amber { color: var(--accent-amber); }
    .text-cyan { color: var(--accent-cyan); }

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
      color: var(--text-muted);
      text-transform: uppercase;
    }

    .empty-state-mini {
      padding: 2rem;
      text-align: center;
      color: var(--text-muted);
      font-size: 0.9rem;
    }
  `]
})
export class LifeOperationsComponent implements OnInit {
  operationsService = inject(OperationsService);

  activeTab = signal<'HEALTHCARE' | 'TRAVEL' | 'ASSETS'>('HEALTHCARE');

  appointments = signal<AppointmentResponse[]>([]);
  vitals = signal<BiometricRecord[]>([]);
  trips = signal<TripResponse[]>([]);
  assets = signal<TrackedAsset[]>([]);
  warranties = signal<WarrantyRecord[]>([]);

  ngOnInit() {
    this.loadOperationsData();
  }

  loadOperationsData() {
    this.operationsService.getAppointments().subscribe({
      next: (res) => { if (res.success && res.data) this.appointments.set(res.data); }
    });

    this.operationsService.getVitals().subscribe({
      next: (res) => { if (res.success && res.data) this.vitals.set(res.data); }
    });

    this.operationsService.getTrips().subscribe({
      next: (res) => { if (res.success && res.data) this.trips.set(res.data); }
    });

    this.operationsService.getAssets().subscribe({
      next: (res) => { if (res.success && res.data) this.assets.set(res.data); }
    });

    this.operationsService.getWarranties().subscribe({
      next: (res) => { if (res.success && res.data) this.warranties.set(res.data); }
    });
  }
}
