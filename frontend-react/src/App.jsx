import React, { useState, useEffect } from 'react';
import { NavLink, Route, Routes, Navigate } from 'react-router-dom';
import { api, PERSONAS } from './api.js';
import { usePolling } from './hooks.js';
import PlaceOrder from './pages/PlaceOrder.jsx';
import Orders from './pages/Orders.jsx';
import OrderTracker from './pages/OrderTracker.jsx';
import Tasklist from './pages/Tasklist.jsx';
import Inventory from './pages/Inventory.jsx';

const nav = [
  ['/', 'Place order'],
  ['/orders', 'Orders'],
  ['/tasks', 'Tasklist'],
  ['/inventory', 'Inventory'],
];

export default function App() {
  const [personaId, setPersonaId] = useState(localStorage.getItem('persona') || PERSONAS[0].id);
  useEffect(() => localStorage.setItem('persona', personaId), [personaId]);
  const persona = PERSONAS.find((p) => p.id === personaId) || PERSONAS[0];
  const engine = usePolling(api.engine, 10000);
  const connected = engine.data?.connected;

  return (
    <div className="min-h-screen">
      <header className="border-b border-slate-200 bg-white">
        <div className="mx-auto flex max-w-6xl flex-wrap items-center justify-between gap-4 px-4 py-3">
          <div className="flex min-w-0 flex-wrap items-center gap-x-8 gap-y-2">
            <span className="text-lg font-extrabold tracking-tight text-brand-600">FashionHub<span className="text-slate-800"> Fulfilment</span></span>
            <nav className="-mx-1 flex max-w-full gap-1 overflow-x-auto px-1">
              {nav.map(([to, label]) => (
                <NavLink key={to} to={to} end={to === '/'}
                  className={({ isActive }) => `whitespace-nowrap rounded-lg px-3 py-1.5 text-sm font-medium ${isActive ? 'bg-brand-50 text-brand-700' : 'text-slate-600 hover:bg-slate-100'}`}>
                  {label}
                </NavLink>
              ))}
            </nav>
          </div>
          <div className="flex flex-wrap items-center gap-x-4 gap-y-2 text-sm">
            <span title={engine.data?.message || engine.error || ''} className="flex items-center gap-1.5 whitespace-nowrap text-slate-600">
              <span className={`h-2.5 w-2.5 rounded-full ${connected ? 'bg-emerald-500' : 'bg-rose-500'}`} />
              Camunda {connected ? `connected (${engine.data.gatewayVersion})` : 'offline'}
            </span>
            <label className="flex items-center gap-2 whitespace-nowrap text-slate-600">
              Signed in as
              <select className="input !w-auto !py-1" value={personaId} onChange={(e) => setPersonaId(e.target.value)}>
                {PERSONAS.map((p) => <option key={p.id} value={p.id}>{p.name} – {p.role}</option>)}
              </select>
            </label>
          </div>
        </div>
      </header>
      {engine.data && !connected && (
        <div className="bg-amber-50 px-4 py-2 text-center text-sm text-amber-800">
          The Camunda engine is not reachable. Start it with <code className="font-mono">c8run.exe start</code> and restart the backend.
        </div>
      )}
      <main className="mx-auto max-w-6xl px-4 py-6 sm:py-8">
        <Routes>
          <Route path="/" element={<PlaceOrder />} />
          <Route path="/orders" element={<Orders />} />
          <Route path="/orders/:id" element={<OrderTracker />} />
          <Route path="/tasks" element={<Tasklist persona={persona} />} />
          <Route path="/inventory" element={<Inventory />} />
          <Route path="*" element={<Navigate to="/" replace />} />
        </Routes>
      </main>
    </div>
  );
}
