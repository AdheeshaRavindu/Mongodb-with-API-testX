/* ===================================================
   ConvertHub — Application Logic
   =================================================== */

// API Endpoints
const CURRENCY_API = 'https://currency-converter.vikumkodikara123.workers.dev/api/currency';
const TEMP_API = 'https://temperature-converter.vikumkodikara123.workers.dev/api/temperatures';

// ==========================================
//  TAB SWITCHING
// ==========================================
function switchTab(tab) {
    const currencySection = document.getElementById('section-currency');
    const tempSection = document.getElementById('section-temperature');
    const tabCurrency = document.getElementById('tab-currency');
    const tabTemp = document.getElementById('tab-temperature');

    if (tab === 'currency') {
        currencySection.classList.remove('hidden');
        tempSection.classList.add('hidden');
        tabCurrency.classList.add('active');
        tabCurrency.classList.remove('temp-active');
        tabCurrency.setAttribute('aria-selected', 'true');
        tabTemp.classList.remove('active');
        tabTemp.classList.remove('temp-active');
        tabTemp.setAttribute('aria-selected', 'false');
        loadCurrencyHistory();
    } else {
        currencySection.classList.add('hidden');
        tempSection.classList.remove('hidden');
        tabTemp.classList.add('active', 'temp-active');
        tabCurrency.classList.remove('active');
        tabTemp.setAttribute('aria-selected', 'true');
        tabCurrency.setAttribute('aria-selected', 'false');
        loadTempHistory();
    }
}

// ==========================================
//  CURRENCY CONVERTER
// ==========================================
async function convertCurrency() {
    const input = document.getElementById('currency-input');
    const amount = parseFloat(input.value);
    const btn = document.getElementById('btn-convert-currency');

    if (!amount || amount <= 0) {
        showToast('Please enter a valid USD amount', 'error');
        input.focus();
        return;
    }

    btn.classList.add('loading');
    btn.innerHTML = `
        <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" class="spin"><circle cx="12" cy="12" r="10" stroke-dasharray="30 60"/></svg>
        Converting...
    `;

    try {
        const res = await fetch(`${CURRENCY_API}/convert?usdAmount=${amount}`, {
            method: 'POST'
        });

        if (!res.ok) {
            const errData = await res.json().catch(() => ({}));
            throw new Error(errData.message || `HTTP ${res.status}`);
        }

        const data = await res.json();

        // Show result
        const resultPanel = document.getElementById('currency-result');
        resultPanel.classList.remove('hidden');

        document.getElementById('currency-input-val').textContent = `$ ${formatNumber(data.inputAmount)}`;
        document.getElementById('currency-output-val').textContent = `Rs ${formatNumber(data.outputAmount)}`;
        document.getElementById('currency-rate-info').textContent = `Rate: 1 USD = ${data.exchangeRate} LKR`;
        document.getElementById('currency-time-info').textContent = formatTimestamp(data.timestamp);

        showToast('Conversion successful! ✨', 'success');
        loadCurrencyHistory();

    } catch (err) {
        console.error('Currency conversion error:', err);
        showToast(`Could not reach currency API: ${err.message}`, 'error');
    } finally {
        btn.classList.remove('loading');
        btn.innerHTML = `
            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><path d="M5 12h14M12 5l7 7-7 7"/></svg>
            Convert
        `;
    }
}

async function loadCurrencyHistory() {
    const tbody = document.getElementById('currency-history-body');

    try {
        const res = await fetch(`${CURRENCY_API}/history`);
        if (!res.ok) throw new Error(`HTTP ${res.status}`);
        const data = await res.json();

        if (!data.length) {
            tbody.innerHTML = '<tr class="empty-row"><td colspan="5">No conversion history yet</td></tr>';
            return;
        }

        // Show latest first
        const sorted = [...data].reverse();
        tbody.innerHTML = sorted.map((item, i) => `
            <tr style="animation: fadeInUp 0.3s ease-out ${i * 0.05}s both">
                <td style="color: var(--text-muted)">${sorted.length - i}</td>
                <td><strong>$ ${formatNumber(item.inputAmount)}</strong></td>
                <td style="color: var(--accent-indigo); font-weight: 600">Rs ${formatNumber(item.outputAmount)}</td>
                <td>${item.exchangeRate}</td>
                <td style="color: var(--text-secondary); font-size: 0.75rem">${formatTimestamp(item.timestamp)}</td>
            </tr>
        `).join('');

    } catch (err) {
        console.error('Load currency history error:', err);
        tbody.innerHTML = '<tr class="empty-row"><td colspan="5" class="history-error">Could not load history. Check your connection.</td></tr>';
    }
}

