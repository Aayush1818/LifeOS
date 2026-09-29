export interface ApiResponse<T> {
  success: boolean;
  message: string;
  data: T;
  timestamp: string;
}

export interface PageResponse<T> {
  content: T[];
  pageNumber: number;
  pageSize: number;
  totalElements: number;
  totalPages: number;
  last: boolean;
}

export interface ErrorResponse {
  title: string;
  status: number;
  detail: string;
  instance?: string;
  timestamp: string;
  validationErrors?: Array<{ field: string; message: string; rejectedValue?: any }>;
}

export interface User {
  id: string;
  email: string;
  firstName: string;
  lastName: string;
  role: string;
  phone?: string;
}

export interface AuthResponse {
  accessToken: string;
  user: User;
}

export interface LoginRequest {
  email: string;
  password: string;
}

export interface RegisterRequest {
  email: string;
  password: string;
  firstName: string;
  lastName: string;
  phone?: string;
}

// ----------------------------------------------------
// Personal Finance & Budgets
// ----------------------------------------------------
export interface Transaction {
  id: string;
  userId?: string;
  amount: number;
  currency: string;
  transactionType: 'INCOME' | 'EXPENSE';
  category: string;
  transactionDate: string;
  description: string;
  isRecurring?: boolean;
  isRefund?: boolean;
  notes?: string;
  documentId?: string;
}

export interface CreateTransactionRequest {
  amount: number;
  currency?: string;
  transactionType: 'INCOME' | 'EXPENSE';
  category: string;
  transactionDate: string;
  paymentMethod?: string;
  description: string;
  isRecurring?: boolean;
  isRefund?: boolean;
  notes?: string;
}

export interface MonthlySummaryResponse {
  month: number;
  year: number;
  totalIncome: number;
  totalExpenses: number;
  netSavings: number;
  savingsRate: number;
  categoryBreakdown: Record<string, number>;
  topExpenseCategories: Array<{ category: string; amount: number; percentage: number }>;
}

export interface Budget {
  id: string;
  category: string;
  monthlyLimit: number;
  currency: string;
  month: number;
  year: number;
  alertThresholds?: number[];
}

export interface BudgetStatusResponse {
  category: string;
  budgetLimit: number;
  currentSpent: number;
  projectedSpent: number;
  percentageUsed: number;
  status: 'NORMAL' | 'WARNING' | 'CRITICAL' | 'EXCEEDED';
}

export interface RecurringTransaction {
  id: string;
  name: string;
  amount: number;
  currency: string;
  category: string;
  frequency: string;
  nextDueDate: string;
  isActive: boolean;
}

// ----------------------------------------------------
// Loans & Insurance
// ----------------------------------------------------
export interface LoanResponse {
  id: string;
  lenderName: string;
  loanType: string;
  principalAmount: number;
  outstandingBalance: number;
  interestRate: number;
  tenureMonths: number;
  monthlyEmi: number;
  emiDueDay: number;
  status: string;
  startDate?: string;
}

export interface AmortizationEntry {
  installmentNumber: number;
  paymentDate: string;
  emiAmount: number;
  principalComponent: number;
  interestComponent: number;
  remainingBalance: number;
}

export interface PrepaymentSimulationRequest {
  prepaymentAmount: number;
  prepaymentMonth: number;
  prepaymentType: 'REDUCE_TENURE' | 'REDUCE_EMI';
}

export interface PrepaymentSimulationResult {
  originalTenureMonths: number;
  newTenureMonths: number;
  monthsSaved: number;
  originalTotalInterest: number;
  newTotalInterest: number;
  interestSaved: number;
  newMonthlyEmi?: number;
}

export interface InsurancePolicyResponse {
  id: string;
  providerName: string;
  policyNumber: string;
  policyName: string;
  policyType: string;
  coverageAmount: number;
  premiumAmount: number;
  premiumFrequency: string;
  startDate?: string;
  endDate?: string;
  nextRenewalDate: string;
  status: string;
}

export interface InsuranceRenewalResponse {
  id: string;
  providerName: string;
  policyName: string;
  policyType: string;
  coverageAmount: number;
  premiumAmount: number;
  premiumFrequency: string;
  nextRenewalDate: string;
  status: string;
}

// ----------------------------------------------------
// Healthcare
// ----------------------------------------------------
export interface AppointmentResponse {
  id: string;
  doctorName: string;
  medicalSpecialty: string;
  clinicName: string;
  appointmentTime: string;
  reasonForVisit?: string;
  status: string;
  cost?: number;
  notes?: string;
}

