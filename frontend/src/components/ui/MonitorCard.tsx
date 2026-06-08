'use client';

import Link from 'next/link';
import StatusBadge from './StatusBadge';
import { Globe, Lock, Timer, Search } from 'lucide-react';
import type { Monitor } from '@/lib/api';

const typeConfig: Record<string, { icon: typeof Globe; label: string; detail: string }> = {
  http: { icon: Globe, label: 'HTTP', detail: 'Endpoint check' },
  ssl: { icon: Lock, label: 'SSL', detail: 'Certificate check' },
  cron: { icon: Timer, label: 'Heartbeat', detail: 'Heartbeat check' },
  keyword: { icon: Search, label: 'Keyword', detail: 'Content check' },
};

function timeAgo(dateStr: string): string {
  const diff = Date.now() - new Date(dateStr).getTime();
  const mins = Math.floor(diff / 60000);
  if (mins < 1) return 'just now';
  if (mins < 60) return `${mins}m ago`;
  const hrs = Math.floor(mins / 60);
  if (hrs < 24) return `${hrs}h ago`;
  return `${Math.floor(hrs / 24)}d ago`;
}

export default function MonitorCard({ m }: { m: Monitor }) {
  const cfg = typeConfig[m.type] || { icon: Globe, label: m.type, detail: 'Monitor' };

  return (
    <Link href={`/dashboard/monitors/${m.id}`} className="group block">
      <article className="card interactive-card p-5">
        <div className="flex items-start justify-between gap-4">
          <div className="flex min-w-0 items-start gap-3">
            <div className="flex h-10 w-10 shrink-0 items-center justify-center rounded-xl border border-slate-200 bg-slate-50">
              <cfg.icon className="h-4 w-4 text-slate-600" />
            </div>
            <div className="min-w-0">
              <div className="flex items-center gap-2">
                <h3 className="truncate text-sm font-semibold text-slate-950 group-hover:text-slate-700">{m.name}</h3>
                {!m.active && <span className="rounded-full bg-amber-50 px-2 py-0.5 text-xs font-medium text-amber-700 ring-1 ring-amber-200">Paused</span>}
              </div>
              <p className="mt-1 truncate font-mono text-xs text-slate-500">{m.url || cfg.detail}</p>
            </div>
          </div>
          <StatusBadge status={m.active ? m.status : 'paused'} size="sm" showLabel={false} />
        </div>
        <div className="mt-5 flex items-center justify-between border-t border-slate-100 pt-4 text-xs text-slate-500">
          <span className="font-medium text-slate-700">{cfg.label}</span>
          <div className="flex items-center gap-2">
            {m.lastResponseTimeMs != null && (
              <span className="font-mono text-slate-600">{m.lastResponseTimeMs}ms</span>
            )}
            {m.lastCheckedAt && <span>{timeAgo(m.lastCheckedAt)}</span>}
            {!m.lastCheckedAt && <span>Every {m.checkIntervalMin}m</span>}
          </div>
        </div>
      </article>
    </Link>
  );
}
