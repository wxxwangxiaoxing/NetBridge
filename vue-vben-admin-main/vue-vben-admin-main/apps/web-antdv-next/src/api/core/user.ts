import type { UserInfo } from '@vben/types';

import { requestClient } from '#/api/request';

/**
 * 获取用户信息
 */
export async function getUserInfoApi() {
  const user = await requestClient.get<{
    email?: string;
    id: number;
    role?: string;
    username: string;
  }>('/user/info');

  return {
    avatar: '',
    desc: user.email || 'NetBridge control console user',
    homePath: '/netbridge/overview',
    realName: user.username,
    roles: user.role ? [user.role] : ['user'],
    token: '',
    userId: String(user.id),
    username: user.username,
  } satisfies UserInfo;
}
