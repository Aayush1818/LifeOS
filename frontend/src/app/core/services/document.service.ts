import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { ApiResponse, DocumentChunk, DocumentResponse, PageResponse } from '../models/api.models';

@Injectable({
  providedIn: 'root'
})
export class DocumentService {
  private http = inject(HttpClient);

  getDocuments(page: number = 0, size: number = 20, category?: string): Observable<ApiResponse<PageResponse<DocumentResponse>>> {
    let params = new HttpParams().set('page', page).set('size', size);
    if (category) {
      params = params.set('category', category);
    }
    return this.http.get<ApiResponse<PageResponse<DocumentResponse>>>('/api/v1/documents', { params });
  }

  uploadDocument(formData: FormData): Observable<ApiResponse<DocumentResponse>> {
    return this.http.post<ApiResponse<DocumentResponse>>('/api/v1/documents/upload', formData);
  }

  getDocument(id: string): Observable<ApiResponse<DocumentResponse>> {
    return this.http.get<ApiResponse<DocumentResponse>>(`/api/v1/documents/${id}`);
  }

  getDocumentChunks(id: string): Observable<ApiResponse<DocumentChunk[]>> {
    return this.http.get<ApiResponse<DocumentChunk[]>>(`/api/v1/documents/${id}/chunks`);
  }

  downloadDocument(id: string): Observable<Blob> {
    return this.http.get(`/api/v1/documents/${id}/download`, { responseType: 'blob' });
  }

  deleteDocument(id: string): Observable<ApiResponse<void>> {
    return this.http.delete<ApiResponse<void>>(`/api/v1/documents/${id}`);
  }
}
