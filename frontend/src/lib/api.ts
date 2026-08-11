import axios from 'axios';

const getBaseUrl = (): string => {
  if (typeof import.meta !== 'undefined' && (import.meta as any).env && (import.meta as any).env.VITE_API_URL) {
    return (import.meta as any).env.VITE_API_URL;
  }
  if (typeof window !== 'undefined' && (window as any).VREZER_API_URL) {
    return (window as any).VREZER_API_URL;
  }
  if (typeof window !== 'undefined' && window.location) {
    const host = window.location.hostname;
    if (host.includes('vercel.app') || host.includes('github.io')) {
      return 'https://vrezer-backend.onrender.com';
    }
  }
  return '';
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
