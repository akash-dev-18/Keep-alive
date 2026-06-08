const API_URL = process.env.NEXT_PUBLIC_API_URL || 'http://localhost:8080';

export const HEARTBEAT_BASE_URL = `${API_URL}/api/v1/heartbeat`;

async function getToken(): Promise<string | null> {
  if (typeof window !== 'undefined') {
    const getWindowToken = async () => {
      const clerk = (window as Window & {
        Clerk?: { session?: { getToken: () => Promise<string | null> } };
      }).Clerk;
      if (clerk?.session) {
        return await clerk.session.getToken();
      }
      return null;
    };

    const token = await getWindowToken();
    if (token) return token;

    return new Promise((resolve) => {
      let attempts = 0;
      const interval = setInterval(async () => {
        attempts++;
        const tok = await getWindowToken();
        if (tok) {
          clearInterval(interval);
          resolve(tok);
        } else if (attempts > 20) {
          clearInterval(interval);
          resolve(null);
        }
      }, 100);
    });
  }

  try {
    const { auth } = await import('@clerk/nextjs/server');
    const session = await auth();
    return await session.getToken();
  } catch {
    return null;
  }
}

async function request<T>(path: string, options: RequestInit = {}): Promise<T> {
  const token = await getToken();
  const res = await fetch(`${API_URL}${path}`, {
    ...options,
    headers: {
      'Content-Type': 'application/json',
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
      ...options.headers,
    },
    cache: 'no-store',
  });
  if (!res.ok) throw new Error(`${res.status}: ${await res.text()}`);
  if (res.status === 204) return undefined as T;
  const text = await res.text();
  if (!text) return undefined as T;
  return JSON.parse(text) as T;
}

export interface Monitor {
  id: string; name: string; type: string; url: string | null;
  pingKey: string | null; checkIntervalMin: number; httpMethod: string;
  expectedStatus: number; timeoutSeconds: number; status: string;
  active: boolean; createdAt: string;
  sslDaysRemaining: number | null; sslExpiresAt: string | null;
  sslIssuer: string | null;
  lastCheckedAt: string | null; lastPingedAt: string | null;
  expectedNextHeartbeatAt: string | null; lastResponseTimeMs: number | null;
  consecutiveFailures: number; alertAfterFailures: number;
  expectedKeyword: string | null;
  basicAuthUsername: string | null;
  hasBasicAuth: boolean; hasBearerToken: boolean;
  requestHeaders: string | null; requestBody: string | null;
}

export interface CreateMonitorRequest {
  name: string; type: string; url?: string; checkIntervalMin: number;
  httpMethod?: string; expectedStatus?: number; timeoutSeconds?: number;
  requestHeaders?: string; requestBody?: string;
  alertAfterFailures?: number; expectedKeyword?: string;
  basicAuthUsername?: string; basicAuthPassword?: string; bearerToken?: string;
}

export interface UpdateMonitorRequest {
  name?: string; url?: string; checkIntervalMin?: number;
  httpMethod?: string; expectedStatus?: number; timeoutSeconds?: number;
  requestHeaders?: string; requestBody?: string;
  alertAfterFailures?: number; expectedKeyword?: string;
  basicAuthUsername?: string; basicAuthPassword?: string; bearerToken?: string;
}

export interface MonitorCheck {
  id: string; monitorId: string; status: string;
  responseTimeMs: number | null; httpStatusCode: number | null;
  errorMessage: string | null; checkedAt: string;
}

export interface Incident {
  id: string; monitorId: string; status: string;
  startedAt: string; resolvedAt: string | null;
  durationSeconds: number | null; cause: string | null;
}

export interface Notification {
  id: string; monitorId: string | null; incidentId: string | null;
  title: string; body: string; isRead: boolean; createdAt: string;
}

export interface StatusPage {
  id: string; name: string; slug: string;
  monitorIds: string[]; isPublic: boolean;
  customDomain: string | null;
  description: string | null; logoUrl: string | null; primaryColor: string | null;
  createdAt: string; updatedAt: string;
}

export interface DashboardSummary {
  totalMonitors: number; activeMonitors: number;
  upMonitors: number; downMonitors: number;
  unknownMonitors: number; sslWarnings: number; openIncidents: number;
}

