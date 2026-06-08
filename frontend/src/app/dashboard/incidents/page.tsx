'use client';

import { useAuth } from '@clerk/nextjs';
import { useEffect, useState } from 'react';
import { api, type Incident } from '@/lib/api';
import Navbar from '@/components/ui/Navbar';
import DashboardLayout from '@/app/dashboard/layout';
import { Skeleton } from '@/components/ui/Skeleton';
import EmptyState from '@/components/ui/EmptyState';
import { AlertTriangle, CheckCircle, Clock, ExternalLink } from 'lucide-react';
import Link from 'next/link';

export default function IncidentsPage() {
  const { userId } = useAuth();
  const [incidents, setIncidents] = useState<(Incident & { monitorName?: string })[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    if (!userId) return;
    (async () => {
      try {
        const [monitors, incs] = await Promise.all([
          api.monitors.list(),
          api.incidents.list(50),
        ]);
        const nameMap = Object.fromEntries(monitors.map(m => [m.id, m.name]));
        setIncidents(incs.map(i => ({ ...i, monitorName: nameMap[i.monitorId] })));
      } catch (e) { console.error(e); }
      finally { setLoading(false); }
    })();
  }, [userId]);

  const openIncidents = incidents.filter(i => i.status === 'open');

  return (
    <>
      <Navbar />
      <DashboardLayout>
        <div className="space-y-6">
          <div className="flex items-center justify-between">
            <div>
              <p className="section-kicker">Incidents</p>
              <h1 className="page-title mt-2">Incidents</h1>
              <p className="page-description">
                {incidents.length} total{openIncidents.length > 0 && <span className="text-red-600 ml-1">&middot; {openIncidents.length} open</span>}
              </p>
            </div>
          </div>

          {loading ? (
            <div className="space-y-2">
              {[...Array(4)].map((_, i) => (
                <div key={i} className="card p-4">
                  <div className="flex items-center gap-3">
                    <Skeleton className="w-4 h-4 rounded" />
                    <div className="flex-1 space-y-2">
                      <Skeleton className="h-4 w-48" />
                      <Skeleton className="h-3 w-64" />
                    </div>
                    <Skeleton className="h-5 w-16 rounded-full" />
                  </div>
                </div>
              ))}
            </div>
          ) : incidents.length === 0 ? (
            <EmptyState
              icon={CheckCircle}
              title="No incidents"
              description="Everything is running smoothly. No downtime events recorded."
            />
          ) : (
            <div className="space-y-2">
              {incidents.map((inc) => (
                <div key={inc.id} className="card p-4 interactive-card">
                  <div className="flex items-start gap-3">
                    {inc.status === 'resolved' ? (
                      <div className="w-7 h-7 rounded-lg bg-emerald-50 border border-emerald-200 flex items-center justify-center shrink-0 mt-0.5">
                        <CheckCircle className="w-3.5 h-3.5 text-emerald-600" />
                      </div>
                    ) : (
                      <div className="w-7 h-7 rounded-lg bg-red-50 border border-red-200 flex items-center justify-center shrink-0 mt-0.5">
                        <AlertTriangle className="w-3.5 h-3.5 text-red-600" />
                      </div>
                    )}
                    <div className="flex-1 min-w-0">
                      <div className="flex items-center gap-2 mb-0.5">
                        <Link
                          href={`/dashboard/monitors/${inc.monitorId}`}
                          className="text-sm font-semibold text-slate-900 hover:text-blue-700 transition-colors truncate flex items-center gap-1"
                        >
                          {inc.monitorName || 'Unknown monitor'}
                          <ExternalLink className="w-2.5 h-2.5 text-slate-400" />
                        </Link>
                        <span className={`text-xs uppercase tracking-wider px-1.5 py-0.5 rounded-full shrink-0 ${
                          inc.status === 'resolved'
                            ? 'text-emerald-700 bg-emerald-50'
                            : 'text-red-700 bg-red-50'
                        }`}>{inc.status}</span>
                      </div>
                      <p className="text-sm text-slate-500 truncate">{inc.cause || 'No cause recorded'}</p>
                      <div className="flex items-center gap-2 text-xs text-slate-400 mt-1">
                        <Clock className="w-3 h-3" />
                        <span>{new Date(inc.startedAt).toLocaleString()}</span>
                        {inc.resolvedAt && (
                          <>
                            <span className="text-slate-300">&rarr;</span>
                            <span>{new Date(inc.resolvedAt).toLocaleString()}</span>
                          </>
                        )}
                        {inc.durationSeconds && <span className="text-slate-400">({Math.round(inc.durationSeconds / 60)}m)</span>}
                      </div>
                    </div>
                  </div>
                </div>
              ))}
            </div>
          )}
        </div>
      </DashboardLayout>
    </>
  );
}
