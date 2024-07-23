// src/app/accounts/login/api/login.ts
import axios from 'axios';
import Cookies from 'js-cookie';

import { ILogInReq } from '@/interfaces/members'; 
import { removeMemberData, setSessionStorageItem } from '@/utils/sessionStorage';

const API = axios.create({
  baseURL: `${process.env.NEXT_PUBLIC_HOST}/auth`,
  withCredentials: true,
});

API.interceptors.response.use(
  (response) => {
    if (
      response.config.url === '/login' ||
      response.config.url === '/refresh'
    ) {
      const {accessToken} = response.data;
      if (accessToken) {
        Cookies.set('access_token', accessToken);
      }
    }
    return response;
  },
  (error) => {
    return Promise.reject(error);
  },
);

export const login = async (memerData: ILogInReq) => {
  const response = await API.post('/login', memerData);
  setSessionStorageItem('memberData', {
    id: response.data.id,
    email: response.data.email,
    nickname: response.data.nickname,
  });
  return response.data;
};

export const logout = async (memberId: string) => {
  await API.post('/logout', { memberId });
  removeMemberData();
};

export const refreshAccessToken = async () => {
  const response = await API.post('/refresh');
  return response.data.accessToken;
};
