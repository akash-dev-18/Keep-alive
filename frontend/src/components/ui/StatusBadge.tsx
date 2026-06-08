interface Props {
  status: string;
  size?: 'sm' | 'md' | 'lg';
  showLabel?: boolean;
}

const styles: Record<string, { dot: string; label: string; text: string }> = {
  up: { dot: 'bg-emerald-500', label: 'bg-emerald-50 text-emerald-700 ring-emerald-200', text: 'Operational' },
  down: { dot: 'bg-red-500', label: 'bg-red-50 text-red-700 ring-red-200', text: 'Down' },
  unknown: { dot: 'bg-slate-400', label: 'bg-slate-100 text-slate-700 ring-slate-200', text: 'Pending' },
  degraded: { dot: 'bg-amber-500', label: 'bg-amber-50 text-amber-700 ring-amber-200', text: 'Degraded' },
  paused: { dot: 'bg-amber-500', label: 'bg-amber-50 text-amber-700 ring-amber-200', text: 'Paused' },
};

export default function StatusBadge({ status, size = 'md', showLabel = true }: Props) {
  const style = styles[status] || styles.unknown;
  const dotSize = size === 'sm' ? 'h-1.5 w-1.5' : size === 'lg' ? 'h-2.5 w-2.5' : 'h-2 w-2';

  if (!showLabel) {
    return <span aria-label={style.text} className={`${dotSize} rounded-full ${style.dot}`} />;
  }

  return (
    <span className={`inline-flex items-center gap-1.5 rounded-full px-2 py-1 text-xs font-medium ring-1 ring-inset ${style.label}`}>
      <span className={`${dotSize} rounded-full ${style.dot}`} />
      {style.text}
    </span>
  );
}
