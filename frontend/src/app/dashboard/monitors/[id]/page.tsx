'use client';

import { use, useCallback, useEffect, useState } from 'react';
import { useAuth } from '@clerk/nextjs';
import Link from 'next/link';
import { api, heartbeatUrl, type Monitor, type MonitorCheck, type Incident, type UpdateMonitorRequest } from '@/lib/api';
import Navbar from '@/components/ui/Navbar';
import DashboardLayout from '@/app/dashboard/layout';
import StatusBadge from '@/components/ui/StatusBadge';
import ResponseTimeChart from '@/components/ui/ResponseTimeChart';
import { SkeletonChart } from '@/components/ui/Skeleton';
import { ArrowLeft, Activity, Clock, Trash2, Play, Pause, CheckCircle, AlertTriangle, Globe, Lock, Timer, Copy, ExternalLink, Edit3, X, Shield, ShieldCheck, ShieldAlert, Plus } from 'lucide-react';

function timeAgo(dateStr: string): string {
  const diff = Date.now() - new Date(dateStr).getTime();
  const mins = Math.floor(diff / 60000);
  if (mins < 1) return 'just now';
  if (mins < 60) return `${mins}m ago`;
  const hrs = Math.floor(mins / 60);
  if (hrs < 24) return `${hrs}h ago`;
  return `${Math.floor(hrs / 24)}d ago`;
}

function formatDate(dateStr: string): string {
  return new Date(dateStr).toLocaleDateString([], { year: 'numeric', month: 'long', day: 'numeric' });
}

const typeMeta: Record<string, { icon: typeof Globe; label: string }> = {
  http: { icon: Globe, label: 'HTTP' },
  ssl: { icon: Lock, label: 'SSL' },
  cron: { icon: Timer, label: 'Heartbeat' },
  keyword: { icon: Globe, label: 'Keyword' },
};

