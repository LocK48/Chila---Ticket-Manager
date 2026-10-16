export type User = {
  id: string;
  email: string;
  displayName: string;
  createdAt: string;
};

export type MembershipRole = "OWNER" | "ADMIN" | "MEMBER";

export type Workspace = {
  id: string;
  name: string;
  description: string | null;
  role: MembershipRole;
  createdAt: string;
  updatedAt: string;
};

export type WorkspaceMember = {
  userId: string;
  email: string;
  displayName: string;
  role: MembershipRole;
  joinedAt: string;
};

export type ProjectStatus = "ACTIVE" | "ARCHIVED";

export type Project = {
  id: string;
  workspaceId: string;
  createdBy: string;
  name: string;
  key: string;
  description: string | null;
  status: ProjectStatus;
  createdAt: string;
  updatedAt: string;
};

export type IssueType = "TASK" | "BUG" | "STORY";
export type IssueStatus = "TODO" | "IN_PROGRESS" | "IN_REVIEW" | "DONE";
export type ProjectDashboard = {
  totalIssues: number;
  overdueIssues: number;
  issuesByStatus: Record<IssueStatus, number>;
  completedLast30Days: number;
  averageLeadTimeDays: number | null;
  averageCycleTimeDays: number | null;
  sprintVelocity: SprintVelocityPoint[];
};
export type SprintVelocityPoint = {
  sprintId: string;
  sprintName: string;
  startDate: string | null;
  committedIssues: number | null;
  completedIssues: number | null;
  unfinishedIssues: number | null;
  completionPercentage: number | null;
  historyAvailable: boolean;
};
export type SprintReport = {
  sprintId: string;
  sprintName: string;
  state: "FUTURE" | "ACTIVE" | "CLOSED";
  startDate: string | null;
  endDate: string | null;
  historyAvailable: boolean;
  committedIssues: number | null;
  completedIssues: number | null;
  unfinishedIssues: number | null;
  completionPercentage: number | null;
  burndown: { date: string; scopeIssues: number; remainingIssues: number }[];
  completionIssues: {
    issueId: string;
    issueKey: string;
    issueTitle: string;
    issueStatus: IssueStatus;
  }[];
};
export type IssuePriority = "LOW" | "MEDIUM" | "HIGH" | "URGENT";
export type AutomationAction = "NOTIFY_REPORTER" | "NOTIFY_ASSIGNEE";
export type AutomationRule = {
  id: string;
  projectId: string;
  name: string;
  enabled: boolean;
  targetStatus: IssueStatus;
  action: AutomationAction;
  createdAt: string;
  updatedAt: string;
};
export type AutomationRuleInput = Pick<
  AutomationRule,
  "name" | "enabled" | "targetStatus" | "action"
