export interface User {
  id: string;
  username: string;
  fullName: string;
  email: string;
  active: boolean;
  roles: string[];
}

export interface AuthResponse {
  token: string;
  tokenType: string;
  userId: string;
  username: string;
  fullName: string;
  email: string;
  roles: string[];
}

export interface Transaction {
  id: string;
  externalReference: string;
  accountReference: string;
  amount: number;
  currency: string;
  transactionType: string;
  channel: string;
  status: string;
  merchantOrDestination?: string;
  failureReason?: string;
  transactionTimestamp: string;
  createdAt: string;
  updatedAt: string;
}

export interface CaseNote {
  id: string;
  caseId: string;
  authorId: string;
  authorUsername: string;
  authorFullName: string;
  noteText: string;
  createdAt: string;
}

export interface ExceptionCase {
  id: string;
  caseNumber: string;
  transaction?: Transaction;
  ruleId?: string;
  ruleCode?: string;
  category: string;
  severity: 'LOW' | 'MEDIUM' | 'HIGH' | 'CRITICAL';
  status: 'OPEN' | 'IN_REVIEW' | 'RESOLVED' | 'REOPENED';
  assignedAnalystId?: string;
  assignedAnalystUsername?: string;
  assignedAnalystFullName?: string;
  resolutionSummary?: string;
  resolutionTimestamp?: string;
  version: number;
  notes: CaseNote[];
  createdAt: string;
  updatedAt: string;
}

export interface CaseTimelineEvent {
  eventType: string;
  actorUsername: string;
  description: string;
  timestamp: string;
  payload?: any;
}

export interface AuditEvent {
  id: string;
  actorId?: string;
  actorUsername: string;
  actorRole: string;
  action: string;
  entityType: string;
  entityId: string;
  details?: string;
  correlationId?: string;
  status: string;
  createdAt: string;
}

export interface ValidationRule {
  id: string;
  ruleCode: string;
  ruleName: string;
  description?: string;
  category: string;
  severity: 'LOW' | 'MEDIUM' | 'HIGH' | 'CRITICAL';
  parameters?: string;
  active: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface DashboardSummary {
  totalTransactions: number;
  successfulTransactions: number;
  failedTransactions: number;
  openExceptions: number;
  inReviewExceptions: number;
  resolvedExceptions: number;
  exceptionRatePercentage: number;
  averageResolutionTimeHours: number;
  casesBySeverity: Record<string, number>;
  exceptionsByCategory: Record<string, number>;
  transactionsByStatus: Record<string, number>;
}

export interface TrendPoint {
  date: string;
  total: number;
  completed: number;
  failed: number;
  exceptions: number;
}

export interface PagedResponse<T> {
  content: T[];
  pageNumber: number;
  pageSize: number;
  totalElements: number;
  totalPages: number;
  isFirst: boolean;
  isLast: boolean;
}

export interface ApiResponse<T> {
  success: boolean;
  message: string;
  data: T;
  timestamp: string;
  correlationId?: string;
}

export interface RowErrorDetail {
  rowNumber: number;
  externalReference: string;
  errorMessage: string;
}

export interface ImportResult {
  totalRowsProcessed: number;
  successfullyImportedCount: number;
  duplicateSkippedCount: number;
  failedValidationCount: number;
  exceptionCasesTriggeredCount: number;
  rowErrors: RowErrorDetail[];
  idempotencyKey?: string;
}
