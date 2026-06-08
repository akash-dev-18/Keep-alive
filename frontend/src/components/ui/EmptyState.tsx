import Link from 'next/link';
import type { LucideIcon } from 'lucide-react';

interface Props {
  icon: LucideIcon;
  title: string;
  description: string;
  action?: { label: string; href: string };
  actionOnClick?: () => void;
}

export default function EmptyState({ icon: Icon, title, description, action, actionOnClick }: Props) {
  return (
    <section className="card flex flex-col items-center justify-center px-6 py-16 text-center">
      <div className="mb-4 flex h-11 w-11 items-center justify-center rounded-xl border border-slate-200 bg-slate-50">
        <Icon className="h-5 w-5 text-slate-500" />
      </div>
      <h3 className="text-base font-semibold tracking-tight text-slate-950">{title}</h3>
      <p className="mt-2 max-w-sm text-sm leading-6 text-slate-600">{description}</p>
      {action && !actionOnClick && (
        <Link href={action.href} className="btn-primary mt-6">
          {action.label}
        </Link>
      )}
      {actionOnClick && (
        <button type="button" onClick={actionOnClick} className="btn-primary mt-6">
          {action?.label || 'Create'}
        </button>
      )}
    </section>
  );
}
