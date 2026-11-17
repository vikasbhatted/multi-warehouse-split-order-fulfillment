import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { api } from '../api.js';
import { usePolling } from '../hooks.js';
import { ErrorBox, Money, PageTitle } from '../components/ui.jsx';

const BASE_ADDRESS = { line1: 'Mauerstrasse 12', city: 'Berlin', postalCode: '10117', country: 'DE' };

// One-click demo scenarios that exercise the different paths of the BPMN models.
const SCENARIOS = [
  { label: 'Split shipment · 3 parcels', hint: 'Items live in 3 warehouses → parallel multi-instance',
    items: [['JACKET-GRN-L', 1], ['JEANS-BLK-32', 1], ['SNEAK-WHT-42', 1], ['SCARF-WOL-OS', 1]] },
  { label: 'Single parcel', hint: 'One warehouse holds everything → consolidated',
    items: [['TSHIRT-WHT-M', 2], ['JEANS-BLU-32', 1]] },
  { label: 'Out of stock', hint: 'Allocation fails → ORDER_REJECTED error boundary',
    items: [['JACKET-GRN-L', 5]] },
  { label: 'Undeliverable address', hint: 'Postal code 00000 → carrier 422 → ADDRESS_INVALID',
    items: [['JEANS-BLK-32', 1], ['SNEAK-WHT-42', 1]], address: { postalCode: '00000' } },
  { label: 'Refund gateway failure', hint: 'Damage either item (no substitute) → refund fails → REFUND_FAILED error boundary',
    items: [['JACKET-GRN-L', 1], ['DRESS-RED-S', 1]], simulateRefundFailure: true },
];

