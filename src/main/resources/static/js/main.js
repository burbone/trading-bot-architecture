document.addEventListener('DOMContentLoaded', () => {
    const statusButton = document.getElementById('statusDisplay');
    const statusContainer = document.getElementById('appContainer');
    const symbolInput = document.getElementById('symbol');
    const percentInput = document.getElementById('percent');
    const passwordInput = document.getElementById('password');
    const formErrors = document.getElementById('formErrors');

    if (!statusButton || !statusContainer) {
        return;
    }

    let isRunning = false;
    let isSubmitting = false;

    const renderStatus = () => {
        if (isSubmitting) {
            statusButton.disabled = true;
            statusButton.textContent = 'Загрузка...';
            statusButton.classList.remove('status-running');
            statusButton.classList.add('status-stopped');
            statusContainer.classList.remove('status-running');
            statusContainer.classList.add('status-stopped');
            return;
        }

        statusButton.disabled = false;

        if (isRunning) {
            statusButton.textContent = 'Запущено';
            statusButton.classList.remove('status-stopped');
            statusButton.classList.add('status-running');
            statusContainer.classList.remove('status-stopped');
            statusContainer.classList.add('status-running');
        } else {
            statusButton.textContent = 'Остановлено';
            statusButton.classList.remove('status-running');
            statusButton.classList.add('status-stopped');
            statusContainer.classList.remove('status-running');
            statusContainer.classList.add('status-stopped');
        }
    };

    const setSubmitting = (value) => {
        isSubmitting = value;
        renderStatus();
    };

    const exchangeValueElements = {};
    document.querySelectorAll('[data-exchange][data-field]').forEach(element => {
        const { exchange, field } = element.dataset;
        if (!exchange || !field) {
            return;
        }
        if (!exchangeValueElements[exchange]) {
            exchangeValueElements[exchange] = {};
        }
        exchangeValueElements[exchange][field] = element;
    });

    const sanitizeSymbol = () => {
        if (!symbolInput) return '';
        const raw = symbolInput.value.toUpperCase();
        const hadTrailingSpace = /\s$/.test(raw);

        let normalizedSymbol = raw
            .replace(/[^A-Z\s]/g, '')
            .replace(/\s{2,}/g, ' ')
            .replace(/^\s+/g, '');

        const parts = normalizedSymbol.trim().split(' ').filter(Boolean);
        if (parts.length > 2) {
            normalizedSymbol = parts.slice(0, 2).join(' ');
        } else {
            normalizedSymbol = parts.join(' ');
        }

        if (hadTrailingSpace && normalizedSymbol.length && normalizedSymbol.split(' ').length < 2) {
            normalizedSymbol += ' ';
        } else if (parts.length >= 2) {
            normalizedSymbol = parts.slice(0, 2).join(' ');
        }

        if (symbolInput.value !== normalizedSymbol) {
            symbolInput.value = normalizedSymbol;
        }
        return normalizedSymbol.trim();
    };

    const sanitizePercent = () => {
        if (!percentInput) return NaN;
        const raw = percentInput.value.replace(',', '.');
        let cleaned = '';
        let hasDot = false;

        for (const char of raw) {
            if (/\d/.test(char)) {
                cleaned += char;
            } else if (char === '.' && !hasDot) {
                cleaned += '.';
                hasDot = true;
            }
        }

        const parts = cleaned.split('.');
        if (parts[1] && parts[1].length > 2) {
            cleaned = `${parts[0]}.${parts[1].slice(0, 2)}`;
        }

        const displayValue = cleaned.replace('.', ',');
        if (percentInput.value !== displayValue) {
            percentInput.value = displayValue;
        }

        return parseFloat(cleaned);
    };

    const setFormErrors = (html = '') => {
        if (formErrors) {
            formErrors.innerHTML = html;
        }
    };

    const validateForm = (showErrors = false) => {
        const errors = [];

        if (symbolInput) {
            const normalizedSymbol = sanitizeSymbol();
            const parts = normalizedSymbol.split(' ').filter(Boolean);
            if (parts.length !== 2 || !parts.every(part => /^[A-Z]+$/.test(part))) {
                errors.push('Поле "Символ" должно содержать два слова из латинских букв в верхнем регистре.');
            }
        }

        if (percentInput) {
            const numericValue = sanitizePercent();

            if (Number.isNaN(numericValue)) {
                errors.push('Поле "Процент" должно содержать число от 0,01 до 100.');
            } else if (numericValue < 0.01 || numericValue > 100) {
                errors.push('Поле "Процент" должно быть в диапазоне 0,01–100.');
            }
        }

        setFormErrors(showErrors ? errors.map(err => `<div>${err}</div>`).join('') : '');

        return errors.length === 0;
    };

    const START_ERROR_MESSAGE = 'Не удалось запустить процесс.';
    const STOP_ERROR_MESSAGE = 'Не удалось остановить процесс.';

    const parseErrorMessage = async (response, fallback) => {
        try {
            const data = await response.json();
            if (typeof data === 'string') {
                return data;
            }
            if (data && typeof data.message === 'string') {
                return data.message;
            }
        } catch (e) {
            // ignore parsing errors
        }
        return fallback;
    };

    const sendStarterRequest = async (symbol, percent) => {
        try {
            const password = passwordInput ? passwordInput.value : '';
            const response = await fetch('/api/starter', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify({
                    symbol: symbol,
                    percent: percent.toString(),
                    password: password
                })
            });

            if (!response.ok) {
                const message = await parseErrorMessage(response, START_ERROR_MESSAGE);
                throw new Error(message);
            }
        } catch (error) {
            if (error instanceof TypeError) {
                throw new Error('Сервер недоступен. Попробуйте позже.');
            }
            throw error;
        }
    };

    const sendStopRequest = async () => {
        try {
            const password = passwordInput ? passwordInput.value : '';
            const response = await fetch('/api/stop', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify({
                    password: password
                })
            });

            if (!response.ok) {
                const message = await parseErrorMessage(response, STOP_ERROR_MESSAGE);
                throw new Error(message);
            }
        } catch (error) {
            if (error instanceof TypeError) {
                throw new Error('Сервер недоступен. Попробуйте позже.');
            }
            throw error;
        }
    };

    const formatDisplayNumber = (value) => {
        if (value === null || value === undefined || value === '') {
            return '0';
        }

        const numericValue = typeof value === 'number'
            ? value
            : Number(String(value).replace(',', '.'));

        if (Number.isNaN(numericValue)) {
            return '0';
        }

        return numericValue
            .toLocaleString('ru-RU', { maximumFractionDigits: 8 })
            .replace(/\u00A0/g, ' ');
    };

    const updateExchangeValues = (exchange, snapshot = {}) => {
        const targets = exchangeValueElements[exchange];
        if (!targets) {
            return;
        }

        if (targets.usdt) {
            targets.usdt.textContent = formatDisplayNumber(snapshot.usdt);
        }
        if (targets.crypto) {
            targets.crypto.textContent = formatDisplayNumber(snapshot.crypto);
        }
        if (targets.coins) {
            targets.coins.textContent = formatDisplayNumber(snapshot.coins);
        }
    };

    const fetchJson = async (url, defaultError) => {
        const response = await fetch(url);
        if (!response.ok) {
            const message = await parseErrorMessage(response, defaultError);
            throw new Error(message);
        }
        return response.json();
    };

    const loadExchangeSnapshot = async (exchange) => {
        try {
            const data = await fetchJson(`/profile/${exchange}`, `Не удалось загрузить данные ${exchange}.`);
            updateExchangeValues(exchange, data);
        } catch (error) {
            console.error(error.message);
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
            const historyList = await fetchJson('/profile/history', 'Не удалось загрузить историю.');
            const historyContainer = document.querySelector('.history-list');

            if (!historyContainer || !Array.isArray(historyList)) {
                return;
            }

            historyContainer.innerHTML = '';

            historyList.forEach(entry => {
                const marker = getHistoryMarker(entry.type);

                const row = document.createElement('div');
                row.className = 'history-row';

                row.innerHTML = `
                    <div class="history-marker ${marker.class}" title="${marker.title}">${marker.text}</div>
                    <div class="history-value">${formatDisplayNumber(entry.bybitPrice)}</div>
                    <div class="history-value">${formatDisplayNumber(entry.kucoinPrice)}</div>
                `;

                historyContainer.appendChild(row);
            });
        } catch (error) {
            console.error(error.message);
        }
    };

    const initData = () => {
        loadExchangeSnapshot('bybit');
        loadExchangeSnapshot('kucoin');
        loadHistory();
    };

    statusButton.addEventListener('click', async () => {
        if (isSubmitting) {
            return;
        }

        if (!isRunning) {
            const isValid = validateForm(true);
            if (!isValid) {
                return;
            }

            const symbol = sanitizeSymbol();
            const percent = sanitizePercent();

            setSubmitting(true);
            try {
                await sendStarterRequest(symbol, percent);
                setFormErrors('');
                isRunning = true;
            } catch (error) {
                setFormErrors(`<div>${error.message || START_ERROR_MESSAGE}</div>`);
            } finally {
                setSubmitting(false);
            }

            return;
        }

        // Остановка алгоритма
        setSubmitting(true);
        try {
            await sendStopRequest();
            setFormErrors('');
            isRunning = false;
        } catch (error) {
            setFormErrors(`<div>${error.message || STOP_ERROR_MESSAGE}</div>`);
        } finally {
            setSubmitting(false);
        }
    });

    renderStatus();

    if (symbolInput) {
        symbolInput.addEventListener('input', () => sanitizeSymbol());
    }

    if (percentInput) {
        percentInput.addEventListener('input', () => sanitizePercent());
    }

    initData();

    // Обновление данных каждые 2 секунды
    setInterval(() => {
        if (isRunning) {
            loadExchangeSnapshot('bybit');
            loadExchangeSnapshot('kucoin');
            loadHistory();
        }
    }, 2000);
});