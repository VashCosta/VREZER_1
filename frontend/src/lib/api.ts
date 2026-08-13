import axios from 'axios';

const getBaseUrl = (): string => {
  let url = '';
  if (typeof import.meta !== 'undefined' && import.meta.env && import.meta.env.VITE_API_URL) {
    url = import.meta.env.VITE_API_URL;
  } else if (typeof window !== 'undefined' && (window as any).VREZER_API_URL) {
    url = (window as any).VREZER_API_URL;
  } else if (typeof window !== 'undefined' && window.location) {
    const host = window.location.hostname;
    if (host.includes('vercel.app') || host.includes('github.io')) {
      url = 'https://vrezer.onrender.com';
    }
  }
  return url ? url.replace(/\/+$/, '') : '';
};

const API_URL = getBaseUrl();

export const api = axios.create({
  baseURL: API_URL,
  timeout: 120000,
});

api.interceptors.request.use((config) => {
  const customKey = localStorage.getItem('vrezerApiKey') || '';
  if (customKey && config.headers && !config.headers['X-GEMINI-API-KEY']) {
    config.headers['X-GEMINI-API-KEY'] = customKey;
  }
  return config;
});

export default api;
