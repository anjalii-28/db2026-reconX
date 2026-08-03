// TICKET-ADV104 / TICKET-ADV105 — EventSource trade feed.
(function () {
  const feed = document.getElementById('trade-feed');
  const statusBadge = document.getElementById('sse-status');
  const streamUrl = '/api/v1/trades/stream';
  const maxFeedEntries = 50;
  let sse = null;

  if (!feed || !window.EventSource) return;

  function updateConnectionBadge(message, variant) {
    if (!statusBadge) return;

    statusBadge.textContent = message;
    statusBadge.dataset.connection = variant;
  }

  function escapeHtml(value) {
    return String(value ?? '').replace(/[&<>'"]/g, (character) => ({
      '&': '&amp;',
      '<': '&lt;',
      '>': '&gt;',
      "'": '&#39;',
      '"': '&quot;'
    }[character]));
  }

  function formatQuantity(value) {
    return new Intl.NumberFormat('en-US').format(Number(value) || 0);
  }

  function formatPrice(value) {
    return new Intl.NumberFormat('en-US', {
      minimumFractionDigits: 2,
      maximumFractionDigits: 4
    }).format(Number(value) || 0);
  }

  function statusModifier(status) {
    switch (String(status || '').toUpperCase()) {
      case 'MATCHED': return 'trade-card--matched';
      case 'BREAK':
      case 'UNMATCHED': return 'trade-card--break';
      case 'PENDING': return 'trade-card--pending';
      default: return '';
    }
  }

  function prependTradeRow(trade) {
    const status = String(trade.status || 'PENDING').toUpperCase();
    const row = document.createElement('article');
    const reference = trade.tradeRef || trade.tradeReference || 'Unknown trade';
    const currency = trade.currency || '';

    row.className = `trade-card ${statusModifier(status)} trade-card--new`.trim();
    row.innerHTML = `
      <header class="trade-card__header">
        <strong>${escapeHtml(reference)}</strong>
        <span>${escapeHtml(status)}</span>
      </header>
      <div class="trade-card__body">
        <span>${escapeHtml(trade.symbol || '—')}</span>
        <span>Qty: ${formatQuantity(trade.qty ?? trade.quantity)}</span>
        <span>Price: ${formatPrice(trade.price)} ${escapeHtml(currency)}</span>
      </div>`;

    feed.prepend(row);
    window.setTimeout(() => row.classList.remove('trade-card--new'), 500);

    while (feed.children.length > maxFeedEntries) {
      feed.lastElementChild.remove();
    }
  }

  function connect() {
    sse = new EventSource(streamUrl);

    sse.onopen = () => updateConnectionBadge('Live', 'live');
    sse.onmessage = (event) => {
      try {
        prependTradeRow(JSON.parse(event.data));
      } catch (error) {
        console.warn('Ignoring invalid trade stream payload.', error);
      }
    };
    sse.onerror = () => updateConnectionBadge('Reconnecting…', 'reconnecting');
  }

  window.addEventListener('beforeunload', () => sse?.close());
  connect();
}());