export interface BiometricRecord {
  id: string;
  recordedAt: string;
  metricType: string;
  metricValue: number;
  unit: string;
  notes?: string;
}

// ----------------------------------------------------
// Travel
// ----------------------------------------------------
export interface TripResponse {
  id: string;
  tripTitle: string;
  destination: string;
  startDate: string;
  endDate: string;
  status: string;
  totalBudget?: number;
  itineraryItems?: ItineraryItem[];
}

export interface ItineraryItem {
  id: string;
  tripId?: string;
  title: string;
  itemType: 'FLIGHT' | 'HOTEL' | 'ACTIVITY' | 'TRAIN' | 'RENTAL' | 'OTHER' | string;
  startTime: string;
  endTime?: string;
  location?: string;
  confirmationCode?: string;
  cost?: number;
}

// ----------------------------------------------------
// Assets & Warranties
// ----------------------------------------------------
export interface TrackedAsset {
  id: string;
  assetName: string;
  category: string;
  purchaseDate: string;
  purchasePrice: number;
  serialNumber?: string;
  modelNumber?: string;
  warrantyExpiresAt?: string;
  status: string;
}

export interface WarrantyRecord {
  id: string;
  assetId: string;
  assetName?: string;
  provider: string;
  durationMonths: number;
  expiryDate: string;
  termsSummary?: string;
  status: string;
}

// ----------------------------------------------------
// AI Assistant & Safe Tool Orchestration (Phase 13 & 14)
// ----------------------------------------------------
export interface Conversation {
  id: string;
  title: string;
  createdAt: string;
  lastMessageAt: string;
}

export interface ChatMessage {
  id?: string;
  role: 'USER' | 'ASSISTANT' | 'SYSTEM';
  content: string;
  createdAt?: string;
  toolCalls?: any[];
}

export interface AssistantCitation {
  citationIndex: number;
  documentId: string;
  chunkId?: string;
  documentTitle: string;
  pageNumber: number;
  sectionTitle?: string;
  sourceCitation: string;
  snippet: string;
  relevanceScore?: number;
}

export interface PendingAction {
  id: string;
  conversationId: string;
  toolName: string;
  parameters: Record<string, any>;
  prompt: string;
  status: 'PENDING' | 'CONFIRMED' | 'REJECTED' | 'EXPIRED' | 'FAILED';
  expiresAt: string;
  createdAt: string;
}

export interface ActionExecutionResponse {
  actionId: string;
  toolName: string;
  status: string;
  success: boolean;
  result?: any;
  message: string;
  executedAt?: string;
}

export interface AssistantMessageResponse {
  conversationId: string;
  userMessage: ChatMessage;
  assistantMessage: ChatMessage;
  citations: AssistantCitation[];
  grounded: boolean;
  hasRelevantContext: boolean;
  usage?: {
    promptTokens: number;
    completionTokens: number;
    totalTokens: number;
  };
  pendingAction?: PendingAction;
  toolCalls?: Array<{ id: string; name: string; arguments: any }>;
}

// ----------------------------------------------------
// Documents & RAG Ingestion (Phase 4 & 11)
// ----------------------------------------------------
export interface DocumentResponse {
  id: string;
  title: string;
  originalFilename: string;
  mimeType: string;
  fileSize: number;
  category: string;
  documentType: string;
  ingestionStatus: string;
  version: number;
  createdAt: string;
}

export interface DocumentChunk {
  id: string;
  chunkIndex: number;
  pageNumber: number;
  sectionTitle?: string;
  content: string;
  tokenCount: number;
  isActive: boolean;
}

// ----------------------------------------------------
// Unified Search (Phase 10)
// ----------------------------------------------------
export interface SearchResultItem {
  entityType: string;
  entityId: string;
  title: string;
  subtitle?: string;
  contentText?: string;
  categoryOrType?: string;
  status?: string;
  amount?: number;
  currency?: string;
  eventDate?: string;
}

export interface SearchCountSummary {
  totalCount: number;
  countsByEntity: Record<string, number>;
}

// ----------------------------------------------------
// Reminders & Notifications (Phase 16)
// ----------------------------------------------------
export interface Reminder {
  id: string;
  title: string;
  description?: string;
  dueDate: string;
  recurrenceRule?: string;
  status: 'ACTIVE' | 'COMPLETED' | 'DISMISSED';
  priority?: 'LOW' | 'MEDIUM' | 'HIGH' | 'URGENT';
  entityType?: string;
  entityId?: string;
  completedAt?: string;
  createdAt?: string;
  updatedAt?: string;
}

