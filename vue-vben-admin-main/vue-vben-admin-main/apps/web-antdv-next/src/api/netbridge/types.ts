export interface PageResult<T> {
  list: T[];
  pageNo: number;
  pageSize: number;
  total: number;
}

export interface ServerItem {
  agentId?: string;
  agentVersion?: string;
  displayName?: string;
  hostname: string;
  id: number;
  ip: string;
  lastHeartbeat?: string;
  os?: string;
  remark?: string;
  status: string;
  tailscaleIp?: string;
  userId?: number;
}

export interface ServiceItem {
  accessType: string;
  accessUrl?: string;
  domain?: string;
  enabled?: number;
  healthStatus?: string;
  id: number;
  name: string;
  port: number;
  protocol: string;
  serverId: number;
  status: string;
  targetHost?: string;
  type: string;
}

export interface TaskItem {
  completedAt?: string;
  createdAt?: string;
  errorMessage?: string;
  id: number;
  payload?: string;
  payloadObj?: Record<string, any> | null;
  result?: string;
  resultObj?: Record<string, any> | null;
  retryCount?: number;
  serverId: number;
  serviceId?: number;
  startedAt?: string;
  status: string;
  type: string;
}

export interface LogItem {
  agentId?: string;
  content: string;
  createdAt: string;
  id: number;
  level: string;
  serverId: number;
  serviceId?: number;
  source: string;
  taskId?: number;
}

export interface InstallCommandResult {
  command: string;
  expiresAt: string;
  token: string;
}

export interface InstallTokenItem {
  createdAt?: string;
  expiresAt?: string;
  hostname?: string;
  id: number;
  status: string;
  token: string;
  usedAt?: string;
}

export interface OperationLogItem {
  action: string;
  createdAt: string;
  detail?: string;
  id: number;
  operatorId?: number;
  operatorName?: string;
  resourceId?: string;
  resourceType: string;
  result: string;
}

export interface ServerDetailItem extends ServerItem {
  recentLogTotal?: number;
  recentLogs?: LogItem[];
  recentTaskTotal?: number;
  recentTasks?: TaskItem[];
  serviceTotal?: number;
  services?: ServiceItem[];
}

export interface ServiceDetailItem extends ServiceItem {
  metrics?: ServiceMetricItem[];
  recentLogTotal?: number;
  recentLogs?: LogItem[];
  recentTaskTotal?: number;
  recentTasks?: TaskItem[];
}

export interface ServerMetricItem {
  collectedAt: string;
  cpuUsage?: number;
  diskUsage?: number;
  id: number;
  loadAverage?: number;
  memoryUsage?: number;
  networkInBytes?: number;
  networkOutBytes?: number;
  serverId: number;
}

export interface ServiceMetricItem {
  collectedAt: string;
  errorCount?: number;
  id: number;
  responseTimeMs?: number;
  serverId: number;
  serviceId: number;
  status?: number;
  successCount?: number;
}
