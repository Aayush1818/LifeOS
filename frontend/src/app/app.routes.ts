import { Routes } from '@angular/router';
import { authGuard } from './core/guards/auth.guard';
import { AuthComponent } from './features/auth/auth.component';
import { ShellComponent } from './layout/shell.component';
import { DashboardComponent } from './features/dashboard/dashboard.component';
import { AssistantComponent } from './features/assistant/assistant.component';
import { DocumentsComponent } from './features/documents/documents.component';
import { SearchComponent } from './features/search/search.component';
import { FinanceComponent } from './features/finance/finance.component';
import { LoansInsuranceComponent } from './features/loans/loans-insurance.component';
import { LifeOperationsComponent } from './features/operations/life-operations.component';
import { RemindersComponent } from './features/reminders/reminders.component';

export const routes: Routes = [
  {
    path: 'auth',
    component: AuthComponent
  },
  {
    path: '',
    component: ShellComponent,
    canActivate: [authGuard],
    children: [
      { path: '', redirectTo: 'dashboard', pathMatch: 'full' },
      { path: 'dashboard', component: DashboardComponent },
      { path: 'assistant', component: AssistantComponent },
      { path: 'reminders', component: RemindersComponent },
      { path: 'documents', component: DocumentsComponent },
      { path: 'search', component: SearchComponent },
      { path: 'finance', component: FinanceComponent },
      { path: 'loans-insurance', component: LoansInsuranceComponent },
      { path: 'life-operations', component: LifeOperationsComponent }
    ]
  },
  {
    path: '**',
    redirectTo: ''
  }
];