export interface CreateReminderRequest {
  title: string;
  description?: string;
  dueAt?: string;
  dueDate?: string;
  recurrencePattern?: string;
  recurrenceRule?: string;
  priority?: string;
  entityType?: string;
  entityId?: string;
}

export interface UpdateReminderRequest {
  title?: string;
  description?: string;
  dueDate?: string;
  recurrenceRule?: string;
  status?: string;
  priority?: string;
}

export interface NotificationItem {
  id: string;
  title: string;
  message: string;
  notificationType: string;
  channel: string;
  read: boolean;
  readAt?: string;
  actionUrl?: string;
  metadata?: Record<string, any>;
  createdAt: string;
}

export interface NotificationCount {
  unreadCount: number;
}

// ----------------------------------------------------
// Security Audit & Data Portability & Comparison (Phase 17)
// ----------------------------------------------------
export interface AuditLog {
  id: string;
  userId: string;
  eventType: string;
  actionOutcome: 'SUCCESS' | 'FAILURE' | 'WARNING';
  ipAddress?: string;
  userAgent?: string;
  details?: string;
  createdAt: string;
}

export interface ClauseDiff {
  category: string;
  clauseName: string;
  changeType: 'ADDED' | 'REMOVED' | 'MODIFIED' | 'UNCHANGED';
  document1Value: string;
  document2Value: string;
  impact: 'POSITIVE' | 'NEGATIVE' | 'NEUTRAL';
}

export interface PolicyComparisonRequest {
  documentId1: string;
  documentId2: string;
}

export interface PolicyComparisonResponse {
  documentId1: string;
  documentTitle1: string;
  documentId2: string;
  documentTitle2: string;
  summary: string;
  clauseDifferences: ClauseDiff[];
  addedBenefits: string[];
  removedBenefits: string[];
  premiumAnalysis: string;
  overallRecommendation: string;
  comparedAt: string;
}

// ----------------------------------------------------
// Financial Obligations & System Diagnostics (Phase 18)
// ----------------------------------------------------
export interface MonthlyObligationItem {
  id: string;
  title: string;
  category: string;
  dueDate: string;
  amount: number;
  currency: string;
  providerOrLender?: string;
  sourceEntityId?: string;
  status: string;
}

export interface MonthlyObligationSummaryResponse {
  month: number;
  year: number;
  totalObligationAmount: number;
  loanEmisTotal: number;
  insurancePremiumsTotal: number;
  recurringBillsTotal: number;
  tripAllocationsTotal: number;
  projectedIncome: number;
  netSurplusOrDeficit: number;
  currency: string;
  items: MonthlyObligationItem[];
}

export interface ComponentHealth {
  status: 'UP' | 'DOWN' | 'DEGRADED';
  latencyMs?: number;
  details?: string;
}

export interface SystemHealthResponse {
  status: 'UP' | 'DOWN' | 'DEGRADED';
  uptimeSeconds: number;
  timestamp: string;
  components: Record<string, ComponentHealth>;
}

export interface SystemMetricsResponse {
  totalUsers: number;
  totalDocuments: number;
  totalTransactions: number;
  totalLoans: number;
  totalPolicies: number;
  totalTrips: number;
  totalReminders: number;
  diskFreeBytes: number;
  diskTotalBytes: number;
  jvmAvailableProcessors: number;
  jvmUsedMemoryBytes: number;
  jvmMaxMemoryBytes: number;
  timestamp: string;
}

// ----------------------------------------------------
// Proactive Insights & Anomaly Detection (Phase 19)
// ----------------------------------------------------
export interface InsightItem {
  id: string;
  insightType: 'SPENDING_SURGE' | 'BUDGET_DEPLETION' | 'HIGH_INTEREST_LOAN' | 'INSURANCE_GAP' | 'WARRANTY_EXPIRING' | 'SCHEDULE_CONFLICT';
  severity: 'INFO' | 'WARNING' | 'CRITICAL';
  title: string;
  description: string;
  actionType: 'VIEW_BUDGET' | 'SIMULATE_PREPAYMENT' | 'RENEW_POLICY' | 'VIEW_WARRANTY' | 'CHECK_ITINERARY';
  actionPayload?: Record<string, any>;
  isDismissed: boolean;
  isActioned: boolean;
  createdAt: string;
}

export interface InsightSummaryResponse {
  totalActive: number;
  criticalCount: number;
  warningCount: number;
  infoCount: number;
  insights: InsightItem[];
}

