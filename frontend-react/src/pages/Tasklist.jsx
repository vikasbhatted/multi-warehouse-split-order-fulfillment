import React, { useEffect, useRef, useState } from 'react';
import { Link } from 'react-router-dom';
import { api } from '../api.js';
import { usePolling } from '../hooks.js';
import { Badge, ErrorBox, PageTitle, Spinner } from '../components/ui.jsx';

const REASONS = [['TORN', 'Torn / ripped'], ['STAINED', 'Stained'], ['MISLABELLED', 'Wrong size / mislabelled'], ['MISSING', 'Missing from shelf']];

export default function Tasklist({ persona }) {
  const [tab, setTab] = useState('available');
  const [selected, setSelected] = useState(null);
  const { data, error, loading, reload } = usePolling(api.tasks, 3000);
  const detailRef = useRef(null);
  // on phones/tablets the detail panel is below the list -> bring it into view when a task is picked
  const select = (key) => {
    setSelected(key);
    if (window.innerWidth < 1024) setTimeout(() => detailRef.current?.scrollIntoView({ behavior: 'smooth', block: 'start' }), 50);
  };

  const all = data || [];
  const mine = all.filter((t) => t.assignee === persona.id);
  const available = all.filter((t) => !t.assignee && t.candidateGroups.some((g) => persona.groups.includes(g)));
  const list = tab === 'mine' ? mine : available;

  useEffect(() => { setSelected(null); }, [persona.id]);

  return (
    <>
      <PageTitle title="Tasklist" subtitle={`${persona.name} · ${persona.role} · groups: ${persona.groups.join(', ')}`} />
      <ErrorBox message={error} />
      <div className="grid gap-6 lg:grid-cols-5">
        <section className="card lg:col-span-2">
          <div className="mb-4 flex gap-2">
            <button className={tab === 'available' ? 'btn-primary' : 'btn-secondary'} onClick={() => setTab('available')}>Available ({available.length})</button>
            <button className={tab === 'mine' ? 'btn-primary' : 'btn-secondary'} onClick={() => setTab('mine')}>Assigned to me ({mine.length})</button>
          </div>
          {loading && !data ? <Spinner /> : !list.length ? (
            <p className="py-6 text-center text-sm text-slate-500">No tasks here.</p>
          ) : (
            <ul className="divide-y">
              {list.map((t) => (
                <li key={t.taskKey}>
                  <button onClick={() => select(t.taskKey)}
                    className={`w-full px-2 py-3 text-left hover:bg-slate-50 ${selected === t.taskKey ? 'bg-brand-50' : ''}`}>
                    <p className="text-sm font-semibold">{t.name}</p>
                    <p className="text-xs text-slate-500">
                      {t.context?.orderId || '…'} {t.context?.warehouseName ? `· ${t.context.warehouseName}` : ''}
                    </p>
                    <p className="mt-1 flex items-center gap-2 text-xs text-slate-400">
                      <span>{t.creationDate ? new Date(t.creationDate).toLocaleTimeString() : ''}</span>
                      {t.assignee && <Badge color="blue">{t.assignee}</Badge>}
                    </p>
                  </button>
                </li>
              ))}
            </ul>
          )}
        </section>

        <section ref={detailRef} className="scroll-mt-4 lg:col-span-3">
          {selected && all.some((t) => t.taskKey === selected)
            ? <TaskDetail key={selected} taskKey={selected} persona={persona} onChanged={() => { reload(); }} onDone={() => { setSelected(null); reload(); }} />
            : <div className="card py-16 text-center text-sm text-slate-500">Select a task to work on it.</div>}
        </section>
      </div>
    </>
  );
}

