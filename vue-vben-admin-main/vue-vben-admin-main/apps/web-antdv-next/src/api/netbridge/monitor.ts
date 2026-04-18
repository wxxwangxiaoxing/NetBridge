import { requestClient } from '#/api/request';

import type {
  PageResult,
  ServerMetricItem,
  ServiceMetricItem,
} from './types';

export async function getServerMetricList(params: Record<string, any>) {
  return requestClient.get<PageResult<ServerMetricItem>>('/monitor/server/list', {
    params,
  });
}

export async function getServiceMetricList(params: Record<string, any>) {
  return requestClient.get<PageResult<ServiceMetricItem>>('/monitor/service/list', {
    params,
  });
}
