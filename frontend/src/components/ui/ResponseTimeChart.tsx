'use client';

import { XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer, Area, AreaChart } from 'recharts';

interface Check {
  responseTimeMs: number | null;
  checkedAt: string;
  status: string;
}

export default function ResponseTimeChart({ data }: { data: Check[] }) {
  const chartData = data
    .filter(c => c.responseTimeMs != null)
    .slice(0, 60).reverse()
    .map(c => ({
      time: new Date(c.checkedAt).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }),
      ms: c.responseTimeMs,
    }));

  if (!chartData.length) return (
    <div className="flex h-56 items-center justify-center rounded-xl border border-dashed border-slate-200 bg-slate-50 text-sm text-slate-500">
      Response data will appear after the first successful check.
    </div>
  );

  return (
    <div className="h-56 w-full">
      <ResponsiveContainer width="100%" height="100%">
        <AreaChart data={chartData} margin={{ left: 0, right: 0, top: 8, bottom: 0 }}>
          <defs>
            <linearGradient id="responseGradient" x1="0" y1="0" x2="0" y2="1">
              <stop offset="0%" stopColor="#2563eb" stopOpacity={0.16} />
              <stop offset="100%" stopColor="#2563eb" stopOpacity={0.02} />
            </linearGradient>
          </defs>
          <CartesianGrid strokeDasharray="3 3" stroke="#e5e7eb" vertical={false} />
          <XAxis dataKey="time" tick={{ fontSize: 11, fill: '#64748b' }} axisLine={false} tickLine={false} minTickGap={30} />
          <YAxis tick={{ fontSize: 11, fill: '#64748b' }} axisLine={false} tickLine={false} width={42} domain={['dataMin - 50', 'dataMax + 50']} />
          <Tooltip
            contentStyle={{ background: '#ffffff', border: '1px solid #e5e7eb', borderRadius: '10px', fontSize: '12px', boxShadow: '0 10px 30px rgba(15, 23, 42, 0.08)' }}
            labelStyle={{ color: '#475569', marginBottom: '4px' }}
            formatter={(value) => [`${value}ms`, 'Response time']}
          />
          <Area type="monotone" dataKey="ms" stroke="#2563eb" strokeWidth={1.8} fill="url(#responseGradient)" dot={false} activeDot={{ r: 3, fill: '#2563eb', strokeWidth: 0 }} />
        </AreaChart>
      </ResponsiveContainer>
    </div>
  );
}
