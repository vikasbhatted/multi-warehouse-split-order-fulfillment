// Thin fetch wrapper around the Spring Boot REST API.
async function request(path, options = {}) {
  const res = await fetch('/api' + path, {
    headers: { 'Content-Type': 'application/json' },
    ...options,
    body: options.body ? JSON.stringify(options.body) : undefined,
  });
  if (!res.ok) {
    let message = `${res.status} ${res.statusText}`;
    try {
      const problem = await res.json();
      message = problem.detail || problem.title || message;
      if (problem.errors) {
        message += ': ' + Object.entries(problem.errors).map(([k, v]) => `${k} ${v}`).join(', ');
      }
    } catch { /* body was not JSON */ }
    const err = new Error(message);
    err.status = res.status;
    throw err;
  }
  return res.status === 204 ? null : res.json();
}

export const api = {
  engine: () => request('/system/engine'),
  catalog: () => request('/catalog'),
  warehouses: () => request('/warehouses'),
  createOrder: (body) => request('/orders', { method: 'POST', body }),
  orders: () => request('/orders'),
  order: (id) => request(`/orders/${id}`),
  tracker: (id) => request(`/orders/${id}/tracker`),
  tasks: () => request('/tasks'),
  task: (key) => request(`/tasks/${key}`),
  claim: (key, assignee) => request(`/tasks/${key}/claim`, { method: 'POST', body: { assignee } }),
  unclaim: (key, assignee) => request(`/tasks/${key}/unclaim`, { method: 'POST', body: { assignee } }),
  complete: (key, assignee, variables) =>
    request(`/tasks/${key}/complete`, { method: 'POST', body: { assignee, variables } }),
};

// Demo personas (no real authentication - see README "Security notes").
export const PERSONAS = [
  { id: 'anna.picker', name: 'Anna', role: 'Warehouse picker', groups: ['warehouse-pickers'] },
  { id: 'raj.support', name: 'Raj', role: 'Customer support', groups: ['customer-support'] },
  { id: 'mia.finance', name: 'Mia', role: 'Finance team', groups: ['finance-team'] },
];
