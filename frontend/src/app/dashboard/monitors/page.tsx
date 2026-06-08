'use client';

import { useAuth } from '@clerk/nextjs';
import { useEffect, useState } from 'react';
import { api, type Monitor } from '@/lib/api';
import Navbar from '@/components/ui/Navbar';
import DashboardLayout from '@/app/dashboard/layout';
import MonitorCard from '@/components/ui/MonitorCard';
import EmptyState from '@/components/ui/EmptyState';
import { SkeletonCard } from '@/components/ui/Skeleton';
import { Monitor as MonitorIcon, Plus, Search } from 'lucide-react';
import Link from 'next/link';

export default function MonitorsPage() {
  const { userId } = useAuth();
  const [monitors, setMonitors] = useState<Monitor[]>([]);
  const [search, setSearch] = useState('');
  const [sortBy, setSortBy] = useState<'name' | 'status' | 'type'>('name');
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    if (!userId) return;
    const load = () => api.monitors.list().then(setMonitors).catch(console.error);
    load().finally(() => setLoading(false));
    const interval = setInterval(load, 30000);
    return () => clearInterval(interval);
  }, [userId]);

  const filtered = monitors
    .filter(m =>
      m.name.toLowerCase().includes(search.toLowerCase()) ||
      (m.url && m.url.toLowerCase().includes(search.toLowerCase()))
    )
    .sort((a, b) => {
      if (sortBy === 'status') return a.status.localeCompare(b.status);
      if (sortBy === 'type') return a.type.localeCompare(b.type);
      return a.name.localeCompare(b.name);
    });

  const downCount = monitors.filter(m => m.status === 'down').length;

  return (
    <>
      <Navbar />
      <DashboardLayout>
        <div className="space-y-6">
          <div className="flex flex-col justify-between gap-4 sm:flex-row sm:items-end">
            <div>
              <p className="section-kicker">Monitoring</p>
              <h1 className="page-title mt-2">Monitors</h1>
              <p className="page-description">
                {monitors.length} total checks{downCount > 0 ? `, ${downCount} currently down` : ' across HTTP, SSL, and cron jobs'}.
              </p>
            </div>
            <Link href="/dashboard/monitors/new" className="btn-primary">
              <Plus className="h-4 w-4" /> New monitor
            </Link>
          </div>

          {/* <div className="card flex flex-col gap-3 p-3 sm:flex-row sm:items-center sm:justify-between">
            <div className="relative w-full sm:max-w-sm">
              <Search className="pointer-events-none absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-slate-400" />
              <input
                type="search"
                placeholder="Search monitors"
                value={search}
                onChange={(e) => setSearch(e.target.value)}
                className="form-input pl-9"
              />
            </div>
            <div className="flex rounded-lg border border-slate-200 bg-slate-50 p-1">
              {(['name', 'status', 'type'] as const).map((s) => (
                <button
                  key={s}
                  type="button"
                  onClick={() => setSortBy(s)}
                  className={`rounded-md px-3 py-1.5 text-sm font-medium capitalize transition-colors ${
                    sortBy === s ? 'bg-white text-slate-950 shadow-sm shadow-slate-950/5' : 'text-slate-500 hover:text-slate-950'
                  }`}
                >
                  {s}
                </button>
              ))}
            </div>
          </div> */}
          <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between bg-zinc-900/50 border border-zinc-800 rounded-xl p-3">
  <div className="relative w-full sm:max-w-sm">
    <Search className="pointer-events-none absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-zinc-500" />
    <input
      type="search"
      placeholder="Search monitors..."
      value={search}
      onChange={(e) => setSearch(e.target.value)}
      className="w-full rounded-lg bg-zinc-900 border border-zinc-800 pl-9 pr-3 py-2 text-sm text-white placeholder:text-zinc-600 focus:outline-none focus:border-blue-500/40 focus:ring-1 focus:ring-blue-500/10 transition-colors"
    />
  </div>
  <div className="flex rounded-lg border border-zinc-800 bg-zinc-900 p-1 gap-0.5">
    {(['name', 'status', 'type'] as const).map((s) => (
      <button
        key={s}
        type="button"
        onClick={() => setSortBy(s)}
        className={`rounded-md px-3 py-1.5 text-xs font-medium capitalize transition-all ${
          sortBy === s 
            ? 'bg-zinc-700 text-white' 
            : 'text-zinc-500 hover:text-zinc-300'
        }`}
      >
        {s}
      </button>
    ))}
  </div>
</div>

          {loading ? (
            <div className="grid gap-3 md:grid-cols-2 lg:grid-cols-3">
              {[...Array(6)].map((_, i) => <SkeletonCard key={i} />)}
            </div>
          ) : filtered.length === 0 ? (
            <EmptyState
              icon={MonitorIcon}
              title={monitors.length === 0 ? 'No monitors yet' : 'No monitors match your search'}
              description={monitors.length === 0 ? 'Add your first endpoint, certificate, or cron heartbeat.' : 'Try a different service name, URL, or monitor type.'}
              action={monitors.length === 0 ? { label: 'Create monitor', href: '/dashboard/monitors/new' } : undefined}
            />
          ) : (
            <div className="grid gap-3 md:grid-cols-2 lg:grid-cols-3">
              {filtered.map((m) => <MonitorCard key={m.id} m={m} />)}
            </div>
          )}
        </div>
      </DashboardLayout>
    </>
  );
}
