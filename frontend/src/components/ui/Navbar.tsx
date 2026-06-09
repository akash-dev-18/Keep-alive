

'use client';

import Link from 'next/link';
import { useAuth, UserButton, SignInButton, SignUpButton } from '@clerk/nextjs';
import { Activity, Bell, Clock, Globe, LayoutDashboard, Menu, Monitor, Settings, Shield, AlertTriangle, X } from 'lucide-react';
import { usePathname } from 'next/navigation';
import { useEffect, useRef, useState } from 'react';
import { motion } from 'framer-motion';
import { api, type Notification } from '@/lib/api';

const navItems = [
  { href: '/dashboard', label: 'Dashboard', icon: LayoutDashboard },
  { href: '/dashboard/keep-alive', label: 'Keep Alive', icon: Activity },
  { href: '/dashboard/monitors', label: 'Monitors', icon: Monitor },
  { href: '/dashboard/ssl', label: 'SSL', icon: Shield },
  { href: '/dashboard/cron', label: 'Heartbeat', icon: Clock },
  { href: '/dashboard/incidents', label: 'Incidents', icon: AlertTriangle },
  { href: '/dashboard/status-pages', label: 'Status pages', icon: Globe },
  { href: '/dashboard/alerts', label: 'Alerts', icon: Bell },
  { href: '/dashboard/settings', label: 'Settings', icon: Settings },
];

