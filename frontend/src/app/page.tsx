'use client';

import { useState, useEffect } from 'react';
import Link from 'next/link';
import Navbar from '@/components/ui/Navbar';
import {
  ArrowRight, Bell, CheckCircle,
  Globe, Lock, Server, Timer,
  Activity, BarChart3
} from 'lucide-react';
import { motion, type Variants } from 'framer-motion';

const features = [
  {
    icon: Globe,
    title: 'HTTP and API checks',
    desc: 'Monitor status codes, latency, redirects, and timeouts for production endpoints.',
  },
  {
    icon: Lock,
    title: 'SSL certificate tracking',
    desc: 'Catch certificate failures and expiry windows before browsers or customers do.',
  },
  {
    icon: Timer,
    title: 'Cron heartbeat monitoring',
    desc: 'Give scheduled jobs a ping URL and detect missed runs with configurable grace periods.',
  },
  {
    icon: Bell,
    title: 'Incident notifications',
    desc: 'Create alerts, internal notifications, and incident history from the same source of truth.',
  },
];

const VISIBLE_BARS = 40;

const fadeUp: Variants = {
  hidden: { opacity: 0, y: 12 },
  visible: { opacity: 1, y: 0, transition: { duration: 0.4, ease: [0.25, 0.1, 0.25, 1] } }
};

const containerVariants: Variants = {
  hidden: {},
  visible: { transition: { staggerChildren: 0.05 } }
};