export interface DashboardActivity {
  recentChecks: MonitorCheck[];
  recentIncidents: Incident[];
  recentFailures: MonitorCheck[];
}

export interface NotificationPreferences {
  emailOnDown: boolean; emailOnUp: boolean; emailOnSslExpiry: boolean;
  updatedAt: string;
}

export interface AlertLog {
  id: string; monitorId: string; incidentId: string | null;
  alertType: string; channel: string; sentTo: string;
  status: string; errorMessage: string | null; sentAt: string;
}

export interface PublicStatusPage {
  page: StatusPage;
  monitors: Monitor[];
  uptime: Record<string, number>;
  incidents: Incident[];
}

export const INTERVAL_PRESETS = [1, 2, 3, 5, 10, 15, 30, 60] as const;
export const TIMEOUT_PRESETS = [5, 10, 20, 30, 60] as const;
export const ALERT_AFTER_PRESETS = [0, 1, 2, 3, 5] as const;

export const api = {
  monitors: {
    list: () => request<Monitor[]>('/api/monitors'),
    get: (id: string) => request<Monitor>(`/api/monitors/${id}`),
    create: (data: CreateMonitorRequest) => request<Monitor>('/api/monitors', { method: 'POST', body: JSON.stringify(data) }),
    update: (id: string, data: UpdateMonitorRequest) => request<Monitor>(`/api/monitors/${id}`, { method: 'PUT', body: JSON.stringify(data) }),
    delete: (id: string) => request<void>(`/api/monitors/${id}`, { method: 'DELETE' }),
    pause: (id: string) => request<void>(`/api/monitors/${id}/pause`, { method: 'POST' }),
    resume: (id: string) => request<void>(`/api/monitors/${id}/resume`, { method: 'POST' }),
    checks: (id: string, limit = 50) => request<MonitorCheck[]>(`/api/monitors/${id}/checks?limit=${limit}`),
    incidents: (id: string) => request<Incident[]>(`/api/monitors/${id}/incidents`),
  },
  incidents: {
    list: (limit = 50) => request<Incident[]>(`/api/incidents?limit=${limit}`),
  },
  notifications: {
    list: () => request<Notification[]>('/api/notifications'),
    markAllRead: () => request<void>('/api/notifications/read-all', { method: 'POST' }),
    markRead: (id: string) => request<void>(`/api/notifications/${id}/read`, { method: 'POST' }),
  },
  notificationPreferences: {
    get: () => request<NotificationPreferences>('/api/notification-preferences'),
    update: (data: Partial<Pick<NotificationPreferences, 'emailOnDown' | 'emailOnUp' | 'emailOnSslExpiry'>>) =>
      request<NotificationPreferences>('/api/notification-preferences', { method: 'PUT', body: JSON.stringify(data) }),
  },
  statusPages: {
    list: () => request<StatusPage[]>('/api/status-pages'),
    get: (id: string) => request<StatusPage>(`/api/status-pages/${id}`),
    create: (data: {
      name: string; slug: string; monitorIds: string[]; isPublic?: boolean;
      customDomain?: string; description?: string; logoUrl?: string; primaryColor?: string;
    }) => request<StatusPage>('/api/status-pages', { method: 'POST', body: JSON.stringify(data) }),
    update: (id: string, data: {
      name?: string; slug?: string; monitorIds?: string[]; isPublic?: boolean;
      customDomain?: string; description?: string; logoUrl?: string; primaryColor?: string;
    }) => request<StatusPage>(`/api/status-pages/${id}`, { method: 'PUT', body: JSON.stringify(data) }),
    delete: (id: string) => request<void>(`/api/status-pages/${id}`, { method: 'DELETE' }),
    getPublic: (slug: string) => request<PublicStatusPage>(`/api/status/${slug}`),
  },
  dashboard: {
    summary: () => request<DashboardSummary>('/api/dashboard/summary'),
    activity: (limit = 10) => request<DashboardActivity>(`/api/dashboard/activity?limit=${limit}`),
  },
  alertLogs: {
    list: () => request<AlertLog[]>('/api/alert-logs'),
  },
};

export function heartbeatUrl(pingKey: string): string {
  return `${HEARTBEAT_BASE_URL}/${pingKey}`;
}