export default function Navbar() {
  const { userId } = useAuth();
  const pathname = usePathname();
  const [mobileOpen, setMobileOpen] = useState(false);
  const [notifications, setNotifications] = useState<Notification[]>([]);
  const [bellOpen, setBellOpen] = useState(false);
  const bellRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    if (!userId) return;
    api.notifications.list().then(setNotifications).catch(() => {});
  }, [userId]);

  useEffect(() => {
    function handleClick(e: MouseEvent) {
      if (bellRef.current && !bellRef.current.contains(e.target as Node)) {
        setBellOpen(false);
      }
    }
    document.addEventListener('mousedown', handleClick);
    return () => document.removeEventListener('mousedown', handleClick);
  }, []);

  async function handleMarkAllRead() {
    await api.notifications.markAllRead().catch(() => {});
    setNotifications(prev => prev.map(n => ({ ...n, isRead: true })));
  }

  if (pathname.startsWith('/status/')) return null;

  const isActive = (href: string) => pathname === href || pathname.startsWith(href + '/');
  const unreadCount = notifications.filter(n => !n.isRead).length;
  const isApp = pathname.startsWith('/dashboard');

  return (

    <header className="sticky top-0 z-50 w-full border-b border-zinc-800/80 bg-zinc-950/70 backdrop-blur-md">
      <nav className="container-page flex h-14 items-center justify-between gap-4" aria-label="Primary navigation">
        
        {/* Left: Brand Logo & Navigation Links */}
        <div className="flex items-center gap-6 overflow-hidden">
          <Link href={userId ? '/dashboard' : '/'} className="flex items-center gap-2 text-white shrink-0">
            <span className="flex h-8 w-8 items-center justify-center rounded-lg bg-blue-600 shadow-[0_0_15px_rgba(59,130,246,0.25)]">
              <Activity className="h-4 w-4 text-white" />
            </span>
            <span className="text-sm font-bold tracking-tight hidden sm:inline-block">KeepAlive</span>
          </Link>

          {userId && isApp ? (
            // Flex box spacing and masking for responsive layouts
            <div className="hidden items-center gap-1 md:flex overflow-x-auto no-scrollbar py-1">
              {navItems.map((item) => {
                const active = isActive(item.href);
                return (
                  <Link
                    key={item.href}
                    href={item.href}
                    className={`relative inline-flex items-center gap-1.5 rounded-md px-2.5 py-1.5 text-xs font-medium transition-colors whitespace-nowrap group z-10 ${
                      active ? 'text-white' : 'text-zinc-400 hover:text-zinc-200'
                    }`}
                  >
                    {active && (
                      <motion.div
                        layoutId="activeNavBackground"
                        className="absolute inset-0 bg-zinc-800/80 rounded-md -z-10 border border-zinc-700/40"
                        transition={{ type: 'spring', stiffness: 380, damping: 30 }}
                      />
                    )}
                    <item.icon className="h-3.5 w-3.5 shrink-0 transition-transform duration-200 group-hover:scale-105" />      
                    <span>{item.label}</span> 
                  </Link>
                );
              })}
            </div>
          ) : (
            <div className="hidden items-center gap-6 md:flex text-sm">
              <a href="#features" className="text-zinc-400 hover:text-white transition-colors">Features</a>
              <a href="#workflow" className="text-zinc-400 hover:text-white transition-colors">Workflow</a>
              <a href="#pricing" className="text-zinc-400 hover:text-white transition-colors">Pricing</a>
            </div>
          )}
        </div>

        {/* Right Actions Block */}
        <div className="flex items-center gap-3 shrink-0">
          {userId ? (
            <>
              {/* Alert Notification Module */}
              <div ref={bellRef} className="relative">
                <button
                  type="button"
                  onClick={() => setBellOpen(o => !o)}
                  className="relative flex h-8 w-8 items-center justify-center rounded-lg border border-zinc-800/60 bg-zinc-900/30 text-zinc-400 hover:border-zinc-700 hover:bg-zinc-900 hover:text-white transition-colors"
                  aria-label="Open notifications"
                >
                  <Bell className="h-3.5 w-3.5" />
                  {unreadCount > 0 && <span className="absolute right-2 top-2 h-1.5 w-1.5 rounded-full bg-blue-500 animate-pulse" />}
                </button>

                {bellOpen && (
                  <div className="absolute right-0 mt-2 w-80 overflow-hidden rounded-xl border border-zinc-800 bg-zinc-950 shadow-2xl shadow-black">
                    <div className="flex items-center justify-between border-b border-zinc-900 px-4 py-2.5 bg-zinc-900/40">
                      <span className="text-xs font-semibold text-zinc-200">Notifications</span>
                      {unreadCount > 0 && (
                        <button type="button" onClick={handleMarkAllRead} className="text-[11px] font-medium text-blue-400 hover:text-blue-300 transition-colors">
                          Mark all read
                        </button>
                      )}
                    </div>
                    <div className="max-h-72 overflow-y-auto divide-y divide-zinc-900">
                      {notifications.length === 0 ? (
                        <p className="px-4 py-8 mercantile text-center text-xs text-zinc-500">No notifications yet.</p>
                      ) : (
                        notifications.slice(0, 20).map(n => (
                          <div key={n.id} className={`px-4 py-3 transition-colors ${n.isRead ? 'bg-transparent' : 'bg-blue-500/5'}`}>
                            <p className="text-xs font-semibold text-zinc-200">{n.title}</p>
                            {n.body && <p className="mt-1 text-[11px] leading-4 text-zinc-400">{n.body}</p>}
                            <p className="mt-2 text-[10px] text-zinc-600">
                              {new Date(n.createdAt).toLocaleString([], { month: 'short', day: 'numeric', hour: '2-digit', minute: '2-digit' })}
                            </p>
                          </div>
                        ))
                      )}
                    </div>
                  </div>
                )}
              </div>

              {/* Clerk Wrapper */}
              <div className="flex items-center border-l border-zinc-800 pl-3">
                <UserButton appearance={{ elements: { avatarBox: 'h-7 w-7 border border-zinc-800' } }} />
              </div>

              {isApp && (
                <button 
                  type="button" 
                  onClick={() => setMobileOpen(!mobileOpen)} 
                  className="flex h-8 w-8 items-center justify-center rounded-lg border border-zinc-800 text-zinc-400 md:hidden hover:bg-zinc-900"
                  aria-label="Toggle navigation"
                >
                  {mobileOpen ? <X className="h-4 w-4" /> : <Menu className="h-4 w-4" />}
                </button>
              )}
            </>
          ) : (
            <div className="flex items-center gap-2">
              <SignInButton mode="modal">
                <button type="button" className="btn-secondary !py-1.5 !px-3 !text-xs">Sign in</button>
              </SignInButton>
              <SignUpButton mode="modal">
                <button type="button" className="btn-primary !py-1.5 !px-3 !text-xs">Start free</button>
              </SignUpButton>
            </div>
          )}
        </div>
      </nav>

      {/* Mobile Menu Integration */}
      {userId && isApp && mobileOpen && (
        <div className="border-t border-zinc-900 bg-zinc-950 md:hidden">
          <div className="container-page py-2 space-y-0.5">
            {navItems.map((item) => {
              const active = isActive(item.href);
              return (
                <Link
                  key={item.href}
                  href={item.href}
                  onClick={() => setMobileOpen(false)}
                  className={`flex items-center gap-3 rounded-md px-3 py-2 text-xs font-medium transition-colors ${
                    active ? 'bg-zinc-900 text-white border border-zinc-800' : 'text-zinc-400 hover:bg-zinc-900/60'
                  }`}
                >
                  <item.icon className="h-4 w-4 shrink-0" />
                  {item.label}
                </Link>
              );
            })}
          </div>
        </div>
      )}
    </header>
  );
}