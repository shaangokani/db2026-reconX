// TICKET-ADV119 — React.memo on TradeRow.
import React from 'react';

function TradeRowImpl({ trade, onClick }) {
  // We include a generic 'onClick' so ADV121 can wire this up.
  // We use standard HTML table elements for the row.
  return (
    <div 
      role="row" 
      className="data-table__row" 
      onClick={() => onClick(trade.id)}
      style={{ cursor: 'pointer' }}
    >
      <span role="cell">{trade.tradeRef}</span>
      <span role="cell">{trade.symbol}</span>
      <span role="cell">{trade.qty}</span>
      <span role="cell">${trade.price.toFixed(2)}</span>
      <span role="cell" className={`status status--${trade.status.toLowerCase()}`}>
        {trade.status}
      </span>
    </div>
  );
}

// Custom comparison function for React.memo
function areEqual(prevProps, nextProps) {
  // Only re-render if the ID, status, or price changes.
  return (
    prevProps.trade.id === nextProps.trade.id &&
    prevProps.trade.status === nextProps.trade.status &&
    prevProps.trade.price === nextProps.trade.price
  );
}

export const TradeRow = React.memo(TradeRowImpl, areEqual);
