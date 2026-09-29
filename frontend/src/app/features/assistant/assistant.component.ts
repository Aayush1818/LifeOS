import { Component, ElementRef, OnInit, ViewChild, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { AssistantService } from '../../core/services/assistant.service';
import {
  AssistantCitation,
  ChatMessage,
  Conversation,
  PendingAction
} from '../../core/models/api.models';

@Component({
  selector: 'app-assistant',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <div class="assistant-container">
      <!-- Left: Conversation Sidebar -->
      <aside class="conversation-sidebar glass-panel">
        <div class="sidebar-top">
          <button (click)="startNewChat()" class="btn btn-primary new-chat-btn">
            <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M12 5v14"/>
              <path d="M5 12h14"/>
            </svg>
            <span>New Chat</span>
          </button>
        </div>

        <div class="conversation-list">
          <div class="list-label">Recent Sessions</div>
          @if (assistantService.conversations().length === 0) {
            <div class="empty-sessions">No conversations yet</div>
          }
          @for (conv of assistantService.conversations(); track conv.id) {
            <div
              class="conversation-item"
              [class.active]="assistantService.activeConversation()?.id === conv.id"
              (click)="selectConversation(conv)">
              <div class="conv-title">{{ conv.title || 'Untitled Session' }}</div>
              <button (click)="deleteConversation($event, conv.id)" class="btn-icon delete-btn" title="Delete session">
                <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <path d="M3 6h18"/>
                  <path d="M19 6v14c0 1-1 2-2 2H7c-1 0-2-1-2-2V6"/>
                  <path d="M8 6V4c0-1 1-2 2-2h4c1 0 2 1 2 2v2"/>
                </svg>
              </button>
            </div>
          }
        </div>
      </aside>

      <!-- Center: Main Chat Feed -->
      <section class="chat-main">
        <!-- Header -->
        <header class="chat-header">
          <div class="header-left">
            <h1 class="chat-title">{{ assistantService.activeConversation()?.title || 'AI Copilot' }}</h1>
            <span class="badge badge-accent">Phase 14 Agentic</span>
          </div>
          <div class="header-actions">
            @if (assistantService.currentCitations().length > 0) {
              <button (click)="toggleCitationsDrawer()" class="btn btn-sm btn-secondary">
                <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <path d="M4 19.5v-15A2.5 2.5 0 0 1 6.5 2H20v20H6.5a2.5 2.5 0 0 1-2.5-2.5Z"/>
                </svg>
                <span>Sources ({{ assistantService.currentCitations().length }})</span>
              </button>
            }
          </div>
        </header>

        <!-- Message History Feed -->
        <div class="chat-feed" #feedContainer>
          @if (assistantService.messages().length === 0 && !assistantService.isLoading()) {
            <div class="welcome-hero">
              <div class="hero-icon">
                <svg width="36" height="36" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <path d="m12 3-1.9 5.8a2 2 0 0 1-1.3 1.3L3 12l5.8 1.9a2 2 0 0 1 1.3 1.3L12 21l1.9-5.8a2 2 0 0 1 1.3-1.3L21 12l-5.8-1.9a2 2 0 0 1-1.3-1.3Z"/>
                </svg>
              </div>
              <h2>How can I assist you with LifeOS today?</h2>
              <p>Grounded answers backed by RAG retrieval and safe human-in-the-loop tool calling.</p>

              <div class="prompt-starters">
                <button (click)="sendPreset('Summarize my active loans, outstanding balance, and monthly EMI.')" class="prompt-chip">
                  💰 Summarize active loans & EMIs
                </button>
                <button (click)="sendPreset('What insurance policies are due for renewal in the next 60 days?')" class="prompt-chip">
                  🛡️ Upcoming insurance renewals
                </button>
                <button (click)="sendPreset('What is my total monthly expense and net savings this month?')" class="prompt-chip">
                  📊 Monthly expense & savings rate
                </button>
                <button (click)="sendPreset('Show my upcoming doctor appointments and healthcare visits.')" class="prompt-chip">
                  🩺 Upcoming doctor appointments
                </button>
              </div>
            </div>
          }

          @for (msg of assistantService.messages(); track $index) {
            <div class="message-row" [class.user-row]="msg.role === 'USER'">
              <div class="message-bubble" [class.user-bubble]="msg.role === 'USER'">
                <div class="message-meta">
                  <span class="sender-name">{{ msg.role === 'USER' ? 'You' : 'LifeOS Assistant' }}</span>
                </div>
                <div class="message-text" [innerHTML]="formatMessage(msg.content)"></div>

                <!-- Tool Calls Summary if executed -->
                @if (msg.toolCalls && msg.toolCalls.length > 0) {
                  <div class="tool-calls-badge">
                    <span class="mono">⚡ Tools Executed:</span>
                    @for (tool of msg.toolCalls; track $index) {
                      <span class="tool-tag">{{ tool.name || tool.toolName }}</span>
                    }
                  </div>
                }
              </div>
            </div>
          }

          <!-- Pending Action HITL Confirmation Barrier Card -->
          @for (action of assistantService.pendingActions(); track action.id) {
            @if (action.status === 'PENDING') {
              <div class="hitl-action-card glass-card">
                <div class="action-header">
                  <div class="action-icon">
                    <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                      <polygon points="12 2 15.09 8.26 22 9.27 17 14.14 18.18 21.02 12 17.77 5.82 21.02 7 14.14 2 9.27 8.91 8.26 12 2"/>
                    </svg>
                  </div>
                  <div>
                    <div class="action-title">Human-in-the-Loop Confirmation Required</div>
                    <div class="action-subtitle">Agent proposing state mutation via <code>{{ action.toolName }}</code></div>
                  </div>
                </div>

                <div class="action-details">
                  <div class="param-group">
                    <div class="param-label">Proposed Action:</div>
                    <div class="param-desc">{{ action.prompt || 'Confirm state change' }}</div>
                  </div>
                  @if (action.parameters) {
                    <div class="parameters-box">
                      <div class="param-label">Parameters:</div>
                      <pre class="params-json">{{ action.parameters | json }}</pre>
                    </div>
                  }
                </div>

                <div class="action-actions">
                  <button
                    [disabled]="actionProcessing()"
                    (click)="confirmPendingAction(action.id)"
                    class="btn btn-sm btn-primary">
                    <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                      <polyline points="20 6 9 17 4 12"/>
                    </svg>
                    <span>Approve & Execute</span>
                  </button>

                  <button
                    [disabled]="actionProcessing()"
                    (click)="rejectPendingAction(action.id)"
                    class="btn btn-sm btn-danger">
                    <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                      <line x1="18" y1="6" x2="6" y2="18"/>
                      <line x1="6" y1="6" x2="18" y2="18"/>
                    </svg>
                    <span>Reject</span>
                  </button>
                </div>
              </div>
            }
          }

          @if (assistantService.isLoading()) {
            <div class="message-row">
              <div class="message-bubble assistant-bubble loading-bubble">
                <div class="typing-indicator">
                  <span></span>
                  <span></span>
                  <span></span>
                </div>
                <span class="loading-label">Synthesizing grounded response...</span>
              </div>
            </div>
          }
        </div>

        <!-- Chat Input Bar -->
        <div class="chat-input-area">
          <form (ngSubmit)="sendMessage()" class="chat-form">
            <input
              type="text"
              [(ngModel)]="userInput"
              name="userInput"
              placeholder="Ask anything about your finances, documents, healthcare, or loans..."
              [disabled]="assistantService.isLoading()"
              class="form-input chat-input"
              autocomplete="off"
            />
            <button
              type="submit"
              [disabled]="!userInput.trim() || assistantService.isLoading()"
              class="btn btn-primary send-btn">
              <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <line x1="22" y1="2" x2="11" y2="13"/>
                <polygon points="22 2 15 22 11 13 2 9 22 2"/>
              </svg>
            </button>
          </form>
        </div>
      </section>

      <!-- Right: Citations Drawer Flyout -->
      @if (showCitationsDrawer()) {
        <aside class="citations-drawer glass-panel">
          <div class="drawer-header">
            <h3>Verified Citations</h3>
            <button (click)="showCitationsDrawer.set(false)" class="btn-icon">
              <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <line x1="18" y1="6" x2="6" y2="18"/>
                <line x1="6" y1="6" x2="18" y2="18"/>
              </svg>
            </button>
          </div>

          <div class="citation-list">
            @for (cit of assistantService.currentCitations(); track cit.citationIndex) {
              <div class="citation-card glass-card">
                <div class="citation-card-header">
                  <span class="citation-pill">[{{ cit.citationIndex }}]</span>
                  <span class="doc-title">{{ cit.documentTitle }}</span>
                </div>
                @if (cit.sectionTitle) {
                  <div class="section-crumb">{{ cit.sectionTitle }} · Page {{ cit.pageNumber }}</div>
                }
                <blockquote class="citation-snippet">"{{ cit.snippet }}"</blockquote>
              </div>
            }
          </div>
        </aside>
      }
    </div>
  `,
  styles: [`
    .assistant-container {
      display: flex;
      height: 100%;
      overflow: hidden;
    }

    .conversation-sidebar {
      width: 280px;
      min-width: 280px;
      border-right: 1px solid var(--border-subtle);
      display: flex;
      flex-direction: column;
      background: rgba(11, 16, 30, 0.7);
    }

    .sidebar-top {
      padding: 1.25rem;
      border-bottom: 1px solid var(--border-subtle);
    }

    .new-chat-btn {
      width: 100%;
      justify-content: center;
    }

    .conversation-list {
      flex: 1;
      overflow-y: auto;
      padding: 1rem;
    }

    .list-label {
      font-size: 0.75rem;
      text-transform: uppercase;
      letter-spacing: 0.05em;
      color: var(--text-muted);
      margin-bottom: 0.75rem;
      font-weight: 600;
    }

    .empty-sessions {
      font-size: 0.85rem;
      color: var(--text-dim);
      padding: 1rem 0;
      text-align: center;
    }

    .conversation-item {
      display: flex;
      align-items: center;
      justify-content: space-between;
      padding: 0.65rem 0.85rem;
      border-radius: var(--radius-sm);
      color: var(--text-secondary);
      cursor: pointer;
      margin-bottom: 0.35rem;
      transition: background var(--transition-fast), color var(--transition-fast);
    }

    .conversation-item:hover {
      background: rgba(255, 255, 255, 0.04);
      color: var(--text-main);
    }

    .conversation-item.active {
      background: rgba(99, 102, 241, 0.15);
      color: var(--primary);
      border: 1px solid rgba(99, 102, 241, 0.3);
      font-weight: 500;
    }

    .conv-title {
      overflow: hidden;
      text-overflow: ellipsis;
      white-space: nowrap;
      font-size: 0.875rem;
      flex: 1;
    }

    .delete-btn {
      opacity: 0;
      transition: opacity var(--transition-fast);
    }

    .conversation-item:hover .delete-btn {
      opacity: 1;
    }

    .chat-main {
      flex: 1;
      display: flex;
      flex-direction: column;
      height: 100%;
      position: relative;
    }

    .chat-header {
      padding: 1.25rem 2rem;
      display: flex;
      align-items: center;
      justify-content: space-between;
      border-bottom: 1px solid var(--border-subtle);
      background: rgba(11, 16, 30, 0.4);
    }

    .header-left {
      display: flex;
      align-items: center;
      gap: 1rem;
    }

    .chat-title {
      font-size: 1.25rem;
      margin: 0;
    }

    .chat-feed {
      flex: 1;
      overflow-y: auto;
      padding: 2rem;
      display: flex;
      flex-direction: column;
      gap: 1.5rem;
    }

    .welcome-hero {
      margin: auto;
      max-width: 600px;
      text-align: center;
      padding: 2rem;
    }

    .hero-icon {
      width: 64px;
      height: 64px;
      border-radius: var(--radius-lg);
      background: var(--primary-glow);
      color: var(--primary);
      display: inline-flex;
      align-items: center;
      justify-content: center;
      margin-bottom: 1.5rem;
    }

    .welcome-hero h2 {
      font-size: 1.75rem;
      margin-bottom: 0.75rem;
    }

    .welcome-hero p {
      color: var(--text-secondary);
      margin-bottom: 2rem;
    }

    .prompt-starters {
      display: grid;
      grid-template-columns: 1fr 1fr;
      gap: 0.75rem;
    }

    .prompt-chip {
      background: var(--bg-card);
      border: 1px solid var(--border-subtle);
      border-radius: var(--radius-md);
      padding: 0.85rem 1rem;
      color: var(--text-secondary);
      font-size: 0.85rem;
      text-align: left;
      cursor: pointer;
      transition: border-color var(--transition-fast), color var(--transition-fast), transform var(--transition-fast);
    }

    .prompt-chip:hover {
      border-color: var(--primary);
      color: var(--text-main);
      transform: translateY(-2px);
    }

    .message-row {
      display: flex;
      width: 100%;
    }

    .message-row.user-row {
      justify-content: flex-end;
    }

    .message-bubble {
      max-width: 75%;
      padding: 1.25rem 1.5rem;
      border-radius: var(--radius-lg);
      background: var(--bg-surface);
      border: 1px solid var(--border-subtle);
      line-height: 1.6;
    }

    .user-bubble {
      background: linear-gradient(135deg, rgba(99, 102, 241, 0.25) 0%, rgba(139, 92, 246, 0.2) 100%);
      border-color: rgba(99, 102, 241, 0.4);
      color: var(--text-main);
    }

    .message-meta {
      font-size: 0.75rem;
      font-weight: 600;
      color: var(--text-muted);
      margin-bottom: 0.5rem;
    }

    .tool-calls-badge {
      margin-top: 0.75rem;
      padding-top: 0.5rem;
      border-top: 1px solid var(--border-subtle);
      font-size: 0.75rem;
      display: flex;
      gap: 0.5rem;
      align-items: center;
      flex-wrap: wrap;
    }

    .tool-tag {
      background: rgba(6, 182, 212, 0.15);
      color: var(--accent-cyan);
      padding: 0.2rem 0.5rem;
      border-radius: 4px;
      font-family: 'JetBrains Mono', monospace;
    }

    .hitl-action-card {
      border-left: 4px solid var(--accent-amber);
      padding: 1.5rem;
      margin: 1rem 0;
      max-width: 80%;
      background: rgba(245, 158, 11, 0.06);
    }

    .action-header {
      display: flex;
      align-items: center;
      gap: 0.75rem;
      margin-bottom: 1rem;
    }

    .action-icon {
      color: var(--accent-amber);
    }

    .action-title {
      font-weight: 600;
      color: var(--accent-amber);
      font-size: 0.95rem;
    }

    .action-subtitle {
      font-size: 0.8rem;
      color: var(--text-secondary);
    }

    .action-details {
      margin-bottom: 1.25rem;
    }

    .param-label {
      font-size: 0.75rem;
      font-weight: 600;
      color: var(--text-muted);
      text-transform: uppercase;
      margin-bottom: 0.25rem;
    }

    .param-desc {
      font-size: 0.9rem;
      color: var(--text-main);
    }

    .params-json {
      background: var(--bg-input);
      padding: 0.75rem;
      border-radius: var(--radius-sm);
      font-size: 0.75rem;
      overflow-x: auto;
      color: var(--accent-cyan);
      margin-top: 0.35rem;
    }

    .action-actions {
      display: flex;
      gap: 0.75rem;
    }

    .loading-bubble {
      display: flex;
      align-items: center;
      gap: 1rem;
    }

    .typing-indicator {
      display: flex;
      gap: 4px;
    }

    .typing-indicator span {
      width: 8px;
      height: 8px;
      background: var(--primary);
      border-radius: 50%;
      animation: bounce 1.4s infinite ease-in-out both;
    }

    .typing-indicator span:nth-child(1) { animation-delay: -0.32s; }
    .typing-indicator span:nth-child(2) { animation-delay: -0.16s; }

    @keyframes bounce {
      0%, 80%, 100% { transform: scale(0); }
      40% { transform: scale(1); }
    }

    .chat-input-area {
      padding: 1.25rem 2rem 2rem;
      background: rgba(11, 16, 30, 0.8);
      border-top: 1px solid var(--border-subtle);
    }

    .chat-form {
      display: flex;
      gap: 0.75rem;
    }

    .chat-input {
      flex: 1;
    }

    .send-btn {
      padding: 0 1.5rem;
    }

    .citations-drawer {
      width: 320px;
      min-width: 320px;
      border-left: 1px solid var(--border-subtle);
      background: rgba(11, 16, 30, 0.9);
      padding: 1.5rem;
      overflow-y: auto;
    }

    .drawer-header {
      display: flex;
      align-items: center;
      justify-content: space-between;
      margin-bottom: 1.5rem;
    }

    .citation-card {
      padding: 1rem;
      margin-bottom: 1rem;
    }

    .citation-card-header {
      display: flex;
      align-items: center;
      gap: 0.5rem;
      margin-bottom: 0.5rem;
    }

    .citation-pill {
      font-size: 0.75rem;
      font-weight: 700;
      color: var(--primary);
      background: rgba(99, 102, 241, 0.15);
      padding: 0.15rem 0.45rem;
      border-radius: 4px;
    }

    .doc-title {
      font-size: 0.85rem;
      font-weight: 600;
      overflow: hidden;
      text-overflow: ellipsis;
      white-space: nowrap;
    }

    .section-crumb {
      font-size: 0.75rem;
      color: var(--text-muted);
      margin-bottom: 0.5rem;
    }

    .citation-snippet {
      font-size: 0.8rem;
      color: var(--text-secondary);
      font-style: italic;
      line-height: 1.4;
      border-left: 2px solid var(--border-light);
      padding-left: 0.5rem;
      margin: 0;
    }
  `]
})
export class AssistantComponent implements OnInit {
  assistantService = inject(AssistantService);

  userInput: string = '';
  showCitationsDrawer = signal<boolean>(false);
  actionProcessing = signal<boolean>(false);

  @ViewChild('feedContainer') private feedContainer!: ElementRef;

  ngOnInit() {
    this.assistantService.loadConversations().subscribe();
    this.assistantService.loadPendingActions().subscribe();
  }

  selectConversation(conv: Conversation) {
    this.assistantService.selectConversation(conv).subscribe(() => {
      this.scrollToBottom();
    });
  }

  startNewChat() {
    this.assistantService.createConversation('New Chat').subscribe(() => {
      this.scrollToBottom();
    });
  }

  deleteConversation(event: Event, id: string) {
    event.stopPropagation();
    if (confirm('Delete this conversation?')) {
      this.assistantService.deleteConversation(id).subscribe();
    }
  }

  sendPreset(prompt: string) {
    this.userInput = prompt;
    this.sendMessage();
  }

  sendMessage() {
    if (!this.userInput.trim() || this.assistantService.isLoading()) return;

    const content = this.userInput.trim();
    this.userInput = '';

    let activeId = this.assistantService.activeConversation()?.id;

    if (!activeId) {
      // Auto-create conversation first
      this.assistantService.createConversation(content.slice(0, 30) + '...').subscribe(res => {
        if (res.success && res.data) {
          this.executeSendMessage(res.data.id, content);
        }
      });
    } else {
      this.executeSendMessage(activeId, content);
    }
  }

  private executeSendMessage(convId: string, content: string) {
    this.scrollToBottom();
    this.assistantService.sendMessage(convId, content).subscribe({
      next: () => {
        this.scrollToBottom();
      },
      error: () => {
        this.scrollToBottom();
      }
    });
  }

  confirmPendingAction(actionId: string) {
    this.actionProcessing.set(true);
    this.assistantService.confirmAction(actionId).subscribe({
      next: () => {
        this.actionProcessing.set(false);
        this.assistantService.messages.update(msgs => [
          ...msgs,
          {
            role: 'SYSTEM',
            content: 'Action confirmed and executed successfully by domain service.',
            createdAt: new Date().toISOString()
          }
        ]);
        this.scrollToBottom();
      },
      error: () => this.actionProcessing.set(false)
    });
  }

  rejectPendingAction(actionId: string) {
    this.actionProcessing.set(true);
    this.assistantService.rejectAction(actionId).subscribe({
      next: () => {
        this.actionProcessing.set(false);
        this.assistantService.messages.update(msgs => [
          ...msgs,
          {
            role: 'SYSTEM',
            content: 'Proposed action was rejected.',
            createdAt: new Date().toISOString()
          }
        ]);
        this.scrollToBottom();
      },
      error: () => this.actionProcessing.set(false)
    });
  }

  toggleCitationsDrawer() {
    this.showCitationsDrawer.update(v => !v);
  }

  formatMessage(content: string): string {
    if (!content) return '';
    // Format [1], [2] citations into clickable tags
    return content.replace(/\[(\d+)\]/g, '<span class="citation-pill">[$1]</span>');
  }

  private scrollToBottom() {
    setTimeout(() => {
      if (this.feedContainer?.nativeElement) {
        this.feedContainer.nativeElement.scrollTop = this.feedContainer.nativeElement.scrollHeight;
      }
    }, 100);
  }
}
