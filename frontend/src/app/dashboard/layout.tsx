'use client';

export default function DashboardLayout({ children }: { children: React.ReactNode }) {
  return (
    <div className="app-shell">
      <main className="container-page py-6 sm:py-8">
        {children}
      </main>
    </div>
  );
}
