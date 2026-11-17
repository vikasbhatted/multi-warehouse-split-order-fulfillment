import React, { useEffect, useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import { api } from '../api.js';
import { usePolling } from '../hooks.js';
import { Badge, ErrorBox, Money, PageTitle, Spinner } from '../components/ui.jsx';

const DOT = { DONE: 'bg-emerald-500', ACTIVE: 'bg-sky-500 animate-pulse', PENDING: 'bg-slate-300', FAILED: 'bg-rose-500' };
const EL_DOT = { COMPLETED: 'bg-emerald-500', ACTIVE: 'bg-sky-500 animate-pulse', TERMINATED: 'bg-slate-400', CANCELED: 'bg-slate-400' };

const PROCESS_LABEL = {
  split_order_fulfillment: 'Main order process',
  warehouse_pick_pack_ship: 'Warehouse sub-workflow (multi-instance)',
  carrier_label_generation: '3PL label call activity',
};

export default function OrderTracker() {
  const { id } = useParams();
  const [done, setDone] = useState(false);
  useEffect(() => { setDone(false); }, [id]);
  const { data, error, loading } = usePolling(async () => {
    const t = await api.tracker(id);
    setDone(t.finished);
    return t;
  }, done ? 8000 : 2000, [id]);

  if (loading && !data) return <Spinner label="Loading order…" />;
  if (!data) return <ErrorBox message={error} />;
  const { order, milestones, engine, operateUrl } = data;

  return (
    <>
      <PageTitle title={`Order ${order.id}`} subtitle={`${order.customerName} · ${order.customerEmail}`}>
        <Link to="/orders" className="btn-secondary">All orders</Link>
        <a href={operateUrl} target="_blank" rel="noreferrer" className="btn-secondary">Open in Operate ↗</a>
      </PageTitle>
      <ErrorBox message={error} />

      <div className="grid gap-6 lg:grid-cols-3">
        <section className="card lg:col-span-1">
          <div className="mb-4 flex items-center justify-between">
            <h2 className="font-semibold">Progress</h2><Badge>{order.status}</Badge>
          </div>
          <ol className="space-y-4">
            {milestones.map((m) => (
              <li key={m.key} className="flex gap-3">
                <span className={`mt-1.5 h-3 w-3 shrink-0 rounded-full ${DOT[m.state] || DOT.PENDING}`} />
                <div>
                  <p className="text-sm font-medium">{m.label}</p>
                  {m.detail && <p className="text-xs text-slate-500">{m.detail}</p>}
                </div>
              </li>
            ))}
          </ol>
          {order.rejectionReason && <div className="mt-4"><ErrorBox message={`Rejected: ${order.rejectionReason}`} /></div>}
          <div className="mt-5 border-t pt-4 text-sm">
            <div className="flex justify-between"><span>Total</span><Money value={order.totalAmount} /></div>
            <div className="flex justify-between text-amber-700"><span>Refunded</span><Money value={order.refundedAmount} /></div>
          </div>
        </section>

        <div className="space-y-6 lg:col-span-2">
          <section className="card">
            <h2 className="mb-4 font-semibold">Parcels</h2>
            {!order.shipments.length ? <p className="text-sm text-slate-500">No parcels registered yet.</p> : (
              <div className="grid gap-4 sm:grid-cols-2">
                {order.shipments.map((s) => (
                  <div key={s.id} className="rounded-lg border border-slate-200 p-4">
                    <div className="flex items-center justify-between"><p className="font-medium">{s.warehouseName}</p><Badge>{s.status}</Badge></div>
                    <p className="mt-1 text-xs text-slate-500">{s.parcelWeightKg} kg · resolution: {s.resolution.replaceAll('_', ' ')}</p>
                    {s.trackingNumber ? (
                      <p className="mt-3 text-sm">
                        <span className="font-mono">{s.trackingNumber}</span> <span className="text-slate-500">({s.carrier})</span><br />
                        <a className="text-brand-700 hover:underline" href={s.labelUrl} target="_blank" rel="noreferrer">Download label (PDF)</a>
                      </p>
                    ) : <p className="mt-3 text-sm text-slate-400">Waiting for carrier label…</p>}
                  </div>
                ))}
              </div>
            )}
          </section>

          <section className="card overflow-x-auto">
            <h2 className="mb-4 font-semibold">Items</h2>
            <table className="w-full text-left text-sm">
              <thead className="border-b text-xs uppercase text-slate-500"><tr><th className="py-2">Item</th><th>Qty</th><th>Warehouse</th><th>Status</th><th>Note</th></tr></thead>
              <tbody>
                {order.items.map((i) => (
                  <tr key={i.id} className="border-b last:border-0">
                    <td className="py-2">{i.name}<br /><span className="font-mono text-xs text-slate-400">{i.sku}</span></td>
                    <td>{i.quantity}</td><td className="text-xs">{i.warehouseId || '—'}</td><td><Badge>{i.status}</Badge></td>
                    <td className="text-xs text-slate-500">
                      {i.damageReason && <>Damaged: {i.damageReason}<br /></>}
                      {i.status === 'SUBSTITUTED' && <>Substitute: {i.proposedSubstituteSku}</>}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
            {order.refunds.length > 0 && (
              <div className="mt-4 border-t pt-3 text-sm">
                <p className="label">Refunds</p>
                {order.refunds.map((r, k) => (
                  <p key={k}><span className="font-mono text-xs">{r.sku}</span> · <Money value={r.amount} /> · {r.type} · <span className="font-mono text-xs">{r.reference}</span></p>
                ))}
              </div>
            )}
          </section>

          <section className="card">
            <h2 className="mb-1 font-semibold">Engine execution</h2>
            <p className="mb-4 text-xs text-slate-500">Live element instances from the Camunda REST API (a second behind Operate).</p>
            {!engine.available ? <p className="text-sm text-slate-500">{engine.message}</p> : engine.instances.map((inst) => (
              <details key={inst.key} className="mb-3 rounded-lg border border-slate-200" open={inst.parentKey === null}>
                <summary className="cursor-pointer select-none px-4 py-2 text-sm font-medium">
                  {PROCESS_LABEL[inst.processDefinitionId] || inst.processDefinitionId}
                  <span className="ml-2 font-mono text-xs text-slate-400">#{inst.key.slice(-6)}</span>
                  <span className="ml-2"><Badge>{inst.state}</Badge></span>
                  {inst.hasIncident && <span className="ml-2"><Badge color="red">INCIDENT</Badge></span>}
                </summary>
                <ul className="space-y-1 px-4 pb-3 pt-1">
                  {inst.elements.map((e, k) => (
                    <li key={k} className="flex items-center gap-2 text-xs">
                      <span className={`h-2 w-2 rounded-full ${e.hasIncident ? 'bg-rose-500' : EL_DOT[e.state] || 'bg-slate-300'}`} />
                      <span className="font-medium">{e.name || e.elementId}</span>
                      <span className="text-slate-400">{e.type}</span>
                    </li>
                  ))}
                </ul>
              </details>
            ))}
          </section>
        </div>
      </div>
    </>
  );
}
