import { Injectable, inject, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, tap } from 'rxjs';
import {
  ActionExecutionResponse,
  ApiResponse,
  AssistantCitation,
  AssistantMessageResponse,
  ChatMessage,
  Conversation,
  PageResponse,
  PendingAction
} from '../models/api.models';

@Injectable({
  providedIn: 'root'
})
export class AssistantService {
  private http = inject(HttpClient);

  readonly conversations = signal<Conversation[]>([]);
  readonly activeConversation = signal<Conversation | null>(null);
  readonly messages = signal<ChatMessage[]>([]);
  readonly pendingActions = signal<PendingAction[]>([]);
  readonly currentCitations = signal<AssistantCitation[]>([]);
  readonly isLoading = signal<boolean>(false);

  loadConversations(): Observable<ApiResponse<PageResponse<Conversation>>> {
    return this.http.get<ApiResponse<PageResponse<Conversation>>>('/api/v1/assistant/conversations?page=0&size=50').pipe(
      tap(res => {
        if (res.success && res.data) {
          this.conversations.set(res.data.content);
        }
      })
    );
  }

  createConversation(title: string = 'New Conversation'): Observable<ApiResponse<Conversation>> {
    return this.http.post<ApiResponse<Conversation>>('/api/v1/assistant/conversations', { title }).pipe(
      tap(res => {
        if (res.success && res.data) {
          this.conversations.update(list => [res.data, ...list]);
          this.activeConversation.set(res.data);
          this.messages.set([]);
          this.currentCitations.set([]);
        }
      })
    );
  }

  selectConversation(conv: Conversation): Observable<ApiResponse<any>> {
    this.activeConversation.set(conv);
    this.isLoading.set(true);
    return this.http.get<ApiResponse<any>>(`/api/v1/assistant/conversations/${conv.id}`).pipe(
      tap({
        next: (res) => {
          if (res.success && res.data) {
            const rawMessages = res.data.messages || [];
            this.messages.set(rawMessages.map((m: any) => ({
              id: m.id,
              role: m.senderRole || m.role,
              content: m.content,
              createdAt: m.createdAt,
              toolCalls: m.toolCalls
            })));
            this.currentCitations.set(res.data.citations || []);
          }
          this.isLoading.set(false);
        },
        error: () => this.isLoading.set(false)
      })
    );
  }

  deleteConversation(id: string): Observable<ApiResponse<void>> {
    return this.http.delete<ApiResponse<void>>(`/api/v1/assistant/conversations/${id}`).pipe(
      tap(() => {
        this.conversations.update(list => list.filter(c => c.id !== id));
        if (this.activeConversation()?.id === id) {
          this.activeConversation.set(null);
          this.messages.set([]);
          this.currentCitations.set([]);
        }
      })
    );
  }

  sendMessage(conversationId: string, content: string): Observable<ApiResponse<AssistantMessageResponse>> {
    this.isLoading.set(true);

    // Optimistically add user message
    const tempUserMsg: ChatMessage = {
      role: 'USER',
      content,
      createdAt: new Date().toISOString()
    };
    this.messages.update(msgs => [...msgs, tempUserMsg]);

    return this.http.post<ApiResponse<AssistantMessageResponse>>(
      `/api/v1/assistant/conversations/${conversationId}/messages`,
      { content, retrievalMode: 'HYBRID', topK: 5, minScore: 0.01 }
    ).pipe(
      tap({
        next: (res) => {
          if (res.success && res.data) {
            const assistantMsg = res.data.assistantMessage;
            this.messages.update(msgs => [...msgs, assistantMsg]);

            if (res.data.citations && res.data.citations.length > 0) {
              this.currentCitations.set(res.data.citations);
            }

            if (res.data.pendingAction) {
              this.pendingActions.update(actions => [res.data.pendingAction!, ...actions.filter(a => a.id !== res.data.pendingAction!.id)]);
            }
          }
          this.isLoading.set(false);
        },
        error: () => this.isLoading.set(false)
      })
    );
  }

  loadPendingActions(): Observable<ApiResponse<PendingAction[]>> {
    return this.http.get<ApiResponse<PendingAction[]>>('/api/v1/assistant/actions/pending').pipe(
      tap(res => {
        if (res.success && res.data) {
          this.pendingActions.set(res.data);
        }
      })
    );
  }

  confirmAction(actionId: string): Observable<ApiResponse<ActionExecutionResponse>> {
    return this.http.post<ApiResponse<ActionExecutionResponse>>(`/api/v1/assistant/actions/${actionId}/confirm`, {}).pipe(
      tap(res => {
        if (res.success) {
          this.pendingActions.update(list => list.filter(a => a.id !== actionId));
        }
      })
    );
  }

  rejectAction(actionId: string): Observable<ApiResponse<ActionExecutionResponse>> {
    return this.http.post<ApiResponse<ActionExecutionResponse>>(`/api/v1/assistant/actions/${actionId}/reject`, {}).pipe(
      tap(res => {
        if (res.success) {
          this.pendingActions.update(list => list.filter(a => a.id !== actionId));
        }
      })
    );
  }
}
