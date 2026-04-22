document.addEventListener('DOMContentLoaded', () => {
    const statusButton = document.getElementById('statusDisplay');
    const statusContainer = document.getElementById('appContainer');
    const passwordInput = document.getElementById('password');
    const formErrors = document.getElementById('formErrors');
    const pairsSection = document.getElementById('pairsSection');
    const addPairBtn = document.getElementById('addPairBtn');
    const mainContent = document.getElementById('mainContent');

    let isRunning = false;
    let isSubmitting = false;
    let pairCount = 1;

    const renderStatus = () => {
        if (isSubmitting) {
            statusButton.disabled = true;
            statusButton.textContent = 'Загрузка...';
            statusButton.className = 'top-status top-input status-stopped';
            statusContainer.className = 'container status-stopped';
            return;
        }
        statusButton.disabled = false;
        if (isRunning) {
            statusButton.textContent = 'Запущено';
            statusButton.className = 'top-status top-input status-running';
            statusContainer.className = 'container status-running';
        } else {
            statusButton.textContent = 'Остановлено';
            statusButton.className = 'top-status top-input status-stopped';
            statusContainer.className = 'container status-stopped';
        }
    };

    const setSubmitting = (val) => { isSubmitting = val; renderStatus(); };
    const setFormErrors = (html = '') => { formErrors.innerHTML = html; };

    const sanitizeSymbol = (input) => {
        const raw = input.value.toUpperCase();
        const hadTrailingSpace = /\s$/.test(raw);
        let normalized = raw.replace(/[^A-Z\s]/g, '').replace(/\s{2,}/g, ' ').replace(/^\s+/g, '');
        const parts = normalized.trim().split(' ').filter(Boolean);
        if (parts.length > 2) normalized = parts.slice(0, 2).join(' ');
        else normalized = parts.join(' ');
        if (hadTrailingSpace && normalized.length && normalized.split(' ').length < 2) normalized += ' ';
        else if (parts.length >= 2) normalized = parts.slice(0, 2).join(' ');
        if (input.value !== normalized) input.value = normalized;
        return normalized.trim();
    };

    const sanitizePercent = (input) => {
        const raw = input.value.replace(',', '.');
        let cleaned = '';
        let hasDot = false;
        for (const char of raw) {
            if (/\d/.test(char)) cleaned += char;
            else if (char === '.' && !hasDot) { cleaned += '.'; hasDot = true; }
        }
        const parts = cleaned.split('.');
        if (parts[1] && parts[1].length > 2) cleaned = `${parts[0]}.${parts[1].slice(0, 2)}`;
        const displayValue = cleaned.replace('.', ',');
        if (input.value !== displayValue) input.value = displayValue;
        return parseFloat(cleaned);
    };

    const addPairRow = () => {
        const idx = pairCount++;
        const row = document.createElement('div');
        row.className = 'pair-row';
        row.id = `pairRow${idx}`;
        row.innerHTML = `
            <input type="text" class="symbol-input top-input" placeholder="Символ BTC USDT">
            <input type="text" class="percent-input top-input" placeholder="0,00">
            <button type="button" class="btn-remove" data-idx="${idx}">×</button>
        `;
        pairsSection.appendChild(row);

        const symbolInput = row.querySelector('.symbol-input');
        const percentInput = row.querySelector('.percent-input');
        symbolInput.addEventListener('input', () => sanitizeSymbol(symbolInput));
        percentInput.addEventListener('input', () => sanitizePercent(percentInput));

        row.querySelector('.btn-remove').addEventListener('click', () => {
            row.remove();
        });
    };

    addPairBtn.addEventListener('click', addPairRow);

    const firstSymbol = document.querySelector('#pairRow0 .symbol-input');
    const firstPercent = document.querySelector('#pairRow0 .percent-input');
    firstSymbol.addEventListener('input', () => sanitizeSymbol(firstSymbol));
    firstPercent.addEventListener('input', () => sanitizePercent(firstPercent));

    const collectPairs = () => {
        const rows = pairsSection.querySelectorAll('.pair-row');
        const pairs = [];
        const errors = [];
        rows.forEach((row, i) => {
            const symbolInput = row.querySelector('.symbol-input');
            const percentInput = row.querySelector('.percent-input');
            const symbol = sanitizeSymbol(symbolInput);
            const percent = sanitizePercent(percentInput);
            const parts = symbol.split(' ').filter(Boolean);
            if (parts.length !== 2 || !parts.every(p => /^[A-Z]+$/.test(p))) {
                errors.push(`Пара ${i + 1}: неверный символ.`);
            }
            if (isNaN(percent) || percent < 0.01 || percent > 100) {
                errors.push(`Пара ${i + 1}: процент должен быть от 0,01 до 100.`);
            }
            if (errors.length === 0) {
                pairs.push({ symbol, percent: percent.toString() });
            }
        });
        return { pairs, errors };
    };

    const parseErrorMessage = async (response, fallback) => {
        try {
            const data = await response.json();
            if (typeof data === 'string') return data;
            if (data && typeof data.message === 'string') return data.message;
        } catch (e) {}
        return fallback;
    };

    const sendStartRequest = async (pairs) => {
        const password = passwordInput.value;
        const response = await fetch('/api/start', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ password, pairs })
        });
        if (!response.ok) {
            const msg = await parseErrorMessage(response, 'Не удалось запустить процесс.');
            throw new Error(msg);
        }
    };

    const sendStopRequest = async () => {
        const password = passwordInput.value;
        const response = await fetch('/api/stop', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ password })
        });
        if (!response.ok) {
            const msg = await parseErrorMessage(response, 'Не удалось остановить процесс.');
            throw new Error(msg);
        }
    };

    statusButton.addEventListener('click', async () => {
        if (isSubmitting) return;

        if (!isRunning) {
            const { pairs, errors } = collectPairs();
            if (errors.length > 0) {
                setFormErrors(errors.map(e => `<div>${e}</div>`).join(''));
                return;
            }
            setSubmitting(true);
            try {
                await sendStartRequest(pairs);
                setFormErrors('');
                isRunning = true;
                renderPairsCards(pairs.map(p => p.symbol));
            } catch (e) {
                setFormErrors(`<div>${e.message}</div>`);
            } finally {
                setSubmitting(false);
            }
            return;
        }

        setSubmitting(true);
        try {
            await sendStopRequest();
            setFormErrors('');
            isRunning = false;
            clearPairsCards();
        } catch (e) {
            setFormErrors(`<div>${e.message}</div>`);
        } finally {
            setSubmitting(false);
        }
    });

    const formatNum = (value) => {
        if (value === null || value === undefined || value === '') return '0';
        const n = typeof value === 'number' ? value : Number(String(value).replace(',', '.'));
        if (isNaN(n)) return '0';
        return n.toLocaleString('ru-RU', { maximumFractionDigits: 6 }).replace(/\u00A0/g, ' ');
    };

    const renderPairsCards = (symbols) => {
        clearPairsCards();
        const grid = document.createElement('div');
        grid.className = 'pairs-grid';
        grid.id = 'pairsGrid';

        symbols.forEach(symbol => {
            const card = document.createElement('div');
            card.className = 'pair-card';
            const key = symbol.replace(' ', '_');
            card.id = `card-${key}`;
            card.innerHTML = `
                <div class="pair-card-title">${symbol}</div>
                <div class="pair-card-status" id="status-${key}">Ожидание</div>
                <div class="usdt-row">
                    <div class="exchange-label">USDT</div>
                    <div class="value-box" id="bybit-usdt-${key}">0</div>
                    <div class="value-box" id="kucoin-usdt-${key}">0</div>
                </div>
                <div class="exchange-row">
                    <div class="exchange-label">Bybit</div>
                    <div class="value-box" id="bybit-crypto-${key}">0</div>
                    <div class="value-box" id="bybit-coins-${key}">0</div>
                </div>
                <div class="exchange-row">
                    <div class="exchange-label">Kucoin</div>
                    <div class="value-box" id="kucoin-crypto-${key}">0</div>
                    <div class="value-box" id="kucoin-coins-${key}">0</div>
                </div>
            `;
            grid.appendChild(card);
        });

        mainContent.insertBefore(grid, mainContent.firstChild);
    };

    const clearPairsCards = () => {
        const grid = document.getElementById('pairsGrid');
        if (grid) grid.remove();
    };

    const processStatusLabel = (process) => {
        const map = {
            'NON': 'Инициализация',
            'READY_FOR_ALGO': 'Ожидание входа',
            'IN_ALGO': 'В алгоритме',
            'READY_TO_EXIT_FROM_ALGO': 'Выход из алгоритма',
            'REBALANCING': 'Ребалансировка',
            'ERROR': 'Ошибка'
        };
        return map[process] || process;
    };

    const loadAllProfiles = async () => {
        try {
            const response = await fetch('/profile/all');
            if (!response.ok) return;
            const data = await response.json();

            (data.pairs || []).forEach(pair => {
                const key = pair.symbol.replace(' ', '_');

                const statusEl = document.getElementById(`status-${key}`);
                if (statusEl) statusEl.textContent = processStatusLabel(pair.process);

                const bybitUsdtEl = document.getElementById(`bybit-usdt-${key}`);
                if (bybitUsdtEl) bybitUsdtEl.textContent = formatNum(data.bybitUsdt);

                const kucoinUsdtEl = document.getElementById(`kucoin-usdt-${key}`);
                if (kucoinUsdtEl) kucoinUsdtEl.textContent = formatNum(data.kucoinUsdt);

                const bybitCryptoEl = document.getElementById(`bybit-crypto-${key}`);
                if (bybitCryptoEl) bybitCryptoEl.textContent = formatNum(pair.bybit.crypto);

                const bybitCoinsEl = document.getElementById(`bybit-coins-${key}`);
                if (bybitCoinsEl) bybitCoinsEl.textContent = formatNum(pair.bybit.coins);

                const kucoinCryptoEl = document.getElementById(`kucoin-crypto-${key}`);
                if (kucoinCryptoEl) kucoinCryptoEl.textContent = formatNum(pair.kucoin.crypto);

                const kucoinCoinsEl = document.getElementById(`kucoin-coins-${key}`);
                if (kucoinCoinsEl) kucoinCoinsEl.textContent = formatNum(pair.kucoin.coins);
            });
        } catch (e) {
            console.error('Error loading profiles:', e.message);
        }
    };

    const getHistoryMarker = (type) => {
        const markers = {
            'R': { text: 'Р', class: 'status-rebalance', title: 'Ребаланс' },
            'D': { text: 'В', class: 'status-entry', title: 'Вход в сделку' },
            'W': { text: 'О', class: 'status-launch', title: 'Ожидание' },
            'ERROR': { text: 'Е', class: 'status-stop', title: 'Ошибка' }
        };
        return markers[type] || { text: '?', class: 'status-stopped', title: 'Неизвестно' };
    };

        const loadHistory = async () => {
            try {
                const response = await fetch('/profile/history');
                if (!response.ok) return;
                const historyList = await response.json();
                const container = document.getElementById('historyList');
                if (!container || !Array.isArray(historyList)) return;

                container.innerHTML = '';
                historyList.forEach(entry => {
                    const marker = getHistoryMarker(entry.type);
                    const row = document.createElement('div');
                    row.className = 'history-row';

                    let symbolDisplay = entry.symbol || '';
                    let bybitDisplay = formatNum(entry.bybitPrice);
                    let kucoinDisplay = formatNum(entry.kucoinPrice);

                    if (entry.type === 'R' && entry.symbol === 'GLOBAL') {
                        symbolDisplay = 'ВСЕ ПАРЫ';
                        bybitDisplay = '—';
                        kucoinDisplay = '—';
                    }

                    row.innerHTML = `
                        <div class="history-marker ${marker.class}" title="${marker.title}">${marker.text}</div>
                        <div class="history-symbol">${symbolDisplay}</div>
                        <div class="history-value">${bybitDisplay}</div>
                        <div class="history-value">${kucoinDisplay}</div>
                    `;
                    container.appendChild(row);
                });
            } catch (e) {
                console.error('Error loading history:', e.message);
            }
        };

    const loadCompletedTrades = async () => {
        try {
            const response = await fetch('/api/trades/completed');
            if (!response.ok) return;
            const trades = await response.json();
            const container = document.getElementById('completedTradesList');
            if (!container || !Array.isArray(trades)) return;

            container.innerHTML = '';
            trades.forEach(trade => {
                const date = new Date(trade.timestamp).toLocaleString('ru-RU');
                const row = document.createElement('div');
                row.className = 'completed-trade-row';
                row.innerHTML = `
                    <div class="trade-cell">${date}</div>
                    <div class="trade-cell">${trade.symbol}</div>
                    <div class="trade-cell">${formatNum(trade.totalProfitUsd)} $ (${formatNum(trade.totalProfitPercent)}%)</div>
                    <div class="trade-cell">${formatNum(trade.profitExcludingPriceGrowthUsd)} $ (${formatNum(trade.profitExcludingPriceGrowthPercent)}%)</div>
                    <div class="trade-cell">${formatNum(trade.bybitEntryPrice)} → ${formatNum(trade.bybitExitPrice)}</div>
                    <div class="trade-cell">${formatNum(trade.kucoinEntryPrice)} → ${formatNum(trade.kucoinExitPrice)}</div>
                `;
                container.appendChild(row);
            });
        } catch (e) {
            console.error('Error loading completed trades:', e.message);
        }
    };

    renderStatus();

    setInterval(() => {
        if (isRunning) {
            loadAllProfiles();
            loadHistory();
            loadCompletedTrades();
        }
    }, 2000);
});