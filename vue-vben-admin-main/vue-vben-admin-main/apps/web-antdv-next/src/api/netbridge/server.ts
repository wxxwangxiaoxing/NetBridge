import { requestClient } from '#/api/request';

import type {
  InstallCommandResult,
  InstallTokenItem,
  PageResult,
  ServerDetailItem,
  ServerItem,
} from './types';

export async function getServerList(params: Record<string, any>) {
  return requestClient.get<PageResult<ServerItem>>('/server/list', { params });
}

export async function getServerDetail(id: number) {
  return requestClient.get<ServerDetailItem>(`/server/${id}/detail`);
}

export async function generateInstallCommand(hostname?: string) {
  return requestClient.post<InstallCommandResult>('/server/generate-command', {
    hostname,
  });
}

export async function getInstallTokenList(params: Record<string, any>) {
  return requestClient.get<PageResult<InstallTokenItem>>('/server/install-token/list', {
    params,
  });
}

export async function revokeInstallToken(id: number) {
  return requestClient.post(`/server/install-token/${id}/revoke`);
}
