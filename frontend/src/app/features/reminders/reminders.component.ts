import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { NotificationService } from '../../core/services/notification.service';
import { CreateReminderRequest, Reminder } from '../../core/models/api.models';

@Component({
  selector: 'app-reminders',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <div class="reminders-container">
      <!-- Header -->
      <header class="reminders-header">
        <div>
          <h1 class="page-title">Reminders & Notification Engine</h1>
          <p class="page-subtitle">Background due-date scanning, loan EMIs, insurance renewals, and recurring tasks</p>
        </div>
        <div class="header-actions">
          <button (click)="openCreateModal()" class="btn btn-primary">
            <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <line x1="12" y1="5" x2="12" y2="19"/>
              <line x1="5" y1="12" x2="19" y2="12"/>
            </svg>
            <span>Create Reminder</span>
          </button>
        </div>
      </header>

      <!-- KPI Metrics -->
      <div class="kpi-grid">
        <div class="kpi-card glass-panel kpi-primary">
          <div class="kpi-label">Active Reminders</div>
          <div class="kpi-value">{{ activeCount() }}</div>
          <div class="kpi-subtext">Total tracked pending items</div>
        </div>
        <div class="kpi-card glass-panel kpi-danger">
          <div class="kpi-label">Overdue / Critical</div>
          <div class="kpi-value text-danger">{{ overdueCount() }}</div>
          <div class="kpi-subtext">Requires immediate attention</div>
        </div>
        <div class="kpi-card glass-panel kpi-warning">
          <div class="kpi-label">Due Soon (7 Days)</div>
          <div class="kpi-value text-warning">{{ upcomingCount() }}</div>
          <div class="kpi-subtext">EMIs & renewals approaching</div>
        </div>
        <div class="kpi-card glass-panel kpi-success">
          <div class="kpi-label">Completed</div>
          <div class="kpi-value text-success">{{ completedCount() }}</div>
          <div class="kpi-subtext">Successfully resolved tasks</div>
        </div>
      </div>

      <!-- Controls & Filter Bar -->
      <div class="controls-bar glass-panel">
        <div class="filter-tabs">
          <button
            class="tab-btn"
            [class.active]="currentTab() === 'ACTIVE'"
            (click)="setTab('ACTIVE')">
            Active
          </button>
          <button
            class="tab-btn"
            [class.active]="currentTab() === 'UPCOMING'"
            (click)="setTab('UPCOMING')">
            Upcoming (7d)
          </button>
          <button
            class="tab-btn"
            [class.active]="currentTab() === 'OVERDUE'"
            (click)="setTab('OVERDUE')">
            Overdue
          </button>
          <button
            class="tab-btn"
            [class.active]="currentTab() === 'COMPLETED'"
            (click)="setTab('COMPLETED')">
            Completed
          </button>
          <button
            class="tab-btn"
            [class.active]="currentTab() === 'DISMISSED'"
            (click)="setTab('DISMISSED')">
            Dismissed
          </button>
        </div>

        <div class="filter-right">
          <select class="select-priority" [ngModel]="priorityFilter()" (ngModelChange)="onPriorityFilterChange($event)">
            <option value="ALL">All Priorities</option>
            <option value="URGENT">Urgent</option>
            <option value="HIGH">High</option>
            <option value="MEDIUM">Medium</option>
            <option value="LOW">Low</option>
          </select>
          <button (click)="loadData()" class="btn-icon" title="Refresh">
            <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <polyline points="23 4 23 10 17 10"/>
              <path d="M20.49 15a9 9 0 1 1-2.12-9.36L23 10"/>
            </svg>
          </button>
        </div>
      </div>

      <!-- Reminder Cards List -->
      @if (loading()) {
        <div class="loading-state">
          <div class="spinner"></div>
          <p>Scanning and loading reminders...</p>
        </div>
      } @else if (filteredReminders().length === 0) {
        <div class="empty-state glass-panel">
          <div class="empty-icon">
            <svg width="48" height="48" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
              <path d="M18 8A6 6 0 0 0 6 8c0 7-3 9-3 9h18s-3-2-3-9"/>
              <path d="M13.73 21a2 2 0 0 1-3.46 0"/>
            </svg>
          </div>
          <h3>No Reminders Found</h3>
          <p>No reminders match your selected tab and priority filter.</p>
          <button (click)="openCreateModal()" class="btn btn-primary" style="margin-top: 1rem;">
            Create a New Reminder
          </button>
        </div>
      } @else {
        <div class="reminders-list">
          @for (item of filteredReminders(); track item.id) {
            <div class="reminder-card glass-panel" [class.overdue]="isOverdue(item.dueDate) && item.status === 'ACTIVE'">
              <div class="card-left">
                @if (item.status === 'ACTIVE') {
                  <button
                    (click)="completeItem(item.id)"
                    class="btn-complete"
                    title="Mark as completed">
                    <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                      <polyline points="20 6 9 17 4 12"/>
                    </svg>
                  </button>
                } @else if (item.status === 'COMPLETED') {
                  <div class="completed-check" title="Completed">
                    <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5">
                      <polyline points="20 6 9 17 4 12"/>
                    </svg>
                  </div>
                } @else {
                  <div class="dismissed-tag" title="Dismissed">
                    <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                      <line x1="18" y1="6" x2="6" y2="18"/>
                      <line x1="6" y1="6" x2="18" y2="18"/>
                    </svg>
                  </div>
                }
              </div>

              <div class="card-body">
                <div class="card-title-row">
                  <h3 class="reminder-title" [class.strikethrough]="item.status === 'COMPLETED'">{{ item.title }}</h3>
                  <div class="tags-group">
                    <span class="priority-badge" [ngClass]="item.priority ? item.priority.toLowerCase() : 'medium'">
                      {{ item.priority || 'MEDIUM' }}
                    </span>
                    @if (item.recurrenceRule && item.recurrenceRule !== 'NONE') {
                      <span class="recurrence-badge">
                        <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                          <polyline points="23 4 23 10 17 10"/>
                          <path d="M20.49 15a9 9 0 1 1-2.12-9.36L23 10"/>
                        </svg>
                        {{ item.recurrenceRule }}
                      </span>
                    }
                    @if (item.entityType) {
                      <span class="entity-badge">{{ item.entityType }}</span>
                    }
                  </div>
                </div>

                @if (item.description) {
                  <p class="reminder-description">{{ item.description }}</p>
                }

                <div class="card-meta">
                  <span class="due-time" [class.text-danger]="isOverdue(item.dueDate) && item.status === 'ACTIVE'">
                    <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                      <circle cx="12" cy="12" r="10"/>
                      <polyline points="12 6 12 12 16 14"/>
                    </svg>
                    Due: {{ item.dueDate | date:'medium' }}
                    @if (isOverdue(item.dueDate) && item.status === 'ACTIVE') {
                      <strong class="badge-overdue">OVERDUE</strong>
                    }
                  </span>
                  @if (item.completedAt) {
                    <span class="completed-meta">Resolved: {{ item.completedAt | date:'short' }}</span>
                  }
                </div>
              </div>

              <div class="card-actions">
                @if (item.status === 'ACTIVE') {
                  <button (click)="dismissItem(item.id)" class="btn-action btn-dismiss" title="Dismiss">
                    Dismiss
                  </button>
                }
                <button (click)="deleteItem(item.id)" class="btn-action btn-delete" title="Delete">
                  <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                    <polyline points="3 6 5 6 21 6"/>
                    <path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2"/>
                  </svg>
                </button>
              </div>
            </div>
          }
        </div>
      }

      <!-- Create Reminder Modal -->
      @if (showModal()) {
        <div class="modal-backdrop" (click)="closeModal()">
          <div class="modal-dialog glass-panel" (click)="$event.stopPropagation()">
            <div class="modal-header">
              <h2 class="modal-title">Create New Reminder</h2>
              <button class="btn-close" (click)="closeModal()">&times;</button>
            </div>

            <div class="modal-body">
              <div class="form-group">
                <label>Reminder Title *</label>
                <input
                  type="text"
                  [(ngModel)]="newReminder.title"
                  placeholder="e.g. Pay HDFC Home Loan EMI"
                  class="form-input" />
              </div>

              <div class="form-group">
                <label>Description (Optional)</label>
                <textarea
                  [(ngModel)]="newReminder.description"
                  placeholder="Additional context or payment instructions..."
                  rows="3"
                  class="form-input"></textarea>
              </div>

              <div class="form-row">
                <div class="form-group">
                  <label>Due Date & Time *</label>
                  <input
                    type="datetime-local"
                    [(ngModel)]="newReminderDueDate"
                    class="form-input" />
                </div>

                <div class="form-group">
                  <label>Priority</label>
                  <select [(ngModel)]="newReminder.priority" class="form-input">
                    <option value="LOW">Low</option>
                    <option value="MEDIUM">Medium</option>
                    <option value="HIGH">High</option>
                    <option value="URGENT">Urgent</option>
                  </select>
                </div>
              </div>

              <div class="form-group">
                <label>Recurrence Cadence</label>
                <select [(ngModel)]="newReminder.recurrenceRule" class="form-input">
                  <option value="NONE">None (One-time)</option>
                  <option value="DAILY">Daily</option>
                  <option value="WEEKLY">Weekly</option>
                  <option value="MONTHLY">Monthly</option>
                  <option value="YEARLY">Yearly</option>
                </select>
              </div>

              @if (reminderError()) {
                <div class="error-banner">{{ reminderError() }}</div>
              }
            </div>

            <div class="modal-footer">
              <button class="btn btn-secondary" (click)="closeModal()">Cancel</button>
              <button
                class="btn btn-primary"
                [disabled]="!newReminder.title || !newReminderDueDate || saving()"
                (click)="saveReminder()">
                <span>{{ saving() ? 'Saving...' : 'Create Reminder' }}</span>
              </button>
            </div>
          </div>
        </div>
      }
    </div>
  `,
  styles: [`
    .reminders-container {
      padding: 2rem;
      max-width: 1400px;
      margin: 0 auto;
      display: flex;
      flex-direction: column;
      gap: 1.75rem;
    }

    .reminders-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
    }

    .page-title {
      font-size: 2rem;
      font-weight: 800;
      color: var(--text-main);
      letter-spacing: -0.02em;
    }

    .page-subtitle {
      font-size: 0.95rem;
      color: var(--text-muted);
      margin-top: 0.25rem;
    }

    .btn {
      display: inline-flex;
      align-items: center;
      gap: 0.5rem;
      padding: 0.65rem 1.25rem;
      border-radius: var(--radius-sm);
      font-weight: 600;
      font-size: 0.9rem;
      cursor: pointer;
      border: none;
      transition: all var(--transition-fast);
      text-decoration: none;
    }

    .btn-primary {
      background: var(--gradient-brand);
      color: #ffffff;
      box-shadow: 0 4px 14px var(--primary-glow);
    }

    .btn-primary:hover:not(:disabled) {
      transform: translateY(-1px);
      box-shadow: 0 6px 20px var(--primary-glow);
    }

    .btn-secondary {
      background: rgba(255, 255, 255, 0.06);
      color: var(--text-main);
      border: 1px solid var(--border-subtle);
    }

    .btn-secondary:hover {
      background: rgba(255, 255, 255, 0.1);
    }

    /* KPI Grid */
    .kpi-grid {
      display: grid;
      grid-template-columns: repeat(4, 1fr);
      gap: 1.25rem;
    }

    .kpi-card {
      padding: 1.5rem;
      border-radius: var(--radius-md);
      position: relative;
      overflow: hidden;
    }

    .kpi-card::before {
      content: '';
      position: absolute;
      top: 0;
      left: 0;
      right: 0;
      height: 3px;
    }

    .kpi-primary::before { background: var(--gradient-brand); }
    .kpi-danger::before { background: var(--accent-rose); }
    .kpi-warning::before { background: var(--accent-amber); }
    .kpi-success::before { background: var(--accent-emerald); }

    .kpi-label {
      font-size: 0.85rem;
      color: var(--text-secondary);
      font-weight: 500;
      text-transform: uppercase;
      letter-spacing: 0.05em;
    }

    .kpi-value {
      font-size: 2.25rem;
      font-weight: 800;
      color: var(--text-main);
      margin: 0.35rem 0;
      letter-spacing: -0.03em;
    }

    .kpi-subtext {
      font-size: 0.8rem;
      color: var(--text-muted);
    }

    .text-danger { color: var(--accent-rose) !important; }
    .text-warning { color: var(--accent-amber) !important; }
    .text-success { color: var(--accent-emerald) !important; }

    /* Controls Bar */
    .controls-bar {
      display: flex;
      justify-content: space-between;
      align-items: center;
      padding: 0.75rem 1.25rem;
      border-radius: var(--radius-md);
    }

    .filter-tabs {
      display: flex;
      gap: 0.5rem;
    }

    .tab-btn {
      background: transparent;
      border: none;
      color: var(--text-secondary);
      padding: 0.5rem 1rem;
      border-radius: var(--radius-sm);
      font-size: 0.875rem;
      font-weight: 600;
      cursor: pointer;
      transition: all var(--transition-fast);
    }

    .tab-btn:hover {
      color: var(--text-main);
      background: rgba(255, 255, 255, 0.05);
    }

    .tab-btn.active {
      color: #ffffff;
      background: rgba(99, 102, 241, 0.2);
      border: 1px solid rgba(99, 102, 241, 0.4);
    }

    .filter-right {
      display: flex;
      align-items: center;
      gap: 0.75rem;
    }

    .select-priority {
      background: rgba(15, 23, 42, 0.8);
      border: 1px solid var(--border-subtle);
      color: var(--text-main);
      padding: 0.45rem 0.85rem;
      border-radius: var(--radius-sm);
      font-size: 0.85rem;
    }

    .btn-icon {
      background: rgba(255, 255, 255, 0.05);
      border: 1px solid var(--border-subtle);
      color: var(--text-secondary);
      border-radius: var(--radius-sm);
      width: 36px;
      height: 36px;
      display: flex;
      align-items: center;
      justify-content: center;
      cursor: pointer;
      transition: all var(--transition-fast);
    }

    .btn-icon:hover {
      color: var(--text-main);
      background: rgba(255, 255, 255, 0.1);
    }

    /* Reminders List */
    .reminders-list {
      display: flex;
      flex-direction: column;
      gap: 0.85rem;
    }

    .reminder-card {
      display: flex;
      align-items: center;
      padding: 1.25rem 1.5rem;
      border-radius: var(--radius-md);
      gap: 1.25rem;
      transition: transform var(--transition-fast), border-color var(--transition-fast);
      position: relative;
    }

    .reminder-card:hover {
      transform: translateY(-2px);
      border-color: rgba(99, 102, 241, 0.4);
    }

    .reminder-card.overdue {
      border-left: 4px solid var(--accent-rose);
      background: rgba(244, 63, 94, 0.04);
    }

    .card-left {
      flex-shrink: 0;
    }

    .btn-complete {
      width: 34px;
      height: 34px;
      border-radius: 50%;
      border: 2px solid rgba(255, 255, 255, 0.2);
      background: transparent;
      color: transparent;
      cursor: pointer;
      display: flex;
      align-items: center;
      justify-content: center;
      transition: all var(--transition-fast);
    }

    .btn-complete:hover {
      border-color: var(--accent-emerald);
      color: var(--accent-emerald);
      background: rgba(16, 185, 129, 0.1);
    }

    .completed-check {
      width: 34px;
      height: 34px;
      border-radius: 50%;
      background: rgba(16, 185, 129, 0.2);
      color: var(--accent-emerald);
      border: 1px solid var(--accent-emerald);
      display: flex;
      align-items: center;
      justify-content: center;
    }

    .dismissed-tag {
      width: 34px;
      height: 34px;
      border-radius: 50%;
      background: rgba(148, 163, 184, 0.15);
      color: var(--text-muted);
      border: 1px solid var(--border-subtle);
      display: flex;
      align-items: center;
      justify-content: center;
    }

    .card-body {
      flex: 1;
      min-width: 0;
    }

    .card-title-row {
      display: flex;
      align-items: center;
      gap: 0.85rem;
      flex-wrap: wrap;
    }

    .reminder-title {
      font-size: 1.05rem;
      font-weight: 700;
      color: var(--text-main);
      margin: 0;
    }

    .reminder-title.strikethrough {
      text-decoration: line-through;
      color: var(--text-muted);
    }

    .tags-group {
      display: flex;
      align-items: center;
      gap: 0.5rem;
    }

    .priority-badge {
      font-size: 0.7rem;
      font-weight: 700;
      padding: 0.15rem 0.5rem;
      border-radius: 4px;
      text-transform: uppercase;
      letter-spacing: 0.05em;
    }

    .priority-badge.urgent {
      background: rgba(244, 63, 94, 0.15);
      color: #fda4af;
      border: 1px solid rgba(244, 63, 94, 0.3);
    }

    .priority-badge.high {
      background: rgba(245, 158, 11, 0.15);
      color: #fcd34d;
      border: 1px solid rgba(245, 158, 11, 0.3);
    }

    .priority-badge.medium {
      background: rgba(99, 102, 241, 0.15);
      color: #c7d2fe;
      border: 1px solid rgba(99, 102, 241, 0.3);
    }

    .priority-badge.low {
      background: rgba(148, 163, 184, 0.15);
      color: #cbd5e1;
      border: 1px solid rgba(148, 163, 184, 0.3);
    }

    .recurrence-badge {
      font-size: 0.725rem;
      padding: 0.15rem 0.5rem;
      border-radius: 4px;
      background: rgba(6, 182, 212, 0.15);
      color: #67e8f9;
      border: 1px solid rgba(6, 182, 212, 0.3);
      display: flex;
      align-items: center;
      gap: 0.25rem;
      font-weight: 600;
    }

    .entity-badge {
      font-size: 0.7rem;
      padding: 0.15rem 0.45rem;
      border-radius: 4px;
      background: rgba(255, 255, 255, 0.06);
      color: var(--text-secondary);
      border: 1px solid var(--border-subtle);
      font-weight: 500;
    }

    .reminder-description {
      font-size: 0.875rem;
      color: var(--text-secondary);
      margin: 0.35rem 0 0.5rem;
      line-height: 1.4;
    }

    .card-meta {
      display: flex;
      align-items: center;
      gap: 1.25rem;
      font-size: 0.8rem;
      color: var(--text-muted);
      margin-top: 0.4rem;
    }

    .due-time {
      display: flex;
      align-items: center;
      gap: 0.35rem;
    }

    .badge-overdue {
      background: var(--accent-rose);
      color: #ffffff;
      padding: 0.1rem 0.35rem;
      border-radius: 3px;
      font-size: 0.65rem;
      font-weight: 800;
      margin-left: 0.35rem;
    }

    .completed-meta {
      color: var(--accent-emerald);
    }

    .card-actions {
      display: flex;
      align-items: center;
      gap: 0.5rem;
    }

    .btn-action {
      background: transparent;
      border: none;
      padding: 0.45rem 0.75rem;
      border-radius: var(--radius-sm);
      font-size: 0.8rem;
      font-weight: 600;
      cursor: pointer;
      transition: all var(--transition-fast);
    }

    .btn-dismiss {
      color: var(--text-muted);
      border: 1px solid var(--border-subtle);
    }

    .btn-dismiss:hover {
      color: var(--text-main);
      background: rgba(255, 255, 255, 0.05);
    }

    .btn-delete {
      color: var(--text-muted);
      padding: 0.45rem;
      display: flex;
      align-items: center;
      justify-content: center;
    }

    .btn-delete:hover {
      color: var(--accent-rose);
      background: rgba(244, 63, 94, 0.1);
    }

    /* States */
    .loading-state, .empty-state {
      padding: 4rem 2rem;
      text-align: center;
      border-radius: var(--radius-lg);
      display: flex;
      flex-direction: column;
      align-items: center;
      gap: 0.75rem;
    }

    .spinner {
      width: 40px;
      height: 40px;
      border: 3px solid rgba(99, 102, 241, 0.2);
      border-top-color: var(--accent-indigo);
      border-radius: 50%;
      animation: spin 0.8s linear infinite;
    }

    @keyframes spin {
      to { transform: rotate(360deg); }
    }

    .empty-icon {
      color: var(--text-muted);
      opacity: 0.5;
    }

    /* Modal */
    .modal-backdrop {
      position: fixed;
      inset: 0;
      background: rgba(0, 0, 0, 0.7);
      backdrop-filter: blur(8px);
      z-index: 100;
      display: flex;
      align-items: center;
      justify-content: center;
      padding: 1.5rem;
    }

    .modal-dialog {
      width: 100%;
      max-width: 540px;
      border-radius: var(--radius-lg);
      box-shadow: 0 20px 40px rgba(0, 0, 0, 0.6);
      animation: modalIn 0.2s cubic-bezier(0.16, 1, 0.3, 1);
    }

    @keyframes modalIn {
      from { opacity: 0; transform: scale(0.95); }
      to { opacity: 1; transform: scale(1); }
    }

    .modal-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      padding: 1.25rem 1.5rem;
      border-bottom: 1px solid var(--border-subtle);
    }

    .modal-title {
      font-size: 1.25rem;
      font-weight: 700;
      color: var(--text-main);
      margin: 0;
    }

    .btn-close {
      background: transparent;
      border: none;
      color: var(--text-muted);
      font-size: 1.5rem;
      cursor: pointer;
    }

    .modal-body {
      padding: 1.5rem;
      display: flex;
      flex-direction: column;
      gap: 1.25rem;
    }

    .form-group {
      display: flex;
      flex-direction: column;
      gap: 0.4rem;
    }

    .form-group label {
      font-size: 0.825rem;
      font-weight: 600;
      color: var(--text-secondary);
    }

    .form-input {
      background: rgba(15, 23, 42, 0.8);
      border: 1px solid var(--border-subtle);
      border-radius: var(--radius-sm);
      padding: 0.65rem 0.85rem;
      color: var(--text-main);
      font-size: 0.9rem;
      outline: none;
      font-family: inherit;
    }

    .form-input:focus {
      border-color: var(--accent-indigo);
      box-shadow: 0 0 0 2px rgba(99, 102, 241, 0.2);
    }

    .form-row {
      display: grid;
      grid-template-columns: 1fr 1fr;
      gap: 1rem;
    }

    .modal-footer {
      display: flex;
      justify-content: flex-end;
      gap: 0.75rem;
      padding: 1.25rem 1.5rem;
      border-top: 1px solid var(--border-subtle);
    }

    .error-banner {
      background: rgba(244, 63, 94, 0.15);
      border: 1px solid var(--accent-rose);
      color: var(--accent-rose);
      padding: 0.75rem 1rem;
      border-radius: var(--radius-sm);
      font-size: 0.85rem;
      margin-top: 1rem;
    }

    @media (max-width: 900px) {
      .kpi-grid { grid-template-columns: 1fr 1fr; }
    }
  `]
})
export class RemindersComponent implements OnInit {
  private notificationService = inject(NotificationService);

  reminders = signal<Reminder[]>([]);
  loading = signal<boolean>(false);
  saving = signal<boolean>(false);
  showModal = signal<boolean>(false);
  reminderError = signal<string>('');

  currentTab = signal<'ACTIVE' | 'UPCOMING' | 'OVERDUE' | 'COMPLETED' | 'DISMISSED'>('ACTIVE');
  priorityFilter = signal<string>('ALL');

  // KPI Counters
  activeCount = signal<number>(0);
  overdueCount = signal<number>(0);
  upcomingCount = signal<number>(0);
  completedCount = signal<number>(0);

  // Modal Form State
  newReminder: Partial<CreateReminderRequest> = {
    title: '',
    description: '',
    priority: 'MEDIUM',
    recurrenceRule: 'NONE'
  };
  newReminderDueDate: string = '';

  ngOnInit() {
    this.loadData();
    this.loadKpis();
  }

  loadData() {
    this.loading.set(true);
    const tab = this.currentTab();

    if (tab === 'UPCOMING') {
      this.notificationService.getUpcomingReminders(7).subscribe({
        next: res => {
          this.reminders.set(res.data || []);
          this.loading.set(false);
        },
        error: () => this.loading.set(false)
      });
    } else if (tab === 'OVERDUE') {
      this.notificationService.getOverdueReminders().subscribe({
        next: res => {
          this.reminders.set(res.data || []);
          this.loading.set(false);
        },
        error: () => this.loading.set(false)
      });
    } else {
      const status = tab === 'COMPLETED' ? 'COMPLETED' : tab === 'DISMISSED' ? 'DISMISSED' : 'ACTIVE';
      this.notificationService.getReminders(status, 0, 50).subscribe({
        next: res => {
          this.reminders.set(res.data?.content || []);
          this.loading.set(false);
        },
        error: () => this.loading.set(false)
      });
    }
  }

  loadKpis() {
    this.notificationService.getReminders('ACTIVE', 0, 100).subscribe({
      next: res => {
        const items = res.data?.content || [];
        this.activeCount.set(items.length);
        const now = new Date();
        const overdue = items.filter(i => new Date(i.dueDate) < now);
        this.overdueCount.set(overdue.length);
      }
    });

    this.notificationService.getUpcomingReminders(7).subscribe({
      next: res => {
        this.upcomingCount.set(res.data?.length || 0);
      }
    });

    this.notificationService.getReminders('COMPLETED', 0, 100).subscribe({
      next: res => {
        this.completedCount.set(res.data?.totalElements || 0);
      }
    });
  }

  filteredReminders(): Reminder[] {
    const list = this.reminders();
    const priority = this.priorityFilter();
    if (priority === 'ALL') {
      return list;
    }
    return list.filter(r => (r.priority || 'MEDIUM').toUpperCase() === priority);
  }

  setTab(tab: 'ACTIVE' | 'UPCOMING' | 'OVERDUE' | 'COMPLETED' | 'DISMISSED') {
    this.currentTab.set(tab);
    this.loadData();
  }

  onPriorityFilterChange(val: string) {
    this.priorityFilter.set(val);
  }

  isOverdue(dueDateStr: string): boolean {
    return new Date(dueDateStr) < new Date();
  }

  completeItem(id: string) {
    this.notificationService.completeReminder(id).subscribe({
      next: () => {
        this.loadData();
        this.loadKpis();
      }
    });
  }

  dismissItem(id: string) {
    this.notificationService.dismissReminder(id).subscribe({
      next: () => {
        this.loadData();
        this.loadKpis();
      }
    });
  }

  deleteItem(id: string) {
    if (!confirm('Are you sure you want to delete this reminder?')) return;
    this.notificationService.deleteReminder(id).subscribe({
      next: () => {
        this.loadData();
        this.loadKpis();
      }
    });
  }

  openCreateModal() {
    const defaultDate = new Date(Date.now() + 24 * 60 * 60 * 1000);
    this.newReminderDueDate = defaultDate.toISOString().slice(0, 16);
    this.newReminder = {
      title: '',
      description: '',
      priority: 'MEDIUM',
      recurrenceRule: 'NONE'
    };
    this.reminderError.set('');
    this.showModal.set(true);
  }

  closeModal() {
    this.showModal.set(false);
    this.reminderError.set('');
  }

  saveReminder() {
    if (!this.newReminder.title || !this.newReminderDueDate) return;
    this.saving.set(true);
    this.reminderError.set('');

    const isoDate = new Date(this.newReminderDueDate).toISOString();
    const req: CreateReminderRequest = {
      title: this.newReminder.title,
      description: this.newReminder.description,
      dueAt: isoDate,
      dueDate: isoDate,
      priority: this.newReminder.priority || 'MEDIUM',
      recurrencePattern: this.newReminder.recurrenceRule || 'NONE',
      recurrenceRule: this.newReminder.recurrenceRule || 'NONE'
    };

    this.notificationService.createReminder(req).subscribe({
      next: () => {
        this.saving.set(false);
        this.reminderError.set('');
        this.closeModal();
        this.loadData();
        this.loadKpis();
      },
      error: (err) => {
        this.saving.set(false);
        this.reminderError.set(err.error?.detail || err.error?.message || 'Failed to create reminder');
      }
    });
  }
}
