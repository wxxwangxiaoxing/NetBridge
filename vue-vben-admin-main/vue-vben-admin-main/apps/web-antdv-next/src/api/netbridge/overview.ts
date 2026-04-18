import { requestClient } from '#/api/request';

import type { LogItem, PageResult, ServerItem, ServiceItem, TaskItem } from './types';

export async function getOverviewData() {
  const [
    serverPage,
    onlineServerPage,
    servicePage,
    runningTaskPage,
    failedTaskPage,
    recentTaskPage,
    errorLogPage,
  ] = await Promise.all([
    requestClient.get<PageResult<ServerItem>>('/server/list', { params: { pageNo: 1, pageSize: 1 } }),
    requestClient.get<PageResult<ServerItem>>('/server/list', { params: { pageNo: 1, pageSize: 1, status: 'online' } }),
    requestClient.get<PageResult<ServiceItem>>('/service/list', { params: { pageNo: 1, pageSize: 1 } }),
    requestClient.get<PageResult<TaskItem>>('/task/list', { params: { pageNo: 1, pageSize: 1, status: 'running' } }),
    requestClient.get<PageResult<TaskItem>>('/task/list', { params: { pageNo: 1, pageSize: 5, status: 'failed' } }),
    requestClient.get<PageResult<TaskItem>>('/task/list', { params: { pageNo: 1, pageSize: 6 } }),
    requestClient.get<PageResult<LogItem>>('/log/list', { params: { pageNo: 1, pageSize: 6, level: 'ERROR' } }),
  ]);

  return {
    failedTasks: failedTaskPage.list,
    recentTasks: recentTaskPage.list,
    summary: {
      onlineServers: onlineServerPage.total,
      runningTasks: runningTaskPage.total,
      services: servicePage.total,
      servers: serverPage.total,
    },
    urgentLogs: errorLogPage.list,
  };
}