export default function HomePage() {
  const [ticker, setTicker] = useState(0);

  useEffect(() => {
    let frame: number;
    const loop = () => {
      setTicker(prev => prev + 0.025);
      frame = requestAnimationFrame(loop);
    };
    frame = requestAnimationFrame(loop);
    return () => cancelAnimationFrame(frame);
  }, []);

  const chartData = Array.from({ length: VISIBLE_BARS }).map((_, i) => {
    const wave = Math.sin(ticker * 0.6 + i * 0.25) * 18
              + Math.sin(ticker * 0.4 + i * 0.15) * 10
              + 52;
    return Math.max(16, Math.min(90, wave));
  });

  return (
    <div className="min-h-screen bg-black text-white antialiased app-grid">
      <Navbar />

      <main>
        {/* ═══════════ HERO ═══════════ */}
        <section className="relative overflow-hidden border-b border-zinc-800/50">
          {/* Ambient light */}
          <div className="pointer-events-none absolute inset-x-0 top-0 h-[60vh] bg-[radial-gradient(ellipse_at_40%_0%,rgba(56,189,248,0.08),transparent_60%)]" />
          <div className="pointer-events-none absolute inset-x-0 top-0 h-[40vh] bg-[radial-gradient(ellipse_at_70%_10%,rgba(139,92,246,0.04),transparent_60%)]" />

          <div className="container-page relative z-10 pt-16 pb-14 lg:pt-24 lg:pb-16">
            <div className="grid gap-10 lg:grid-cols-[1fr_1.2fr] lg:items-center">

              {/* ── Left column ── */}
              <motion.div variants={containerVariants} initial="hidden" animate="visible">
                <motion.div variants={fadeUp}>
                  <span className="inline-flex items-center gap-1.5 rounded-md border border-zinc-800 bg-zinc-900/50 px-2 py-0.5 text-[11px] font-medium text-zinc-400">
                    <Activity className="h-3 w-3 text-blue-400" />
                    Uptime monitoring for small teams
                  </span>
                </motion.div>

               <motion.h1 variants={fadeUp} className="mt-5 text-[clamp(2.25rem,4.5vw,3.75rem)] font-bold tracking-[-0.035em] text-white leading-[1.04]">
  Know the moment
  <span className="block bg-gradient-to-r from-sky-400 to-violet-400 bg-clip-text text-transparent">
  production breaks.
</span>
</motion.h1>

          <motion.p variants={fadeUp} className="mt-4 max-w-md text-sm leading-6 text-zinc-500 sm:text-base sm:leading-7">
  I built this because I kept missing downtime on my own projects. It checks your endpoints, SSL certs, and cron jobs — and texts you when something breaks.
</motion.p>

                <motion.div variants={fadeUp} className="mt-7 flex items-center gap-3">
                  <Link
                    href="/dashboard"
                    className="btn-primary inline-flex items-center gap-1.5"
                  >
                    Start monitoring
                    <ArrowRight className="h-3.5 w-3.5" />
                  </Link>
                  <a
                    href="#features"
                    className="btn-secondary inline-flex items-center gap-1.5"
                  >
                    View features
                  </a>
                </motion.div>

                <motion.p variants={fadeUp} className="mt-2.5 text-xs text-zinc-600">
  Made by <span className="text-zinc-400">@akash_ak_18_</span> · Free tier, no credit card
</motion.p>

                {/* Trust indicators */}
                <motion.div variants={fadeUp} className="mt-8 flex items-center gap-6 border-t border-zinc-800/50 pt-5">
                  {[
                    { value: '90.45%', label: 'Uptime' },
                    { value: '6 Monitors Active', label: 'Endpoints' },
                    { value: '184ms', label: 'Avg response' },
                  ].map(item => (
                    <div key={item.label} className="flex items-baseline gap-1.5">
                      <span className="text-sm font-semibold text-white">{item.value}</span>
                      <span className="text-xs text-zinc-600">{item.label}</span>
                    </div>
                  ))}
                </motion.div>
              </motion.div>

              {/* ── Right column: Dashboard preview ── */}
              <motion.div
                initial={{ opacity: 0, y: 20, scale: 0.97 }}
                animate={{ opacity: 1, y: 0, scale: 1 }}
                transition={{ duration: 0.5, delay: 0.1, ease: [0.25, 0.1, 0.25, 1] }}
              >
                  <div className="relative rounded-2xl border border-zinc-800/70 bg-[#050505] shadow-[0_40px_120px_rgba(0,0,0,0.7)] overflow-hidden">
                  {/* Window chrome */}
                  <div className="flex items-center gap-4 border-b border-zinc-800/50 px-4 py-2.5">
                    <div className="flex items-center gap-1.5">
                      <div className="h-2.5 w-2.5 rounded-full bg-zinc-700" />
                      <div className="h-2.5 w-2.5 rounded-full bg-zinc-700" />
                      <div className="h-2.5 w-2.5 rounded-full bg-zinc-700" />
                    </div>
                    <span className="text-[11px] font-medium text-zinc-600">
                      keepalive.app/dashboard
                    </span>
                    <div className="ml-auto flex items-center gap-2">
                      <span className="flex items-center gap-1.5 rounded bg-emerald-500/8 px-2 py-0.5 text-[11px] font-medium text-emerald-400">
                        <span className="h-1.5 w-1.5 rounded-full bg-emerald-500" />
                        All operational
                      </span>
                    </div>
                  </div>

                  {/* Dashboard body */}
                  <div className="p-4 space-y-4">
                    {/* Top stat cards */}
                    <div className="grid grid-cols-4 gap-2">
                      {[
                        { label: 'Monitors', value: '6', sub: 'All healthy' },
                        { label: 'Uptime (30d)', value: '99.97%', sub: '12 incidents' },
                        { label: 'SSL expiry', value: '14d', sub: 'api subdomain' },
                        { label: 'Last incident', value: '14h ago', sub: 'Resolved' },
                      ].map(s => (
                        <div key={s.label} className="rounded-lg border border-zinc-800/40 bg-zinc-900/40 px-3 py-2.5">
                          <div className="text-[10px] font-medium text-zinc-500 uppercase tracking-wider">{s.label}</div>
                          <div className="mt-0.5 text-base font-semibold text-white tracking-tight">{s.value}</div>
                          <div className="text-[11px] text-zinc-600">{s.sub}</div>
                        </div>
                      ))}
                    </div>

                    {/* Middle row: chart + timeline */}
                    <div className="grid grid-cols-[1.3fr_0.7fr] gap-2">
                      {/* Chart card */}
                      <div className="rounded-lg border border-zinc-800/40 bg-zinc-900/40 px-3 py-2.5">
                        <div className="flex items-center justify-between mb-2">
                          <div className="flex items-center gap-1.5">
                            <BarChart3 className="h-3 w-3 text-zinc-500" />
                            <span className="text-[11px] font-medium text-zinc-400">Response time (30 min)</span>
                          </div>
                          <span className="text-[11px] text-zinc-500">184ms avg</span>
                        </div>
                        <div className="flex items-end gap-[2px] h-12">
                          {chartData.map((h, i) => (
                            <div
                              key={i}
                              className="flex-1 transition-all duration-[60ms] rounded-[1px]"
                              style={{
                                height: `${h}%`,
                                backgroundColor: i > 28 && i < 35
                                  ? 'rgba(239,68,68,0.6)'
                                  : 'rgba(56,189,248,0.5)',
                              }}
                            />
                          ))}
                        </div>
                        <div className="mt-1.5 flex items-center justify-between text-[10px] text-zinc-600">
                          <span>09:30</span>
                          <span className="text-emerald-500/70">+2.1%</span>
                          <span>10:00</span>
                        </div>
                      </div>

                      {/* Activity timeline */}
                      <div className="rounded-lg border border-zinc-800/40 bg-zinc-900/40 px-3 py-2.5">
                        <span className="text-[11px] font-medium text-zinc-400">Recent activity</span>
                        <div className="mt-2 space-y-1.5">
                          {[
                            { label: 'api.keepalive.app', status: 'up' as const, time: '2m ago' },
                            { label: 'SSL check passed', status: 'up' as const, time: '7m ago' },
                            { label: 'Heartbeat missed', status: 'down' as const, time: '15m ago' },
                          ].map(evt => (
                            <div key={evt.label} className="flex items-center gap-2">
                              <span className={`h-1.5 w-1.5 shrink-0 rounded-full ${
                                evt.status === 'up' ? 'bg-emerald-500' : 'bg-red-500'
                              }`} />
                              <span className="flex-1 truncate text-[11px] text-zinc-400">{evt.label}</span>
                              <span className="text-[10px] text-zinc-600 shrink-0">{evt.time}</span>
                            </div>
                          ))}
                        </div>
                      </div>
                    </div>

                    {/* Bottom: endpoints list */}
                    <div className="rounded-lg border border-zinc-800/40 bg-zinc-900/40 px-3 py-2.5">
                      <div className="flex items-center justify-between mb-1.5">
                        <span className="text-[11px] font-medium text-zinc-400">Endpoints</span>
                        <span className="text-[10px] text-zinc-600">6 / 6 operational</span>
                      </div>
                      <div className="space-y-1">
                        {[
                          { name: 'api.keepalive.app', rt: '42ms', status: 'up' as const },
                          { name: 'app.keepalive.app', rt: '56ms', status: 'up' as const },
                          { name: 'auth.keepalive.app', rt: '38ms', status: 'up' as const },
                          { name: 'billing.keepalive.app', rt: '89ms', status: 'up' as const },
                        ].map(ep => (
                          <div key={ep.name} className="flex items-center justify-between py-0.5">
                            <div className="flex items-center gap-2">
                              <span className="h-1.5 w-1.5 shrink-0 rounded-full bg-emerald-500/80" />
                              <span className="text-[11px] text-zinc-500 font-mono">{ep.name}</span>
                            </div>
                            <span className="text-[10px] text-zinc-600">{ep.rt}</span>
                          </div>
                        ))}
                      </div>
                    </div>
                  </div>
                </div>
              </motion.div>
            </div>
          </div>
        </section>

        {/* ═══════════ TRUST STRIP ═══════════ */}
        {/* <section className="border-b border-zinc-800/50">
          <div className="container-page py-7 sm:py-8">
            <div className="grid grid-cols-2 gap-4 sm:gap-6 sm:grid-cols-4">
              {[
                { label: 'HTTP Monitoring', value: '1,248' },
                { label: 'SSL Monitoring', value: '14.2M' },
                { label: 'Hearbeat Monitoring', value: '87s' },
                { label: 'Incident Tracking', value: '4' },
              ].map(s => (
                <div key={s.label}>
                  <div className="text-[11px] font-medium text-zinc-600 uppercase tracking-wider">{s.label}</div>
                  <div className="mt-0.5 text-xl font-semibold text-white tracking-tight">{s.value}</div>
                </div>
              ))}
            </div>
          </div>
        </section> */}

        {/* ═══════════ FEATURES ═══════════ */}
        <section id="features" className="container-page py-16 sm:py-20">
          <motion.div
            initial={{ opacity: 0, y: 12 }}
            whileInView={{ opacity: 1, y: 0 }}
            viewport={{ once: true, margin: '-80px' }}
            transition={{ duration: 0.4 }}
          >
            <span className="section-kicker">Capabilities</span>
            <h2 className="mt-2 text-2xl font-bold tracking-tight text-white sm:text-3xl">
              What you can monitor
            </h2>
            <p className="mt-2 max-w-xl text-sm text-zinc-500 sm:text-base">
              KeepAlive covers the three signals that matter for production systems — HTTP, SSL, and cron — in one clean workspace.
            </p>
          </motion.div>

          <div className="mt-8 grid gap-2 sm:grid-cols-2 lg:grid-cols-4">
            {features.map((f, idx) => (
              <motion.div
                key={f.title}
                initial={{ opacity: 0, y: 10 }}
                whileInView={{ opacity: 1, y: 0 }}
                viewport={{ once: true, margin: '-40px' }}
                transition={{ duration: 0.35, delay: idx * 0.04 }}
                whileHover={{
                  y: -3,
                  transition: { duration: 0.15, ease: [0.25, 0.1, 0.25, 1] }
                }}
                whileTap={{
                  scale: 0.97,
                  y: 0,
                  transition: { duration: 0.1 }
                }}
                className="premium-card p-5 group hover:border-blue-500/30 hover:shadow-[0_0_40px_rgba(59,130,246,0.1)] transition-all duration-300"
>
                <motion.div
                  className="flex h-8 w-8 items-center justify-center rounded-md border border-zinc-800 bg-zinc-900/60"
                  whileTap={{ scale: 0.9 }}
                  transition={{ duration: 0.1 }}
                >
                  <f.icon className="h-3.5 w-3.5 text-blue-400" />
                </motion.div>
                <h3 className="mt-3 text-sm font-semibold text-zinc-200">{f.title}</h3>
                <p className="mt-1 text-xs leading-5 text-zinc-500">{f.desc}</p>
              </motion.div>
            ))}
          </div>
        </section>

        {/* ═══════════ WORKFLOW ═══════════ */}
        <section id="workflow" className="border-y border-zinc-800/50">
          <div className="container-page py-16 sm:py-20">
            <div className="grid gap-10 lg:grid-cols-[0.9fr_1.1fr] lg:items-start">
              <motion.div
                initial={{ opacity: 0, x: -10 }}
                whileInView={{ opacity: 1, x: 0 }}
                viewport={{ once: true, margin: '-80px' }}
                transition={{ duration: 0.4 }}
              >
                <span className="text-[11px] font-medium uppercase tracking-widest text-blue-400">
                  Workflow
                </span>
                <h2 className="mt-2 text-2xl font-bold tracking-tight text-white sm:text-3xl">
                  From failure to resolution
                </h2>
                <p className="mt-2 text-sm text-zinc-500 sm:text-base">
                  A failed check becomes an incident. Incidents trigger notifications.
                  Recovery closes the loop. Every step leaves a record.
                </p>
              </motion.div>

              <div className="relative flex flex-col gap-0">
                {/* Connecting line */}
                <div className="absolute left-[19px] top-5 bottom-5 w-px bg-gradient-to-b from-blue-500/30 via-blue-500/20 to-blue-500/10" />

                {[
                  {
                    step: '01',
                    title: 'Add a monitor',
                    badge: 'Setup',
                    badgeClass: 'bg-blue-500/10 text-blue-400 border-blue-500/20',
                    icon: (
                      <svg className="w-4 h-4" fill="none" stroke="currentColor" strokeWidth={1.5} viewBox="0 0 24 24">
                        <path strokeLinecap="round" strokeLinejoin="round" d="M12 3a9 9 0 100 18A9 9 0 0012 3zm0 0v2m0 14v2M3 12h2m14 0h2" />
                      </svg>
                    ),
                    desc: 'Add an HTTP endpoint, SSL certificate, or cron heartbeat with the expected check interval.',
                  },
                  {
                    step: '02',
                    title: 'Checks run automatically',
                    badge: 'Automated',
                    badgeClass: 'bg-blue-500/10 text-blue-400 border-blue-500/20',
                    icon: (
                      <svg className="w-4 h-4" fill="none" stroke="currentColor" strokeWidth={1.5} viewBox="0 0 24 24">
                        <path strokeLinecap="round" strokeLinejoin="round" d="M12 6v6l4 2m6-2a10 10 0 11-20 0 10 10 0 0120 0z" />
                      </svg>
                    ),
                    desc: 'Our workers evaluate each monitor on schedule and record every result immutably.',
                  },
                  {
                    step: '03',
                    title: 'Get alerted',
                    badge: 'Alert',
                    badgeClass: 'bg-yellow-500/10 text-yellow-400 border-yellow-500/20',
                    icon: (
                      <svg className="w-4 h-4" fill="none" stroke="currentColor" strokeWidth={1.5} viewBox="0 0 24 24">
                        <path strokeLinecap="round" strokeLinejoin="round" d="M15 17h5l-1.405-1.405A2.032 2.032 0 0118 14.158V11a6 6 0 10-12 0v3.159c0 .538-.214 1.055-.595 1.436L4 17h5m6 0v1a3 3 0 11-6 0v-1m6 0H9" />
                      </svg>
                    ),
                    desc: 'A failure opens an incident, sends a notification, and updates your dashboard in real time.',
                  },
                  {
                    step: '04',
                    title: 'Review & resolve',
                    badge: 'Resolved',
                    badgeClass: 'bg-green-500/10 text-green-400 border-green-500/20',
                    icon: (
                      <svg className="w-4 h-4" fill="none" stroke="currentColor" strokeWidth={1.5} viewBox="0 0 24 24">
                        <path strokeLinecap="round" strokeLinejoin="round" d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z" />
                      </svg>
                    ),
                    desc: 'Close the incident when the service recovers. The full timeline is saved for postmortem analysis.',
                  },
                ].map((item, i) => (
                  <motion.div
                    key={item.step}
                    initial={{ opacity: 0, x: 10 }}
                    whileInView={{ opacity: 1, x: 0 }}
                    viewport={{ once: true, margin: '-40px' }}
                    transition={{ duration: 0.35, delay: i * 0.05 }}
                    whileHover={{ x: 4, transition: { duration: 0.15 } }}
                    className="group relative z-10 flex gap-4 rounded-xl px-4 py-3.5 transition-colors duration-200 hover:bg-white/[0.03]"
                  >
                    {/* Step number bubble */}
                    <div className="flex h-10 w-10 shrink-0 items-center justify-center rounded-xl border border-zinc-700/60 bg-zinc-900 text-[11px] font-semibold text-zinc-400 transition-all duration-200 group-hover:border-blue-500/40 group-hover:bg-blue-500/10 group-hover:text-blue-400">
                      {item.step}
                    </div>

                    {/* Content */}
                    <div className="min-w-0 flex-1 pt-0.5">
                      <div className="flex items-center gap-2">
                        <h3 className="text-sm font-semibold text-white">{item.title}</h3>
                        <span className={`rounded-full border px-2 py-0.5 text-[10px] font-medium ${item.badgeClass}`}>
                          {item.badge}
                        </span>
                      </div>
                      <p className="mt-1 text-sm leading-6 text-zinc-500">{item.desc}</p>
                    </div>

                    {/* Icon */}
                    <div className="shrink-0 pt-1 text-zinc-700 transition-colors duration-200 group-hover:text-zinc-500">
                      {item.icon}
                    </div>
                  </motion.div>
                ))}
              </div>
            </div>
          </div>
        </section>

        {/* ═══════════ PRICING ═══════════ */}
        <section id="pricing" className="container-page py-16 sm:py-20">
          <div className="grid gap-10 lg:grid-cols-[0.9fr_1.1fr] lg:items-start">
            <motion.div
              initial={{ opacity: 0, y: 12 }}
              whileInView={{ opacity: 1, y: 0 }}
              viewport={{ once: true }}
              transition={{ duration: 0.4 }}
            >
              <span className="section-kicker">Pricing</span>
              <h2 className="mt-2 text-2xl font-bold tracking-tight text-white sm:text-3xl">
                Start small, scale when needed
              </h2>
              <p className="mt-2 text-sm text-zinc-500 sm:text-base">
                The free tier is enough to monitor a small project. Pro adds capacity for teams running multiple customer-facing services.
              </p>
            </motion.div>

            <div className="grid gap-2 sm:grid-cols-2">
              <motion.div
                initial={{ opacity: 0, y: 10 }}
                whileInView={{ opacity: 1, y: 0 }}
                viewport={{ once: true }}
                transition={{ duration: 0.35, delay: 0.05 }}
                whileHover={{
                  y: -3,
                  transition: { duration: 0.15, ease: [0.25, 0.1, 0.25, 1] }
                }}
                whileTap={{
                  scale: 0.97,
                  y: 0,
                  transition: { duration: 0.1 }
                }}
                className="premium-card p-5"
              >
              <div className="absolute top-0 right-0 bg-blue-500 text-white text-[10px] font-semibold px-2 py-0.5 rounded-bl-lg tracking-wider">
                                PRO
                              </div>
                <h3 className="text-base font-semibold text-zinc-200">Free</h3>
                <p className="mt-1 text-sm text-zinc-500">For indie developers.</p>
                <p className="mt-4 text-3xl font-bold text-white">$0</p>
                <ul className="mt-4 space-y-2.5 text-sm">
                  <li className="flex items-center gap-2.5">
                    <CheckCircle className="h-3.5 w-3.5 text-emerald-500 shrink-0" />
                    <span className="text-zinc-400">10 monitors</span>
                  </li>
                  <li className="flex items-center gap-2.5">
                    <CheckCircle className="h-3.5 w-3.5 text-emerald-500 shrink-0" />
                    <span className="text-zinc-400">1-minute intervals</span>
                  </li>
                  <li className="flex items-center gap-2.5">
                    <CheckCircle className="h-3.5 w-3.5 text-emerald-500 shrink-0" />
                    <span className="text-zinc-400">10 status page</span>
                  </li>
                </ul>
              </motion.div>

              <motion.div
                initial={{ opacity: 0, y: 10 }}
                whileInView={{ opacity: 1, y: 0 }}
                viewport={{ once: true }}
                transition={{ duration: 0.35, delay: 0.1 }}
                className="rounded-xl border border-blue-500/30 bg-zinc-900/60 p-5 relative overflow-hidden"
              >
                <div className="absolute top-0 right-0 bg-blue-500 text-white text-[10px] font-semibold px-2 py-0.5 rounded-bl-lg tracking-wider">
                  PRO
                </div>
                <h3 className="text-base font-semibold text-zinc-200">Pro</h3>
                <p className="mt-1 text-sm text-zinc-500">For production teams.</p>
                <p className="mt-4 text-3xl font-bold text-white">$0<span className="text-base font-normal text-zinc-500">/mo</span></p>
                <ul className="mt-4 space-y-2.5 text-sm">
                  <li className="flex items-center gap-2.5">
                    <CheckCircle className="h-3.5 w-3.5 text-blue-400 shrink-0" />
                    <span className="text-zinc-300">10 monitors</span>
                  </li>
                  <li className="flex items-center gap-2.5">
                    <CheckCircle className="h-3.5 w-3.5 text-blue-400 shrink-0" />
                    <span className="text-zinc-300">1-minute intervals</span>
                  </li>
                  <li className="flex items-center gap-2.5">
                    <CheckCircle className="h-3.5 w-3.5 text-blue-400 shrink-0" />
                    <span className="text-zinc-300">10 status pages</span>
                  </li>
                </ul>
              </motion.div>
            </div>
          </div>
        </section>
      </main>

      <footer className="border-t border-zinc-800/50">
        <div className="container-page flex items-center justify-between py-6 text-xs text-zinc-600">
          <div className="flex items-center gap-2">
            <Server className="h-3.5 w-3.5 text-zinc-500" />
            <span className="font-semibold text-zinc-400">KeepAlive</span>
          </div>
          <p>Reliable monitoring for APIs, websites, SSL certificates, and scheduled jobs..</p>
        </div>
      </footer>
    </div>
  );
}