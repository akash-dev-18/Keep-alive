'use client';

import { useAuth, useUser } from '@clerk/nextjs';
import { useEffect, useState } from 'react';
import { api, type NotificationPreferences } from '@/lib/api';
import Navbar from '@/components/ui/Navbar';
import DashboardLayout from '@/app/dashboard/layout';
import { UserCircle, Bell, Shield, Mail, Copy } from 'lucide-react';

export default function SettingsPage() {
  const { userId } = useAuth();
  const { user } = useUser();
  const [activeTab, setActiveTab] = useState('profile');
  const [copied, setCopied] = useState(false);
  const [prefs, setPrefs] = useState<NotificationPreferences | null>(null);
  const [saving, setSaving] = useState(false);

  useEffect(() => {
    if (!userId) return;
    api.notificationPreferences.get().then(setPrefs).catch(() => {});
  }, [userId]);

  const tabs = [
    { id: 'profile', label: 'Profile', icon: UserCircle },
    { id: 'notifications', label: 'Notifications', icon: Bell },
    { id: 'security', label: 'Security', icon: Shield },
  ];

//   async function updatePref(key: keyof Pick<NotificationPreferences, 'emailOnDown' | 'emailOnUp' | 'emailOnSslExpiry'>, value: boolean) {
//     if (!prefs) return;
//     setSaving(true);
//     try {
//       const updated = await api.notificationPreferences.update({ [key]: value });
//       setPrefs(updated);
//     } catch (e) { console.error(e); }
//     finally { setSaving(false); }
//   }

async function updatePref(
  key: keyof Pick<NotificationPreferences, 'emailOnDown' | 'emailOnUp' | 'emailOnSslExpiry'>,
  value: boolean
) {
  if (!prefs) return;

  const previousValue = prefs[key];

  setPrefs((prev) => (prev ? { ...prev, [key]: value } : null));
  setSaving(true);

  try {
    const updated = await api.notificationPreferences.update({ [key]: value });
    setPrefs(updated);
  } catch (e) {
    console.error(e);
    setPrefs((prev) => (prev ? { ...prev, [key]: previousValue } : null));
  } finally {
    setSaving(false);
  }
}
  if (!userId) {
    return (
      <>
        <Navbar />
        <DashboardLayout>
          <p className="text-sm text-slate-500 py-16 text-center">Sign in to manage settings.</p>
        </DashboardLayout>
      </>
    );
  }

  return (
    <>
      <Navbar />
      <DashboardLayout>
        <div className="space-y-6 max-w-2xl">
          <div>
            <p className="section-kicker">Settings</p>
            <h1 className="page-title mt-2">Settings</h1>
          </div>

          <div className="flex gap-1 border-b border-slate-200 pb-1">
            {tabs.map((tab) => (
              <button key={tab.id} onClick={() => setActiveTab(tab.id)}
                className={`flex items-center gap-2 px-4 py-2.5 text-sm font-medium rounded-t-lg ${
                  activeTab === tab.id ? 'text-slate-950 bg-white border border-b-white border-slate-200 -mb-[1px]' : 'text-slate-500'
                }`}>
                <tab.icon className="w-4 h-4" />{tab.label}
              </button>
            ))}
          </div>

          {activeTab === 'profile' && (
            <div className="card p-5 space-y-4">
              <div>
                <label className="text-xs text-slate-500 uppercase font-medium">Email</label>
                <p className="text-sm mt-1">{user?.primaryEmailAddress?.emailAddress}</p>
              </div>
              <div>
                <label className="text-xs text-slate-500 uppercase font-medium">User ID</label>
                <div className="flex items-center gap-2 mt-1">
                  <p className="text-sm font-mono">{userId}</p>
                  <button onClick={() => { navigator.clipboard.writeText(userId!); setCopied(true); setTimeout(() => setCopied(false), 2000); }}>
                    <Copy className="w-3.5 h-3.5" />
                  </button>
                </div>
              </div>
            </div>
          )}

          {activeTab === 'notifications' && prefs && (
            <div className="card p-5 space-y-4">
              <h2 className="text-sm font-semibold">Email alert preferences</h2>
              {[
                { key: 'emailOnDown' as const, label: 'Downtime', desc: 'When a monitor goes down' },
                { key: 'emailOnUp' as const, label: 'Recovery', desc: 'When a monitor recovers' },
                { key: 'emailOnSslExpiry' as const, label: 'SSL expiry', desc: 'Certificate expiring soon' },
              ].map((item) => (
                <label key={item.key} className="flex items-center gap-3 p-3 rounded-xl border border-slate-200 cursor-pointer">
                  <input type="checkbox" checked={prefs[item.key]} disabled={saving}
                    onChange={(e) => updatePref(item.key, e.target.checked)}
                    className="rounded border-slate-300" />
                  <div className="flex items-center gap-2">
                    <Mail className="w-4 h-4 text-slate-600" />
                    <div>
                      <p className="text-sm font-medium">{item.label}</p>
                      <p className="text-xs text-slate-500">{item.desc}</p>
                    </div>
                  </div>
                </label>
              ))}
            </div>
          )}

          {activeTab === 'security' && (
            <div className="card p-5 text-sm text-slate-500">API keys and team management coming soon.</div>
          )}
        </div>
      </DashboardLayout>
    </>
  );
}
