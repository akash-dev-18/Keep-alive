'use client';

import { useAuth } from '@clerk/nextjs';
import { useRouter } from 'next/navigation';
import { useEffect, useState } from 'react';
import { api, type DashboardSummary, type DashboardActivity, type Monitor, type MonitorCheck } from '@/lib/api';
import Navbar from '@/components/ui/Navbar';
import DashboardLayout from '@/app/dashboard/layout';
import StatsCards from '@/components/ui/StatsCards';
import MonitorCard from '@/components/ui/MonitorCard';
import ResponseTimeChart from '@/components/ui/ResponseTimeChart';
import EmptyState from '@/components/ui/EmptyState';
import { SkeletonCard, SkeletonChart, SkeletonStats } from '@/components/ui/Skeleton';
import { Activity, Clock, Plus, TrendingUp } from 'lucide-react';
import Link from 'next/link';

function timeAgo(dateStr: string): string {
  const diff = Date.now() - new Date(dateStr).getTime();
  const mins = Math.floor(diff / 60000);
  if (mins < 1) return 'just now';
  if (mins < 60) return `${mins}m ago`;
  const hrs = Math.floor(mins / 60);
  if (hrs < 24) return `${hrs}h ago`;
  return `${Math.floor(hrs / 24)}d ago`;
}