function TaskDetail({ taskKey, persona, onChanged, onDone }) {
  const { data: task, error: loadError, reload } = usePolling(() => api.task(taskKey), 5000, [taskKey]);
  const [err, setErr] = useState(null);
  const [busy, setBusy] = useState(false);
  const [form, setForm] = useState(null);

  // initialise the form once per task
  useEffect(() => {
    if (!task || form) return;
    const ctx = task.context || {};
    if (task.elementId === 'UT_Pick') setForm({ damaged: {} });
    else if (task.elementId === 'UT_ApproveSubstitution') setForm({ approved: true });
    else if (task.elementId === 'UT_CorrectAddress') setForm({ ...(ctx.shippingAddress || {}) });
    else if (task.elementId === 'UT_ManualRefund') setForm({ note: '' });
  }, [task, form]);

  if (!task) return <div className="card"><ErrorBox message={loadError} />{!loadError && <Spinner />}</div>;
  const ctx = task.context || {};
  const mineTask = task.assignee === persona.id;
  const canClaim = !task.assignee && task.candidateGroups.some((g) => persona.groups.includes(g));

  const run = async (fn, finish) => {
    setBusy(true); setErr(null);
    try { await fn(); finish ? onDone() : (reload(), onChanged()); } catch (e) { setErr(e.message); } finally { setBusy(false); }
  };

  const buildVariables = () => {
    switch (task.elementId) {
      case 'UT_Pick':
        return { damagedItems: Object.entries(form.damaged).filter(([, v]) => v).map(([sku, reason]) => ({ sku, reason })) };
      case 'UT_ApproveSubstitution':
        return { substitutionApproved: !!form.approved };
      case 'UT_CorrectAddress':
        return { shippingAddress: { line1: form.line1, city: form.city, postalCode: form.postalCode, country: form.country } };
      case 'UT_ManualRefund':
        return { manualRefundNote: form.note };
      default:
        return {};
    }
  };

  return (
    <div className="card">
      <div className="mb-1 flex flex-wrap items-start justify-between gap-2">
        <h2 className="text-lg font-bold">{task.name}</h2>
        {task.assignee ? <Badge color="blue">assigned: {task.assignee}</Badge> : <Badge>unassigned</Badge>}
      </div>
      <p className="mb-4 text-sm text-slate-500">
        Order {ctx.orderId ? <Link className="font-mono text-brand-700 hover:underline" to={`/orders/${ctx.orderId}`}>{ctx.orderId}</Link> : '…'}
        {ctx.warehouseName && <> · {ctx.warehouseName}</>} · customer {ctx.customerName}
      </p>
      <ErrorBox message={err || loadError} />

      <div className="my-4">
        {form && task.elementId === 'UT_Pick' && <PickForm ctx={ctx} form={form} setForm={setForm} disabled={!mineTask} />}
        {form && task.elementId === 'UT_ApproveSubstitution' && <ApproveForm ctx={ctx} form={form} setForm={setForm} disabled={!mineTask} />}
        {form && task.elementId === 'UT_CorrectAddress' && <AddressForm form={form} setForm={setForm} disabled={!mineTask} />}
        {form && task.elementId === 'UT_ManualRefund' && <RefundForm ctx={ctx} form={form} setForm={setForm} disabled={!mineTask} />}
      </div>

      <div className="flex flex-wrap gap-2 border-t pt-4">
        {canClaim && <button className="btn-primary" disabled={busy} onClick={() => run(() => api.claim(taskKey, persona.id))}>Claim task</button>}
        {mineTask && <button className="btn-secondary" disabled={busy} onClick={() => run(() => api.unclaim(taskKey, persona.id))}>Unclaim</button>}
        {mineTask && <button className="btn-primary" disabled={busy || !form} onClick={() => run(() => api.complete(taskKey, persona.id, buildVariables()), true)}>Complete task</button>}
        {!canClaim && !mineTask && <p className="text-sm text-slate-500">This task is not assigned to you and your groups cannot claim it.</p>}
      </div>
    </div>
  );
}

