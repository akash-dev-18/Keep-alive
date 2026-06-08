'use client';

import { useAuth } from '@clerk/nextjs';
import { useEffect, useState } from 'react';
import { api, type Monitor } from '@/lib/api';
import Navbar from '@/components/ui/Navbar';
import DashboardLayout from '@/app/dashboard/layout';
import StatusBadge from '@/components/ui/StatusBadge';
import { SkeletonCard } from '@/components/ui/Skeleton';
import EmptyState from '@/components/ui/EmptyState';
import { Shield, ShieldAlert, ShieldCheck, Lock, ExternalLink } from 'lucide-react';
import Link from 'next/link';

export default function SSLPage() {
  const { userId } = useAuth();
  const [monitors, setMonitors] = useState<Monitor[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    if (!userId) return;
    api.monitors.list()
      .then((all) => setMonitors(all.filter(m => m.type === 'ssl')))
      .catch(console.error)
      .finally(() => setLoading(false));
  }, [userId]);

  const expired = monitors.filter(m => m.status === 'down');
  const warning = monitors.filter(m => m.status === 'unknown');
  const valid = monitors.filter(m => m.status === 'up');

  return (
    <>
      <Navbar />
      <DashboardLayout>
        <div className="space-y-6">
          <div>
            <p className="section-kicker">SSL</p>
            <h1 className="page-title mt-2">SSL Certificates</h1>
            <p className="page-description">
              {monitors.length} certificate{monitors.length !== 1 ? 's' : ''} monitored
              {expired.length > 0 && <span className="text-red-600 ml-1">&middot; {expired.length} expired</span>}
            </p>
          </div>

          {loading ? (
            <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-3">
              {[...Array(3)].map((_, i) => <SkeletonCard key={i} />)}
            </div>
          ) : monitors.length === 0 ? (
            <EmptyState
              icon={Shield}
              title="No SSL monitors"
              description="Create an SSL monitor to track certificate expiry dates."
              action={{ label: 'Create SSL monitor', href: '/dashboard/monitors/new' }}
            />
          ) : (
            <div className="space-y-3">
              {[
                { label: 'Expired', items: expired, icon: ShieldAlert, color: 'text-red-600', bg: 'bg-red-50', border: 'border-red-200' },
                { label: 'Expiring soon', items: warning, icon: ShieldAlert, color: 'text-amber-600', bg: 'bg-amber-50', border: 'border-amber-200' },
                { label: 'Valid', items: valid, icon: ShieldCheck, color: 'text-emerald-600', bg: 'bg-emerald-50', border: 'border-emerald-200' },
              ].map(section => section.items.length > 0 && (
                <div key={section.label}>
                  <p className="text-sm font-semibold text-slate-700 mb-2">{section.label}</p>
                  <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-3">
                    {section.items.map(m => (
                      <Link key={m.id} href={`/dashboard/monitors/${m.id}`} className="block group">
                        <div className={`card p-4 interactive-card ${section.bg} ${section.border}`}>
                          <div className="flex items-center justify-between mb-3">
                            <div className="flex items-center gap-3 min-w-0">
                              <div className={`${section.bg} ${section.color} w-9 h-9 rounded-lg border ${section.border} flex items-center justify-center`}>
                                <section.icon className="w-4 h-4" />
                              </div>
                              <div className="min-w-0">
                                <div className="flex items-center gap-2">
                                  <h3 className="text-sm font-semibold text-slate-900 group-hover:text-blue-700 transition-colors truncate">{m.name}</h3>
                                  <ExternalLink className="w-3 h-3 text-slate-400 shrink-0" />
                                </div>
                                {m.url && <p className="text-xs text-slate-500 truncate font-mono">{m.url}</p>}
                              </div>
                            </div>
                            <StatusBadge status={m.status} size="sm" showLabel={false} />
                          </div>
                          <div className="flex items-center gap-1.5 text-xs text-slate-500">
                            <Lock className="w-3 h-3" />
                            <span>{m.status === 'up' ? 'Certificate valid' : m.status === 'down' ? 'Certificate error' : 'Checking...'}</span>
                          </div>
                        </div>
                      </Link>
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
