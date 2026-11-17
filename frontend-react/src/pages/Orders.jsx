import React from 'react';
import { Link } from 'react-router-dom';
import { api } from '../api.js';
import { usePolling } from '../hooks.js';
import { Badge, ErrorBox, Money, PageTitle, Spinner } from '../components/ui.jsx';

export default function Orders() {
  const { data, error, loading } = usePolling(api.orders, 3000);
  if (loading) return <Spinner />;
  return (
    <>
      <PageTitle title="Orders" subtitle="Every order is one Camunda process instance.">
        <Link to="/" className="btn-primary">New order</Link>
      </PageTitle>
      <ErrorBox message={error} />
      <div className="card overflow-x-auto">
        {!data?.length ? (
          <p className="py-6 text-center text-sm text-slate-500">No orders yet – place one to start a process instance.</p>
        ) : (
          <table className="w-full text-left text-sm">
            <thead className="border-b text-xs uppercase text-slate-500">
              <tr><th className="py-2">Order</th><th>Customer</th><th>Items</th><th>Total</th><th>Refunded</th><th>Status</th><th>Placed</th></tr>
            </thead>
            <tbody>
              {data.map((o) => (
                <tr key={o.id} className="border-b last:border-0 hover:bg-slate-50">
                  <td className="py-2"><Link className="font-mono text-brand-700 hover:underline" to={`/orders/${o.id}`}>{o.id}</Link></td>
                  <td>{o.customerName}</td>
                  <td>{o.itemCount}</td>
                  <td><Money value={o.totalAmount} /></td>
                  <td>{Number(o.refundedAmount) > 0 ? <Money value={o.refundedAmount} /> : '—'}</td>
                  <td><Badge>{o.status}</Badge></td>
                  <td className="whitespace-nowrap text-slate-500">{new Date(o.createdAt).toLocaleString('en-IN', { dateStyle: 'medium', timeStyle: 'short' })}</td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </div>
    </>
  );
}
