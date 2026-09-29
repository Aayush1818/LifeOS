import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { ApiResponse, PageResponse, SearchResultItem } from '../models/api.models';

@Injectable({
  providedIn: 'root'
})
export class SearchService {
  private http = inject(HttpClient);

  search(query: string, entities?: string[], page: number = 0, size: number = 20): Observable<ApiResponse<PageResponse<SearchResultItem>>> {
    let params = new HttpParams()
      .set('query', query)
      .set('page', page)
      .set('size', size);

    if (entities && entities.length > 0) {
      params = params.set('entities', entities.join(','));
    }

    return this.http.get<ApiResponse<PageResponse<SearchResultItem>>>('/api/v1/search', { params });
  }

  suggest(prefix: string): Observable<ApiResponse<string[]>> {
    const params = new HttpParams().set('prefix', prefix);
    return this.http.get<ApiResponse<string[]>>('/api/v1/search/suggest', { params });
  }
}
