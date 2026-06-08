'use client';

import { useAuth } from '@clerk/nextjs';
import { useEffect, useState } from 'react';
import { api, type StatusPage, type Monitor } from '@/lib/api';
import Navbar from '@/components/ui/Navbar';
import DashboardLayout from '@/app/dashboard/layout';
import { Skeleton } from '@/components/ui/Skeleton';
import EmptyState from '@/components/ui/EmptyState';
import { Globe, ExternalLink, CheckCircle, XCircle, Plus, Edit3, Trash2, Copy, X, AlertTriangle } from 'lucide-react';
import Link from 'next/link';

export default function StatusPagesPage() {
  const { userId } = useAuth();
  const [pages, setPages] = useState<StatusPage[]>([]);
  const [monitors, setMonitors] = useState<Monitor[]>([]);
  const [loading, setLoading] = useState(true);
  const [showForm, setShowForm] = useState(false);
  const [editingId, setEditingId] = useState<string | null>(null);

  const [formName, setFormName] = useState('');
  const [formSlug, setFormSlug] = useState('');
  const [formMonitorIds, setFormMonitorIds] = useState<string[]>([]);
  const [formPublic, setFormPublic] = useState(true);
  const [formDescription, setFormDescription] = useState('');
  const [formLogoUrl, setFormLogoUrl] = useState('');
  const [formPrimaryColor, setFormPrimaryColor] = useState('#06b6d4');
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState('');

  function load() {
    if (!userId) return;
    Promise.all([
      api.statusPages.list(),
      api.monitors.list(),
    ]).then(([p, m]) => {
      setPages(p);
      setMonitors(m);
    }).catch(console.error).finally(() => setLoading(false));
  }

  useEffect(() => { load(); }, [userId]);

  function resetForm() {
    setFormName(''); setFormSlug(''); setFormMonitorIds([]); setFormPublic(true);
    setFormDescription(''); setFormLogoUrl(''); setFormPrimaryColor('#06b6d4'); setError('');
  }

  function openCreate() {
    resetForm();
    setEditingId(null);
    setShowForm(true);
  }

  function openEdit(p: StatusPage) {
    setFormName(p.name);
    setFormSlug(p.slug);
    setFormMonitorIds(p.monitorIds);
    setFormPublic(p.isPublic);
    setFormDescription(p.description || '');
    setFormLogoUrl(p.logoUrl || '');
    setFormPrimaryColor(p.primaryColor || '#06b6d4');
    setEditingId(p.id);
    setShowForm(true);
    setError('');
  }

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    if (!formName.trim()) { setError('Name is required.'); return; }
    if (!formSlug.trim()) { setError('Slug is required.'); return; }
    setSaving(true);
    setError('');
    try {
      if (editingId) {
        await api.statusPages.update(editingId, {
          name: formName.trim(), slug: formSlug.trim(), monitorIds: formMonitorIds, isPublic: formPublic,
          description: formDescription.trim() || undefined, logoUrl: formLogoUrl.trim() || undefined, primaryColor: formPrimaryColor,
        });
      } else {
        await api.statusPages.create({
          name: formName.trim(), slug: formSlug.trim(), monitorIds: formMonitorIds, isPublic: formPublic,
          description: formDescription.trim() || undefined, logoUrl: formLogoUrl.trim() || undefined, primaryColor: formPrimaryColor,
        });
      }
      setShowForm(false);
      load();
    } catch (err: unknown) {
      setError(err instanceof Error ? err.message : 'Failed to save status page.');
    } finally { setSaving(false); }
  }

  async function handleDelete(id: string) {
    if (!confirm('Delete this status page permanently?')) return;
    try {
      await api.statusPages.delete(id);
      load();
    } catch (e) { console.error(e); }
  }

  return (
    <>
      <Navbar />
      <DashboardLayout>
        <div className="space-y-6">
          <div className="flex items-start justify-between gap-4">
            <div>
              <p className="section-kicker">Status pages</p>
              <h1 className="page-title mt-2">Status Pages</h1>
              <p className="page-description">
                {pages.length > 0 ? `${pages.length} page${pages.length !== 1 ? 's' : ''} created` : 'Public status pages for your users'}
              </p>
            </div>
            <button onClick={openCreate} className="btn-primary text-sm shrink-0">
              <Plus className="w-4 h-4" /> New page
            </button>
          </div>

          {loading ? (
            <div className="space-y-2">
              {[...Array(2)].map((_, i) => (
                <div key={i} className="card p-4">
                  <div className="flex items-center justify-between">
                    <div className="space-y-2">
                      <Skeleton className="h-4 w-32" />
                      <Skeleton className="h-3 w-48" />
                    </div>
                    <Skeleton className="h-6 w-16 rounded-full" />
                  </div>
                </div>
              ))}
            </div>
          ) : pages.length === 0 && !showForm ? (
            <EmptyState
              icon={Globe}
              title="No status pages"
              description="Create a public status page to keep your users informed about service health."
              action={{ label: 'Create status page', href: '#' }}
              actionOnClick={openCreate}
            />
          ) : (
            <div className="space-y-2">
              {pages.map((p) => (
                <div key={p.id} className="card p-4 interactive-card">
                  <div className="flex items-center justify-between">
                    <div className="flex items-center gap-3 min-w-0">
                      <div className="w-9 h-9 rounded-lg bg-slate-100 border border-slate-200 flex items-center justify-center shrink-0">
                        <Globe className="w-4 h-4 text-slate-600" />
                      </div>
                      <div className="min-w-0">
                        <h3 className="text-sm font-semibold text-slate-900">{p.name}</h3>
                        <p className="text-sm text-slate-500 font-mono truncate">/status/{p.slug}</p>
                        <p className="text-xs text-slate-400 mt-0.5">{p.monitorIds.length} monitor{p.monitorIds.length !== 1 ? 's' : ''}</p>
                      </div>
                    </div>
                    <div className="flex items-center gap-2">
                      {p.isPublic ? (
                        <span className="flex items-center gap-1 text-xs text-emerald-700 bg-emerald-50 px-2 py-0.5 rounded-full">
                          <CheckCircle className="w-3 h-3" /> Public
                        </span>
                      ) : (
                        <span className="flex items-center gap-1 text-xs text-slate-500 bg-slate-100 px-2 py-0.5 rounded-full">
                          <XCircle className="w-3 h-3" /> Private
                        </span>
                      )}
                      <Link href={`/status/${p.slug}`} target="_blank" className="btn-secondary text-xs p-2">
                        <ExternalLink className="w-3.5 h-3.5" />
                      </Link>
                      <button onClick={() => openEdit(p)} className="btn-secondary text-xs p-2">
                        <Edit3 className="w-3.5 h-3.5" />
                      </button>
                      <button onClick={() => handleDelete(p.id)} className="btn-danger text-xs p-2">
                        <Trash2 className="w-3.5 h-3.5" />
                      </button>
                    </div>
                  </div>
                </div>
              ))}
            </div>
          )}
        </div>
      </DashboardLayout>

      {showForm && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50 backdrop-blur-sm" onClick={() => setShowForm(false)}>
          <div className="w-full max-w-lg mx-4" onClick={(e) => e.stopPropagation()}>
            <form onSubmit={handleSubmit} className="card p-5 sm:p-6">
              <div className="flex items-center justify-between mb-6">
                <h2 className="text-base font-semibold text-slate-950">{editingId ? 'Edit status page' : 'Create status page'}</h2>
                <button type="button" onClick={() => setShowForm(false)} className="text-slate-400 hover:text-slate-600">
                  <X className="w-4 h-4" />
                </button>
              </div>

              <div className="space-y-4">
                <div>
                  <label className="form-label" htmlFor="sp-name">Page name</label>
                  <input id="sp-name" type="text" value={formName} onChange={(e) => setFormName(e.target.value)} className="form-input" placeholder="My Status Page" />
                </div>
                <div>
                  <label className="form-label" htmlFor="sp-slug">Slug</label>
                  <input id="sp-slug" type="text" value={formSlug} onChange={(e) => setFormSlug(e.target.value.replace(/\s+/g, '-').toLowerCase())} className="form-input font-mono" placeholder="my-status" />
                  <p className="mt-1 text-xs text-slate-500">Your page will be at /status/{formSlug || 'slug'}</p>
                </div>
                <div>
                  <label className="form-label">Description</label>
                  <textarea value={formDescription} onChange={(e) => setFormDescription(e.target.value)} className="form-input min-h-[80px]" placeholder="Optional public description" />
                </div>
                <div className="grid gap-4 sm:grid-cols-2">
                  <div>
                    <label className="form-label">Logo URL</label>
                    <input type="url" value={formLogoUrl} onChange={(e) => setFormLogoUrl(e.target.value)} className="form-input" placeholder="https://..." />
                  </div>
                  <div>
                    <label className="form-label">Primary color</label>
                    <input type="color" value={formPrimaryColor} onChange={(e) => setFormPrimaryColor(e.target.value)} className="form-input h-10" />
                  </div>
                </div>
                <div>
                  <label className="form-label">Include monitors</label>
                  <div className="max-h-40 overflow-y-auto space-y-1.5 border border-slate-200 rounded-xl p-2">
                    {monitors.map((m) => (
                      <label key={m.id} className="flex items-center gap-2.5 px-2 py-1.5 rounded-lg hover:bg-slate-50 cursor-pointer">
                        <input
                          type="checkbox"
                          checked={formMonitorIds.includes(m.id)}
                          onChange={(e) => {
                            if (e.target.checked) setFormMonitorIds([...formMonitorIds, m.id]);
                            else setFormMonitorIds(formMonitorIds.filter(id => id !== m.id));
                          }}
                          className="rounded border-slate-300 text-slate-950 focus:ring-slate-950"
                        />
                        <span className="text-sm text-slate-700">{m.name}</span>
                        <span className="text-xs text-slate-400 ml-auto">{m.type}</span>
                      </label>
                    ))}
                    {monitors.length === 0 && <p className="text-sm text-slate-400 py-2 text-center">Create monitors first</p>}
                  </div>
                </div>
                <label className="flex items-center gap-2.5 cursor-pointer">
                  <input
                    type="checkbox"
                    checked={formPublic}
                    onChange={(e) => setFormPublic(e.target.checked)}
                    className="rounded border-slate-300 text-slate-950 focus:ring-slate-950"
                  />
                  <span className="text-sm text-slate-700">Make public</span>
                </label>

                {error && (
                  <div className="rounded-xl border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-700 flex items-center gap-2" role="alert">
                    <AlertTriangle className="w-4 h-4 shrink-0" /> {error}
                  </div>
                )}
              </div>

              <div className="flex justify-end gap-3 mt-6 pt-5 border-t border-slate-100">
                <button type="button" onClick={() => setShowForm(false)} className="btn-secondary">Cancel</button>
                <button type="submit" disabled={saving} className="btn-primary">{saving ? 'Saving...' : editingId ? 'Save changes' : 'Create page'}</button>
              </div>
            </form>
          </div>
        </div>
      )}
    </>
  );
}
