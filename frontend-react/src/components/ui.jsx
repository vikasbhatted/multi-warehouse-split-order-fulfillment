import React from 'react';

const COLORS = {
  green: 'bg-emerald-100 text-emerald-800',
  blue: 'bg-sky-100 text-sky-800',
  amber: 'bg-amber-100 text-amber-800',
  red: 'bg-rose-100 text-rose-800',
  slate: 'bg-slate-100 text-slate-700',
};

const STATUS_COLOR = {
  CREATED: 'slate', VALIDATED: 'blue', IN_FULFILLMENT: 'blue', COMPLETED: 'green',
  PARTIALLY_REFUNDED: 'amber', REJECTED: 'red',
  ALLOCATED: 'slate', PICKED: 'blue', DAMAGED: 'red', SUBSTITUTED: 'amber', REFUNDED: 'amber', SHIPPED: 'green',
  REGISTERED: 'slate', DAMAGE_HANDLING: 'amber',
  ACTIVE: 'blue', TERMINATED: 'slate', FAILED: 'red', DONE: 'green', PENDING: 'slate',
};

export function Badge({ children, color }) {
  const c = COLORS[color || STATUS_COLOR[children] || 'slate'];
  return <span className={`inline-block rounded-full px-2.5 py-0.5 text-xs font-semibold ${c}`}>{String(children).replaceAll('_', ' ')}</span>;
}

const INR = new Intl.NumberFormat('en-IN', { style: 'currency', currency: 'INR' });

/** All amounts are Indian rupees (₹1,23,456.00 grouping). */
export function Money({ value }) {
  return <>{INR.format(Number(value ?? 0))}</>;
}

export function ErrorBox({ message }) {
  if (!message) return null;
  return <div className="rounded-lg border border-rose-200 bg-rose-50 px-4 py-3 text-sm text-rose-700">{message}</div>;
}

export function Spinner({ label = 'Loading…' }) {
  return <p className="py-6 text-center text-sm text-slate-500">{label}</p>;
}

export function PageTitle({ title, subtitle, children }) {
  return (
    <div className="mb-6 flex flex-wrap items-end justify-between gap-3">
      <div>
        <h1 className="text-2xl font-bold text-slate-900">{title}</h1>
        {subtitle && <p className="mt-1 text-sm text-slate-500">{subtitle}</p>}
      </div>
      <div className="flex gap-2">{children}</div>
    </div>
  );
}