export default function MonitorDetailPage({ params }: { params: Promise<{ id: string }> }) {
  const { id } = use(params);
  const { userId } = useAuth();
  const [monitor, setMonitor] = useState<Monitor | null>(null);
  const [checks, setChecks] = useState<MonitorCheck[]>([]);
  const [incidents, setIncidents] = useState<Incident[]>([]);
  const [loading, setLoading] = useState(true);
  const [deleting, setDeleting] = useState(false);
  const [editing, setEditing] = useState(false);
  const [copied, setCopied] = useState(false);

  const [editName, setEditName] = useState('');
  const [editUrl, setEditUrl] = useState('');
  const [editInterval, setEditInterval] = useState(5);
  const [editHttpMethod, setEditHttpMethod] = useState('GET');
  const [editExpectedStatus, setEditExpectedStatus] = useState(200);
  const [editTimeout, setEditTimeout] = useState(30);
  const [editHeaders, setEditHeaders] = useState<{ key: string; value: string }[]>([{ key: '', value: '' }]);
  const [editBody, setEditBody] = useState('');
  const [saving, setSaving] = useState(false);

  const load = useCallback(() => {
    if (!userId) return;
    Promise.all([
      api.monitors.get(id),
      api.monitors.checks(id),
      api.monitors.incidents(id),
    ]).then(([m, c, i]) => {
      setMonitor(m);
      setChecks(c);
      setIncidents(i);
      setEditName(m.name);
      setEditUrl(m.url || '');
      setEditInterval(m.checkIntervalMin);
      setEditHttpMethod(m.httpMethod);
      setEditExpectedStatus(m.expectedStatus);
      setEditTimeout(m.timeoutSeconds);
      if (m.requestHeaders) {
        try {
          const parsed = JSON.parse(m.requestHeaders) as Record<string, string>;
          setEditHeaders(Object.entries(parsed).map(([key, value]) => ({ key, value })));
        } catch { setEditHeaders([{ key: '', value: '' }]); }
      }
      if (m.requestBody) setEditBody(m.requestBody);
    }).catch(console.error).finally(() => setLoading(false));
  }, [id, userId]);

  useEffect(() => {
    load();
    const interval = setInterval(load, 30000);
    return () => clearInterval(interval);
  }, [load]);

  async function togglePause() {
    if (!monitor) return;
    try {
      if (monitor.active) await api.monitors.pause(id);
      else await api.monitors.resume(id);
      load();
    } catch (e) { console.error(e); }
  }

  async function handleDelete() {
    if (!confirm('Delete this monitor permanently? This cannot be undone.')) return;
    setDeleting(true);
    try {
      await api.monitors.delete(id);
      window.location.href = '/dashboard/monitors';
    } catch (e) { console.error(e); }
  }

  async function handleEdit(e: React.FormEvent) {
    e.preventDefault();
    setSaving(true);
    try {
      const body: UpdateMonitorRequest = {
        name: editName.trim(),
        checkIntervalMin: editInterval,
        timeoutSeconds: editTimeout,
      };
      if (monitor?.type !== 'cron') body.url = editUrl;
      if (monitor?.type === 'http') {
        body.httpMethod = editHttpMethod;
        body.expectedStatus = editExpectedStatus;
        const validHeaders = editHeaders.filter(h => h.key.trim());
        if (validHeaders.length > 0) body.requestHeaders = JSON.stringify(Object.fromEntries(validHeaders.map(h => [h.key.trim(), h.value])));
        if (editBody.trim() && ['POST', 'PUT', 'PATCH'].includes(editHttpMethod)) body.requestBody = editBody;
      }
      await api.monitors.update(id, body);
      setEditing(false);
      load();
    } catch (err) { console.error(err); }
    finally { setSaving(false); }
  }

  function copyPingUrl() {
    if (!monitor?.pingKey) return;
    const url = heartbeatUrl(monitor.pingKey);
    navigator.clipboard.writeText(url).then(() => {
      setCopied(true);
      setTimeout(() => setCopied(false), 2000);
    }).catch(() => {});
  }

  if (loading) {
    return (
      <>
        <Navbar />
        <DashboardLayout>
          <div className="space-y-4">
            <div className="skeleton h-4 w-32" />
            <div className="card p-5">
              <div className="flex items-start justify-between">
                <div className="space-y-2">
                  <div className="skeleton h-6 w-48" />
                  <div className="skeleton h-3 w-64" />
                </div>
                <div className="flex gap-2">
                  <div className="skeleton h-8 w-20 rounded-lg" />
                  <div className="skeleton h-8 w-20 rounded-lg" />
                </div>
              </div>
            </div>
            <div className="grid grid-cols-1 lg:grid-cols-3 gap-4">
              <div className="lg:col-span-2"><SkeletonChart /></div>
              <div className="card p-4">
                <div className="skeleton h-4 w-24 mb-4" />
                {[...Array(5)].map((_, i) => <div key={i} className="skeleton h-6 w-full mb-2" />)}
              </div>
            </div>
          </div>
        </DashboardLayout>
      </>
    );
  }

  if (!monitor) {
    return (
      <>
        <Navbar />
        <DashboardLayout>
          <div className="text-center py-16">
            <p className="text-sm text-slate-500 mb-4">Monitor not found.</p>
            <Link href="/dashboard/monitors" className="text-sm font-medium text-blue-700 hover:text-blue-900">Back to monitors</Link>
          </div>
        </DashboardLayout>
      </>
    );
  }

  const meta = typeMeta[monitor.type] || { icon: Globe, label: monitor.type };
  const avgResponse = checks.filter(c => c.responseTimeMs != null).length > 0
    ? Math.round(checks.filter(c => c.responseTimeMs != null).reduce((a, c) => a + (c.responseTimeMs || 0), 0) / checks.filter(c => c.responseTimeMs != null).length)
    : null;
  const uptime = checks.length > 0
    ? Math.round((checks.filter(c => c.status === 'up' || c.status === 'degraded').length / checks.length) * 100)
    : null;
  const displayAvgResponse = avgResponse ?? monitor.lastResponseTimeMs;

  const sslStatus = monitor.sslDaysRemaining !== null
    ? monitor.sslDaysRemaining <= 0
      ? { label: 'Expired', color: 'text-red-600', bg: 'bg-red-50', icon: ShieldAlert }
      : monitor.sslDaysRemaining <= 30
        ? { label: `${monitor.sslDaysRemaining} days left`, color: 'text-amber-600', bg: 'bg-amber-50', icon: ShieldAlert }
        : { label: `${monitor.sslDaysRemaining} days left`, color: 'text-emerald-600', bg: 'bg-emerald-50', icon: ShieldCheck }
    : null;

  return (
    <>
      <Navbar />
      <DashboardLayout>
        <Link
          href="/dashboard/monitors"
          className="inline-flex items-center gap-1.5 text-sm text-slate-500 hover:text-slate-950 transition-colors mb-4"
        >
          <ArrowLeft className="w-4 h-4" /> All monitors
        </Link>

        <div className="card p-5 mb-4">
          <div className="flex flex-col sm:flex-row sm:items-start justify-between gap-4">
            <div className="flex items-start gap-4">
              <div className="w-10 h-10 rounded-xl bg-slate-100 border border-slate-200 flex items-center justify-center shrink-0">
                <meta.icon className="w-5 h-5 text-slate-600" />
              </div>
              <div>
                <div className="flex items-center gap-3 mb-1">
                  <h1 className="text-lg font-bold text-slate-950">{monitor.name}</h1>
                  <StatusBadge status={monitor.status} size="sm" />
                </div>
                <div className="flex flex-wrap items-center gap-2 text-sm text-slate-500">
                  {monitor.url && <span className="font-mono bg-slate-100 px-2 py-0.5 rounded">{monitor.url}</span>}
                  <span className="bg-slate-100 px-2 py-0.5 rounded">{meta.label}</span>
                  <span className="bg-slate-100 px-2 py-0.5 rounded">{monitor.checkIntervalMin}m interval</span>
                  {!monitor.active && <span className="text-amber-700 bg-amber-50 px-2 py-0.5 rounded">Paused</span>}
                </div>
              </div>
            </div>
            <div className="flex items-center gap-2">
  <button onClick={() => setEditing(true)} className="btn-secondary text-sm">
    <Edit3 className="w-3.5 h-3.5" /> Edit
  </button>
  <button onClick={togglePause} className="btn-secondary text-sm">
    {monitor.active ? <Pause className="w-3.5 h-3.5" /> : <Play className="w-3.5 h-3.5" />}
    {monitor.active ? 'Pause' : 'Resume'}
  </button>
  {/* separator */}
  <div className="w-px h-5 bg-zinc-800 mx-1" />
  <button onClick={handleDelete} disabled={deleting} className="btn-danger text-sm opacity-60 hover:opacity-100 transition-opacity">
    <Trash2 className="w-3.5 h-3.5" />
  </button>
</div>
          </div>

          <div className="grid grid-cols-2 sm:grid-cols-4 gap-3 mt-4 pt-4 border-t border-zinc-800/50">
  {uptime !== null && (
    <div className="rounded-xl bg-zinc-900 border border-zinc-800 px-4 py-3">
      <p className="text-xl font-bold text-white">{uptime}%</p>
      <p className="text-xs text-zinc-500 mt-0.5">Uptime</p>
    </div>
  )}
  {displayAvgResponse != null && (
    <div className="rounded-xl bg-zinc-900 border border-zinc-800 px-4 py-3">
      <p className="text-xl font-bold text-white">{displayAvgResponse}ms</p>
      <p className="text-xs text-zinc-500 mt-0.5">Avg response</p>
    </div>
  )}
  {monitor.lastCheckedAt && (
    <div className="rounded-xl bg-zinc-900 border border-zinc-800 px-4 py-3">
      <p className="text-xl font-bold text-white">{timeAgo(monitor.lastCheckedAt)}</p>
      <p className="text-xs text-zinc-500 mt-0.5">Last checked</p>
    </div>
  )}
  <div className="rounded-xl bg-zinc-900 border border-zinc-800 px-4 py-3">
    <p className="text-xl font-bold text-white">{checks.length}</p>
    <p className="text-xs text-zinc-500 mt-0.5">Total checks</p>
  </div>
</div>
        </div>

        {monitor.type === 'cron' && monitor.pingKey && (
          <div className="card p-4 mb-4 border-blue-200 bg-blue-50/50">
            <div className="flex items-start justify-between gap-4">
              <div className="flex items-start gap-3">
                <div className="w-8 h-8 rounded-lg bg-blue-100 border border-blue-200 flex items-center justify-center shrink-0">
                  <ExternalLink className="w-4 h-4 text-blue-700" />
                </div>
                <div>
                  <p className="text-sm font-semibold text-slate-950">Ping URL</p>
                  <p className="text-xs text-slate-500 mt-0.5 font-mono break-all">
                    {heartbeatUrl(monitor.pingKey)}
                  </p>
                  <p className="text-xs text-slate-400 mt-1">Call this URL from your cron job on every successful run.</p>
                </div>
              </div>
              <button onClick={copyPingUrl} className="btn-secondary text-xs shrink-0">
                <Copy className="w-3 h-3" /> {copied ? 'Copied!' : 'Copy'}
              </button>
            </div>
          </div>
        )}

        {monitor.type === 'ssl' && sslStatus && (
          <div className="card p-4 mb-4">
            <div className="flex items-center gap-3">
              <div className={`w-8 h-8 rounded-lg ${sslStatus.bg} border flex items-center justify-center shrink-0`}>
                <sslStatus.icon className={`w-4 h-4 ${sslStatus.color}`} />
              </div>
              <div>
                <p className="text-sm font-semibold text-slate-950">SSL Certificate</p>
                <div className="flex items-center gap-2 mt-0.5">
                  <span className={`text-xs font-medium ${sslStatus.color}`}>{sslStatus.label}</span>
                  {monitor.sslExpiresAt && (
                    <span className="text-xs text-slate-400">Expires {formatDate(monitor.sslExpiresAt)}</span>
                  )}
                </div>
              </div>
            </div>
          </div>
        )}

        <div className="grid grid-cols-1 lg:grid-cols-3 gap-4 mb-4">
          <div className="lg:col-span-2 card p-5">
            <div className="flex items-center justify-between mb-3">
              <div className="flex items-center gap-2">
                <Activity className="w-4 h-4 text-blue-700" />
                <span className="text-sm font-semibold text-slate-950">Response time</span>
              </div>
              {displayAvgResponse != null && (
                <span className="text-xs bg-slate-100 px-2 py-0.5 rounded font-medium text-slate-600">Avg: {displayAvgResponse}ms</span>
              )}
            </div>
            <ResponseTimeChart data={checks} />
          </div>

          <div className="card p-4">
            <div className="flex items-center justify-between mb-3">
              <h2 className="text-xs font-semibold text-slate-500 uppercase tracking-widest">Recent checks</h2>
              {checks[0]?.checkedAt && <span className="text-xs text-slate-400">{timeAgo(checks[0].checkedAt)}</span>}
            </div>
            <div className="space-y-0.5 max-h-80 overflow-y-auto">
              {checks.slice(0, 50).map((c) => (
                <div key={c.id} className="flex items-center justify-between py-2 px-2 rounded-md hover:bg-slate-50 transition-colors">
                  <div className="flex items-center gap-2.5">
                    {c.status === 'up' ? (
                      <CheckCircle className="w-3.5 h-3.5 text-emerald-600 shrink-0" />
                    ) : c.status === 'degraded' ? (
                      <AlertTriangle className="w-3.5 h-3.5 text-amber-600 shrink-0" />
                    ) : (
                      <AlertTriangle className="w-3.5 h-3.5 text-red-600 shrink-0" />
                    )}
                    <span className="text-sm text-slate-500 font-mono">
                      {new Date(c.checkedAt).toLocaleString([], { month: 'short', day: 'numeric', hour: '2-digit', minute: '2-digit' })}
                    </span>
                  </div>
                  <div className="flex items-center gap-2.5">
                    {c.httpStatusCode && (
                      <span className={`text-xs font-mono ${c.httpStatusCode >= 400 ? 'text-red-600' : 'text-slate-500'}`}>
                        {c.httpStatusCode}
                      </span>
                    )}
                    {c.responseTimeMs != null && (
                      <span className={`text-xs font-mono min-w-[40px] text-right ${c.responseTimeMs > 1000 ? 'text-red-600' : 'text-slate-500'}`}>
                        {c.responseTimeMs}ms
                      </span>
                    )}
                  </div>
                </div>
              ))}
              {checks.length === 0 && <p className="text-sm text-slate-400 py-6 text-center">No checks recorded yet.</p>}
            </div>
          </div>
        </div>

        <div className="card p-4">
          <div className="flex items-center gap-2 mb-4">
            <Clock className="w-4 h-4 text-amber-600" />
            <span className="text-xs font-semibold text-slate-500 uppercase tracking-widest">Incidents</span>
            {incidents.length > 0 && (
              <span className="text-xs bg-slate-100 px-2 py-0.5 rounded text-slate-500">{incidents.length}</span>
            )}
          </div>
          {incidents.length === 0 ? (
            <div className="flex items-center justify-center py-8 text-center">
              <div>
                <CheckCircle className="w-8 h-8 text-emerald-600/30 mx-auto mb-2" />
                <p className="text-sm text-slate-500">No incidents. All clear!</p>
              </div>
            </div>
          ) : (
            <div className="space-y-1.5">
              {incidents.map((inc) => (
                <div
                  key={inc.id}
                  className={`flex items-center justify-between py-2.5 px-3 rounded-lg transition-colors ${
                    inc.status === 'resolved' ? 'bg-slate-50' : 'bg-red-50 border border-red-200'
                  }`}
                >
                  <div className="flex items-center gap-3 min-w-0">
                    {inc.status === 'resolved' ? (
                      <CheckCircle className="w-3.5 h-3.5 text-emerald-600 shrink-0" />
                    ) : (
                      <AlertTriangle className="w-3.5 h-3.5 text-red-600 shrink-0" />
                    )}
                    <div className="min-w-0">
                      <p className="text-sm text-slate-700 truncate">{inc.cause || 'Unknown cause'}</p>
                      <div className="flex items-center gap-2 text-xs text-slate-400 mt-0.5">
                        <span>{new Date(inc.startedAt).toLocaleString([], { month: 'short', day: 'numeric', hour: '2-digit', minute: '2-digit' })}</span>
                        {inc.resolvedAt && (
                          <>
                            <span>&rarr;</span>
                            <span>{new Date(inc.resolvedAt).toLocaleString([], { month: 'short', day: 'numeric', hour: '2-digit', minute: '2-digit' })}</span>
                          </>
                        )}
                        {inc.durationSeconds && <span className="text-slate-400">({Math.round(inc.durationSeconds / 60)}m)</span>}
                      </div>
                    </div>
                  </div>
                  <span className={`text-xs uppercase tracking-wider px-2 py-0.5 rounded-full shrink-0 ${
                    inc.status === 'resolved'
                      ? 'text-emerald-700 bg-emerald-50'
                      : 'text-red-700 bg-red-50'
                  }`}>
                    {inc.status}
                  </span>
                </div>
              ))}
            </div>
          )}
        </div>
      </DashboardLayout>

      {editing && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50 backdrop-blur-sm" onClick={() => setEditing(false)}>
          <div className="w-full max-w-lg mx-4" onClick={(e) => e.stopPropagation()}>
            <form onSubmit={handleEdit} className="card p-5 sm:p-6">
              <div className="flex items-center justify-between mb-6">
                <h2 className="text-base font-semibold text-slate-950">Edit monitor</h2>
                <button type="button" onClick={() => setEditing(false)} className="text-slate-400 hover:text-slate-600">
                  <X className="w-4 h-4" />
                </button>
              </div>

              <div className="space-y-4">
                <div>
                  <label className="form-label" htmlFor="edit-name">Monitor name</label>
                  <input id="edit-name" type="text" value={editName} onChange={(e) => setEditName(e.target.value)} className="form-input" />
                </div>

                {monitor.type !== 'cron' && (
                  <div>
                    <label className="form-label" htmlFor="edit-url">URL</label>
                    <input id="edit-url" type="url" value={editUrl} onChange={(e) => setEditUrl(e.target.value)} className="form-input font-mono" />
                  </div>
                )}

                {monitor.type === 'http' && (
                  <div className="grid gap-4 sm:grid-cols-2">
                    <div>
                      <label className="form-label" htmlFor="edit-method">HTTP method</label>
                      <select id="edit-method" value={editHttpMethod} onChange={(e) => setEditHttpMethod(e.target.value)} className="form-input">
                        {['GET', 'HEAD', 'POST', 'PUT', 'DELETE'].map((m) => <option key={m}>{m}</option>)}
                      </select>
                    </div>
                    <div>
                      <label className="form-label" htmlFor="edit-status">Expected status</label>
                      <input id="edit-status" type="number" value={editExpectedStatus} onChange={(e) => setEditExpectedStatus(Number(e.target.value))} className="form-input" />
                    </div>
                  </div>
                )}

                <div className="grid gap-4 sm:grid-cols-2">
                  <div>
                    <label className="form-label" htmlFor="edit-interval">Interval <span className="font-normal text-slate-500">minutes</span></label>
                    <input id="edit-interval" type="number" min={1} value={editInterval} onChange={(e) => setEditInterval(Number(e.target.value))} className="form-input" />
                  </div>
                  <div>
                    <label className="form-label" htmlFor="edit-timeout">Timeout <span className="font-normal text-slate-500">seconds</span></label>
                    <input id="edit-timeout" type="number" min={5} max={120} value={editTimeout} onChange={(e) => setEditTimeout(Number(e.target.value))} className="form-input" />
                  </div>
                </div>
              </div>

              <div className="flex justify-end gap-3 mt-6 pt-5 border-t border-slate-100">
                <button type="button" onClick={() => setEditing(false)} className="btn-secondary">Cancel</button>
                <button type="submit" disabled={saving} className="btn-primary">{saving ? 'Saving...' : 'Save changes'}</button>
              </div>
            </form>
          </div>
        </div>
      )}
    </>
  );
}
