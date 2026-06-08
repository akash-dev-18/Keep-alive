'use client';

import { use, useEffect, useState } from 'react';
import { api, type PublicStatusPage } from '@/lib/api';
import StatusBadge from '@/components/ui/StatusBadge';
import { Skeleton } from '@/components/ui/Skeleton';
import { Activity, Clock } from 'lucide-react';

export default function PublicStatusPage({ params }: { params: Promise<{ slug: string }> }) {
  const { slug } = use(params);
  const [data, setData] = useState<PublicStatusPage | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  useEffect(() => {
    api.statusPages.getPublic(slug)
      .then(setData)
      .catch(() => setError('Status page not found'))
      .finally(() => setLoading(false));
  }, [slug]);

  if (loading) {
    return (
      <div className="min-h-screen bg-slate-50">
        <div className="max-w-2xl mx-auto px-4 py-16">
          <Skeleton className="h-6 w-48 mx-auto mb-10" />
          <div className="space-y-2">
            {[...Array(3)].map((_, i) => <div key={i} className="card p-4"><Skeleton className="h-4 w-full" /></div>)}
          </div>
        </div>
      </div>
    );
  }

  if (error || !data) {
    return (
      <div className="min-h-screen bg-slate-50 flex items-center justify-center">
        <div className="card p-8 text-center max-w-sm">
          <h1 className="text-base font-semibold text-slate-900 mb-1">Status page not found</h1>
          <p className="text-sm text-slate-500">{error || 'This status page does not exist or is private.'}</p>
        </div>
      </div>
    );
  }

  const { page, monitors, uptime, incidents } = data;
  const primaryColor = page.primaryColor || '#06b6d4';
  const allUp = monitors.every(m => m.status === 'up' || m.status === 'degraded');

  return (
    <div className="min-h-screen bg-slate-50">
      <div className="max-w-2xl mx-auto px-4 py-12">
        <div className="text-center mb-10">
          {page.logoUrl ? (
            <img src={page.logoUrl} alt="" className="h-12 mx-auto mb-4 object-contain" />
          ) : (
            <div className="w-12 h-12 rounded-2xl mx-auto mb-4 flex items-center justify-center border" style={{ borderColor: primaryColor, backgroundColor: `${primaryColor}15` }}>
              <Activity className="w-6 h-6" style={{ color: primaryColor }} />
            </div>
          )}
          <h1 className="text-xl font-bold text-slate-950 mb-2">{page.name}</h1>
          {page.description && <p className="text-sm text-slate-600 mb-3 max-w-md mx-auto">{page.description}</p>}
          <div className="flex items-center justify-center gap-2">
            <span className={`w-2 h-2 rounded-full ${allUp ? 'bg-emerald-500' : 'bg-red-500'}`} />
            <span className={`text-sm font-medium ${allUp ? 'text-emerald-700' : 'text-red-700'}`}>
              {allUp ? 'All systems operational' : 'Some systems degraded'}
            </span>
          </div>
        </div>

        <div className="space-y-2">
          {monitors.map((m) => (
            <div key={m.id} className="card p-4">
              <div className="flex items-center justify-between gap-4">
                <div className="min-w-0 flex-1">
                  <p className="text-sm font-medium text-slate-900">{m.name}</p>
                  {m.url && <p className="text-xs text-slate-500 font-mono truncate">{m.url}</p>}
                </div>
                <div className="text-right shrink-0">
                  <StatusBadge status={m.status} size="sm" />
                  {uptime[m.id] != null && (
                    <p className="text-xs text-slate-500 mt-1">{uptime[m.id]}% uptime (30d)</p>
                  )}
                </div>
              </div>
            </div>
          ))}
        </div>

        {incidents.length > 0 && (
          <section className="mt-10">
            <h2 className="text-sm font-semibold text-slate-950 mb-3">Recent incidents</h2>
            <div className="space-y-2">
              {incidents.slice(0, 10).map((i) => (
                <div key={i.id} className="card p-4 text-sm">
                  <div className="flex items-center justify-between">
                    <span className={i.status === 'open' ? 'text-red-700 font-medium' : 'text-emerald-700'}>{i.status}</span>
                    <span className="text-xs text-slate-500">{new Date(i.startedAt).toLocaleDateString()}</span>
                  </div>
                  {i.cause && <p className="mt-1 text-xs text-slate-600">{i.cause}</p>}
                </div>
              ))}
            </div>
          </section>
        )}

        <footer className="mt-16 text-center">
          <p className="text-xs text-slate-400">Powered by KeepAlive</p>
        </footer>
      </div>
    </div>
  );
}
