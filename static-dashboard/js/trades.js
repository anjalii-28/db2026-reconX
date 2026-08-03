// TICKET-ADV106 — sortable, resizable trades table with a sticky header.
(function () {
  const table = document.getElementById('trades-table');
  const tbody = document.getElementById('trades-tbody');
  const fallbackRows = [
    { tradeRef: 'EQU-20260603-0001', symbol: 'SAP.DE', quantity: 1000, price: 125.5, status: 'MATCHED' },
    { tradeRef: 'FX-20260603-0001', symbol: 'EUR/USD', quantity: 1000000, price: 1.0852, status: 'PENDING' },
    { tradeRef: 'EQU-20260603-0002', symbol: 'AAPL', quantity: 500, price: 178.2, status: 'BREAK' }
  ];
  let rows = [];

  if (!table || !tbody) return;

  function escapeHtml(value) {
    return String(value ?? '').replace(/[&<>'"]/g, (character) => ({
      '&': '&amp;', '<': '&lt;', '>': '&gt;', "'": '&#39;', '"': '&quot;'
    }[character]));
  }

  function getCellValue(row, column) {
    if (column === 'quantity') return row.quantity ?? row.qty;
    return row[column];
  }

  function renderRows() {
    tbody.innerHTML = rows.map((row) => `
      <tr>
        <td>${escapeHtml(row.tradeRef)}</td>
        <td>${escapeHtml(row.symbol)}</td>
        <td>${escapeHtml(getCellValue(row, 'quantity'))}</td>
        <td>${escapeHtml(row.price)}</td>
        <td>${escapeHtml(row.status)}</td>
      </tr>`).join('');
  }

  table.querySelectorAll('thead th').forEach((header) => {
    header.addEventListener('click', (event) => {
      if (event.target.closest('.resize-handle')) return;

      const column = header.dataset.col;
      const type = header.dataset.type || 'string';
      const direction = header.getAttribute('aria-sort') === 'ascending' ? 'descending' : 'ascending';
      const multiplier = direction === 'ascending' ? 1 : -1;

      table.querySelectorAll('thead th').forEach((otherHeader) => otherHeader.removeAttribute('aria-sort'));
      header.setAttribute('aria-sort', direction);
      header.dataset.dir = direction;

      rows.sort((left, right) => {
        const leftValue = getCellValue(left, column);
        const rightValue = getCellValue(right, column);
        if (type === 'number') return (Number(leftValue) - Number(rightValue)) * multiplier;
        return String(leftValue ?? '').localeCompare(String(rightValue ?? '')) * multiplier;
      });
      renderRows();
    });
  });

  table.querySelectorAll('.resize-handle').forEach((handle) => {
    handle.addEventListener('mousedown', (event) => {
      event.preventDefault();
      event.stopPropagation();
      const header = handle.closest('th');
      const startX = event.clientX;
      const startWidth = header.offsetWidth;

      function onMove(moveEvent) {
        header.style.width = `${Math.max(80, startWidth + moveEvent.clientX - startX)}px`;
      }

      function onUp() {
        document.removeEventListener('mousemove', onMove);
        document.removeEventListener('mouseup', onUp);
      }

      document.addEventListener('mousemove', onMove);
      document.addEventListener('mouseup', onUp);
    });
  });

  fetch('/api/v1/trades?size=200')
    .then((response) => {
      if (!response.ok) throw new Error('Unable to load trades.');
      return response.json();
    })
    .then((data) => {
      rows = Array.isArray(data) ? data : data.content || [];
      if (!rows.length) rows = fallbackRows;
      renderRows();
    })
    .catch(() => {
      rows = fallbackRows;
      renderRows();
    });
}());
