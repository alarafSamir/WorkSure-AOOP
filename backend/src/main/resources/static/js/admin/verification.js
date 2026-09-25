import { badge } from '../ui.js';
import { requireRole, authenticatedFetch, downloadVerificationDocument, logout } from '../auth.js';
const content = document.querySelector('#content');
const status = document.querySelector('#page-status');
const message = document.querySelector('#action-message');
const filter = document.querySelector('#status-filter');
const refresh = document.querySelector('#refresh');
let documents = [];
let busy = false;
document.querySelector('#logout').addEventListener('click', logout);
function text(tag, value) {
  const element = document.createElement(tag);
  element.textContent = value;
  return element;
}
function showError(error) {
  if (error.status === 401) return logout();
  message.className = 'error';
  message.textContent = error.message;
}
function render() {
  const list = document.querySelector('#documents-list');
  list.replaceChildren();
  const visible = documents.filter(doc => filter.value === 'all' || doc.status === filter.value);
  document.querySelector('#documents-status').textContent = visible.length ? `${visible.length} documents shown.` : 'No documents with this status.';
  for (const doc of visible) {
    const item = document.createElement('li');
    item.dataset.documentId = doc.id;
    item.append(text('h2', `${doc.worker_name} — document #${doc.id}`),
      text('p', `Worker #${doc.worker_id} | Type: ${doc.doc_type}`),
      badge(text('p', `Status: ${doc.status}`), doc.status),
      text('p', `Submitted (UTC): ${doc.created_at}`),
      badge(text('p', `Worker verification: ${Number(doc.worker_is_verified) ? 'Verified' : 'Not verified'}`), Number(doc.worker_is_verified) ? 'verified' : 'unverified'));
    if (doc.reviewed_at) item.append(text('p', `Reviewed (UTC): ${doc.reviewed_at}`));
    if (doc.admin_note) item.append(text('p', `Review note: ${doc.admin_note}`));
    const download = text('button', 'Download document');
    download.type = 'button';
    download.dataset.action = 'download';
    download.addEventListener('click', async () => {
      download.disabled = true;
      try { await downloadVerificationDocument(doc.id); }
      catch (error) { showError(error); }
      finally { download.disabled = busy; }
    });
    const form = document.createElement('form');
    const label = text('label', 'Review note (optional)');
    const note = document.createElement('textarea');
    note.id = `note-${doc.id}`;
    note.name = 'admin_note';
    note.value = doc.admin_note || '';
    label.htmlFor = note.id;
    form.append(label, note);
    for (const [value, label] of [['approved', 'Approve'], ['rejected', 'Reject']]) {
      const button = text('button', label);
      button.type = 'submit';
      button.value = value;
      button.disabled = doc.status === value;
      form.append(button);
    }
    form.addEventListener('submit', async event => {
      event.preventDefault();
      const decision = event.submitter?.value;
      if (busy || !['approved', 'rejected'].includes(decision)) return;
      busy = true;
      content.querySelectorAll('button, textarea, select').forEach(control => { control.disabled = true; });
      message.textContent = 'Saving decision…';
      try {
        const result = await authenticatedFetch(`/admin/documents/${doc.id}`, {
          method: 'PATCH', headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({ status: decision, admin_note: note.value.trim() || null }),
        });
        message.className = 'success';
        message.textContent = result.message;
        await fetchDocuments();
      } catch (error) { showError(error); }
      finally {
        busy = false;
        filter.disabled = refresh.disabled = false;
        render();
      }
    });
    item.append(download, form);
    list.append(item);
  }
}
async function fetchDocuments() {
  // Never retain stale decision controls if a refresh fails.
  documents = [];
  render();
  const result = await authenticatedFetch('/admin/documents');
  documents = result.data;
  render();
}
async function load() {
  if (busy) return;
  content.hidden = true;
  status.textContent = 'Loading verification documents…';
  try {
    if (!await requireRole('admin')) return;
    await fetchDocuments();
    content.hidden = false;
    status.textContent = '';
  } catch (error) {
    if (error.status === 401) return logout();
    status.textContent = `Could not load documents. ${error.message} Reload to retry.`;
  }
}
filter.addEventListener('change', render);
refresh.addEventListener('click', load);
window.addEventListener('pageshow', load);
