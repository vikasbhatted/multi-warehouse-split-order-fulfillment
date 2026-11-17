import React from 'react';
import { api } from '../api.js';
import { usePolling } from '../hooks.js';
import { ErrorBox, Money, PageTitle, Spinner } from '../components/ui.jsx';

export default function Inventory() {
  const cat = usePolling(api.catalog, 5000);
  const wh = usePolling(api.warehouses, 60000);
  if (cat.loading || wh.loading) return <Spinner />;
  const warehouses = wh.data || [];
  return (
    <>
      <PageTitle title="Inventory" subtitle="Stock per fulfilment centre. Allocation reserves stock when an order is validated." />
      <ErrorBox message={cat.error || wh.error} />
      <div className="card overflow-x-auto">
        <table className="w-full text-left text-sm">
          <thead className="border-b text-xs uppercase text-slate-500">
            <tr>
              <th className="py-2 pr-4">SKU</th><th className="pr-4">Product</th><th className="pr-4">Price</th>
              {warehouses.map((w) => <th key={w.id} className="pr-4">{w.city}</th>)}
              <th>Substitute</th>
            </tr>
          </thead>
          <tbody>
            {(cat.data || []).map((p) => (
              <tr key={p.sku} className="border-b last:border-0">
                <td className="py-2 pr-4 font-mono text-xs">{p.sku}</td>
                <td className="pr-4">{p.name}</td>
                <td className="pr-4"><Money value={p.price} /></td>
                {warehouses.map((w) => (
                  <td key={w.id} className={`pr-4 ${(p.stockByWarehouse[w.id] ?? 0) === 0 ? 'text-slate-300' : ''}`}>
                    {p.stockByWarehouse[w.id] ?? 0}
                  </td>
                ))}
                <td className="font-mono text-xs text-slate-500">{p.substituteSku || '—'}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </>
  );
}
