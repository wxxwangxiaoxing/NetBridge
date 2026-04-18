import { requestClient } from '#/api/request';

import type { LogItem, OperationLogItem, PageResult } from './types';

export async function getLogList(params: Record<string, any>) {
  return requestClient.get<PageResult<LogItem>>('/log/list', { params });
}

export async function getOperationLogList(params: Record<string, any>) {
  return requestClient.get<PageResult<OperationLogItem>>('/log/operation/list', {
    params,
  });
}
