import { requestClient } from '#/api/request';

import type { PageResult, TaskItem } from './types';

export async function getTaskList(params: Record<string, any>) {
  return requestClient.get<PageResult<TaskItem>>('/task/list', { params });
}

export async function retryTask(id: number) {
  return requestClient.post<TaskItem>(`/task/${id}/retry`);
}

export async function cancelTask(id: number) {
  return requestClient.post<TaskItem>(`/task/${id}/cancel`);
}
