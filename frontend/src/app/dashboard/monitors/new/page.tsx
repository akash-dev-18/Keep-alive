'use client';

import { useRouter } from 'next/navigation';
import { useState } from 'react';
import { api, type CreateMonitorRequest, INTERVAL_PRESETS, TIMEOUT_PRESETS, ALERT_AFTER_PRESETS } from '@/lib/api';
import Navbar from '@/components/ui/Navbar';
import DashboardLayout from '@/app/dashboard/layout';
import { ArrowLeft, Clock, Globe, Lock, Search } from 'lucide-react';
import Link from 'next/link';

const typeOptions = [
  { value: 'http' as const, icon: Globe, label: 'HTTP / API', desc: 'Check a website, API route, or webhook endpoint.' },
  { value: 'ssl' as const, icon: Lock, label: 'SSL certificate', desc: 'Track certificate availability and expiry.' },
  { value: 'cron' as const, icon: Clock, label: 'Heartbeat', desc: 'Generate a ping URL for scheduled jobs.' },
  { value: 'keyword' as const, icon: Search, label: 'Keyword', desc: 'Fail if expected text is not found in the page.' },
];

export default function NewMonitorPage() {
  const router = useRouter();
  const [type, setType] = useState<'http' | 'ssl' | 'cron' | 'keyword'>('http');
  const [name, setName] = useState('');
  const [url, setUrl] = useState('');
  const [interval, setInterval] = useState(2);
  const [customInterval, setCustomInterval] = useState(false);
  const [httpMethod, setHttpMethod] = useState('GET');
  const [expectedStatus, setExpectedStatus] = useState(200);
  const [timeoutSecs, setTimeoutSecs] = useState(30);
  const [alertAfter, setAlertAfter] = useState(1);
  const [expectedKeyword, setExpectedKeyword] = useState('');
  const [basicAuthUser, setBasicAuthUser] = useState('');
  const [basicAuthPass, setBasicAuthPass] = useState('');
  const [bearerToken, setBearerToken] = useState('');
  const [headers, setHeaders] = useState<{ key: string; value: string }[]>([{ key: '', value: '' }]);
  const [body, setBody] = useState('');
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState('');

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    if (!name.trim()) { setError('Monitor name is required.'); return; }
    setSaving(true);
    setError('');
    try {
      const req: CreateMonitorRequest = {
        name: name.trim(),
        type,
        checkIntervalMin: interval,
        timeoutSeconds: timeoutSecs,
        alertAfterFailures: alertAfter,
      };
      if (type !== 'cron') {
        if (!url.trim()) { setError('URL is required.'); setSaving(false); return; }
        req.url = url.trim();
      }
      if (type === 'http') {
        req.httpMethod = httpMethod;
        req.expectedStatus = expectedStatus;
        const validHeaders = headers.filter(h => h.key.trim());
        if (validHeaders.length > 0) req.requestHeaders = JSON.stringify(Object.fromEntries(validHeaders.map(h => [h.key.trim(), h.value])));
        if (body.trim() && (httpMethod === 'POST' || httpMethod === 'PUT' || httpMethod === 'PATCH')) {
          req.requestBody = body;
        }
        if (basicAuthUser.trim()) {
          req.basicAuthUsername = basicAuthUser.trim();
          if (basicAuthPass) req.basicAuthPassword = basicAuthPass;
        }
        if (bearerToken.trim()) req.bearerToken = bearerToken.trim();
      }
      if (type === 'keyword') {
        if (!expectedKeyword.trim()) { setError('Expected keyword is required.'); setSaving(false); return; }
        req.expectedKeyword = expectedKeyword.trim();
      }
      await api.monitors.create(req);
      router.push('/dashboard/monitors');
    } catch (err: unknown) {
      setError(err instanceof Error ? err.message : 'Failed to create monitor.');
      setSaving(false);
    }
  }

  return (
    <>
      <Navbar />
      <DashboardLayout>
        <div className="mx-auto max-w-3xl space-y-6">
          <Link href="/dashboard/monitors" className="inline-flex items-center gap-2 text-sm font-medium text-slate-600 hover:text-slate-950">
            <ArrowLeft className="h-4 w-4" /> Back to monitors
          </Link>

          <div>
            <p className="section-kicker">New monitor</p>
            <h1 className="page-title mt-2">Create a production check</h1>
            <p className="page-description">Choose the signal you want KeepAlive to watch and configure the expected result.</p>
          </div>

          <form
  onSubmit={handleSubmit}
  className="rounded-3xl border border-slate-800 bg-slate-950/60 p-8 backdrop-blur-sm shadow-[0_20px_80px_rgba(0,0,0,0.35)]"
>
            <fieldset>
              <legend className="text-sm font-semibold text-slate-950">Monitor type</legend>
              {/* <div className="mt-3 grid gap-3 sm:grid-cols-2 lg:grid-cols-4">
                {typeOptions.map((t) => (
                  <button
                    key={t.value}
                    type="button"
                    onClick={() => { setType(t.value); setError(''); }}
                    className={`rounded-xl border p-4 text-left transition-colors ${
                      type === t.value ? 'border-slate-950 bg-slate-50' : 'border-slate-200 bg-white hover:border-slate-300'
                    }`}
                  >
                    <t.icon className="h-5 w-5 text-slate-700" />
                    <p className="mt-3 text-sm font-semibold text-slate-950">{t.label}</p>
                    <p className="mt-1 text-xs leading-5 text-slate-500">{t.desc}</p>
                  </button>
                ))}
              </div> */}
              <div className="mt-5 grid gap-5 md:grid-cols-2 xl:grid-cols-4">
  {typeOptions.map((t) => (
    <button
      key={t.value}
      type="button"
      onClick={() => {
        setType(t.value);
        setError('');
      }}
      className={`group relative overflow-hidden rounded-2xl border p-6 text-left transition-all duration-300 ease-out
      ${
        type === t.value
          ? 'border-blue-500 bg-blue-500/10 shadow-[0_0_40px_rgba(59,130,246,0.25)] scale-[1.02]'
          : 'border-slate-800 bg-slate-900/40 hover:-translate-y-1 hover:border-blue-500/50 hover:bg-slate-900 hover:shadow-[0_12px_40px_rgba(59,130,246,0.15)]'
      }`}
    >
      <div className="absolute inset-0 bg-gradient-to-br from-blue-500/10 via-transparent to-transparent opacity-0 transition-opacity duration-300 group-hover:opacity-100" />

      {type === t.value && (
        <div className="absolute right-4 top-4 h-2.5 w-2.5 rounded-full bg-blue-500 shadow-[0_0_12px_rgb(59,130,246)]" />
      )}

      <div className="relative z-10">
        <div
          className={`inline-flex rounded-xl p-3 transition-all duration-300 ${
            type === t.value
              ? 'bg-blue-500/20'
              : 'bg-slate-800 group-hover:bg-blue-500/10'
          }`}
        >
          <t.icon
            className={`h-6 w-6 transition-colors duration-300 ${
              type === t.value
                ? 'text-blue-400'
                : 'text-slate-400 group-hover:text-blue-400'
            }`}
          />
        </div>

        <p
          className={`mt-4 text-sm font-semibold transition-colors ${
            type === t.value
              ? 'text-white'
              : 'text-slate-200 group-hover:text-white'
          }`}
        >
          {t.label}
        </p>

        <p
          className={`mt-2 text-xs leading-5 ${
            type === t.value
              ? 'text-slate-300'
              : 'text-slate-500'
          }`}
        >
          {t.desc}
        </p>
      </div>
    </button>
  ))}
</div>
            </fieldset>

            <div className="mt-8 grid gap-5">
              <div>
                <label className="form-label" htmlFor="monitor-name">Monitor name</label>
                <input id="monitor-name" type="text" value={name} onChange={(e) => setName(e.target.value)} className="form-input" />
              </div>

              {type !== 'cron' && (
                <div>
                  <label className="form-label" htmlFor="monitor-url">URL</label>
                  <input id="monitor-url" type="url" value={url} onChange={(e) => setUrl(e.target.value)} className="form-input font-mono" placeholder="https://example.com" />
                </div>
              )}

              {type === 'keyword' && (
                <div>
                  <label className="form-label" htmlFor="expected-keyword">Expected keyword</label>
                  <input id="expected-keyword" type="text" value={expectedKeyword} onChange={(e) => setExpectedKeyword(e.target.value)} className="form-input" placeholder="e.g. Welcome" />
                </div>
              )}

              {type === 'http' && (
                <>
                  <div className="grid gap-4 sm:grid-cols-2">
                    <div>
                      <label className="form-label" htmlFor="http-method">HTTP method</label>
                      <select id="http-method" value={httpMethod} onChange={(e) => setHttpMethod(e.target.value)} className="form-input">
                        {['GET', 'HEAD', 'POST', 'PUT', 'DELETE', 'PATCH'].map((m) => <option key={m}>{m}</option>)}
                      </select>
                    </div>
                    <div>
                      <label className="form-label" htmlFor="expected-status">Expected status</label>
                      <input id="expected-status" type="number" value={expectedStatus} onChange={(e) => setExpectedStatus(Number(e.target.value))} className="form-input" />
                    </div>
                  </div>

                  <div className="grid gap-4 sm:grid-cols-2">
                    <div>
                      <label className="form-label">Basic auth username <span className="font-normal text-slate-500">optional</span></label>
                      <input type="text" value={basicAuthUser} onChange={(e) => setBasicAuthUser(e.target.value)} className="form-input" />
                    </div>
                    <div>
                      <label className="form-label">Basic auth password <span className="font-normal text-slate-500">optional</span></label>
                      <input type="password" value={basicAuthPass} onChange={(e) => setBasicAuthPass(e.target.value)} className="form-input" />
                    </div>
                  </div>

                  <div>
                    <label className="form-label">Bearer token <span className="font-normal text-slate-500">optional</span></label>
                    <input type="password" value={bearerToken} onChange={(e) => setBearerToken(e.target.value)} className="form-input font-mono text-xs" />
                  </div>

                  <div>
                    <label className="form-label">Request headers <span className="font-normal text-slate-500">optional</span></label>
                    <div className="space-y-2">
                      {headers.map((h, i) => (
                        <div key={i} className="flex gap-2">
                          <input type="text" value={h.key} onChange={(e) => { const n = [...headers]; n[i] = { ...n[i], key: e.target.value }; setHeaders(n); }} className="form-input flex-1 font-mono text-xs" placeholder="Header" />
                          <input type="text" value={h.value} onChange={(e) => { const n = [...headers]; n[i] = { ...n[i], value: e.target.value }; setHeaders(n); }} className="form-input flex-[2] font-mono text-xs" placeholder="Value" />
                        </div>
                      ))}
                      <button type="button" onClick={() => setHeaders([...headers, { key: '', value: '' }])} className="text-xs text-blue-700">+ Add header</button>
                    </div>
                  </div>

                  {(httpMethod === 'POST' || httpMethod === 'PUT' || httpMethod === 'PATCH') && (
                    <div>
                      <label className="form-label">Request body</label>
                      <textarea value={body} onChange={(e) => setBody(e.target.value)} className="form-input font-mono text-xs min-h-[100px]" />
                    </div>
                  )}
                </>
              )}

              <div className="grid gap-4 sm:grid-cols-3">
                <div>
                  <label className="form-label">Check interval</label>
                  {!customInterval ? (
                    <select value={interval} onChange={(e) => setInterval(Number(e.target.value))} className="form-input">
                      {INTERVAL_PRESETS.map((v) => <option key={v} value={v}>{v} min</option>)}
                      <option value={-1} onClick={() => setCustomInterval(true)}>Custom...</option>
                    </select>
                  ) : (
                    <input type="number" min={1} value={interval} onChange={(e) => setInterval(Number(e.target.value))} className="form-input" />
                  )}
                  {!customInterval && (
                    <button type="button" onClick={() => setCustomInterval(true)} className="mt-1 text-xs text-blue-700">Custom interval</button>
                  )}
                </div>
                <div>
                  <label className="form-label">Timeout</label>
                  <select value={timeoutSecs} onChange={(e) => setTimeoutSecs(Number(e.target.value))} className="form-input">
                    {TIMEOUT_PRESETS.map((v) => <option key={v} value={v}>{v} sec</option>)}
                  </select>
                </div>
                <div>
                  <label className="form-label">Alert after failures</label>
                  <select value={alertAfter} onChange={(e) => setAlertAfter(Number(e.target.value))} className="form-input">
                    {ALERT_AFTER_PRESETS.map((v) => (
                      <option key={v} value={v}>{v === 0 ? 'Immediately' : `${v} failure${v > 1 ? 's' : ''}`}</option>
                    ))}
                  </select>
                </div>
              </div>

              {type === 'cron' && (
                <div className="rounded-xl border border-slate-800 bg-slate-950/90 p-4 text-sm text-slate-300">
                  A heartbeat URL will be generated after creation. Ping it when your job succeeds.
                </div>
              )}

              {error && <div className="rounded-xl border border-red-500/30 bg-red-500/10 px-4 py-3 text-sm text-red-300">{error}</div>}

              <div className="flex flex-col-reverse gap-3 border-t border-slate-100 pt-5 sm:flex-row sm:justify-end">
                <Link href="/dashboard/monitors" className="btn-secondary">Cancel</Link>
                <button type="submit" disabled={saving} className="btn-primary">{saving ? 'Creating...' : 'Create monitor'}</button>
              </div>
            </div>
          </form>
        </div>
      </DashboardLayout>
    </>
  );
}