function PickForm({ ctx, form, setForm, disabled }) {
  const set = (sku, reason) => setForm({ damaged: { ...form.damaged, [sku]: reason } });
  return (
    <div>
      <p className="mb-3 text-sm text-slate-600">Pick and pack every item. Tick <b>Damaged</b> for any item that cannot be shipped – this triggers the substitution / partial refund sub-process.</p>
      <ul className="divide-y rounded-lg border">
        {(ctx.items || []).map((i) => (
          <li key={i.sku} className="flex flex-wrap items-center gap-3 px-3 py-3">
            <div className="min-w-0 flex-1"><p className="text-sm font-medium">{i.quantity} × {i.name}</p><p className="font-mono text-xs text-slate-400">{i.sku}</p></div>
            <label className="flex items-center gap-1.5 text-sm">
              <input type="checkbox" disabled={disabled} checked={!!form.damaged[i.sku]} onChange={(e) => set(i.sku, e.target.checked ? 'TORN' : '')} /> Damaged
            </label>
            {form.damaged[i.sku] && (
              <select className="input !w-auto" disabled={disabled} value={form.damaged[i.sku]} onChange={(e) => set(i.sku, e.target.value)}>
                {REASONS.map(([v, l]) => <option key={v} value={v}>{l}</option>)}
              </select>
            )}
          </li>
        ))}
      </ul>
      {disabled && <p className="mt-2 text-xs text-slate-400">Claim the task to edit.</p>}
    </div>
  );
}

function ApproveForm({ ctx, form, setForm, disabled }) {
  const damaged = (ctx.items || []).filter((i) => i.status === 'DAMAGED' || i.proposedSubstituteSku);
  return (
    <div>
      <p className="mb-3 text-sm text-slate-600">Call the customer and offer the substitute(s). If the customer declines, all damaged items are refunded. If nobody answers before the timer expires, the engine refunds automatically.</p>
      <ul className="mb-4 divide-y rounded-lg border">
        {damaged.map((i) => (
          <li key={i.sku} className="px-3 py-3 text-sm">
            <p><b>{i.name}</b> <span className="text-slate-400">({i.damageReason})</span></p>
            {i.proposedSubstituteSku
              ? <p className="text-emerald-700">Substitute offered: {i.proposedSubstituteName} <span className="font-mono text-xs">{i.proposedSubstituteSku}</span></p>
              : <p className="text-slate-500">No substitute in this warehouse → will be refunded.</p>}
          </li>
        ))}
      </ul>
      <label className="flex items-center gap-2 text-sm font-medium">
        <input type="checkbox" disabled={disabled} checked={form.approved} onChange={(e) => setForm({ approved: e.target.checked })} />
        Customer accepts the substitute(s)
      </label>
    </div>
  );
}

function AddressForm({ form, setForm, disabled }) {
  const f = (k, label) => (
    <div key={k}><label className="label">{label}</label>
      <input className="input" disabled={disabled} value={form[k] || ''} onChange={(e) => setForm({ ...form, [k]: e.target.value })} /></div>
  );
  return (
    <div>
      <p className="mb-3 text-sm text-slate-600">The carrier rejected this address (HTTP 422). Correct it – the label request is retried automatically.</p>
      <div className="grid gap-3 sm:grid-cols-2">{f('line1', 'Address line 1')}{f('city', 'City')}{f('postalCode', 'Postal code')}{f('country', 'Country (ISO-2)')}</div>
    </div>
  );
}

function RefundForm({ ctx, form, setForm, disabled }) {
  return (
    <div>
      <p className="mb-3 text-sm text-slate-600">The automatic refund failed (payment gateway error). Refund the damaged item(s) manually and record the reference.</p>
      <ul className="mb-3 text-sm">
        {(ctx.items || []).filter((i) => i.status === 'DAMAGED').map((i) => <li key={i.sku}>• {i.quantity} × {i.name} ({i.damageReason})</li>)}
      </ul>
      <label className="label">Refund note / gateway reference</label>
      <textarea className="input" rows={3} disabled={disabled} value={form.note} onChange={(e) => setForm({ note: e.target.value })} />
    </div>
  );
}
