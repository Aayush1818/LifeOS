import { Component, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { AuthService } from '../../core/services/auth.service';

@Component({
  selector: 'app-auth',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <div class="auth-container">
      <div class="auth-card glass-card">
        <div class="auth-header">
          <div class="brand-logo">
            <svg width="28" height="28" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <circle cx="12" cy="12" r="10"/>
              <path d="M12 2a14.5 14.5 0 0 0 0 20 14.5 14.5 0 0 0 0-20"/>
              <path d="M2 12h20"/>
            </svg>
          </div>
          <h1 class="auth-title">
            <span class="gradient-text">LifeOS</span>
          </h1>
          <p class="auth-subtitle">AI-Powered Personal Knowledge & Life Management</p>
        </div>

        <div class="auth-tabs">
          <button [class.active]="isLogin()" (click)="isLogin.set(true); error.set('')" class="tab-btn">Sign In</button>
          <button [class.active]="!isLogin()" (click)="isLogin.set(false); error.set('')" class="tab-btn">Create Account</button>
        </div>

        @if (error()) {
          <div class="error-banner">
            <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <circle cx="12" cy="12" r="10"/>
              <line x1="12" y1="8" x2="12" y2="12"/>
              <line x1="12" y1="16" x2="12.01" y2="16"/>
            </svg>
            <span>{{ error() }}</span>
          </div>
        }

        <form (ngSubmit)="onSubmit()" class="auth-form">
          @if (!isLogin()) {
            <div class="name-grid">
              <div class="form-group">
                <label class="form-label">First Name</label>
                <input type="text" [(ngModel)]="firstName" name="firstName" required class="form-input" placeholder="Aayush">
              </div>
              <div class="form-group">
                <label class="form-label">Last Name</label>
                <input type="text" [(ngModel)]="lastName" name="lastName" required class="form-input" placeholder="Raj">
              </div>
            </div>
          }

          <div class="form-group">
            <label class="form-label">Email Address</label>
            <input type="email" [(ngModel)]="email" name="email" required class="form-input" placeholder="you@domain.com">
          </div>

          <div class="form-group">
            <label class="form-label">Password</label>
            <input type="password" [(ngModel)]="password" name="password" required class="form-input" placeholder="••••••••••••">
          </div>

          <button type="submit" [disabled]="loading()" class="btn btn-primary auth-submit-btn">
            @if (loading()) {
              <span>Authenticating...</span>
            } @else {
              <span>{{ isLogin() ? 'Sign In to LifeOS' : 'Create LifeOS Account' }}</span>
            }
          </button>
        </form>

        <div class="auth-footer">
          <p class="security-notice">
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <rect width="18" height="11" x="3" y="11" rx="2" ry="2"/>
              <path d="M7 11V7a5 5 0 0 1 10 0v4"/>
            </svg>
            <span>Zero-trust multi-tenant isolation with HMAC-SHA512 JWT</span>
          </p>
        </div>
      </div>
    </div>
  `,
  styles: [`
    .auth-container {
      min-height: 100vh;
      display: flex;
      align-items: center;
      justify-content: center;
      padding: 1.5rem;
      background: radial-gradient(circle at 50% 20%, rgba(99, 102, 241, 0.15) 0%, transparent 60%),
                  var(--bg-primary);
    }

    .auth-card {
      width: 100%;
      max-width: 440px;
      padding: 2.25rem 2rem;
      border-radius: var(--radius-lg);
      background: rgba(15, 23, 42, 0.85);
      border: 1px solid var(--border-light);
      box-shadow: 0 20px 50px rgba(0, 0, 0, 0.6);
      animation: slideUp 0.35s ease-out;
    }

    .auth-header {
      text-align: center;
      margin-bottom: 1.75rem;
    }

    .brand-logo {
      width: 48px;
      height: 48px;
      margin: 0 auto 0.75rem;
      border-radius: var(--radius-md);
      background: var(--gradient-brand);
      color: #fff;
      display: flex;
      align-items: center;
      justify-content: center;
      box-shadow: 0 4px 18px var(--primary-glow);
    }

    .auth-title {
      font-size: 1.75rem;
      margin-bottom: 0.25rem;
    }

    .auth-subtitle {
      font-size: 0.825rem;
      color: var(--text-muted);
    }

    .auth-tabs {
      display: flex;
      background: rgba(7, 10, 19, 0.6);
      padding: 0.25rem;
      border-radius: var(--radius-sm);
      margin-bottom: 1.5rem;
      border: 1px solid var(--border-subtle);
    }

    .tab-btn {
      flex: 1;
      padding: 0.5rem;
      font-family: 'Outfit', sans-serif;
      font-size: 0.85rem;
      font-weight: 600;
      border: none;
      background: transparent;
      color: var(--text-muted);
      border-radius: 6px;
      cursor: pointer;
      transition: all var(--transition-fast);
    }

    .tab-btn.active {
      background: rgba(99, 102, 241, 0.2);
      color: #ffffff;
      box-shadow: 0 2px 8px rgba(0, 0, 0, 0.2);
    }

    .error-banner {
      display: flex;
      align-items: center;
      gap: 0.5rem;
      background: rgba(244, 63, 94, 0.12);
      border: 1px solid rgba(244, 63, 94, 0.3);
      color: var(--accent-rose);
      padding: 0.65rem 0.85rem;
      border-radius: var(--radius-sm);
      font-size: 0.8rem;
      margin-bottom: 1.25rem;
      animation: fadeIn 0.2s ease-in;
    }

    .name-grid {
      display: grid;
      grid-template-columns: 1fr 1fr;
      gap: 0.75rem;
    }

    .auth-submit-btn {
      width: 100%;
      margin-top: 0.75rem;
      padding: 0.75rem;
      font-size: 0.95rem;
    }

    .auth-footer {
      margin-top: 1.5rem;
      text-align: center;
    }

    .security-notice {
      display: inline-flex;
      align-items: center;
      gap: 0.35rem;
      font-size: 0.725rem;
      color: var(--text-muted);
    }
  `]
})
export class AuthComponent {
  private authService = inject(AuthService);
  private router = inject(Router);

  isLogin = signal<boolean>(true);
  loading = signal<boolean>(false);
  error = signal<string>('');

  email = '';
  password = '';
  firstName = '';
  lastName = '';

  onSubmit() {
    this.error.set('');
    this.loading.set(true);

    if (this.isLogin()) {
      this.authService.login({ email: this.email, password: this.password }).subscribe({
        next: (res) => {
          this.loading.set(false);
          if (res.success) {
            this.router.navigate(['/dashboard']);
          }
        },
        error: (err) => {
          this.loading.set(false);
          this.error.set(err.error?.detail || err.error?.message || 'Invalid credentials. Please try again.');
        }
      });
    } else {
      this.authService.register({
        email: this.email,
        password: this.password,
        firstName: this.firstName,
        lastName: this.lastName
      }).subscribe({
        next: (res) => {
          this.loading.set(false);
          if (res.success) {
            this.router.navigate(['/dashboard']);
          }
        },
        error: (err) => {
          this.loading.set(false);
          this.error.set(err.error?.detail || err.error?.message || 'Failed to create account. Please check your inputs.');
        }
      });
    }
  }
}
