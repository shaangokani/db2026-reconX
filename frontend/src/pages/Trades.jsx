// TICKET-ADV114 — Compound DataTable.
// TICKET-ADV117 — useDebouncedSearch.
import React, { useState, useCallback, useEffect } from 'react';
import { withAuth } from '@components/withAuth.jsx';
import DataTable from '@components/DataTable.jsx';
import { useDebouncedSearch } from '@hooks/useDebouncedSearch.js';
import { TradeRow } from '@components/TradeRow.jsx';
import { api } from '@services/apiService.js';

function Trades() {
  const [search, setSearch] = useState('');
  const debounced = useDebouncedSearch(search, 300);
  const [page, setPage] = useState(0);
  const [data, setData] = useState({ items: [], totalPages: 0 });
  const [selectedId, setSelectedId] = useState(null);

  // TICKET-ADV121: useCallback to prevent unnecessary re-renders of TradeRow
  const handleSelect = useCallback((id) => {
    setSelectedId(id);
    console.log("Selected trade:", id);
  }, []);

  useEffect(() => {
    let cancelled = false;
    const params = new URLSearchParams({ page: String(page) });
    if (debounced) params.set('status', debounced);

    api.listTrades(`?${params.toString()}`)
      .then((res) => {
        if (cancelled) return;
        setData({
          // TradeRow/Dashboard both expect {symbol, qty}; the REST DTO
          // returns instrumentSymbol/quantity — map at the boundary.
          items: res.items.map((t) => ({
            id: t.id,
            tradeRef: t.tradeRef,
            symbol: t.instrumentSymbol,
            qty: t.quantity,
            price: t.price,
            status: t.status,
          })),
          totalPages: res.totalPages,
        });
      })
      .catch(() => {
        if (!cancelled) setData({ items: [], totalPages: 0 });
      });

    return () => { cancelled = true; };
  }, [page, debounced]);

  return (
    <section>
      <h2>Trades</h2>
      <input
        aria-label="Filter by status"
        placeholder="status filter (PENDING/MATCHED/…)"
        value={search}
        onChange={(e) => setSearch(e.target.value.toUpperCase())}
      />
      <DataTable>
        <DataTable.Header columns={[
          { key: 'tradeRef', label: 'Ref' },
          { key: 'symbol',   label: 'Symbol' },
          { key: 'qty',      label: 'Qty' },
          { key: 'price',    label: 'Price' },
          { key: 'status',   label: 'Status' },
        ]} />
        <DataTable.Body
          rows={data.items}
          render={(t) => (
            <TradeRow key={t.id} trade={t} onClick={handleSelect} />
          )}
        />
        <DataTable.Pagination
          page={page}
          totalPages={Math.max(1, data.totalPages)}
          onChange={setPage}
        />
      </DataTable>
    </section>
  );
}

export default withAuth(Trades);
