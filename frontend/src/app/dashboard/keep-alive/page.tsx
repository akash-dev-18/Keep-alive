'use client';

import { useAuth } from '@clerk/nextjs';
import { useEffect, useState } from 'react';
import { api, type Monitor, type MonitorCheck } from '@/lib/api';
import Navbar from '@/components/ui/Navbar';
import DashboardLayout from '@/app/dashboard/layout';
import StatusBadge from '@/components/ui/StatusBadge';
import { SkeletonCard } from '@/components/ui/Skeleton';
import EmptyState from '@/components/ui/EmptyState';
import { Globe, Plus, Activity, Clock, CheckCircle, AlertTriangle, ExternalLink, Trash2 } from 'lucide-react';
import Link from 'next/link';

export default function KeepAlivePage() {
  const { userId } = useAuth();
  const [monitors, setMonitors] = useState<Monitor[]>([]);
  const [checksMap, setChecksMap] = useState<Record<string, MonitorCheck[]>>({});
  const [loading, setLoading] = useState(true);
  const [showForm, setShowForm] = useState(false);
  const [url, setUrl] = useState('');
  const [name, setName] = useState('');
  const [interval, setInterval] = useState(5);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState('');

  function load() {
    if (!userId) return;
    setLoading(true);
    api.monitors.list()
      .then(async (all) => {
        const httpMonitors = all.filter(m => m.type === 'http');
        setMonitors(httpMonitors);
        const cm: Record<string, MonitorCheck[]> = {};
        await Promise.all(httpMonitors.map(async (m) => {
          try { cm[m.id] = await api.monitors.checks(m.id); } catch {}
        }));
        setChecksMap(cm);
      })
      .catch(console.error)
      .finally(() => setLoading(false));
  }

  useEffect(() => { load(); }, [userId]); // eslint-disable-line react-hooks/set-state-in-effect

  async function handleAdd(e: React.FormEvent) {
    e.preventDefault();
    if (!url.trim()) { setError('URL is required.'); return; }
    setSaving(true);
    setError('');
    try {
      await api.monitors.create({
        name: name.trim() || url.trim(),
        type: 'http',
        url: url.trim(),
        checkIntervalMin: interval,
      });
      setShowForm(false);
      setUrl('');
      setName('');
      setInterval(5);
      load();
    } catch (err: unknown) {
      setError(err instanceof Error ? err.message : 'Failed to create pinger.');
      setSaving(false);
    }
  }

  async function handleDelete(id: string) {
    if (!confirm('Stop pinging this URL?')) return;
    try {
      await api.monitors.delete(id);
      load();
    } catch (e) { console.error(e); }
  }

  const avgResponse = (checks: MonitorCheck[]) => {
    const valid = checks.filter(c => c.responseTimeMs != null);
    if (!valid.length) return null;
    return Math.round(valid.reduce((a, c) => a + (c.responseTimeMs || 0), 0) / valid.length);
  };

  const uptime = (checks: MonitorCheck[]) => {
    if (!checks.length) return null;
    return Math.round((checks.filter(c => c.status === 'up').length / checks.length) * 100);
  };

  return (
    <>
      <Navbar />
      <DashboardLayout>
        <div className="space-y-6">
          <div className="flex items-start justify-between gap-4">
            <div>
              <p className="section-kicker">Keep Alive</p>
              <h1 className="page-title mt-2">Keep Alive</h1>
              <p className="page-description">
                Ping your URLs periodically so Render, Railway, and other free-tier hosts never put them to sleep.
              </p>
            </div>
            <button onClick={() => setShowForm(true)} className="btn-primary text-sm shrink-0">
              <Plus className="w-4 h-4" /> Add URL
            </button>
          </div>

          {loading ? (
            <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-3">
              {[...Array(3)].map((_, i) => <SkeletonCard key={i} />)}
            </div>
          ) : monitors.length === 0 && !showForm ? (
            <EmptyState
              icon={Globe}
              title="No URLs being pinged"
              description="Add a URL and KeepAlive will ping it every few minutes so your free-tier app never goes to sleep."
              actionOnClick={() => setShowForm(true)}
              action={{ label: 'Add your first URL', href: '#' }}
            />
          ) : (
            <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-3">
              {monitors.map((m) => {
                const checks = checksMap[m.id] || [];
                const avg = avgResponse(checks);
                const upt = uptime(checks);
                const lastCheck = checks[0];
                return (
                  <div key={m.id} className="card p-4 interactive-card">
                    <div className="flex items-start justify-between gap-3 mb-3">
                      <div className="flex items-start gap-3 min-w-0">
                        <div className={`w-9 h-9 rounded-lg border flex items-center justify-center shrink-0 ${
                          m.status === 'up' ? 'bg-emerald-50 border-emerald-200 text-emerald-600'
                            : m.status === 'down' ? 'bg-red-50 border-red-200 text-red-600'
                            : 'bg-slate-50 border-slate-200 text-slate-600'
                        }`}>
                          {m.status === 'up' ? <CheckCircle className="w-4 h-4" /> : m.status === 'down' ? <AlertTriangle className="w-4 h-4" /> : <Globe className="w-4 h-4" />}
                        </div>
                        <div className="min-w-0">
                          <Link href={`/dashboard/monitors/${m.id}`} className="text-sm font-semibold text-slate-900 hover:text-blue-700 transition-colors truncate block">
                            {m.name}
                          </Link>
                          <p className="text-xs text-slate-500 font-mono truncate">{m.url}</p>
                        </div>
                      </div>
                      <StatusBadge status={m.status} size="sm" showLabel={false} />
                    </div>

                    <div className="flex items-center gap-3 text-xs text-slate-500 mb-3">
                      <span className="inline-flex items-center gap-1"><Clock className="w-3 h-3" /> Every {m.checkIntervalMin}m</span>
                      {avg && <span className="inline-flex items-center gap-1"><Activity className="w-3 h-3" /> {avg}ms</span>}
                      {upt != null && <span>{upt}% up</span>}
                    </div>

                    {lastCheck && (
                      <div className="text-xs text-slate-400 flex items-center gap-1 pb-3 border-b border-slate-100">
                        <Clock className="w-3 h-3" />
                        Last pinged {new Date(lastCheck.checkedAt).toLocaleString([], { month: 'short', day: 'numeric', hour: '2-digit', minute: '2-digit' })}
                      </div>
                    )}

                    <div className="mt-3 flex gap-2">
                      <Link href={`/dashboard/monitors/${m.id}`} className="btn-secondary text-xs flex-1 flex items-center justify-center gap-1">
                        <ExternalLink className="w-3 h-3" /> Details
                      </Link>
                      <button onClick={() => handleDelete(m.id)} className="btn-danger text-xs p-2">
                        <Trash2 className="w-3.5 h-3.5" />
                      </button>
                    </div>
                  </div>
                );
              })}
            </div>
          )}
        </div>
      </DashboardLayout>

      {showForm && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50 backdrop-blur-sm" onClick={() => setShowForm(false)}>
          <div className="w-full max-w-md mx-4" onClick={(e) => e.stopPropagation()}>
            <form onSubmit={handleAdd} className="card p-5 sm:p-6">
              <div className="flex items-center justify-between mb-6">
                <h2 className="text-base font-semibold text-slate-950">Add URL to ping</h2>
                <button type="button" onClick={() => setShowForm(false)} className="text-slate-400 hover:text-slate-600">
                  ✕
                </button>
              </div>

              <div className="space-y-4">
                <div className="rounded-xl border border-blue-200 bg-blue-50 p-3 text-xs text-blue-900 leading-5">
                  KeepAlive will ping this URL every few minutes so free-tier hosts (Render, Railway, Fly.io, etc.) don&apos;t put your app to sleep.
                </div>
                <div>
                  <label className="form-label" htmlFor="ka-url">Your website URL</label>
                  <input id="ka-url" type="url" value={url} onChange={(e) => setUrl(e.target.value)} className="form-input font-mono" placeholder="https://my-app.onrender.com" required />
                </div>
                <div>
                  <label className="form-label" htmlFor="ka-name">Label <span className="font-normal text-slate-500">optional</span></label>
                  <input id="ka-name" type="text" value={name} onChange={(e) => setName(e.target.value)} className="form-input" placeholder="My Render App" />
                </div>
                <div>
                  <label className="form-label" htmlFor="ka-interval">Ping every <span className="font-normal text-slate-500">minutes</span></label>
                  <input id="ka-interval" type="number" min={1} value={interval} onChange={(e) => setInterval(Number(e.target.value))} className="form-input" />
                  <p className="mt-1 text-xs text-slate-500">5 min is usually enough to keep free instances awake.</p>
                </div>

                {error && (
                  <div className="rounded-xl border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-700" role="alert">
                    {error}
                  </div>
                )}
              </div>

              <div className="flex justify-end gap-3 mt-6 pt-5 border-t border-slate-100">
                <button type="button" onClick={() => setShowForm(false)} className="btn-secondary">Cancel</button>
                <button type="submit" disabled={saving} className="btn-primary">{saving ? 'Adding...' : 'Start pinging'}</button>
              </div>
            </form>
          </div>
        </div>
      )}
    </>
  );
}