>;
export type AutomationExecution = {
  id: string;
  ruleId: string | null;
  ruleName: string;
  issueKey: string;
  outcome: "SUCCEEDED" | "SKIPPED";
  detail: string;
  executedAt: string;
};
export type Issue = {
  id: string;
  projectId: string;
  sprintId: string | null;
  epicId: string | null;
  parentIssueId: string | null;
  releaseId: string | null;
  projectKey: string;
  issueKey: string;
  issueNumber: number;
  title: string;
  description: string | null;
  type: IssueType;
  status: IssueStatus;
  priority: IssuePriority;
  reporterId: string;
  reporterName: string;
  assigneeId: string | null;
  assigneeName: string | null;
  dueDate: string | null;
  archivedAt: string | null;
  createdAt: string;
  updatedAt: string;
};
export type IssuePage = {
  items: Issue[];
  page: number;
  size: number;
  totalItems: number;
  totalPages: number;
};
export type Epic = {
  id: string;
  projectId: string;
  name: string;
  summary: string | null;
  color: string;
  totalIssues: number;
  completedIssues: number;
  createdAt: string;
  updatedAt: string;
};
export type EpicInput = { name: string; summary: string | null; color: string };
export type ReleaseState = "UNRELEASED" | "RELEASED" | "ARCHIVED";
export type ProjectRelease = {
  id: string;
  projectId: string;
  name: string;
  description: string | null;
  startDate: string | null;
  releaseDate: string | null;
  state: ReleaseState;
  issueCount: number;
  createdAt: string;
  updatedAt: string;
};
export type ReleaseInput = {
  name: string;
  description: string | null;
  startDate: string | null;
  releaseDate: string | null;
  state: ReleaseState | null;
};
export type IssueLinkType = "BLOCKS" | "DEPENDS_ON" | "RELATES_TO";
export type RelatedIssue = {
  id: string;
  issueKey: string;
  title: string;
  status: string;
};
export type IssueLink = {
  id: string;
  sourceIssueId: string;
  sourceIssueKey: string;
  sourceTitle: string;
  targetIssueId: string;
  targetIssueKey: string;
  targetTitle: string;
  type: IssueLinkType;
  createdAt: string;
};
export type IssueRelationships = {
  parent: RelatedIssue | null;
  children: RelatedIssue[];
  links: IssueLink[];
};
export type IssueComment = {
  id: string;
  issueId: string;
  authorId: string;
  authorName: string;
  body: string;
  createdAt: string;
};
export type IssueActivity = {
  id: string;
  issueId: string;
  actorId: string;
  actorName: string;
  action: string;
  fieldName: string | null;
  oldValue: string | null;
  newValue: string | null;
  createdAt: string;
};
export type Notification = {
  id: string;
  issueId: string | null;
  issueTitle: string | null;
  eventType: string;
  message: string;
  createdAt: string;
  readAt: string | null;
};
export type PageResult<T> = {
  items: T[];
  page: number;
  size: number;
  totalItems: number;
  totalPages: number;
};
export type IssueFilters = {
  status?: IssueStatus;
  priority?: IssuePriority;
  type?: IssueType;
  assigneeId?: string;
  unassigned?: boolean;
  search?: string;
  includeArchived?: boolean;
  sprintId?: string;
  epicId?: string;
  dueFrom?: string;
  dueTo?: string;
  page?: number;
  size?: number;
  sort?: string;
  direction?: "asc" | "desc";
};
export type SavedFilterVisibility = "PRIVATE" | "WORKSPACE";
export type SavedFilter = {
  id: string;
  projectId: string;
  creatorId: string;
  name: string;
  visibility: SavedFilterVisibility;
  status: IssueStatus | null;
  priority: IssuePriority | null;
  type: IssueType | null;
  assigneeId: string | null;
  unassigned: boolean;
  sprintId: string | null;
  epicId: string | null;
  dueFrom: string | null;
  dueTo: string | null;
  search: string | null;
  createdAt: string;
  updatedAt: string;
};
export type SavedFilterInput = Omit<
  SavedFilter,
  "id" | "projectId" | "creatorId" | "createdAt" | "updatedAt"
>;
export type WorkflowTransition = { from: IssueStatus; to: IssueStatus };
export type Workflow = {
  statuses: IssueStatus[];
  transitions: WorkflowTransition[];
};
export type SprintState = "FUTURE" | "ACTIVE" | "CLOSED";
export type Sprint = {
  id: string;
  projectId: string;
  name: string;
  goal: string | null;
  state: SprintState;
  startDate: string | null;
  endDate: string | null;
  createdAt: string;
  updatedAt: string;
};
export type SprintInput = {
  name: string;
  goal: string | null;
  startDate: string | null;
  endDate: string | null;
};

export type RegistrationInput = {
  email: string;
  password: string;
  displayName: string;
  verificationCode: string;
};

export type ApiErrorBody = {
  code?: string;
  message?: string;
  fieldErrors?: Record<string, string>;
};

export type IssueInput = {
  title: string;
  description: string | null;
  type: IssueType;
  status: IssueStatus;
  priority: IssuePriority;
  assigneeId: string | null;
  dueDate: string | null;
};
