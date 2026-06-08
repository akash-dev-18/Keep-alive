'use client';

import { Activity, AlertTriangle, ArrowDown, ArrowUp } from 'lucide-react';

interface Stat { label: string; value: number; icon: 'up' | 'down' | 'warning' | 'total' | 'active'; }

const config = {
  up: { Icon: ArrowUp, color: 'text-emerald-700', bg: 'bg-emerald-50', label: 'Healthy services' },
  down: { Icon: ArrowDown, color: 'text-red-700', bg: 'bg-red-50', label: 'Needs attention' },
  warning: { Icon: AlertTriangle, color: 'text-amber-700', bg: 'bg-amber-50', label: 'Active incidents' },
  total: { Icon: Activity, color: 'text-blue-700', bg: 'bg-blue-50', label: 'Total monitors' },
  active: { Icon: Activity, color: 'text-slate-700', bg: 'bg-slate-100', label: 'Currently checking' },
};

export default function StatsCards({ stats }: { stats: Stat[] }) {
  return (
    <section className="grid grid-cols-2 gap-3 lg:grid-cols-3 xl:grid-cols-6">
      {stats.map((s) => {
        const { Icon, color, bg, label } = config[s.icon];
        return (
          <div key={s.label} className="card p-5">
            <div className="flex items-start justify-between gap-3">
              <div>
                <p className="text-sm font-medium text-slate-600">{s.label}</p>
                <p className="mt-2 text-3xl font-semibold tracking-tight text-slate-950 tabular-nums">{s.value}</p>
              </div>
              <span className={`flex h-9 w-9 items-center justify-center rounded-xl ${bg} ${color}`}>
                <Icon className="h-4 w-4" />
              </span>
            </div>
            <p className="mt-3 text-xs text-slate-500">{label}</p>
          </div>
        );
      })}
    </section>
  );
}
