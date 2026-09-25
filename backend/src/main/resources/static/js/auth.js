import { apiFetch } from './api.js';

// Session storage keeps this demo login in the current browser tab.
// The backend still validates every Bearer token and enforces permissions.
const TOKEN_KEY = 'worksure_token';
const dashboards = {
  customer: '/customer/dashboard.html',
  worker: '/worker/dashboard.html',
  admin: '/admin/dashboard.html',
};

export function storeToken(token) {
  if (typeof token !== 'string' || !token) throw new Error('No login token was returned.');
  sessionStorage.setItem(TOKEN_KEY, token);
}

export function getToken() {
  return sessionStorage.getItem(TOKEN_KEY);
}

export function clearToken() {
  sessionStorage.removeItem(TOKEN_KEY);
}

export function authenticatedFetch(path, options = {}) {
  const headers = new Headers(options.headers);
  const token = getToken();
  if (token) headers.set('Authorization', `Bearer ${token}`);
  return apiFetch(path, { ...options, headers });
}

// File downloads need a Blob rather than the JSON handled by apiFetch.
// Keep the JWT in the request header, never in a URL or download link.
export async function downloadVerificationDocument(id) {
  if (!Number.isSafeInteger(Number(id)) || Number(id) < 1) throw new Error('Invalid document ID.');
  let response;
  try {
    response = await fetch(`/api/verification-documents/${Number(id)}`, {
      headers: { Authorization: `Bearer ${getToken() || ''}` },
      cache: 'no-store',
    });
  } catch {
    throw new Error('Cannot reach the server. Please try again.');
  }
  if (!response.ok) {
    const data = await response.json().catch(() => null);
    const error = new Error(data?.message || `Download failed (HTTP ${response.status}).`);
    error.status = response.status;
    throw error;
  }
  const blob = await response.blob();
  const url = URL.createObjectURL(blob);
  const link = document.createElement('a');
  link.href = url;
  const name = response.headers.get('Content-Disposition')?.match(/filename="(verification-\d+\.[a-zA-Z0-9]+)"/);
  link.download = name?.[1] || `verification-${Number(id)}`;
  document.body.appendChild(link);
  link.click();
  link.remove();
  setTimeout(() => URL.revokeObjectURL(url), 1000);
}

export async function loadCurrentUser() {
  const token = getToken();
  if (!token) return null;
  try {
    const data = await authenticatedFetch('/auth/me');
    if (!data.user || !dashboards[data.user.role]) {
      throw new Error('The server returned an unexpected user.');
    }
    return data.user;
  } catch (error) {
    if (error.status === 401) {
      clearToken();
      return null;
    }
    throw error;
  }
}

export async function isLoggedIn() {
  return Boolean(await loadCurrentUser());
}

export function hasRole(user, role) {
  return Boolean(user && user.role === role);
}

export function redirectToDashboard(user) {
  const destination = dashboards[user?.role];
  if (!destination) throw new Error('Unknown account role.');
  window.location.replace(destination);
}

export async function requireRole(role) {
  const user = await loadCurrentUser();
  if (!user) {
    window.location.replace('/login.html');
    return null;
  }
  if (!hasRole(user, role)) {
    redirectToDashboard(user);
    return null;
  }
  return user;
}

export function logout() {
  clearToken();
  window.location.replace('/login.html');
}

export async function completeAuthentication(token) {
  storeToken(token);
  try {
    const user = await loadCurrentUser();
    if (!user) throw new Error('Your session could not be verified. Please log in again.');
    redirectToDashboard(user);
  } catch (error) {
    clearToken();
    throw error;
  }
}
