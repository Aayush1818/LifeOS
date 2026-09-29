import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { DocumentService } from '../../core/services/document.service';
import { DocumentChunk, DocumentResponse } from '../../core/models/api.models';

@Component({
  selector: 'app-documents',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <div class="documents-container">
      <!-- Header -->
      <header class="page-header">
        <div>
          <h1 class="page-title">Document Intelligence & Knowledge</h1>
          <p class="page-subtitle">Secure encrypted vault with Apache Tika extraction and pgvector HNSW chunking</p>
        </div>
        <button (click)="showUploadModal.set(true)" class="btn btn-primary">
          <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"/>
            <polyline points="17 8 12 3 7 8"/>
            <line x1="12" y1="3" x2="12" y2="15"/>
          </svg>
          <span>Upload Document</span>
        </button>
      </header>

      <!-- Category Filter Pills -->
      <div class="category-filters">
        <button
          [class.active]="selectedCategory() === ''"
          (click)="filterCategory('')"
          class="filter-pill">
          All Documents ({{ documents().length }})
        </button>
        @for (cat of categories; track cat) {
          <button
            [class.active]="selectedCategory() === cat"
            (click)="filterCategory(cat)"
            class="filter-pill">
            {{ cat }}
          </button>
        }
      </div>

      <!-- Document Table / Grid -->
      @if (loading()) {
        <div class="loading-state">
          <div class="spinner"></div>
          <span>Loading document catalog...</span>
        </div>
      } @else if (documents().length === 0) {
        <div class="empty-state glass-card">
          <div class="empty-icon">
            <svg width="36" height="36" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M15 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V7Z"/>
              <path d="M14 2v4a2 2 0 0 0 2 2h4"/>
            </svg>
          </div>
          <h3>No documents found</h3>
          <p>Upload PDFs, insurance policies, medical reports, or receipts to enable AI grounding.</p>
          <button (click)="showUploadModal.set(true)" class="btn btn-sm btn-primary">Upload Now</button>
        </div>
      } @else {
        <div class="glass-card table-container">
          <table class="data-table">
            <thead>
              <tr>
                <th>Document</th>
                <th>Category</th>
                <th>Size</th>
                <th>Ingestion Status</th>
                <th>Uploaded</th>
                <th class="text-right">Actions</th>
              </tr>
            </thead>
            <tbody>
              @for (doc of filteredDocuments(); track doc.id) {
                <tr>
                  <td>
                    <div class="doc-item">
                      <div class="doc-icon">
                        <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                          <path d="M15 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V7Z"/>
                          <path d="M14 2v4a2 2 0 0 0 2 2h4"/>
                        </svg>
                      </div>
                      <div>
                        <div class="doc-title-text">{{ doc.title }}</div>
                        <div class="doc-filename">{{ doc.originalFilename }} · v{{ doc.version }}</div>
                      </div>
                    </div>
                  </td>
                  <td>
                    <span class="badge badge-subtle">{{ doc.category }}</span>
                  </td>
                  <td>{{ formatBytes(doc.fileSize) }}</td>
                  <td>
                    <span class="status-badge" [ngClass]="getStatusClass(doc.ingestionStatus)">
                      {{ doc.ingestionStatus }}
                    </span>
                  </td>
                  <td>{{ doc.createdAt | date:'mediumDate' }}</td>
                  <td class="text-right">
                    <div class="action-buttons">
                      <button (click)="inspectChunks(doc)" class="btn-icon" title="Inspect RAG Chunks">
                        <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                          <path d="m21 21-4.3-4.3"/>
                          <circle cx="11" cy="11" r="8"/>
                        </svg>
                      </button>
                      <button (click)="download(doc)" class="btn-icon" title="Download File">
                        <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                          <path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"/>
                          <polyline points="7 10 12 15 17 10"/>
                          <line x1="12" y1="15" x2="12" y2="3"/>
                        </svg>
                      </button>
                      <button (click)="deleteDoc(doc.id)" class="btn-icon text-danger" title="Delete">
                        <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                          <path d="M3 6h18"/>
                          <path d="M19 6v14c0 1-1 2-2 2H7c-1 0-2-1-2-2V6"/>
                        </svg>
                      </button>
                    </div>
                  </td>
                </tr>
              }
            </tbody>
          </table>
        </div>
      }

      <!-- Upload Modal -->
      @if (showUploadModal()) {
        <div class="modal-backdrop" (click)="showUploadModal.set(false)">
          <div class="modal-card glass-card" (click)="$event.stopPropagation()">
            <div class="modal-header">
              <h3>Upload Document to Vault</h3>
              <button (click)="showUploadModal.set(false)" class="btn-icon">
                <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <line x1="18" y1="6" x2="6" y2="18"/>
                  <line x1="6" y1="6" x2="18" y2="18"/>
                </svg>
              </button>
            </div>

            <form (ngSubmit)="submitUpload()" class="modal-body">
              <div class="form-group">
                <label class="form-label">Document Title</label>
                <input
                  type="text"
                  [(ngModel)]="uploadTitle"
                  name="uploadTitle"
                  placeholder="e.g. Health Insurance Policy 2026"
                  required
                  class="form-input"
                />
              </div>

              <div class="form-group">
                <label class="form-label">Category</label>
                <select [(ngModel)]="uploadCategory" name="uploadCategory" class="form-select">
                  @for (cat of categories; track cat) {
                    <option [value]="cat">{{ cat }}</option>
                  }
                </select>
              </div>

              <div class="form-group">
                <label class="form-label">Select File (PDF, DOCX, TXT, PNG)</label>
                <input
                  type="file"
                  (change)="onFileSelected($event)"
                  required
                  class="form-input-file"
                />
              </div>

              @if (uploadError()) {
                <div class="error-banner">{{ uploadError() }}</div>
              }

              <div class="modal-footer">
                <button type="button" (click)="showUploadModal.set(false)" class="btn btn-secondary">Cancel</button>
                <button type="submit" [disabled]="uploading() || !selectedFile" class="btn btn-primary">
                  {{ uploading() ? 'Extracting & Ingesting...' : 'Upload & Process' }}
                </button>
              </div>
            </form>
          </div>
        </div>
      }

      <!-- Chunk Explorer Drawer -->
      @if (activeInspectionDoc()) {
        <aside class="chunk-drawer glass-panel">
          <div class="drawer-header">
            <div>
              <h3>RAG Chunk Inspector</h3>
              <div class="subtext">{{ activeInspectionDoc()?.title }}</div>
            </div>
            <button (click)="activeInspectionDoc.set(null)" class="btn-icon">
              <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <line x1="18" y1="6" x2="6" y2="18"/>
                <line x1="6" y1="6" x2="18" y2="18"/>
              </svg>
            </button>
          </div>

          <div class="drawer-content">
            @if (loadingChunks()) {
              <div class="loading-state">
                <div class="spinner"></div>
                <span>Loading pgvector chunks...</span>
              </div>
            } @else if (chunks().length === 0) {
              <div class="empty-state">No chunks ingested yet.</div>
            } @else {
              <div class="chunk-metrics">
                <span class="badge badge-accent">{{ chunks().length }} Chunks</span>
                <span class="badge badge-subtle">HNSW 1536-dim</span>
              </div>

              <div class="chunk-list">
                @for (chunk of chunks(); track chunk.id) {
                  <div class="chunk-card glass-card">
                    <div class="chunk-header">
                      <span class="chunk-index">Chunk #{{ chunk.chunkIndex }}</span>
                      <span class="token-count">{{ chunk.tokenCount }} tokens</span>
                    </div>
                    @if (chunk.sectionTitle) {
                      <div class="chunk-section">{{ chunk.sectionTitle }} (Page {{ chunk.pageNumber }})</div>
                    }
                    <pre class="chunk-text">{{ chunk.content }}</pre>
                  </div>
                }
              </div>
            }
          </div>
        </aside>
      }
    </div>
  `,
  styles: [`
    .documents-container {
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

    .category-filters {
      display: flex;
      gap: 0.5rem;
      margin-bottom: 1.5rem;
      overflow-x: auto;
      padding-bottom: 0.5rem;
    }

    .filter-pill {
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

    .filter-pill:hover, .filter-pill.active {
      background: rgba(99, 102, 241, 0.15);
      color: var(--primary);
      border-color: var(--primary);
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

    .doc-item {
      display: flex;
      align-items: center;
      gap: 0.75rem;
    }

    .doc-icon {
      color: var(--primary);
      width: 36px;
      height: 36px;
      border-radius: var(--radius-sm);
      background: rgba(99, 102, 241, 0.1);
      display: flex;
      align-items: center;
      justify-content: center;
    }

    .doc-title-text {
      font-weight: 600;
      color: var(--text-main);
      font-size: 0.9rem;
    }

    .doc-filename {
      font-size: 0.75rem;
      color: var(--text-muted);
    }

    .status-badge {
      font-size: 0.75rem;
      font-weight: 600;
      padding: 0.2rem 0.55rem;
      border-radius: 4px;
      text-transform: uppercase;
    }

    .status-processed {
      background: rgba(16, 185, 129, 0.15);
      color: var(--accent-emerald);
    }

    .status-processing {
      background: rgba(245, 158, 11, 0.15);
      color: var(--accent-amber);
    }

    .status-failed {
      background: rgba(244, 63, 94, 0.15);
      color: var(--accent-rose);
    }

    .action-buttons {
      display: flex;
      justify-content: flex-end;
      gap: 0.5rem;
    }

    .text-right {
      text-align: right;
    }

    .text-danger {
      color: var(--accent-rose);
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

    .modal-footer {
      display: flex;
      justify-content: flex-end;
      gap: 0.75rem;
      margin-top: 1rem;
    }

    .chunk-drawer {
      position: fixed;
      top: 0;
      right: 0;
      width: 420px;
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
      align-items: center;
    }

    .drawer-content {
      flex: 1;
      overflow-y: auto;
      padding: 1.5rem;
    }

    .chunk-metrics {
      display: flex;
      gap: 0.5rem;
      margin-bottom: 1rem;
    }

    .chunk-card {
      padding: 1rem;
      margin-bottom: 1rem;
    }

    .chunk-header {
      display: flex;
      justify-content: space-between;
      font-size: 0.75rem;
      font-weight: 600;
      color: var(--primary);
      margin-bottom: 0.35rem;
    }

    .chunk-section {
      font-size: 0.75rem;
      color: var(--text-muted);
      margin-bottom: 0.5rem;
    }

    .chunk-text {
      font-size: 0.75rem;
      white-space: pre-wrap;
      word-break: break-word;
      color: var(--text-secondary);
      background: var(--bg-input);
      padding: 0.6rem;
      border-radius: var(--radius-sm);
      max-height: 150px;
      overflow-y: auto;
    }

    .empty-state {
      padding: 3rem;
      text-align: center;
    }

    .empty-icon {
      color: var(--text-muted);
      margin-bottom: 1rem;
    }

    .loading-state {
      padding: 3rem;
      text-align: center;
      color: var(--text-muted);
      display: flex;
      flex-direction: column;
      align-items: center;
      gap: 1rem;
    }
  `]
})
export class DocumentsComponent implements OnInit {
  documentService = inject(DocumentService);

  readonly categories = ['FINANCE', 'HEALTHCARE', 'LEGAL', 'TAX', 'IDENTITY', 'TRAVEL', 'WARRANTY', 'OTHER'];

  documents = signal<DocumentResponse[]>([]);
  selectedCategory = signal<string>('');
  loading = signal<boolean>(false);

  showUploadModal = signal<boolean>(false);
  uploadTitle: string = '';
  uploadCategory: string = 'FINANCE';
  selectedFile: File | null = null;
  uploading = signal<boolean>(false);
  uploadError = signal<string>('');

  activeInspectionDoc = signal<DocumentResponse | null>(null);
  chunks = signal<DocumentChunk[]>([]);
  loadingChunks = signal<boolean>(false);

  ngOnInit() {
    this.loadDocuments();
  }

  loadDocuments() {
    this.loading.set(true);
    this.documentService.getDocuments(0, 50).subscribe({
      next: (res) => {
        if (res.success && res.data) {
          this.documents.set(res.data.content);
        }
        this.loading.set(false);
      },
      error: () => this.loading.set(false)
    });
  }

  filterCategory(cat: string) {
    this.selectedCategory.set(cat);
  }

  filteredDocuments(): DocumentResponse[] {
    const cat = this.selectedCategory();
    if (!cat) return this.documents();
    return this.documents().filter(d => d.category === cat);
  }

  onFileSelected(event: any) {
    const file = event.target?.files?.[0];
    if (file) {
      this.selectedFile = file;
      if (!this.uploadTitle) {
        this.uploadTitle = file.name.replace(/\.[^/.]+$/, '');
      }
    }
  }

  submitUpload() {
    if (!this.selectedFile) return;

    this.uploading.set(true);
    this.uploadError.set('');

    const formData = new FormData();
    formData.append('file', this.selectedFile);
    formData.append('title', this.uploadTitle || this.selectedFile.name);
    formData.append('category', this.uploadCategory || 'FINANCIAL');

    this.documentService.uploadDocument(formData).subscribe({
      next: (res) => {
        this.uploading.set(false);
        if (res.success && res.data) {
          this.documents.update(docs => [res.data, ...docs]);
          this.showUploadModal.set(false);
          this.uploadTitle = '';
          this.selectedFile = null;
        }
      },
      error: (err) => {
        this.uploading.set(false);
        this.uploadError.set(err.error?.detail || err.error?.message || 'Failed to upload document');
      }
    });
  }

  inspectChunks(doc: DocumentResponse) {
    this.activeInspectionDoc.set(doc);
    this.loadingChunks.set(true);
    this.documentService.getDocumentChunks(doc.id).subscribe({
      next: (res) => {
        if (res.success && res.data) {
          this.chunks.set(res.data);
        }
        this.loadingChunks.set(false);
      },
      error: () => this.loadingChunks.set(false)
    });
  }

  download(doc: DocumentResponse) {
    this.documentService.downloadDocument(doc.id).subscribe((blob) => {
      const url = window.URL.createObjectURL(blob);
      const a = document.createElement('a');
      a.href = url;
      a.download = doc.originalFilename || 'document';
      a.click();
      window.URL.revokeObjectURL(url);
    });
  }

  deleteDoc(id: string) {
    if (confirm('Are you sure you want to delete this document and its vector embeddings?')) {
      this.documentService.deleteDocument(id).subscribe(() => {
        this.documents.update(docs => docs.filter(d => d.id !== id));
        if (this.activeInspectionDoc()?.id === id) {
          this.activeInspectionDoc.set(null);
        }
      });
    }
  }

  getStatusClass(status: string): string {
    switch (status) {
      case 'PROCESSED':
      case 'STORED':
        return 'status-processed';
      case 'PROCESSING':
      case 'PENDING':
        return 'status-processing';
      default:
        return 'status-failed';
    }
  }

  formatBytes(bytes: number): string {
    if (!bytes || bytes === 0) return '0 B';
    const k = 1024;
    const sizes = ['B', 'KB', 'MB', 'GB'];
    const i = Math.floor(Math.log(bytes) / Math.log(k));
    return parseFloat((bytes / Math.pow(k, i)).toFixed(1)) + ' ' + sizes[i];
  }
}
