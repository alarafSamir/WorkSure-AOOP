const API_BASE = '/api';

// Example: apiFetch('/services/categories').
// For JSON requests, pass a stringified body and Content-Type: application/json.
export async function apiFetch(path, options = {}) {
  const headers = new Headers(options.headers);
  if (!headers.has('Accept')) headers.set('Accept', 'application/json');

  let response;
  try {
    response = await fetch(`${API_BASE}${path}`, { ...options, headers });
  } catch {
    throw new Error('Cannot reach the server. Check your connection and try again.');
  }

  const text = await response.text();
  let data = null;
  if (text) {
    try {
      data = JSON.parse(text);
    } catch {
      if (!response.ok) throw new Error(`Request failed (HTTP ${response.status}).`);
      throw new Error('The server did not return valid JSON.');
    }
  }

  if (!response.ok) {
    const error = new Error(data?.message || `Request failed (HTTP ${response.status}).`);
    error.status = response.status;
    throw error;
  }

  return data;
}