// ==========================================
//  TEMPERATURE CONVERTER
// ==========================================
async function convertTemperature() {
    const input = document.getElementById('temp-input');
    const value = parseFloat(input.value);
    const unit = document.getElementById('temp-unit').value;
    const btn = document.getElementById('btn-convert-temp');

    if (isNaN(value)) {
        showToast('Please enter a valid temperature value', 'error');
        input.focus();
        return;
    }

    btn.classList.add('loading');
    btn.innerHTML = `
        <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" class="spin"><circle cx="12" cy="12" r="10" stroke-dasharray="30 60"/></svg>
        Converting...
    `;

    try {
        const res = await fetch(`${TEMP_API}/convert?value=${value}&unit=${unit}`, {
            method: 'POST'
        });

        if (!res.ok) {
            const errText = await res.text();
            throw new Error(errText || `HTTP ${res.status}`);
        }

        const data = await res.json();

        // Show result
        const resultPanel = document.getElementById('temp-result');
        resultPanel.classList.remove('hidden');

        const unitSymbols = { Celsius: '°C', Fahrenheit: '°F', Kelvin: 'K' };
        const inSymbol = unitSymbols[data.inputUnit] || '';
        const outSymbol = unitSymbols[data.outputUnit] || '';

        document.getElementById('temp-input-val').textContent = `${formatNumber(data.inputTemperature)} ${inSymbol}`;
        document.getElementById('temp-output-val').textContent = `${formatNumber(data.outputTemperature)} ${outSymbol}`;
        document.getElementById('temp-unit-info').textContent = `${data.inputUnit} → ${data.outputUnit}`;
        document.getElementById('temp-time-info').textContent = formatTimestamp(data.timestamp);

        showToast('Conversion successful! 🔥', 'success');
        loadTempHistory();

    } catch (err) {
        console.error('Temperature conversion error:', err);
        showToast(`Could not reach temperature API: ${err.message}`, 'error');
    } finally {
        btn.classList.remove('loading');
        btn.innerHTML = `
            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><path d="M5 12h14M12 5l7 7-7 7"/></svg>
            Convert
        `;
    }
}

async function loadTempHistory() {
    const tbody = document.getElementById('temp-history-body');

    try {
        const res = await fetch(`${TEMP_API}/history`);
        if (!res.ok) throw new Error(`HTTP ${res.status}`);
        const data = await res.json();

        if (!data.length) {
            tbody.innerHTML = '<tr class="empty-row"><td colspan="5">No conversion history yet</td></tr>';
            return;
        }

        const unitSymbols = { Celsius: '°C', Fahrenheit: '°F', Kelvin: 'K' };
        const sorted = [...data].reverse();

        tbody.innerHTML = sorted.map((item, i) => `
            <tr style="animation: fadeInUp 0.3s ease-out ${i * 0.05}s both">
                <td style="color: var(--text-muted)">${sorted.length - i}</td>
                <td><strong>${formatNumber(item.inputTemperature)} ${unitSymbols[item.inputUnit] || ''}</strong></td>
                <td style="color: var(--accent-rose); font-weight: 600">${formatNumber(item.outputTemperature)} ${unitSymbols[item.outputUnit] || ''}</td>
                <td>${item.inputUnit} → ${item.outputUnit}</td>
                <td style="color: var(--text-secondary); font-size: 0.75rem">${formatTimestamp(item.timestamp)}</td>
            </tr>
        `).join('');

    } catch (err) {
        console.error('Load temp history error:', err);
        tbody.innerHTML = '<tr class="empty-row"><td colspan="5" class="history-error">Could not load history. Check your connection.</td></tr>';
    }
}

// ==========================================
//  UTILITIES
// ==========================================
function formatNumber(num) {
    if (num === undefined || num === null) return '—';
    return Number(num).toLocaleString('en-US', {
        minimumFractionDigits: 2,
        maximumFractionDigits: 2
    });
}

function formatTimestamp(ts) {
    if (!ts) return '';
    try {
        const date = new Date(ts);
        if (isNaN(date.getTime())) return ts;
        return date.toLocaleString('en-US', {
            month: 'short',
            day: 'numeric',
            hour: '2-digit',
            minute: '2-digit',
            second: '2-digit'
        });
    } catch {
        return ts;
    }
}

function showToast(message, type = 'success') {
    const toast = document.getElementById('toast');
    const msg = document.getElementById('toast-msg');

    toast.className = `toast ${type}`;
    msg.textContent = message;

    // Reset animation
    toast.style.animation = 'none';
    toast.offsetHeight; // trigger reflow
    toast.style.animation = '';

    setTimeout(() => {
        toast.style.animation = 'toastOut 0.4s ease-in forwards';
        setTimeout(() => {
            toast.classList.add('hidden');
        }, 400);
    }, 3000);
}

// ==========================================
//  KEYBOARD SHORTCUTS
// ==========================================
document.addEventListener('keydown', (e) => {
    if (e.key === 'Enter') {
        const currencySection = document.getElementById('section-currency');
        if (!currencySection.classList.contains('hidden')) {
            if (document.activeElement === document.getElementById('currency-input')) {
                convertCurrency();
            }
        } else {
            if (document.activeElement === document.getElementById('temp-input') ||
                document.activeElement === document.getElementById('temp-unit')) {
                convertTemperature();
            }
        }
    }
});

// ==========================================
//  SPINNING ANIMATION (for loading state)
// ==========================================
const spinStyle = document.createElement('style');
spinStyle.textContent = `
    @keyframes spin {
        from { transform: rotate(0deg); }
        to { transform: rotate(360deg); }
    }
    .spin {
        animation: spin 1s linear infinite;
    }
`;
document.head.appendChild(spinStyle);

// ==========================================
//  INIT
// ==========================================
document.addEventListener('DOMContentLoaded', () => {
    loadCurrencyHistory();
});
