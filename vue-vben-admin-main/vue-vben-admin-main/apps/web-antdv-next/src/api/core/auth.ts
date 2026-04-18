import { baseRequestClient, requestClient } from '#/api/request';

export namespace AuthApi {
  /** 登录接口参数 */
  export interface LoginParams {
    password?: string;
    username?: string;
  }

  /** 登录接口返回值 */
  export interface LoginResult {
    accessToken: string;
  }

  export interface RefreshTokenResult {
    data: string;
    status: number;
  }
}

/**
 * 登录
 */
export async function loginApi(data: AuthApi.LoginParams) {
  const result = await requestClient.post<{
    token: string;
    user: {
      id: number;
      role?: string;
      username: string;
    };
  }>('/user/login', data);
  return {
    accessToken: result.token,
  };
}

/**
 * 刷新accessToken
 */
export async function refreshTokenApi() {
  return {
    data: '',
    status: 200,
  };
}

/**
 * 退出登录
 */
export async function logoutApi() {
  return Promise.resolve(true);
}

/**
 * 获取用户权限码
 */
export async function getAccessCodesApi() {
  return ['netbridge'];
}
