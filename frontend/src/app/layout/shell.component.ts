import { Component, OnInit, inject, signal } from '@angular/core';
import { Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { CommonModule } from '@angular/common';
import { AuthService } from '../core/services/auth.service';
import { AssistantService } from '../core/services/assistant.service';
import { NotificationService } from '../core/services/notification.service';
import { AuditService } from '../core/services/audit.service';
import { SystemService } from '../core/services/system.service';
import { AuditLog, NotificationItem, SystemHealthResponse, SystemMetricsResponse } from '../core/models/api.models';

@Component({
  selector: 'app-shell',
  standalone: true,
  imports: [CommonModule, RouterOutlet, RouterLink, RouterLinkActive],
  template: `
    <div class="shell-layout" (click)="closeNotifications()">
      <!-- Sidebar -->
      <aside class="sidebar glass-panel" (click)="$event.stopPropagation()">
        <div class="sidebar-header">
          <div class="brand">
            <div class="brand-icon">
              <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <circle cx="12" cy="12" r="10"/>
                <path d="M12 2a14.5 14.5 0 0 0 0 20 14.5 14.5 0 0 0 0-20"/>
                <path d="M2 12h20"/>
              </svg>
            </div>
            <div class="brand-name">
              <span class="gradient-text">LifeOS</span>
              <span class="brand-badge">PRO</span>
            </div>
          </div>

          <div class="header-right-actions">
            <!-- System Health Status Pill (Phase 18) -->
            <button class="btn-system-status" (click)="openSystemDiagModal()" [title]="'System Health: ' + (systemHealth()?.status || 'UP')">
              <span class="status-pulse-dot" [ngClass]="getSystemStatusDotClass()"></span>
              <span class="status-label">{{ systemHealth()?.status || 'UP' }}</span>
            </button>

            <!-- Notification Bell Trigger -->
            <div class="notif-trigger-wrapper">
              <button class="btn-notif-bell" (click)="toggleNotifications($event)" title="Notifications">
              <svg width="19" height="19" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <path d="M18 8A6 6 0 0 0 6 8c0 7-3 9-3 9h18s-3-2-3-9"/>
                <path d="M13.73 21a2 2 0 0 1-3.46 0"/>
              </svg>
              @if (unreadCount() > 0) {
                <span class="notif-count-badge">{{ unreadCount() > 99 ? '99+' : unreadCount() }}</span>
              }
            </button>

            <!-- Notifications Flyout -->
            @if (showNotifications()) {
              <div class="notifications-flyout glass-panel" (click)="$event.stopPropagation()">
                <div class="flyout-header">
                  <div class="flyout-title">
                    <span>Notifications</span>
                    @if (unreadCount() > 0) {
                      <span class="unread-pill">{{ unreadCount() }} new</span>
                    }
                  </div>
                  <div class="flyout-actions">
                    @if (unreadCount() > 0) {
                      <button class="btn-mark-all" (click)="markAllAsRead()">Mark all read</button>
                    }
                  </div>
                </div>

                <div class="flyout-content">
                  @if (notificationsLoading()) {
                    <div class="flyout-loading">Loading alerts...</div>
                  } @else if (notifications().length === 0) {
                    <div class="flyout-empty">
                      <svg width="32" height="32" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
                        <path d="M18 8A6 6 0 0 0 6 8c0 7-3 9-3 9h18s-3-2-3-9"/>
                        <path d="M13.73 21a2 2 0 0 1-3.46 0"/>
                      </svg>
                      <p>All caught up! No notifications.</p>
                    </div>
                  } @else {
                    <div class="notif-items-list">
                      @for (item of notifications(); track item.id) {
                        <div class="notif-item" [class.unread]="!item.read" (click)="handleNotificationClick(item)">
                          <div class="notif-icon-col">
                            <span class="notif-type-dot" [ngClass]="getNotifDotClass(item.notificationType)"></span>
                          </div>
                          <div class="notif-details">
                            <div class="notif-item-title">{{ item.title }}</div>
                            <div class="notif-item-msg">{{ item.message }}</div>
                            <div class="notif-item-time">{{ item.createdAt | date:'short' }}</div>
                          </div>
                          <button class="btn-delete-notif" (click)="deleteNotification(item.id, $event)" title="Dismiss">
                            &times;
                          </button>
                        </div>
                      }
                    </div>
                  }
                </div>

                <div class="flyout-footer">
                  <a routerLink="/reminders" (click)="showNotifications.set(false)" class="btn-view-all">
                    View Reminders & Tasks &rarr;
                  </a>
                </div>
              </div>
            }
          </div>
          </div>
        </div>

        <nav class="sidebar-nav">
          <a routerLink="/dashboard" routerLinkActive="active" class="nav-item">
            <svg class="nav-icon" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <rect width="7" height="9" x="3" y="3" rx="1"/>
              <rect width="7" height="5" x="14" y="3" rx="1"/>
              <rect width="7" height="9" x="14" y="12" rx="1"/>
              <rect width="7" height="5" x="3" y="16" rx="1"/>
            </svg>
            <span>Dashboard</span>
          </a>

          <a routerLink="/assistant" routerLinkActive="active" class="nav-item">
            <svg class="nav-icon" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path d="m12 3-1.9 5.8a2 2 0 0 1-1.3 1.3L3 12l5.8 1.9a2 2 0 0 1 1.3 1.3L12 21l1.9-5.8a2 2 0 0 1 1.3-1.3L21 12l-5.8-1.9a2 2 0 0 1-1.3-1.3Z"/>
            </svg>
            <span>AI Assistant</span>
            @if (assistantService.pendingActions().length > 0) {
              <span class="pending-badge">{{ assistantService.pendingActions().length }}</span>
            }
          </a>

          <a routerLink="/reminders" routerLinkActive="active" class="nav-item">
            <svg class="nav-icon" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M18 8A6 6 0 0 0 6 8c0 7-3 9-3 9h18s-3-2-3-9"/>
              <path d="M13.73 21a2 2 0 0 1-3.46 0"/>
            </svg>
            <span>Reminders</span>
            @if (unreadCount() > 0) {
              <span class="notif-badge-pill">{{ unreadCount() }}</span>
            }
          </a>

          <a routerLink="/finance" routerLinkActive="active" class="nav-item">
            <svg class="nav-icon" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <line x1="12" y1="1" x2="12" y2="23"/>
              <path d="M17 5H9.5a3.5 3.5 0 0 0 0 7h5a3.5 3.5 0 0 1 0 7H6"/>
            </svg>
            <span>Finance & Budgets</span>
          </a>

          <a routerLink="/loans-insurance" routerLinkActive="active" class="nav-item">
            <svg class="nav-icon" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <rect width="20" height="14" x="2" y="5" rx="2"/>
              <line x1="2" x2="22" y1="10" y2="10"/>
            </svg>
            <span>Loans & Insurance</span>
          </a>

          <a routerLink="/life-operations" routerLinkActive="active" class="nav-item">
            <svg class="nav-icon" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M19 14c1.49-1.46 3-3.21 3-5.5A5.5 5.5 0 0 0 16.5 3c-1.76 0-3 .5-4.5 2-1.5-1.5-2.74-2-4.5-2A5.5 5.5 0 0 0 2 8.5c0 2.3 1.5 4.05 3 5.5l7 7Z"/>
            </svg>
            <span>Life Operations</span>
          </a>

          <a routerLink="/documents" routerLinkActive="active" class="nav-item">
            <svg class="nav-icon" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M15 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V7Z"/>
              <path d="M14 2v4a2 2 0 0 0 2 2h4"/>
              <path d="M10 9H8"/>
              <path d="M16 13H8"/>
              <path d="M16 17H8"/>
            </svg>
            <span>Documents & RAG</span>
          </a>

          <a routerLink="/search" routerLinkActive="active" class="nav-item">
            <svg class="nav-icon" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <circle cx="11" cy="11" r="8"/>
              <path d="m21 21-4.3-4.3"/>
            </svg>
            <span>Unified Search</span>
          </a>
        </nav>

        <div class="sidebar-footer">
          <div class="user-profile">
            <div class="user-avatar">
              {{ getUserInitial() }}
            </div>
            <div class="user-info">
              <div class="user-name">{{ authService.currentUser()?.firstName }} {{ authService.currentUser()?.lastName }}</div>
              <div class="user-email">{{ authService.currentUser()?.email }}</div>
            </div>
            <button (click)="logout()" class="btn-logout" title="Sign out">
              <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <path d="M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4"/>
                <polyline points="16 17 21 12 16 7"/>
                <line x1="21" x2="9" y1="12" y2="12"/>
              </svg>
            </button>
          </div>

          <!-- Phase 17: Secondary Utilities (Audit & GDPR Export) -->
          <div class="sidebar-secondary-actions">
            <button class="btn-side-action" (click)="openAuditModal()" title="View Security Audit Trail">
              <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z"/>
              </svg>
              <span>Security Logs</span>
            </button>
            <button class="btn-side-action" (click)="exportUserData()" [disabled]="exporting()" title="Download GDPR Data Archive">
              <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"/>
                <polyline points="7 10 12 15 17 10"/>
                <line x1="12" y1="15" x2="12" y2="3"/>
              </svg>
              <span>{{ exporting() ? 'Exporting...' : 'GDPR Export' }}</span>
            </button>
          </div>
          @if (exportSuccessMsg()) {
            <div class="export-success-toast">{{ exportSuccessMsg() }}</div>
          }
        </div>
      </aside>

      <!-- Main Content Area -->
      <main class="main-viewport">
        <router-outlet></router-outlet>
      </main>

      <!-- Phase 17: Security Audit Logs Modal -->
      @if (showAuditModal()) {
        <div class="modal-backdrop" (click)="closeAuditModal()">
          <div class="modal-card audit-modal glass-card" (click)="$event.stopPropagation()">
            <div class="modal-header">
              <div>
                <h3>Security & Activity Audit Logs</h3>
                <div class="subtext">Immutable log of authentication, HITL actions, exports & policy comparisons</div>
              </div>
              <button (click)="closeAuditModal()" class="btn-icon">
                <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <line x1="18" y1="6" x2="6" y2="18"/>
                  <line x1="6" y1="6" x2="18" y2="18"/>
                </svg>
              </button>
            </div>

            <div class="audit-filter-bar">
              <label class="filter-label">Filter Event:</label>
              <select class="form-select audit-select" [value]="auditFilter()" (change)="onFilterChange($event)">
                <option value="">All Security Events</option>
                <option value="AUTH_LOGIN_SUCCESS">Login Success</option>
                <option value="AUTH_LOGIN_FAILURE">Login Failure</option>
                <option value="AUTH_LOGOUT">Logout</option>
                <option value="DATA_EXPORT_REQUESTED">GDPR Data Export</option>
                <option value="ACTION_HITL_CONFIRMED">HITL Action Confirmed</option>
                <option value="ACTION_HITL_REJECTED">HITL Action Rejected</option>
                <option value="POLICY_COMPARISON_EXECUTED">Policy Comparison</option>
              </select>
            </div>

            <div class="audit-modal-body">
              @if (auditLoading()) {
                <div class="loading-state">Loading audit events...</div>
              } @else if (auditLogs().length === 0) {
                <div class="empty-state-mini">No audit records found for the selected filter.</div>
              } @else {
                <table class="data-table audit-table">
                  <thead>
                    <tr>
                      <th>Event Type</th>
                      <th>Outcome</th>
                      <th>Timestamp</th>
                      <th>IP Address</th>
                      <th>Details</th>
                    </tr>
                  </thead>
                  <tbody>
                    @for (log of auditLogs(); track log.id) {
                      <tr>
                        <td>
                          <span class="event-type-badge">{{ log.eventType }}</span>
                        </td>
                        <td>
                          <span class="outcome-pill" [class.success]="log.actionOutcome === 'SUCCESS'" [class.failure]="log.actionOutcome === 'FAILURE'">
                            {{ log.actionOutcome }}
                          </span>
                        </td>
                        <td class="text-muted">{{ log.createdAt | date:'short' }}</td>
                        <td class="text-muted">{{ log.ipAddress || '127.0.0.1' }}</td>
                        <td class="details-col">
                          <code>{{ log.details }}</code>
                        </td>
                      </tr>
                    }
                  </tbody>
                </table>
              }
            </div>
          </div>
        </div>
      }

      <!-- Phase 18: System Health & Diagnostics Modal -->
      @if (showSystemDiagModal()) {
        <div class="modal-backdrop" (click)="closeSystemDiagModal()">
          <div class="modal-card diag-modal glass-card" (click)="$event.stopPropagation()">
            <div class="modal-header">
              <div>
                <div class="diag-title-row">
                  <h3>System Diagnostics & Observability</h3>
                  <span class="diag-status-badge" [ngClass]="getSystemStatusBadgeClass()">
                    {{ systemHealth()?.status || 'UP' }}
                  </span>
                </div>
                <div class="subtext">
                  Uptime: {{ formatUptime(systemHealth()?.uptimeSeconds || 0) }} &bull; Live engine probes & platform metrics
                </div>
              </div>
              <div class="diag-header-actions">
                <button (click)="refreshDiagnostics()" class="btn-icon" [disabled]="diagLoading()" title="Refresh Probes">
                  <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                    <polyline points="23 4 23 10 17 10"/>
                    <polyline points="1 20 1 14 7 14"/>
                    <path d="M3.51 9a9 9 0 0 1 14.85-3.36L23 10M1 14l4.64 4.36A9 9 0 0 0 20.49 15"/>
                  </svg>
                </button>
                <button (click)="closeSystemDiagModal()" class="btn-icon">
                  <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                    <line x1="18" y1="6" x2="6" y2="18"/>
                    <line x1="6" y1="6" x2="18" y2="18"/>
                  </svg>
                </button>
              </div>
            </div>

            <div class="diag-modal-body">
              <!-- Live Probes Grid -->
              <div class="diag-section-title">Component Probes</div>
              <div class="probes-grid">
                @for (entry of getComponentEntries(); track entry.key) {
                  <div class="probe-card glass-card">
                    <div class="probe-header">
                      <div class="probe-name">
                        <span class="probe-dot" [ngClass]="getProbeDotClass(entry.value.status)"></span>
                        <span>{{ formatComponentName(entry.key) }}</span>
                      </div>
                      <span class="probe-latency" *ngIf="entry.value.latencyMs !== undefined && entry.value.latencyMs !== null">
                        {{ entry.value.latencyMs }}ms
                      </span>
                    </div>
                    <div class="probe-details">{{ entry.value.details }}</div>
                  </div>
                }
              </div>

              <!-- Platform Metrics Grid -->
              <div class="diag-section-title">Platform & Storage Metrics</div>
              <div class="metrics-stat-grid">
                <div class="stat-card glass-card">
                  <div class="stat-label">Total Users</div>
                  <div class="stat-val">{{ systemMetrics()?.totalUsers ?? '-' }}</div>
                </div>
                <div class="stat-card glass-card">
                  <div class="stat-label">Documents</div>
                  <div class="stat-val">{{ systemMetrics()?.totalDocuments ?? '-' }}</div>
                </div>
                <div class="stat-card glass-card">
                  <div class="stat-label">Transactions</div>
                  <div class="stat-val">{{ systemMetrics()?.totalTransactions ?? '-' }}</div>
                </div>
                <div class="stat-card glass-card">
                  <div class="stat-label">Loans & Policies</div>
                  <div class="stat-val">{{ (systemMetrics()?.totalLoans || 0) + (systemMetrics()?.totalPolicies || 0) }}</div>
                </div>
                <div class="stat-card glass-card">
                  <div class="stat-label">Free Storage</div>
                  <div class="stat-val">{{ formatBytes(systemMetrics()?.diskFreeBytes || 0) }}</div>
                </div>
                <div class="stat-card glass-card">
                  <div class="stat-label">JVM Memory</div>
                  <div class="stat-val">{{ formatBytes(systemMetrics()?.jvmUsedMemoryBytes || 0) }}</div>
                </div>
                <div class="stat-card glass-card">
                  <div class="stat-label">CPU Cores</div>
                  <div class="stat-val">{{ systemMetrics()?.jvmAvailableProcessors ?? '-' }}</div>
                </div>
              </div>
            </div>
          </div>
        </div>
      }
    </div>
  `,
  styles: [`
    .shell-layout {
      display: flex;
      height: 100vh;
      overflow: hidden;
      background: var(--bg-primary);
    }

    .sidebar {
      width: 270px;
      min-width: 270px;
      display: flex;
      flex-direction: column;
      border-right: 1px solid var(--border-subtle);
      background: rgba(11, 16, 30, 0.95);
      z-index: 20;
      position: relative;
    }

    .sidebar-header {
      padding: 1.25rem 1.25rem;
      border-bottom: 1px solid var(--border-subtle);
      display: flex;
      align-items: center;
      justify-content: space-between;
    }

    .brand {
      display: flex;
      align-items: center;
      gap: 0.75rem;
    }

    .brand-icon {
      width: 36px;
      height: 36px;
      border-radius: var(--radius-sm);
      background: var(--gradient-brand);
      color: #fff;
      display: flex;
      align-items: center;
      justify-content: center;
      box-shadow: 0 4px 12px var(--primary-glow);
    }

    .brand-name {
      font-size: 1.35rem;
      font-weight: 800;
      letter-spacing: -0.03em;
      display: flex;
      align-items: center;
      gap: 0.5rem;
    }

    .brand-badge {
      font-size: 0.625rem;
      padding: 0.15rem 0.4rem;
      border-radius: 4px;
      background: rgba(99, 102, 241, 0.2);
      color: #a5b4fc;
      border: 1px solid rgba(99, 102, 241, 0.4);
      font-weight: 700;
    }

    /* Notification Bell & Dropdown */
    .notif-trigger-wrapper {
      position: relative;
    }

    .btn-notif-bell {
      position: relative;
      background: rgba(255, 255, 255, 0.05);
      border: 1px solid var(--border-subtle);
      border-radius: var(--radius-sm);
      width: 36px;
      height: 36px;
      display: flex;
      align-items: center;
      justify-content: center;
      color: var(--text-secondary);
      cursor: pointer;
      transition: all var(--transition-fast);
    }

    .btn-notif-bell:hover {
      background: rgba(255, 255, 255, 0.1);
      color: var(--text-main);
    }

    .notif-count-badge {
      position: absolute;
      top: -4px;
      right: -4px;
      background: var(--accent-rose);
      color: #ffffff;
      font-size: 0.65rem;
      font-weight: 800;
      padding: 0.1rem 0.35rem;
      border-radius: 9999px;
      border: 2px solid #0b101e;
      animation: pulseGlow 2s infinite ease-in-out;
    }

    .notifications-flyout {
      position: absolute;
      top: 45px;
      left: 0;
      width: 340px;
      border-radius: var(--radius-md);
      box-shadow: 0 16px 36px rgba(0, 0, 0, 0.5);
      z-index: 100;
      overflow: hidden;
      animation: flyoutIn 0.2s cubic-bezier(0.16, 1, 0.3, 1);
    }

    @keyframes flyoutIn {
      from { opacity: 0; transform: translateY(-8px); }
      to { opacity: 1; transform: translateY(0); }
    }

    .flyout-header {
      display: flex;
      align-items: center;
      justify-content: space-between;
      padding: 0.85rem 1rem;
      border-bottom: 1px solid var(--border-subtle);
    }

    .flyout-title {
      display: flex;
      align-items: center;
      gap: 0.5rem;
      font-size: 0.9rem;
      font-weight: 700;
      color: var(--text-main);
    }

    .unread-pill {
      font-size: 0.65rem;
      font-weight: 700;
      padding: 0.1rem 0.4rem;
      border-radius: 9999px;
      background: rgba(244, 63, 94, 0.2);
      color: #fda4af;
      border: 1px solid rgba(244, 63, 94, 0.4);
    }

    .btn-mark-all {
      background: transparent;
      border: none;
      color: var(--accent-indigo);
      font-size: 0.75rem;
      font-weight: 600;
      cursor: pointer;
    }

    .btn-mark-all:hover {
      text-decoration: underline;
    }

    .flyout-content {
      max-height: 380px;
      overflow-y: auto;
    }

    .flyout-loading, .flyout-empty {
      padding: 2.5rem 1rem;
      text-align: center;
      color: var(--text-muted);
      font-size: 0.85rem;
      display: flex;
      flex-direction: column;
      align-items: center;
      gap: 0.5rem;
    }

    .notif-items-list {
      display: flex;
      flex-direction: column;
    }

    .notif-item {
      display: flex;
      align-items: flex-start;
      gap: 0.75rem;
      padding: 0.75rem 1rem;
      border-bottom: 1px solid rgba(255, 255, 255, 0.04);
      cursor: pointer;
      transition: background var(--transition-fast);
      position: relative;
    }

    .notif-item:hover {
      background: rgba(255, 255, 255, 0.04);
    }

    .notif-item.unread {
      background: rgba(99, 102, 241, 0.06);
    }

    .notif-icon-col {
      padding-top: 0.25rem;
    }

    .notif-type-dot {
      display: inline-block;
      width: 8px;
      height: 8px;
      border-radius: 50%;
      background: var(--text-muted);
    }

    .dot-reminder { background: var(--accent-indigo); box-shadow: 0 0 6px var(--primary-glow); }
    .dot-loan { background: var(--accent-amber); box-shadow: 0 0 6px rgba(245, 158, 11, 0.4); }
    .dot-insurance { background: var(--accent-cyan); box-shadow: 0 0 6px rgba(6, 182, 212, 0.4); }
    .dot-warranty { background: var(--accent-emerald); box-shadow: 0 0 6px rgba(16, 185, 129, 0.4); }
    .dot-appointment { background: var(--accent-rose); box-shadow: 0 0 6px rgba(244, 63, 94, 0.4); }

    .notif-details {
      flex: 1;
      min-width: 0;
    }

    .notif-item-title {
      font-size: 0.825rem;
      font-weight: 600;
      color: var(--text-main);
      margin-bottom: 0.15rem;
    }

    .notif-item-msg {
      font-size: 0.75rem;
      color: var(--text-secondary);
      line-height: 1.35;
      overflow: hidden;
      text-overflow: ellipsis;
      display: -webkit-box;
      -webkit-line-clamp: 2;
      -webkit-box-orient: vertical;
    }

    .notif-item-time {
      font-size: 0.675rem;
      color: var(--text-muted);
      margin-top: 0.25rem;
    }

    .btn-delete-notif {
      background: transparent;
      border: none;
      color: var(--text-muted);
      font-size: 1.1rem;
      cursor: pointer;
      opacity: 0.6;
      padding: 0 0.25rem;
    }

    .btn-delete-notif:hover {
      opacity: 1;
      color: var(--accent-rose);
    }

    .flyout-footer {
      padding: 0.65rem 1rem;
      border-top: 1px solid var(--border-subtle);
      text-align: center;
      background: rgba(0, 0, 0, 0.2);
    }

    .btn-view-all {
      font-size: 0.8rem;
      color: var(--accent-indigo);
      font-weight: 600;
      text-decoration: none;
    }

    .btn-view-all:hover {
      text-decoration: underline;
    }

    /* Navigation */
    .sidebar-nav {
      flex: 1;
      padding: 1rem 0.75rem;
      display: flex;
      flex-direction: column;
      gap: 0.35rem;
      overflow-y: auto;
    }

    .nav-item {
      display: flex;
      align-items: center;
      gap: 0.75rem;
      padding: 0.65rem 0.85rem;
      border-radius: var(--radius-sm);
      color: var(--text-secondary);
      font-family: 'Outfit', sans-serif;
      font-size: 0.9rem;
      font-weight: 500;
      text-decoration: none;
      transition: all var(--transition-fast);
      position: relative;
    }

    .nav-item:hover {
      color: var(--text-main);
      background: rgba(255, 255, 255, 0.04);
    }

    .nav-item.active {
      color: #ffffff;
      background: rgba(99, 102, 241, 0.15);
      border: 1px solid rgba(99, 102, 241, 0.3);
      box-shadow: 0 2px 8px rgba(99, 102, 241, 0.2);
    }

    .nav-icon {
      stroke-width: 1.8;
      transition: transform var(--transition-fast);
    }

    .nav-item:hover .nav-icon {
      transform: scale(1.1);
    }

    .pending-badge {
      margin-left: auto;
      background: var(--accent-amber);
      color: #000;
      font-size: 0.7rem;
      font-weight: 700;
      padding: 0.1rem 0.45rem;
      border-radius: 9999px;
      animation: pulseGlow 2s infinite ease-in-out;
    }

    .notif-badge-pill {
      margin-left: auto;
      background: rgba(99, 102, 241, 0.25);
      color: #a5b4fc;
      border: 1px solid rgba(99, 102, 241, 0.5);
      font-size: 0.7rem;
      font-weight: 700;
      padding: 0.1rem 0.45rem;
      border-radius: 9999px;
    }

    .sidebar-footer {
      padding: 1rem 1.25rem;
      border-top: 1px solid var(--border-subtle);
    }

    .user-profile {
      display: flex;
      align-items: center;
      gap: 0.75rem;
    }

    .user-avatar {
      width: 36px;
      height: 36px;
      border-radius: 50%;
      background: var(--gradient-cyan);
      color: #ffffff;
      font-weight: 700;
      display: flex;
      align-items: center;
      justify-content: center;
      font-size: 0.875rem;
      flex-shrink: 0;
    }

    .user-info {
      flex: 1;
      overflow: hidden;
    }

    .user-name {
      font-size: 0.85rem;
      font-weight: 600;
      white-space: nowrap;
      overflow: hidden;
      text-overflow: ellipsis;
      color: var(--text-main);
    }

    .user-email {
      font-size: 0.725rem;
      color: var(--text-muted);
      white-space: nowrap;
      overflow: hidden;
      text-overflow: ellipsis;
    }

    .btn-logout {
      background: transparent;
      border: none;
      color: var(--text-muted);
      cursor: pointer;
      padding: 0.35rem;
      border-radius: 4px;
      display: flex;
      align-items: center;
      justify-content: center;
      transition: color var(--transition-fast), background var(--transition-fast);
    }

    .btn-logout:hover {
      color: var(--accent-rose);
      background: rgba(244, 63, 94, 0.1);
    }

    .main-viewport {
      flex: 1;
      overflow-y: auto;
      background: radial-gradient(circle at 50% 0%, rgba(99, 102, 241, 0.08) 0%, transparent 60%),
                  var(--bg-primary);
    }

    /* Phase 17: Secondary Utilities & Audit Modal */
    .sidebar-secondary-actions {
      display: flex;
      gap: 0.5rem;
      margin-top: 0.75rem;
      padding-top: 0.75rem;
      border-top: 1px solid var(--border-subtle);
    }

    .btn-side-action {
      flex: 1;
      display: flex;
      align-items: center;
      justify-content: center;
      gap: 0.4rem;
      background: rgba(255, 255, 255, 0.04);
      border: 1px solid var(--border-subtle);
      border-radius: var(--radius-sm);
      color: var(--text-secondary);
      font-size: 0.75rem;
      font-weight: 500;
      padding: 0.45rem 0.25rem;
      cursor: pointer;
      transition: all var(--transition-fast);
    }

    .btn-side-action:hover:not(:disabled) {
      background: rgba(99, 102, 241, 0.12);
      border-color: rgba(99, 102, 241, 0.3);
      color: #ffffff;
    }

    .btn-side-action:disabled {
      opacity: 0.6;
      cursor: not-allowed;
    }

    .export-success-toast {
      margin-top: 0.5rem;
      padding: 0.4rem 0.6rem;
      background: rgba(16, 185, 129, 0.15);
      border: 1px solid rgba(16, 185, 129, 0.3);
      color: var(--accent-emerald);
      border-radius: var(--radius-sm);
      font-size: 0.75rem;
      text-align: center;
    }

    .audit-modal {
      width: 900px;
      max-width: 95vw;
      max-height: 85vh;
      display: flex;
      flex-direction: column;
      background: rgba(15, 23, 42, 0.95);
      border: 1px solid var(--border-subtle);
      box-shadow: 0 20px 40px rgba(0, 0, 0, 0.6);
      border-radius: var(--radius-md);
    }

    .audit-filter-bar {
      padding: 1rem 1.5rem;
      border-bottom: 1px solid var(--border-subtle);
      display: flex;
      align-items: center;
      gap: 0.75rem;
      background: rgba(255, 255, 255, 0.02);
    }

    .audit-select {
      max-width: 250px;
      font-size: 0.85rem;
      padding: 0.35rem 0.75rem;
    }

    .audit-modal-body {
      flex: 1;
      overflow-y: auto;
      padding: 1rem 1.5rem;
    }

    .audit-table {
      width: 100%;
      border-collapse: collapse;
      font-size: 0.85rem;
    }

    .audit-table th, .audit-table td {
      padding: 0.75rem 0.85rem;
      border-bottom: 1px solid var(--border-subtle);
    }

    .audit-table th {
      font-size: 0.75rem;
      text-transform: uppercase;
      color: var(--text-muted);
    }

    .event-type-badge {
      font-family: monospace;
      font-size: 0.75rem;
      background: rgba(99, 102, 241, 0.15);
      color: #a5b4fc;
      padding: 0.2rem 0.5rem;
      border-radius: 4px;
    }

    .outcome-pill {
      font-size: 0.7rem;
      font-weight: 700;
      padding: 0.15rem 0.5rem;
      border-radius: 9999px;
    }

    .outcome-pill.success {
      background: rgba(16, 185, 129, 0.15);
      color: var(--accent-emerald);
    }

    .outcome-pill.failure {
      background: rgba(244, 63, 94, 0.15);
      color: var(--accent-rose);
    }

    .details-col {
      max-width: 280px;
      overflow: hidden;
      text-overflow: ellipsis;
      white-space: nowrap;
    }

    .details-col code {
      font-size: 0.75rem;
      color: var(--text-muted);
    }

    /* Phase 18: System Status Pill & Diagnostics Modal */
    .header-right-actions {
      display: flex;
      align-items: center;
      gap: 0.5rem;
    }

    .btn-system-status {
      display: flex;
      align-items: center;
      gap: 0.35rem;
      background: rgba(255, 255, 255, 0.05);
      border: 1px solid var(--border-subtle);
      border-radius: var(--radius-full);
      padding: 0.25rem 0.55rem;
      cursor: pointer;
      color: var(--text-secondary);
      font-size: 0.7rem;
      font-weight: 700;
      transition: all var(--transition-fast);
    }

    .btn-system-status:hover {
      background: rgba(255, 255, 255, 0.1);
      color: var(--text-main);
    }

    .status-pulse-dot {
      width: 7px;
      height: 7px;
      border-radius: 50%;
    }

    .dot-up {
      background: var(--accent-emerald);
      box-shadow: 0 0 8px rgba(16, 185, 129, 0.8);
      animation: pulseGlow 2s infinite ease-in-out;
    }

    .dot-degraded {
      background: var(--accent-amber);
      box-shadow: 0 0 8px rgba(245, 158, 11, 0.8);
    }

    .dot-down {
      background: var(--accent-rose);
      box-shadow: 0 0 8px rgba(244, 63, 94, 0.8);
    }

    .diag-modal {
      width: 820px;
      max-width: 95vw;
      max-height: 85vh;
      display: flex;
      flex-direction: column;
      background: rgba(15, 23, 42, 0.96);
      border: 1px solid var(--border-subtle);
      border-radius: var(--radius-md);
      box-shadow: 0 24px 48px rgba(0, 0, 0, 0.7);
    }

    .diag-title-row {
      display: flex;
      align-items: center;
      gap: 0.75rem;
      margin-bottom: 0.25rem;
    }

    .diag-status-badge {
      font-size: 0.65rem;
      font-weight: 800;
      padding: 0.15rem 0.45rem;
      border-radius: 4px;
      text-transform: uppercase;
    }

    .diag-status-up {
      background: rgba(16, 185, 129, 0.2);
      color: var(--accent-emerald);
      border: 1px solid rgba(16, 185, 129, 0.4);
    }

    .diag-status-degraded {
      background: rgba(245, 158, 11, 0.2);
      color: var(--accent-amber);
      border: 1px solid rgba(245, 158, 11, 0.4);
    }

    .diag-status-down {
      background: rgba(244, 63, 94, 0.2);
      color: var(--accent-rose);
      border: 1px solid rgba(244, 63, 94, 0.4);
    }

    .diag-header-actions {
      display: flex;
      align-items: center;
      gap: 0.5rem;
    }

    .diag-modal-body {
      flex: 1;
      overflow-y: auto;
      padding: 1.5rem;
      display: flex;
      flex-direction: column;
      gap: 1.5rem;
    }

    .diag-section-title {
      font-size: 0.8rem;
      text-transform: uppercase;
      letter-spacing: 0.05em;
      color: var(--text-muted);
      font-weight: 700;
      margin-bottom: -0.5rem;
    }

    .probes-grid {
      display: grid;
      grid-template-columns: repeat(auto-fit, minmax(320px, 1fr));
      gap: 1rem;
    }

    .probe-card {
      padding: 1rem 1.25rem;
    }

    .probe-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin-bottom: 0.4rem;
    }

    .probe-name {
      display: flex;
      align-items: center;
      gap: 0.5rem;
      font-weight: 600;
      font-size: 0.9rem;
      color: var(--text-main);
    }

    .probe-dot {
      width: 8px;
      height: 8px;
      border-radius: 50%;
    }

    .probe-latency {
      font-size: 0.75rem;
      font-family: monospace;
      color: var(--text-muted);
    }

    .probe-details {
      font-size: 0.775rem;
      color: var(--text-secondary);
    }

    .metrics-stat-grid {
      display: grid;
      grid-template-columns: repeat(auto-fit, minmax(130px, 1fr));
      gap: 0.85rem;
    }

    .stat-card {
      padding: 0.85rem 1rem;
      text-align: center;
    }

    .stat-label {
      font-size: 0.7rem;
      text-transform: uppercase;
      color: var(--text-muted);
      margin-bottom: 0.35rem;
    }

    .stat-val {
      font-size: 1.25rem;
      font-weight: 700;
      font-family: 'Outfit', sans-serif;
      color: var(--text-main);
    }
  `]
})
export class ShellComponent implements OnInit {
  authService = inject(AuthService);
  assistantService = inject(AssistantService);
  notificationService = inject(NotificationService);
  systemService = inject(SystemService);
  router = inject(Router);

  unreadCount = signal<number>(0);
  notifications = signal<NotificationItem[]>([]);
  notificationsLoading = signal<boolean>(false);
  showNotifications = signal<boolean>(false);

  // Phase 18: System Health & Diagnostics
  showSystemDiagModal = signal<boolean>(false);
  systemHealth = signal<SystemHealthResponse | null>(null);
  systemMetrics = signal<SystemMetricsResponse | null>(null);
  diagLoading = signal<boolean>(false);

  ngOnInit() {
    this.assistantService.loadPendingActions().subscribe();
    this.loadUnreadCount();
    this.loadSystemHealthPill();
  }

  loadSystemHealthPill() {
    this.systemService.getSystemHealth().subscribe({
      next: (res) => {
        if (res.success) {
          this.systemHealth.set(res.data);
        }
      }
    });
  }

  openSystemDiagModal() {
    this.showSystemDiagModal.set(true);
    this.refreshDiagnostics();
  }

  closeSystemDiagModal() {
    this.showSystemDiagModal.set(false);
  }

  refreshDiagnostics() {
    this.diagLoading.set(true);
    this.systemService.getSystemHealth().subscribe({
      next: (res) => {
        if (res.success) this.systemHealth.set(res.data);
      }
    });

    this.systemService.getSystemMetrics().subscribe({
      next: (res) => {
        if (res.success) this.systemMetrics.set(res.data);
        this.diagLoading.set(false);
      },
      error: () => this.diagLoading.set(false)
    });
  }

  getComponentEntries(): Array<{ key: string; value: any }> {
    const comps = this.systemHealth()?.components;
    if (!comps) return [];
    return Object.keys(comps).map(k => ({ key: k, value: comps[k] }));
  }

  formatComponentName(key: string): string {
    switch (key) {
      case 'database': return 'PostgreSQL Database';
      case 'pgvector': return 'pgvector Extension';
      case 'documentStorage': return 'Document Storage';
      case 'apacheTika': return 'Apache Tika Engine';
      default: return key;
    }
  }

  getSystemStatusDotClass(): string {
    const s = this.systemHealth()?.status;
    if (s === 'UP') return 'dot-up';
    if (s === 'DEGRADED') return 'dot-degraded';
    return 'dot-down';
  }

  getSystemStatusBadgeClass(): string {
    const s = this.systemHealth()?.status;
    if (s === 'UP') return 'diag-status-up';
    if (s === 'DEGRADED') return 'diag-status-degraded';
    return 'diag-status-down';
  }

  getProbeDotClass(status: string): string {
    if (status === 'UP') return 'dot-up';
    if (status === 'DEGRADED') return 'dot-degraded';
    return 'dot-down';
  }

  formatUptime(sec: number): string {
    if (sec < 60) return `${sec}s`;
    const mins = Math.floor(sec / 60);
    if (mins < 60) return `${mins}m ${sec % 60}s`;
    const hrs = Math.floor(mins / 60);
    return `${hrs}h ${mins % 60}m`;
  }

  formatBytes(bytes: number): string {
    if (!bytes || bytes === 0) return '0 B';
    const k = 1024;
    const sizes = ['B', 'KB', 'MB', 'GB', 'TB'];
    const i = Math.floor(Math.log(bytes) / Math.log(k));
    return parseFloat((bytes / Math.pow(k, i)).toFixed(1)) + ' ' + sizes[i];
  }

  loadUnreadCount() {
    this.notificationService.getUnreadCount().subscribe({
      next: res => {
        this.unreadCount.set(res.data?.unreadCount || 0);
      }
    });
  }

  toggleNotifications(event: MouseEvent) {
    event.stopPropagation();
    const nextState = !this.showNotifications();
    this.showNotifications.set(nextState);
    if (nextState) {
      this.loadNotifications();
    }
  }

  closeNotifications() {
    this.showNotifications.set(false);
  }

  loadNotifications() {
    this.notificationsLoading.set(true);
    this.notificationService.getNotifications(0, 15, false).subscribe({
      next: res => {
        this.notifications.set(res.data?.content || []);
        this.notificationsLoading.set(false);
      },
      error: () => this.notificationsLoading.set(false)
    });
  }

  markAllAsRead() {
    this.notificationService.markAllAsRead().subscribe({
      next: () => {
        this.unreadCount.set(0);
        this.notifications.update(items =>
          items.map(i => ({ ...i, read: true }))
        );
      }
    });
  }

  handleNotificationClick(item: NotificationItem) {
    if (!item.read) {
      this.notificationService.markAsRead(item.id).subscribe({
        next: () => {
          item.read = true;
          this.unreadCount.update(c => Math.max(0, c - 1));
        }
      });
    }

    if (item.actionUrl) {
      this.closeNotifications();
      this.router.navigateByUrl(item.actionUrl);
    }
  }

  deleteNotification(id: string, event: MouseEvent) {
    event.stopPropagation();
    this.notificationService.deleteNotification(id).subscribe({
      next: () => {
        this.notifications.update(items => items.filter(i => i.id !== id));
        this.loadUnreadCount();
      }
    });
  }

  getNotifDotClass(type: string): string {
    switch (type) {
      case 'REMINDER_DUE': return 'dot-reminder';
      case 'LOAN_EMI': return 'dot-loan';
      case 'INSURANCE_EXPIRY': return 'dot-insurance';
      case 'WARRANTY_EXPIRY': return 'dot-warranty';
      case 'APPOINTMENT_ALERT': return 'dot-appointment';
      default: return 'dot-reminder';
    }
  }

  getUserInitial(): string {
    const user = this.authService.currentUser();
    if (user?.firstName) {
      return user.firstName.charAt(0).toUpperCase();
    }
    return 'U';
  }

  // Phase 17: Security Audit & Data Portability
  auditService = inject(AuditService);
  showAuditModal = signal<boolean>(false);
  auditLogs = signal<AuditLog[]>([]);
  auditLoading = signal<boolean>(false);
  auditFilter = signal<string>('');
  exporting = signal<boolean>(false);
  exportSuccessMsg = signal<string | null>(null);

  openAuditModal() {
    this.showAuditModal.set(true);
    this.loadAuditLogs();
  }

  closeAuditModal() {
    this.showAuditModal.set(false);
  }

  onFilterChange(event: Event) {
    const select = event.target as HTMLSelectElement;
    this.auditFilter.set(select.value);
    this.loadAuditLogs();
  }

  loadAuditLogs() {
    this.auditLoading.set(true);
    const filter = this.auditFilter() || undefined;
    this.auditService.getAuditLogs(filter, 0, 30).subscribe({
      next: (res) => {
        if (res.success && res.data) {
          this.auditLogs.set(res.data.content || []);
        }
        this.auditLoading.set(false);
      },
      error: () => this.auditLoading.set(false)
    });
  }

  exportUserData() {
    this.exporting.set(true);
    this.exportSuccessMsg.set(null);

    this.auditService.downloadUserDataExport().subscribe({
      next: (blob) => {
        const url = window.URL.createObjectURL(blob);
        const link = document.createElement('a');
        link.href = url;
        const dateStr = new Date().toISOString().slice(0, 10);
        link.download = `lifeos_gdpr_export_${dateStr}.json`;
        document.body.appendChild(link);
        link.click();
        document.body.removeChild(link);
        window.URL.revokeObjectURL(url);

        this.exporting.set(false);
        this.exportSuccessMsg.set('GDPR data archive exported successfully!');
        setTimeout(() => this.exportSuccessMsg.set(null), 5000);
      },
      error: () => {
        this.exporting.set(false);
        this.exportSuccessMsg.set('Export failed. Please try again.');
        setTimeout(() => this.exportSuccessMsg.set(null), 5000);
      }
    });
  }

  logout() {
    this.authService.logout();
    this.router.navigate(['/auth']);
  }
}
