import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { SearchService } from '../../core/services/search.service';
import { SearchResultItem } from '../../core/models/api.models';

@Component({
  selector: 'app-search',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <div class="search-container">
      <header class="page-header">
        <div>
          <h1 class="page-title">Unified Cross-Domain Search</h1>
          <p class="page-subtitle">Native PostgreSQL full-text search with cover density ranking across 14 LifeOS entities</p>
        </div>
      </header>

      <!-- Search Input Box -->
      <div class="search-box-wrapper glass-card">
        <div class="search-icon">
          <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <circle cx="11" cy="11" r="8"/>
            <path d="m21 21-4.3-4.3"/>
          </svg>
        </div>
        <input
          type="text"
          [(ngModel)]="searchQuery"
          (input)="onQueryInput()"
          (keyup.enter)="executeSearch()"
          placeholder="Search by keywords, exact phrases 'quotes', doctor names, trip destinations..."
          class="search-input"
          autofocus
        />
        @if (searchQuery) {
          <button (click)="clearSearch()" class="btn-icon">
            <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <line x1="18" y1="6" x2="6" y2="18"/>
              <line x1="6" y1="6" x2="18" y2="18"/>
            </svg>
          </button>
        }
        <button (click)="executeSearch()" class="btn btn-primary search-btn">
          Search
        </button>

        <!-- Autocomplete Suggestions Dropdown -->
        @if (suggestions().length > 0) {
          <div class="suggestions-dropdown glass-card">
            @for (sug of suggestions(); track sug) {
              <div (click)="selectSuggestion(sug)" class="suggestion-item">
                <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <path d="m21 21-4.3-4.3"/>
                  <circle cx="11" cy="11" r="8"/>
                </svg>
                <span>{{ sug }}</span>
              </div>
            }
          </div>
        }
      </div>

      <!-- Entity Filter Pills -->
      <div class="entity-filters">
        <button
          [class.active]="selectedEntity() === ''"
          (click)="filterEntity('')"
          class="entity-pill">
          All Entities
        </button>
        @for (ent of entityTypes; track ent.value) {
          <button
            [class.active]="selectedEntity() === ent.value"
            (click)="filterEntity(ent.value)"
            class="entity-pill">
            {{ ent.label }}
          </button>
        }
      </div>

      <!-- Search Results Area -->
      @if (loading()) {
        <div class="loading-state">
          <div class="spinner"></div>
          <span>Querying PostgreSQL full-text search indexes...</span>
        </div>
      } @else if (results().length === 0 && hasSearched()) {
        <div class="empty-state glass-card">
          <div class="empty-icon">
            <svg width="40" height="40" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <circle cx="11" cy="11" r="8"/>
              <line x1="21" y1="21" x2="16.65" y2="16.65"/>
              <line x1="8" y1="11" x2="14" y2="11"/>
            </svg>
          </div>
          <h3>No matching results found</h3>
          <p>Try refining your query, checking for typos, or broadening your filters.</p>
        </div>
      } @else if (results().length > 0) {
        <div class="results-header">
          <span>Found {{ results().length }} matching result(s)</span>
        </div>

        <div class="results-list">
          @for (item of filteredResults(); track item.entityId) {
            <div class="result-card glass-card">
              <div class="result-top">
                <div class="result-title-group">
                  <span class="entity-tag" [ngClass]="getEntityBadgeClass(item.entityType)">
                    {{ item.entityType }}
                  </span>
                  <h3 class="result-title">{{ item.title }}</h3>
                </div>
                @if (item.amount !== undefined && item.amount !== null) {
                  <div class="result-amount">
                    {{ item.amount | currency:(item.currency || 'USD') }}
                  </div>
                }
              </div>

              @if (item.subtitle) {
                <div class="result-subtitle">{{ item.subtitle }}</div>
              }

              @if (item.contentText) {
                <div class="result-snippet" [innerHTML]="formatSnippet(item.contentText)"></div>
              }

              <div class="result-footer">
                @if (item.categoryOrType) {
                  <span class="meta-tag">{{ item.categoryOrType }}</span>
                }
                @if (item.status) {
                  <span class="meta-tag">{{ item.status }}</span>
                }
                @if (item.eventDate) {
                  <span class="meta-date">{{ item.eventDate | date:'mediumDate' }}</span>
                }
              </div>
            </div>
          }
        </div>
      }
    </div>
  `,
  styles: [`
    .search-container {
      padding: 2rem;
      max-width: 1200px;
      margin: 0 auto;
      height: 100%;
      overflow-y: auto;
    }

    .page-header {
      margin-bottom: 2rem;
    }

    .search-box-wrapper {
      position: relative;
      display: flex;
      align-items: center;
      padding: 0.5rem 1rem;
      border-radius: var(--radius-lg);
      margin-bottom: 1.5rem;
      background: var(--bg-surface);
      border: 1px solid var(--border-light);
    }

    .search-icon {
      color: var(--text-muted);
      margin-right: 0.75rem;
      display: flex;
      align-items: center;
    }

    .search-input {
      flex: 1;
      background: transparent;
      border: none;
      outline: none;
      color: var(--text-main);
      font-size: 1.1rem;
      padding: 0.75rem 0;
    }

    .search-btn {
      padding: 0.65rem 1.5rem;
      margin-left: 0.5rem;
    }

    .suggestions-dropdown {
      position: absolute;
      top: calc(100% + 8px);
      left: 0;
      right: 0;
      background: var(--bg-surface);
      border: 1px solid var(--border-light);
      border-radius: var(--radius-md);
      z-index: 50;
      max-height: 240px;
      overflow-y: auto;
      box-shadow: 0 10px 30px rgba(0, 0, 0, 0.5);
    }

    .suggestion-item {
      padding: 0.75rem 1.25rem;
      display: flex;
      align-items: center;
      gap: 0.75rem;
      color: var(--text-secondary);
      font-size: 0.9rem;
      cursor: pointer;
      transition: background var(--transition-fast), color var(--transition-fast);
    }

    .suggestion-item:hover {
      background: rgba(99, 102, 241, 0.1);
      color: var(--primary);
    }

    .entity-filters {
      display: flex;
      gap: 0.5rem;
      margin-bottom: 2rem;
      overflow-x: auto;
      padding-bottom: 0.5rem;
    }

    .entity-pill {
      background: var(--bg-surface);
      border: 1px solid var(--border-subtle);
      color: var(--text-secondary);
      padding: 0.4rem 0.85rem;
      border-radius: var(--radius-full);
      font-size: 0.8rem;
      cursor: pointer;
      white-space: nowrap;
      transition: all var(--transition-fast);
    }

    .entity-pill:hover, .entity-pill.active {
      background: rgba(99, 102, 241, 0.15);
      color: var(--primary);
      border-color: var(--primary);
    }

    .results-header {
      font-size: 0.85rem;
      color: var(--text-muted);
      margin-bottom: 1rem;
    }

    .results-list {
      display: flex;
      flex-direction: column;
      gap: 1rem;
    }

    .result-card {
      padding: 1.25rem 1.5rem;
    }

    .result-top {
      display: flex;
      justify-content: space-between;
      align-items: flex-start;
      margin-bottom: 0.5rem;
    }

    .result-title-group {
      display: flex;
      align-items: center;
      gap: 0.75rem;
    }

    .result-title {
      font-size: 1.05rem;
      margin: 0;
      color: var(--text-main);
    }

    .result-amount {
      font-size: 1.1rem;
      font-weight: 700;
      color: var(--accent-emerald);
      font-family: 'Outfit', sans-serif;
    }

    .result-subtitle {
      font-size: 0.85rem;
      color: var(--text-muted);
      margin-bottom: 0.5rem;
    }

    .result-snippet {
      font-size: 0.85rem;
      color: var(--text-secondary);
      line-height: 1.5;
      background: var(--bg-input);
      padding: 0.6rem 0.85rem;
      border-radius: var(--radius-sm);
      margin-bottom: 0.75rem;
    }

    .result-footer {
      display: flex;
      gap: 0.5rem;
      align-items: center;
    }

    .meta-tag {
      font-size: 0.75rem;
      background: rgba(255, 255, 255, 0.05);
      padding: 0.15rem 0.5rem;
      border-radius: 4px;
      color: var(--text-muted);
    }

    .meta-date {
      font-size: 0.75rem;
      color: var(--text-muted);
      margin-left: auto;
    }

    .entity-tag {
      font-size: 0.7rem;
      font-weight: 700;
      padding: 0.2rem 0.5rem;
      border-radius: 4px;
      text-transform: uppercase;
      letter-spacing: 0.04em;
    }

    .tag-document { background: rgba(99, 102, 241, 0.2); color: var(--primary); }
    .tag-transaction { background: rgba(16, 185, 129, 0.2); color: var(--accent-emerald); }
    .tag-loan { background: rgba(245, 158, 11, 0.2); color: var(--accent-amber); }
    .tag-insurance { background: rgba(6, 182, 212, 0.2); color: var(--accent-cyan); }
    .tag-appointment { background: rgba(244, 63, 94, 0.2); color: var(--accent-rose); }
    .tag-trip { background: rgba(168, 85, 247, 0.2); color: var(--accent-purple); }
    .tag-asset { background: rgba(59, 130, 246, 0.2); color: #3b82f6; }
    .tag-default { background: rgba(255, 255, 255, 0.1); color: var(--text-secondary); }

    .loading-state, .empty-state {
      padding: 3rem;
      text-align: center;
      color: var(--text-muted);
    }

    .empty-icon {
      color: var(--text-muted);
      margin-bottom: 1rem;
    }
  `]
})
export class SearchComponent implements OnInit {
  searchService = inject(SearchService);

  readonly entityTypes = [
    { label: 'Documents', value: 'DOCUMENT' },
    { label: 'Transactions', value: 'TRANSACTION' },
    { label: 'Loans', value: 'LOAN' },
    { label: 'Insurance', value: 'INSURANCE_POLICY' },
    { label: 'Appointments', value: 'HEALTH_APPOINTMENT' },
    { label: 'Trips', value: 'TRIP' },
    { label: 'Assets', value: 'ASSET' },
    { label: 'Warranties', value: 'WARRANTY' }
  ];

  searchQuery: string = '';
  selectedEntity = signal<string>('');
  suggestions = signal<string[]>([]);
  results = signal<SearchResultItem[]>([]);
  loading = signal<boolean>(false);
  hasSearched = signal<boolean>(false);

  private debounceTimer: any;

  ngOnInit() {
    // Initial popular search or empty
  }

  onQueryInput() {
    clearTimeout(this.debounceTimer);
    if (!this.searchQuery || this.searchQuery.trim().length < 2) {
      this.suggestions.set([]);
      return;
    }

    this.debounceTimer = setTimeout(() => {
      this.searchService.suggest(this.searchQuery.trim()).subscribe({
        next: (res) => {
          if (res.success && res.data) {
            this.suggestions.set(res.data);
          }
        },
        error: () => this.suggestions.set([])
      });
    }, 250);
  }

  selectSuggestion(sug: string) {
    this.searchQuery = sug;
    this.suggestions.set([]);
    this.executeSearch();
  }

  executeSearch() {
    if (!this.searchQuery.trim()) return;

    this.suggestions.set([]);
    this.loading.set(true);
    this.hasSearched.set(true);

    const entities = this.selectedEntity() ? [this.selectedEntity()] : undefined;

    this.searchService.search(this.searchQuery.trim(), entities, 0, 50).subscribe({
      next: (res) => {
        if (res.success && res.data) {
          this.results.set(res.data.content || []);
        } else {
          this.results.set([]);
        }
        this.loading.set(false);
      },
      error: () => {
        this.results.set([]);
        this.loading.set(false);
      }
    });
  }

  clearSearch() {
    this.searchQuery = '';
    this.suggestions.set([]);
    this.results.set([]);
    this.hasSearched.set(false);
  }

  filterEntity(ent: string) {
    this.selectedEntity.set(ent);
    if (this.searchQuery.trim()) {
      this.executeSearch();
    }
  }

  filteredResults(): SearchResultItem[] {
    const ent = this.selectedEntity();
    if (!ent) return this.results();
    return this.results().filter(r => r.entityType === ent);
  }

  formatSnippet(text: string): string {
    if (!text) return '';
    // Format bold matching words if formatted
    return text.replace(/<b>/g, '<mark>').replace(/<\/b>/g, '</mark>');
  }

  getEntityBadgeClass(type: string): string {
    switch (type) {
      case 'DOCUMENT': return 'tag-document';
      case 'TRANSACTION': return 'tag-transaction';
      case 'LOAN': return 'tag-loan';
      case 'INSURANCE_POLICY': return 'tag-insurance';
      case 'HEALTH_APPOINTMENT': return 'tag-appointment';
      case 'TRIP': return 'tag-trip';
      case 'ASSET': return 'tag-asset';
      default: return 'tag-default';
    }
  }
}
