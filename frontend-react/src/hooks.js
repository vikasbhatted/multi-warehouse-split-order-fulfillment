import { useEffect, useRef, useState, useCallback } from 'react';

/**
 * Calls `fn` immediately and then every `ms` milliseconds; exposes data / error / reload.
 * - never runs two requests at the same time (a slow backend cannot pile up requests)
 * - ignores responses that arrive after the deps changed or the component unmounted
 */
export function usePolling(fn, ms, deps = [], enabled = true) {
  const [data, setData] = useState(null);
  const [error, setError] = useState(null);
  const [loading, setLoading] = useState(true);
  const fnRef = useRef(fn);
  fnRef.current = fn;
  const generation = useRef(0);
  const inFlight = useRef(false);

  const load = useCallback(async () => {
    if (inFlight.current) return;
    inFlight.current = true;
    const gen = generation.current;
    try {
      const result = await fnRef.current();
      if (gen === generation.current) {
        setData(result);
        setError(null);
      }
    } catch (e) {
      if (gen === generation.current) setError(e.message);
    } finally {
      inFlight.current = false;
      if (gen === generation.current) setLoading(false);
    }
  }, []);

  useEffect(() => {
    if (!enabled) return undefined;
    generation.current += 1;
    inFlight.current = false;
    setLoading(true);
    load();
    const id = setInterval(load, ms);
    return () => {
      generation.current += 1;
      clearInterval(id);
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [ms, enabled, load, ...deps]);

  return { data, error, loading, reload: load };
}
