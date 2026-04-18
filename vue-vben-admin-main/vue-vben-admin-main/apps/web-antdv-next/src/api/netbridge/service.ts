import { requestClient } from '#/api/request';

import type { PageResult, ServiceDetailItem, ServiceItem } from './types';

export interface ServicePayload {
  accessType: string;
  domain?: string;
  enabled?: number;
  healthStatus?: string;
  id?: number;
  name: string;
  port: number;
  protocol: string;
  serverId: number;
  status?: string;
  targetHost?: string;
  type: string;
  accessUrl?: string;
  remark?: string;
}

export async function getServiceList(params: Record<string, any>) {
  return requestClient.get<PageResult<ServiceItem>>('/service/list', { params });
}

export async function getServiceDetail(id: number) {
  return requestClient.get<ServiceDetailItem>(`/service/${id}/detail`);
}

export async function createService(data: ServicePayload) {
  return requestClient.post<ServiceItem>('/service/create', data);
}

export async function updateService(data: ServicePayload) {
  return requestClient.put<ServiceItem>('/service/update', data);
}

export async function deleteService(id: number) {
  return requestClient.delete<boolean>(`/service/${id}`);
}

export async function startService(id: number) {
  return requestClient.post(`/service/${id}/start`);
}

export async function stopService(id: number) {
  return requestClient.post(`/service/${id}/stop`);
}

export async function restartService(id: number) {
  return requestClient.post(`/service/${id}/restart`);
}

export async function checkService(id: number) {
  return requestClient.get(`/service/${id}/check`);
}