export default function DashboardPage() {
  const { userId } = useAuth();
  const router = useRouter();
  const [summary, setSummary] = useState<DashboardSummary | null>(null);
  const [monitors, setMonitors] = useState<Monitor[]>([]);
  const [recentChecks, setRecentChecks] = useState<MonitorCheck[]>([]);
  const [activity, setActivity] = useState<DashboardActivity | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    if (!userId) return;
    Promise.all([
      api.monitors.list(),
      api.dashboard.summary(),
      api.dashboard.activity(10),
    ]).then(async ([mon, sum, act]) => {
      setMonitors(mon);
      setSummary(sum);
      setActivity(act);
      const withRt = mon.find(m => m.lastResponseTimeMs != null);
      const target = withRt ?? mon.find(m => m.type === 'http') ?? mon[0];
      if (target) {
        api.monitors.checks(target.id).then(setRecentChecks).catch(() => {});
      }
    }).catch(console.error).finally(() => setLoading(false));
  }, [userId]);

  useEffect(() => {
    if (!userId) return;
    const refresh = () => {
      api.dashboard.summary().then(setSummary).catch(() => {});
      api.dashboard.activity(10).then(setActivity).catch(() => {});
      api.monitors.list().then(mon => {
        setMonitors(mon);
        const withRt = mon.find(m => m.lastResponseTimeMs != null);
        const target = withRt ?? mon.find(m => m.type === 'http') ?? mon[0];
        if (target) api.monitors.checks(target.id).then(setRecentChecks).catch(() => {});
      }).catch(() => {});
    };
    const interval = setInterval(refresh, 30000);
    return () => clearInterval(interval);
  }, [userId]);

  if (!userId) {
    return (
      <div className="flex min-h-screen items-center justify-center bg-slate-50 px-4">
        <div className="card max-w-sm p-8 text-center">
          <Activity className="mx-auto h-10 w-10 text-slate-700" />
          <h1 className="mt-4 text-xl font-semibold text-slate-950">Sign in required</h1>
          <p className="mt-2 text-sm leading-6 text-slate-600">Use your account to view monitors, incidents, and status pages.</p>
          <button onClick={() => router.push('/')} className="btn-primary mt-6">Go to home</button>
        </div>
      </div>
    );
  }

  const stats = summary ? [
    { label: 'Total', value: summary.totalMonitors, icon: 'total' as const },
    { label: 'Active', value: summary.activeMonitors, icon: 'active' as const },
    { label: 'Up', value: summary.upMonitors, icon: 'up' as const },
    { label: 'Down', value: summary.downMonitors, icon: 'down' as const },
    { label: 'Open incidents', value: summary.openIncidents, icon: 'warning' as const },
    { label: 'SSL warnings', value: summary.sslWarnings, icon: 'warning' as const },
  ] : [];

  const responseChecks = recentChecks.filter(c => c.responseTimeMs != null);
  const avgResponse = responseChecks.length > 0
    ? Math.round(responseChecks.reduce((a, c) => a + (c.responseTimeMs || 0), 0) / responseChecks.length)
    : null;
  const lastCheck = recentChecks[0]?.checkedAt;

  return (
    <>
      <Navbar />
      <DashboardLayout>
        {loading ? (
          <div className="space-y-6">
            <div className="flex items-center justify-between gap-4">
              <div className="space-y-2">
                <div className="skeleton h-7 w-40" />
                <div className="skeleton h-4 w-64" />
              </div>
              <div className="skeleton h-10 w-32 rounded-lg" />
            </div>
            <SkeletonStats />
            <SkeletonChart />
            <div className="grid gap-3 md:grid-cols-2 lg:grid-cols-3">
              {[...Array(3)].map((_, i) => <SkeletonCard key={i} />)}
            </div>
          </div>
        ) : monitors.length === 0 ? (
          <EmptyState
            icon={Activity}
            title="Create your first monitor"
            description="Start with an HTTP endpoint, SSL certificate, or cron heartbeat. Results appear here as soon as checks run."
            action={{ label: 'Create monitor', href: '/dashboard/monitors/new' }}
          />
        ) : (
          <div className="space-y-8">
            <div className="flex flex-col justify-between gap-4 sm:flex-row sm:items-end">
              <div>
                <p className="section-kicker">Workspace</p>
                <h1 className="page-title mt-2">Operations dashboard</h1>
                <p className="page-description">A current view of monitor health, response time, incidents, and SSL risk.</p>
              </div>
              <Link href="/dashboard/monitors/new" className="btn-primary">
                <Plus className="h-4 w-4" /> New monitor
              </Link>
            </div>

            <StatsCards stats={stats} />

            <section className="card p-5 sm:p-6">
              <div className="mb-5 flex flex-col justify-between gap-3 sm:flex-row sm:items-start">
                <div>
                  <div className="flex items-center gap-2 text-sm font-semibold text-slate-950">
                    <TrendingUp className="h-4 w-4 text-blue-700" /> Response time
                  </div>
                  <p className="mt-1 text-sm text-slate-500">Latest response samples from your first monitor.</p>
                </div>
                <div className="flex items-center gap-3 text-xs text-slate-500">
                  {lastCheck && <span className="inline-flex items-center gap-1"><Clock className="h-3.5 w-3.5" /> Last check {timeAgo(lastCheck)}</span>}
                  {avgResponse != null && <span className="rounded-full bg-slate-100 px-2.5 py-1 font-medium text-slate-700">Avg {avgResponse}ms</span>}
                </div>
              </div>
              <ResponseTimeChart data={recentChecks} />
            </section>

            {activity && (activity.recentFailures.length > 0 || activity.recentIncidents.length > 0) && (
              <section className="grid gap-4 lg:grid-cols-2">
                {activity.recentFailures.length > 0 && (
                  <div className="card p-5">
                    <h2 className="text-base font-semibold text-slate-950">Recent failures</h2>
                    <ul className="mt-3 space-y-2">
                      {activity.recentFailures.slice(0, 5).map((c) => (
                        <li key={c.id} className="text-sm text-slate-600">
                          <span className="font-medium text-red-700">Down</span> — {timeAgo(c.checkedAt)}
                          {c.errorMessage && <span className="block text-xs text-slate-500 truncate">{c.errorMessage}</span>}
                        </li>
                      ))}
                    </ul>
                  </div>
                )}
                {activity.recentIncidents.length > 0 && (
                  <div className="card p-5">
                    <h2 className="text-base font-semibold text-slate-950">Recent incidents</h2>
                    <ul className="mt-3 space-y-2">
                      {activity.recentIncidents.slice(0, 5).map((i) => (
                        <li key={i.id} className="text-sm">
                          <span className={i.status === 'open' ? 'text-red-700' : 'text-emerald-700'}>{i.status}</span>
                          <span className="text-slate-500"> — {timeAgo(i.startedAt)}</span>
                        </li>
                      ))}
                    </ul>
                  </div>
                )}
              </section>
            )}

            <section>
              <div className="mb-4 flex items-center justify-between">
                <div>
                  <h2 className="text-base font-semibold text-slate-950">Monitors</h2>
                  <p className="mt-1 text-sm text-slate-500">{monitors.length} configured checks across this workspace.</p>
                </div>
                <Link href="/dashboard/monitors" className="text-sm font-medium text-blue-700 hover:text-blue-900">View all</Link>
              </div>
              <div className="grid gap-3 md:grid-cols-2 lg:grid-cols-3">
                {monitors.slice(0, 6).map((m) => <MonitorCard key={m.id} m={m} />)}
              </div>
            </section>
          </div>
        )}
      </DashboardLayout>
    </>
  );
}
