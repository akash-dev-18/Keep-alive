'use client';

import { useAuth } from '@clerk/nextjs';
import { useEffect, useState } from 'react';
import { api, type AlertLog, type Monitor } from '@/lib/api';
import Navbar from '@/components/ui/Navbar';
import DashboardLayout from '@/app/dashboard/layout';
import { Skeleton } from '@/components/ui/Skeleton';
import EmptyState from '@/components/ui/EmptyState';
import { AlertTriangle, Bell, CheckCircle, Clock, Mail, XCircle } from 'lucide-react';
import Link from 'next/link';

export default function AlertsPage() {
  const { userId } = useAuth();
  const [logs, setLogs] = useState<AlertLog[]>([]);
  const [monitors, setMonitors] = useState<Map<string, string>>(new Map());
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    if (!userId) return;
    Promise.all([
      api.alertLogs.list(),
      api.monitors.list(),
    ]).then(([l, m]) => {
      setLogs(l);
      setMonitors(new Map(m.map((m: Monitor) => [m.id, m.name])));
    }).catch(console.error).finally(() => setLoading(false));
  }, [userId]);

  return (
    <>
      <Navbar />
      <DashboardLayout>
        <div className="space-y-6">
          <div>
            <p className="section-kicker">Alerts</p>
            <h1 className="page-title mt-2">Alert Logs</h1>
            <p className="page-description">
              {logs.length > 0 ? `${logs.length} alert${logs.length !== 1 ? 's' : ''} sent` : 'Audit trail for all alert dispatches'}
            </p>
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
          ) : logs.length === 0 ? (
            <EmptyState
              icon={Bell}
              title="No alerts sent"
              description="Alert logs will appear here when incidents trigger notifications."
            />
          ) : (
            <div className="space-y-2">
              {logs.map((log) => {
                const isError = log.status === 'failed';
                return (
                  <div key={log.id} className={`card p-4 interactive-card ${isError ? 'border-red-200 bg-red-50/30' : ''}`}>
                    <div className="flex items-start gap-3">
                      <div className={`w-7 h-7 rounded-lg border flex items-center justify-center shrink-0 mt-0.5 ${
                        isError ? 'bg-red-50 border-red-200' : 'bg-emerald-50 border-emerald-200'
                      }`}>
                        {isError ? (
                          <XCircle className="w-3.5 h-3.5 text-red-600" />
                        ) : (
                          <CheckCircle className="w-3.5 h-3.5 text-emerald-600" />
                        )}
                      </div>
                      <div className="flex-1 min-w-0">
                        <div className="flex items-center gap-2 mb-0.5">
                          <span className="text-sm font-semibold text-slate-900 truncate">
                            {monitors.get(log.monitorId) || 'Unknown monitor'}
                          </span>
                          <span className={`text-xs uppercase tracking-wider px-1.5 py-0.5 rounded-full ${
                            log.status === 'sent' ? 'text-emerald-700 bg-emerald-50' : 'text-red-700 bg-red-50'
                          }`}>{log.status}</span>
                        </div>
                        <div className="flex flex-wrap items-center gap-x-3 gap-y-1 text-xs text-slate-500 mt-1">
                          <span className="inline-flex items-center gap-1"><AlertTriangle className="w-3 h-3" /> {log.alertType}</span>
                          <span className="inline-flex items-center gap-1"><Mail className="w-3 h-3" /> {log.channel}</span>
                          <span className="font-mono">{log.sentTo}</span>
                        </div>
                        {log.errorMessage && (
                          <p className="text-xs text-red-600 mt-1">{log.errorMessage}</p>
                        )}
                        <div className="flex items-center gap-1 text-xs text-slate-400 mt-1">
                          <Clock className="w-3 h-3" />
                          <span>{new Date(log.sentAt).toLocaleString()}</span>
                        </div>
                      </div>
                    </div>
                  </div>
                );
              })}
            </div>
          )}
        </div>
      </DashboardLayout>
    </>
  );
}