export default function PlaceOrder() {
  const nav = useNavigate();
  const { data: catalog } = usePolling(api.catalog, 15000);
  const [name, setName] = useState('Lena Fischer');
  const [email, setEmail] = useState('lena.fischer@example.com');
  const [address, setAddress] = useState(BASE_ADDRESS);
  const [items, setItems] = useState([{ sku: 'JACKET-GRN-L', quantity: 1 }, { sku: 'JEANS-BLK-32', quantity: 1 },
    { sku: 'SNEAK-WHT-42', quantity: 1 }, { sku: 'SCARF-WOL-OS', quantity: 1 }]);
  const [simulateRefundFailure, setSim] = useState(false);
  const [error, setError] = useState(null);
  const [busy, setBusy] = useState(false);

  const products = catalog || [];
  const price = (sku) => products.find((p) => p.sku === sku)?.price ?? 0;
  const qty = (q) => Number(q) || 0; // quantity may be '' while the user is typing
  const total = items.reduce((s, i) => s + price(i.sku) * qty(i.quantity), 0);
  const unitCount = items.reduce((s, i) => s + qty(i.quantity), 0);

  const applyScenario = (s) => {
    setItems(s.items.map(([sku, quantity]) => ({ sku, quantity })));
    setAddress({ ...BASE_ADDRESS, ...(s.address || {}) });
    setSim(!!s.simulateRefundFailure);
    setError(null);
  };
  const setItem = (idx, patch) => setItems((arr) => arr.map((it, i) => (i === idx ? { ...it, ...patch } : it)));

  const submit = async (e) => {
    e.preventDefault();
    setBusy(true);
    setError(null);
    try {
      const payload = items.map((i) => ({ sku: i.sku, quantity: Math.max(1, Math.min(5, qty(i.quantity) || 1)) }));
      const created = await api.createOrder({ customerName: name, customerEmail: email, shippingAddress: address, items: payload, simulateRefundFailure });
      nav(`/orders/${created.id}`);
    } catch (err) {
      setError(err.message);
      setBusy(false);
    }
  };

  return (
    <>
      <PageTitle title="Place an order" subtitle="Submitting the form starts the split_order_fulfillment process in Camunda." />
      <div className="mb-6 card">
        <p className="label">Demo scenarios</p>
        <div className="flex flex-wrap gap-2">
          {SCENARIOS.map((s) => (
            <button key={s.label} type="button" className="btn-secondary" title={s.hint} onClick={() => applyScenario(s)}>{s.label}</button>
          ))}
        </div>
      </div>

      <form onSubmit={submit} className="grid gap-6 lg:grid-cols-3">
        <div className="space-y-6 lg:col-span-2">
          <section className="card">
            <h2 className="mb-4 font-semibold">Items</h2>
            <div className="space-y-3">
              {items.map((it, idx) => (
                <div key={idx} className="flex flex-wrap items-center gap-3 sm:flex-nowrap">
                  <select className="input min-w-0 basis-full sm:basis-auto sm:flex-1" value={it.sku} onChange={(e) => setItem(idx, { sku: e.target.value })}>
                    {products.map((p) => <option key={p.sku} value={p.sku}>{p.name} — {p.totalStock} in stock</option>)}
                    {!products.length && <option>{it.sku}</option>}
                  </select>
                  <input className="input !w-20" type="number" min="1" max="5" required value={it.quantity} aria-label="Quantity"
                    onChange={(e) => {
                      const raw = e.target.value;
                      setItem(idx, { quantity: raw === '' ? '' : Math.max(1, Math.min(5, Math.trunc(Number(raw)) || 1)) });
                    }}
                    onBlur={() => { if (!qty(it.quantity)) setItem(idx, { quantity: 1 }); }} />
                  <span className="ml-auto w-28 text-right text-sm text-slate-600 sm:ml-0"><Money value={price(it.sku) * qty(it.quantity)} /></span>
                  <button type="button" className="text-slate-400 hover:text-rose-600" disabled={items.length === 1}
                    onClick={() => setItems((a) => a.filter((_, i) => i !== idx))} aria-label="Remove item">✕</button>
                </div>
              ))}
            </div>
            <button type="button" className="btn-secondary mt-4" disabled={items.length >= 10}
              onClick={() => setItems((a) => [...a, { sku: products[0]?.sku || '', quantity: 1 }])}>+ Add item</button>
          </section>

          <section className="card">
            <h2 className="mb-4 font-semibold">Customer & delivery</h2>
            <div className="grid gap-4 sm:grid-cols-2">
              <div><label className="label">Name</label><input className="input" required value={name} onChange={(e) => setName(e.target.value)} /></div>
              <div><label className="label">Email</label><input className="input" type="email" required value={email} onChange={(e) => setEmail(e.target.value)} /></div>
              <div className="sm:col-span-2"><label className="label">Address line 1</label><input className="input" required value={address.line1} onChange={(e) => setAddress({ ...address, line1: e.target.value })} /></div>
              <div><label className="label">City</label><input className="input" required value={address.city} onChange={(e) => setAddress({ ...address, city: e.target.value })} /></div>
              <div className="grid grid-cols-2 gap-4">
                <div><label className="label">Postal code</label><input className="input" required value={address.postalCode} onChange={(e) => setAddress({ ...address, postalCode: e.target.value })} /></div>
                <div><label className="label">Country</label><input className="input" required minLength={2} maxLength={2} pattern="[A-Za-z]{2}" title="2-letter ISO country code, e.g. IN" value={address.country} onChange={(e) => setAddress({ ...address, country: e.target.value.toUpperCase() })} /></div>
              </div>
            </div>
          </section>
        </div>

        <aside className="space-y-6">
          <section className="card">
            <h2 className="mb-3 font-semibold">Summary</h2>
            <div className="flex justify-between text-sm"><span>Items</span><span>{unitCount}</span></div>
            <div className="mt-1 flex justify-between text-lg font-bold"><span>Total</span><Money value={total} /></div>
            <label className="mt-4 flex items-start gap-2 text-sm text-slate-600">
              <input type="checkbox" className="mt-1" checked={simulateRefundFailure} onChange={(e) => setSim(e.target.checked)} />
              <span>Simulate payment-gateway outage for refunds <span className="block text-xs text-slate-400">demo flag → REFUND_FAILED path</span></span>
            </label>
            <div className="mt-4"><ErrorBox message={error} /></div>
            <button className="btn-primary mt-4 w-full" disabled={busy}>{busy ? 'Starting process…' : 'Place order'}</button>
          </section>
        </aside>
      </form>
    </>
  );
}
