'use client';

import { useAuth } from '@clerk/nextjs';
import { useEffect, useState } from 'react';
import { api, heartbeatUrl, type Monitor } from '@/lib/api';
import Navbar from '@/components/ui/Navbar';
import DashboardLayout from '@/app/dashboard/layout';
import StatusBadge from '@/components/ui/StatusBadge';
import { SkeletonCard } from '@/components/ui/Skeleton';
import EmptyState from '@/components/ui/EmptyState';
import { Timer, Clock, CheckCircle, AlertTriangle, XCircle, ExternalLink, Copy } from 'lucide-react';
import Link from 'next/link';

export default function CronPage() {
  const { userId } = useAuth();
  const [monitors, setMonitors] = useState<Monitor[]>([]);
  const [loading, setLoading] = useState(true);
  const [copied, setCopied] = useState<string | null>(null);

  useEffect(() => {
    if (!userId) return;
    api.monitors.list()
      .then((all) => setMonitors(all.filter(m => m.type === 'cron')))
      .catch(console.error)
      .finally(() => setLoading(false));
  }, [userId]);

  const healthy = monitors.filter(m => m.status === 'up');
  const down = monitors.filter(m => m.status === 'down');
  const unknown = monitors.filter(m => m.status === 'unknown' || m.status === 'pending');

  function copyPingUrl(pingKey: string) {
    if (!pingKey) return;
    const url = heartbeatUrl(pingKey);
    navigator.clipboard.writeText(url).then(() => {
      setCopied(pingKey);
      setTimeout(() => setCopied(null), 2000);
    });
  }

  return (
    <>
      <Navbar />
      <DashboardLayout>
        <div className="space-y-6">
          <div>
            <p className="section-kicker">Cron</p>
            <h1 className="page-title mt-2">Cron Heartbeats</h1>
            <p className="page-description">
              {monitors.length} heartbeat{monitors.length !== 1 ? 's' : ''} monitored
              {down.length > 0 && <span className="text-red-600 ml-1">&middot; {down.length} missed</span>}
            </p>
          </div>

          {loading ? (
            <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-3">
              {[...Array(3)].map((_, i) => <SkeletonCard key={i} />)}
            </div>
          ) : monitors.length === 0 ? (
            <EmptyState
              icon={Timer}
              title="No cron monitors"
              description="Create a cron heartbeat monitor to get notified when scheduled jobs miss a ping."
              action={{ label: 'Create cron monitor', href: '/dashboard/monitors/new' }}
            />
          ) : (
            <div className="space-y-3">
              {[
                { label: 'Healthy', items: healthy, icon: CheckCircle, color: 'text-emerald-600', bg: 'bg-emerald-50', border: 'border-emerald-200' },
                { label: 'Missed', items: down, icon: XCircle, color: 'text-red-600', bg: 'bg-red-50', border: 'border-red-200' },
                { label: 'Pending', items: unknown, icon: Clock, color: 'text-slate-600', bg: 'bg-slate-50', border: 'border-slate-200' },
              ].map(section => section.items.length > 0 && (
                <div key={section.label}>
                  <p className="text-sm font-semibold text-slate-700 mb-2">{section.label}</p>
                  <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-3">
                    {section.items.map(m => (
                      <div key={m.id} className={`card p-4 ${section.bg} ${section.border}`}>
                        <div className="flex items-start justify-between mb-3">
                          <div className="flex items-center gap-3 min-w-0">
                            <div className={`${section.bg} ${section.color} w-9 h-9 rounded-lg border ${section.border} flex items-center justify-center`}>
                              <section.icon className="w-4 h-4" />
                            </div>
                            <div className="min-w-0">
                              <Link href={`/dashboard/monitors/${m.id}`} className="text-sm font-semibold text-slate-900 hover:text-blue-700 transition-colors truncate block">
                                {m.name}
                              </Link>
                              <p className="text-xs text-slate-500">Every {m.checkIntervalMin}m</p>
                            </div>
                          </div>
                          <StatusBadge status={m.status} size="sm" showLabel={false} />
                        </div>
                        {m.pingKey && (
                          <div className="mt-3 pt-3 border-t border-inherit">
                            <div className="flex items-center justify-between gap-2">
                              <span className="text-xs text-slate-500 font-mono truncate">
                                {heartbeatUrl(m.pingKey!)}
                              </span>
                              <button onClick={() => copyPingUrl(m.pingKey!)} className="btn-secondary text-xs p-1.5 shrink-0">
                                <Copy className="w-3 h-3" /> {copied === m.pingKey ? 'Copied!' : 'Copy'}
                              </button>
                            </div>
                          </div>
                        )}
                      </div>
                    ))}
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
