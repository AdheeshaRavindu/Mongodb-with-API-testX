/* ===================================================
   ConvertHub — Application Logic
   =================================================== */

// API Endpoints — same-origin via nginx proxy (Docker) or direct gateway (local dev)
const isLocalRuntime = ['localhost', '127.0.0.1'].includes(window.location.hostname);
const useSameOriginProxy = window.location.port === '3000' || window.location.port === '80';
const API_BASE = useSameOriginProxy
    ? ''
    : isLocalRuntime
        ? 'http://localhost:8080'
        : 'https://temperature-converter.vikumkodikara123.workers.dev';
const AUTH_API = `${API_BASE}/auth/google`;
const AUTH_ME_API = `${API_BASE}/auth/me`;
const CURRENCY_API = useSameOriginProxy || isLocalRuntime
    ? `${API_BASE}/api/currency`
    : 'https://currency-converter.vikumkodikara123.workers.dev/api/currency';
const TEMP_API = useSameOriginProxy || isLocalRuntime
    ? `${API_BASE}/api/temperatures`
    : 'https://temperature-converter.vikumkodikara123.workers.dev/api/temperatures';

const APP_TOKEN_KEY = 'converthub_app_jwt';
const USER_KEY = 'converthub_user';
const HISTORY_PAGE_SIZE = 8;

let currencyDirection = 'usd-lkr';
let currencyHistoryState = { scope: 'all', page: 0 };
let tempHistoryState = { scope: 'all', page: 0 };

function getAppToken() {
    return localStorage.getItem(APP_TOKEN_KEY) || '';
}

function getStoredUser() {
    const raw = localStorage.getItem(USER_KEY);
    if (!raw) return null;
    try {
        return JSON.parse(raw);
    } catch {
        return null;
    }
}

function saveAuthSession(token, user) {
    localStorage.setItem(APP_TOKEN_KEY, token);
    localStorage.setItem(USER_KEY, JSON.stringify(user));
    updateAuthBarUI();
}

function clearAuthSession() {
    localStorage.removeItem(APP_TOKEN_KEY);
    localStorage.removeItem(USER_KEY);
    localStorage.removeItem('converthub_google_id_token');
    updateAuthBarUI();
}

function logout() {
    clearAuthSession();
    showToast('Signed out', 'success');
}

async function copyAppToken() {
    const token = getAppToken();
    if (!token) return;

    try {
        await navigator.clipboard.writeText(token);
        showToast('Token copied for Postman', 'success');
    } catch {
        showToast('Could not copy token', 'error');
    }
}

function promptSignInOnHome() {
    updateAuthBarUI();
    const authBar = document.getElementById('auth-bar');
    if (authBar) {
        authBar.scrollIntoView({ behavior: 'smooth', block: 'nearest' });
    }
}

function handleUnauthorized() {
    clearAuthSession();
    showToast('Session expired — please sign in again', 'error');
    if (window.location.pathname === '/' || window.location.pathname.endsWith('/index.html')) {
        promptSignInOnHome();
    } else {
        window.location.href = '/?signin=required';
    }
}

function updateAuthBarUI() {
    const token = getAppToken();
    const user = getStoredUser();
    const status = document.getElementById('auth-token-status');
    const signInContainer = document.getElementById('auth-signin-container');
    const userBox = document.getElementById('auth-user');
    const userName = document.getElementById('auth-user-name');
    const userEmail = document.getElementById('auth-user-email');
    const userPicture = document.getElementById('auth-user-picture');
    const logoutBtn = document.getElementById('btn-logout');
    const copyTokenBtn = document.getElementById('btn-copy-token');

    const isLoggedIn = Boolean(token);

    if (status) {
        status.textContent = isLoggedIn
            ? `Signed in as ${user?.name || user?.email || 'user'}`
            : 'Not signed in — API calls will return 401';
    }

    if (signInContainer) signInContainer.hidden = isLoggedIn;
    if (logoutBtn) logoutBtn.hidden = !isLoggedIn;
    if (copyTokenBtn) copyTokenBtn.hidden = !isLoggedIn;
    if (userBox) userBox.hidden = !isLoggedIn;

    if (userEmail) {
        if (isLoggedIn && user?.email) {
            userEmail.textContent = user.email;
            userEmail.hidden = false;
        } else {
            userEmail.hidden = true;
        }
    }

    if (isLoggedIn && user) {
        if (userName) userName.textContent = user.name || user.email || 'Signed in';
        if (userPicture) {
            if (user.picture) {
                userPicture.src = user.picture;
                userPicture.hidden = false;
            } else {
                userPicture.hidden = true;
            }
        }
    }
}

function authHeaders() {
    const token = getAppToken();
    if (!token) {
        return {};
    }
    return { Authorization: `Bearer ${token}` };
}

function requireAuthOrToast() {
    if (!getAppToken()) {
        showToast('Sign in with Google first', 'error');
        return false;
    }
    return true;
}

async function refreshCurrentUser() {
    if (!getAppToken()) return;

    try {
        const res = await authFetch(AUTH_ME_API);
        if (!res.ok) return;
        const user = await res.json();
        localStorage.setItem(USER_KEY, JSON.stringify(user));
        updateAuthBarUI();
    } catch (err) {
        if (err.message !== 'Unauthorized') {
            console.warn('Could not refresh user profile:', err);
        }
    }
}

async function handleCredential(response) {
    try {
        const data = await exchangeGoogleToken(response.credential);
        localStorage.removeItem('converthub_google_id_token');
        saveAuthSession(data.token, data.user);
        await refreshCurrentUser();
        const displayName = data.user?.name || data.user?.email || 'user';
        showToast(`Signed in as ${displayName}`, 'success');
        loadCurrencyRateAndStats();
        loadCurrencyHistory();
    } catch (err) {
        console.error('Auth exchange failed:', err);
        showToast(`Sign-in failed: ${err.message}`, 'error');
    }
}

window.handleCredential = handleCredential;

async function exchangeGoogleToken(idToken) {
    let res;
    try {
        res = await fetch(AUTH_API, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ idToken })
        });
    } catch {
        throw new Error(
            `Cannot reach auth API at ${AUTH_API || window.location.origin + '/auth/google'}. ` +
            'Ensure Docker is running (docker compose up -d).'
        );
    }

    if (!res.ok) {
        const errText = await res.text();
        throw new Error(errText || `Authentication failed (HTTP ${res.status})`);
    }

    return res.json();
}

async function authFetch(url, options = {}) {
    const res = await fetch(url, {
        ...options,
        headers: {
            ...(options.headers || {}),
            ...authHeaders()
        }
    });

    if (res.status === 401) {
        handleUnauthorized();
        throw new Error('Unauthorized');
    }

    return res;
}

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
        loadCurrencyRateAndStats();
        loadCurrencyHistory();
    } else {
        currencySection.classList.add('hidden');
        tempSection.classList.remove('hidden');
        tabTemp.classList.add('active', 'temp-active');
        tabCurrency.classList.remove('active');
        tabTemp.setAttribute('aria-selected', 'true');
        tabCurrency.setAttribute('aria-selected', 'false');
        loadTemperatureStats();
        loadTemperatureUnits();
        loadTempHistory();
    }
}

// ==========================================
//  CURRENCY
// ==========================================
function setCurrencyDirection(direction) {
    currencyDirection = direction;
    const usdBtn = document.getElementById('currency-dir-usd-lkr');
    const lkrBtn = document.getElementById('currency-dir-lkr-usd');
    const label = document.getElementById('currency-input-label');
    const prefix = document.getElementById('currency-input-prefix');
    const input = document.getElementById('currency-input');

    if (usdBtn) usdBtn.classList.toggle('active', direction === 'usd-lkr');
    if (lkrBtn) lkrBtn.classList.toggle('active', direction === 'lkr-usd');

    if (direction === 'lkr-usd') {
        if (label) label.textContent = 'Enter LKR Amount';
        if (prefix) prefix.textContent = 'Rs';
        if (input) input.placeholder = '30000.00';
    } else {
        if (label) label.textContent = 'Enter USD Amount';
        if (prefix) prefix.textContent = '$';
        if (input) input.placeholder = '100.00';
    }
}

function resetCurrencyHistoryPage() {
    currencyHistoryState.page = 0;
}

function changeCurrencyPage(delta) {
    currencyHistoryState.page = Math.max(0, currencyHistoryState.page + delta);
    loadCurrencyHistory();
}

async function loadCurrencyRateAndStats() {
    if (!getAppToken()) return;

    try {
        const [rateRes, statsRes] = await Promise.all([
            authFetch(`${CURRENCY_API}/rate`),
            authFetch(`${CURRENCY_API}/stats`)
        ]);

        if (rateRes.ok) {
            const rate = await rateRes.json();
            const el = document.getElementById('currency-live-rate');
            if (el) el.textContent = `1 ${rate.fromCurrency} = ${rate.rate} ${rate.toCurrency}`;
        }

        if (statsRes.ok) {
            const stats = await statsRes.json();
            const countEl = document.getElementById('currency-stat-count');
            const usdEl = document.getElementById('currency-stat-usd');
            const lkrEl = document.getElementById('currency-stat-lkr');
            if (countEl) countEl.textContent = stats.totalConversions ?? '—';
            if (usdEl) usdEl.textContent = `$ ${formatNumber(stats.totalUsdConverted)}`;
            if (lkrEl) lkrEl.textContent = `Rs ${formatNumber(stats.totalLkrOutput)}`;
        }
    } catch (err) {
        if (err.message !== 'Unauthorized') {
            console.warn('Currency stats error:', err);
        }
    }
}

async function convertCurrency() {
    const input = document.getElementById('currency-input');
    const amount = parseFloat(input.value);
    const btn = document.getElementById('btn-convert-currency');

    if (!amount || amount <= 0) {
        showToast('Please enter a valid amount', 'error');
        input.focus();
        return;
    }

    if (!requireAuthOrToast()) return;

    const isReverse = currencyDirection === 'lkr-usd';
    const url = isReverse
        ? `${CURRENCY_API}/convert/reverse?lkrAmount=${amount}`
        : `${CURRENCY_API}/convert?usdAmount=${amount}`;

    btn.classList.add('loading');
    btn.innerHTML = `
        <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" class="spin"><circle cx="12" cy="12" r="10" stroke-dasharray="30 60"/></svg>
        Converting...
    `;

    try {
        const res = await authFetch(url, { method: 'POST' });

        if (!res.ok) {
            const errText = await res.text();
            throw new Error(errText || `HTTP ${res.status}`);
        }

        const data = await res.json();
        const resultPanel = document.getElementById('currency-result');
        resultPanel.classList.remove('hidden');

        const inputCurrency = data.inputCurrency || (isReverse ? 'LKR' : 'USD');
        const outputCurrency = data.outputCurrency || (isReverse ? 'USD' : 'LKR');
        const inputPrefix = inputCurrency === 'USD' ? '$' : 'Rs';
        const outputPrefix = outputCurrency === 'USD' ? '$' : 'Rs';

        document.getElementById('currency-input-val').textContent =
            `${inputPrefix} ${formatNumber(data.inputAmount)}`;
        document.getElementById('currency-output-val').textContent =
            `${outputPrefix} ${formatNumber(data.outputAmount)}`;
        document.getElementById('currency-rate-info').textContent =
            `Rate: 1 USD = ${data.exchangeRate} LKR`;
        document.getElementById('currency-time-info').textContent = formatTimestamp(data.timestamp);

        showToast('Conversion successful! ✨', 'success');
        loadCurrencyRateAndStats();
        loadCurrencyHistory();
    } catch (err) {
        if (err.message !== 'Unauthorized') {
            console.error('Currency conversion error:', err);
            showToast(`Could not reach currency API: ${err.message}`, 'error');
        }
    } finally {
        btn.classList.remove('loading');
        btn.innerHTML = `
            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><path d="M5 12h14M12 5l7 7-7 7"/></svg>
            Convert
        `;
    }
}

function buildCurrencyHistoryUrl() {
    const { scope, page } = currencyHistoryState;
    if (scope === 'mine') {
        return `${CURRENCY_API}/history/mine?page=${page}&size=${HISTORY_PAGE_SIZE}`;
    }
    return `${CURRENCY_API}/history?page=${page}&size=${HISTORY_PAGE_SIZE}`;
}

function updateHistoryPagination(prefix, state, meta, hasUnitFilter) {
    const pagination = document.getElementById(`${prefix}-pagination`);
    const pageInfo = document.getElementById(`${prefix}-page-info`);
    const prevBtn = document.getElementById(`${prefix}-prev`);
    const nextBtn = document.getElementById(`${prefix}-next`);

    if (hasUnitFilter || !meta) {
        if (pagination) pagination.hidden = true;
        return;
    }

    if (pagination) pagination.hidden = false;
    const totalPages = Math.max(meta.totalPages || 1, 1);
    const currentPage = (meta.page ?? state.page) + 1;

    if (pageInfo) pageInfo.textContent = `Page ${currentPage} of ${totalPages}`;
    if (prevBtn) prevBtn.disabled = state.page <= 0;
    if (nextBtn) nextBtn.disabled = currentPage >= totalPages;
}

function renderCurrencyHistoryRows(rows, meta) {
    const tbody = document.getElementById('currency-history-body');

    if (!rows.length) {
        tbody.innerHTML = '<tr class="empty-row"><td colspan="6">No conversion history yet</td></tr>';
        return;
    }

    const startIndex = meta ? meta.page * meta.size : 0;
    tbody.innerHTML = rows.map((item, i) => {
        const inputCur = item.inputCurrency || 'USD';
        const outputCur = item.outputCurrency || 'LKR';
        const inPrefix = inputCur === 'USD' ? '$' : 'Rs';
        const outPrefix = outputCur === 'USD' ? '$' : 'Rs';
        return `
        <tr style="animation: fadeInUp 0.3s ease-out ${i * 0.05}s both">
            <td style="color: var(--text-muted)">${startIndex + i + 1}</td>
            <td><strong>${inPrefix} ${formatNumber(item.inputAmount)}</strong></td>
            <td style="color: var(--accent-indigo); font-weight: 600">${outPrefix} ${formatNumber(item.outputAmount)}</td>
            <td>${item.exchangeRate}</td>
            <td style="color: var(--text-secondary); font-size: 0.75rem">${formatTimestamp(item.timestamp)}</td>
            <td>
                <button type="button" class="delete-btn" data-delete-currency="${item.id}" title="Delete record">×</button>
            </td>
        </tr>`;
    }).join('');
}

async function deleteCurrencyHistory(id) {
    if (!requireAuthOrToast() || !id) return;
    if (!confirm('Delete this conversion record?')) return;

    try {
        const res = await authFetch(`${CURRENCY_API}/history/${id}`, { method: 'DELETE' });
        if (!res.ok) throw new Error(`HTTP ${res.status}`);
        showToast('Record deleted', 'success');
        loadCurrencyRateAndStats();
        loadCurrencyHistory();
    } catch (err) {
        if (err.message !== 'Unauthorized') {
            showToast(`Delete failed: ${err.message}`, 'error');
        }
    }
}

async function loadCurrencyHistory() {
    const tbody = document.getElementById('currency-history-body');
    if (!getAppToken()) {
        tbody.innerHTML = '<tr class="empty-row"><td colspan="6">Sign in to view history</td></tr>';
        return;
    }

    try {
        const scope = currencyHistoryState.scope;
        const countRes = await authFetch(`${CURRENCY_API}/history/count?mine=${scope === 'mine'}`);
        if (countRes.ok) {
            const count = await countRes.json();
            const countEl = document.getElementById('currency-history-count');
            if (countEl) countEl.textContent = `${count} record${count === 1 ? '' : 's'}`;
        }

        const res = await authFetch(buildCurrencyHistoryUrl());
        if (!res.ok) throw new Error(`HTTP ${res.status}`);
        const data = await res.json();

        const rows = Array.isArray(data) ? data : (data.content || []);
        const meta = Array.isArray(data) ? null : data;

        renderCurrencyHistoryRows(rows, meta);
        updateHistoryPagination('currency', currencyHistoryState, meta, false);
    } catch (err) {
        if (err.message === 'Unauthorized') return;
        console.error('Load currency history error:', err);
        tbody.innerHTML = '<tr class="empty-row"><td colspan="6" class="history-error">Could not load history. Sign in and check your connection.</td></tr>';
    }
}

// ==========================================
//  TEMPERATURE
// ==========================================
function resetTempHistoryPage() {
    tempHistoryState.page = 0;
}

function changeTempPage(delta) {
    tempHistoryState.page = Math.max(0, tempHistoryState.page + delta);
    loadTempHistory();
}

async function loadTemperatureUnits() {
    if (!getAppToken()) return;

    try {
        const res = await authFetch(`${TEMP_API}/units`);
        if (!res.ok) return;
        const units = await res.json();
        const select = document.getElementById('temp-unit');
        const filterSelect = document.getElementById('temp-history-filter');
        if (!select || !units.length) return;

        const unitToValue = (name) => name.toLowerCase();
        const unitSymbols = { Celsius: '°C', Fahrenheit: '°F', Kelvin: 'K' };

        select.innerHTML = units.map((u) =>
            `<option value="${unitToValue(u)}">${unitSymbols[u] || ''} ${u}</option>`
        ).join('');

        if (filterSelect) {
            const current = filterSelect.value;
            filterSelect.innerHTML =
                '<option value="all">All</option>' +
                units.map((u) => `<option value="${u}">${u}</option>`).join('');
            if ([...filterSelect.options].some((o) => o.value === current)) {
                filterSelect.value = current;
            }
        }
    } catch (err) {
        if (err.message !== 'Unauthorized') {
            console.warn('Could not load temperature units:', err);
        }
    }
}

async function loadTemperatureStats() {
    if (!getAppToken()) return;

    try {
        const res = await authFetch(`${TEMP_API}/stats`);
        if (!res.ok) return;
        const stats = await res.json();
        const byUnit = stats.byUnit || {};

        const countEl = document.getElementById('temp-stat-count');
        const cEl = document.getElementById('temp-stat-celsius');
        const fEl = document.getElementById('temp-stat-fahrenheit');
        const kEl = document.getElementById('temp-stat-kelvin');

        if (countEl) countEl.textContent = stats.totalConversions ?? '—';
        if (cEl) cEl.textContent = byUnit.Celsius ?? 0;
        if (fEl) fEl.textContent = byUnit.Fahrenheit ?? 0;
        if (kEl) kEl.textContent = byUnit.Kelvin ?? 0;
    } catch (err) {
        if (err.message !== 'Unauthorized') {
            console.warn('Temperature stats error:', err);
        }
    }
}

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

    if (!requireAuthOrToast()) return;

    btn.classList.add('loading');
    btn.innerHTML = `
        <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" class="spin"><circle cx="12" cy="12" r="10" stroke-dasharray="30 60"/></svg>
        Converting...
    `;

    try {
        const res = await authFetch(`${TEMP_API}/convert?value=${value}&unit=${unit}`, {
            method: 'POST'
        });

        if (!res.ok) {
            const errText = await res.text();
            throw new Error(errText || `HTTP ${res.status}`);
        }

        const data = await res.json();
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
        loadTemperatureStats();
        loadTempHistory();
        fetchAndShowSafetyWarning(value, unit);
    } catch (err) {
        if (err.message !== 'Unauthorized') {
            console.error('Temperature conversion error:', err);
            showToast(`Could not reach temperature API: ${err.message}`, 'error');
        }
    } finally {
        btn.classList.remove('loading');
        btn.innerHTML = `
            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><path d="M5 12h14M12 5l7 7-7 7"/></svg>
            Convert
        `;
    }
}

async function fetchAndShowSafetyWarning(value, unit) {
    try {
        const res = await authFetch(
            `${TEMP_API}/safety-check?value=${encodeURIComponent(value)}&unit=${encodeURIComponent(unit)}`
        );
        if (!res.ok) {
            const errText = await res.text().catch(() => '');
            showSafetyAlert(
                errText || 'Safety check unavailable.',
                'warning'
            );
            return;
        }
        const message = (await res.text()).trim();
        if (message) {
            showSafetyAlert(message);
        }
    } catch (err) {
        if (err.message === 'Unauthorized') return;
        console.warn('Safety check error:', err);
        showSafetyAlert('Could not load safety warning.', 'warning');
    }
}

function showSafetyAlert(message, forceType) {
    const alert = document.getElementById('temp-safety-alert');
    const msg = document.getElementById('temp-safety-msg');
    if (!alert || !msg) return;

    const isSafe = forceType === 'safe'
        || (!forceType && message.toLowerCase().includes('comfortable and safe'));
    alert.className = `safety-alert ${isSafe ? 'safety-alert-safe' : 'safety-alert-warning'}`;
    msg.textContent = message;
    alert.classList.remove('hidden');
}

function buildTempHistoryUrl() {
    const filterEl = document.getElementById('temp-history-filter');
    const filter = filterEl ? filterEl.value : 'all';
    const { scope, page } = tempHistoryState;

    if (filter !== 'all') {
        return `${TEMP_API}/history/filter?unit=${encodeURIComponent(filter)}`;
    }
    if (scope === 'mine') {
        return `${TEMP_API}/history/mine?page=${page}&size=${HISTORY_PAGE_SIZE}`;
    }
    return `${TEMP_API}/history?page=${page}&size=${HISTORY_PAGE_SIZE}`;
}

function renderTempHistoryRows(rows, meta) {
    const tbody = document.getElementById('temp-history-body');
    const unitSymbols = { Celsius: '°C', Fahrenheit: '°F', Kelvin: 'K' };

    if (!rows.length) {
        tbody.innerHTML = '<tr class="empty-row"><td colspan="6">No conversion history yet</td></tr>';
        return;
    }

    const startIndex = meta ? meta.page * meta.size : 0;
    tbody.innerHTML = rows.map((item, i) => `
        <tr style="animation: fadeInUp 0.3s ease-out ${i * 0.05}s both">
            <td style="color: var(--text-muted)">${startIndex + i + 1}</td>
            <td><strong>${formatNumber(item.inputTemperature)} ${unitSymbols[item.inputUnit] || ''}</strong></td>
            <td style="color: var(--accent-rose); font-weight: 600">${formatNumber(item.outputTemperature)} ${unitSymbols[item.outputUnit] || ''}</td>
            <td>${item.inputUnit} → ${item.outputUnit}</td>
            <td style="color: var(--text-secondary); font-size: 0.75rem">${formatTimestamp(item.timestamp)}</td>
            <td>
                <button type="button" class="delete-btn" data-delete-temp="${item.id}" title="Delete record">×</button>
            </td>
        </tr>
    `).join('');
}

async function deleteTempHistory(id) {
    if (!requireAuthOrToast() || !id) return;
    if (!confirm('Delete this conversion record?')) return;

    try {
        const res = await authFetch(`${TEMP_API}/history/${id}`, { method: 'DELETE' });
        if (!res.ok) throw new Error(`HTTP ${res.status}`);
        showToast('Record deleted', 'success');
        loadTemperatureStats();
        loadTempHistory();
    } catch (err) {
        if (err.message !== 'Unauthorized') {
            showToast(`Delete failed: ${err.message}`, 'error');
        }
    }
}

async function loadTempHistory() {
    const tbody = document.getElementById('temp-history-body');
    const filterEl = document.getElementById('temp-history-filter');
    const filter = filterEl ? filterEl.value : 'all';
    const hasUnitFilter = filter !== 'all';

    if (!getAppToken()) {
        tbody.innerHTML = '<tr class="empty-row"><td colspan="6">Sign in to view history</td></tr>';
        return;
    }

    try {
        const scope = tempHistoryState.scope;
        const countRes = await authFetch(`${TEMP_API}/history/count?mine=${scope === 'mine'}`);
        if (countRes.ok) {
            const count = await countRes.json();
            const countEl = document.getElementById('temp-history-count');
            if (countEl) countEl.textContent = `${count} record${count === 1 ? '' : 's'}`;
        }

        const res = await authFetch(buildTempHistoryUrl());
        if (!res.ok) throw new Error(`HTTP ${res.status}`);
        const data = await res.json();

        const rows = Array.isArray(data) ? data : (data.content || []);
        const meta = Array.isArray(data) ? null : data;

        renderTempHistoryRows(rows, meta);
        updateHistoryPagination('temp', tempHistoryState, meta, hasUnitFilter);
    } catch (err) {
        if (err.message === 'Unauthorized') return;
        console.error('Load temp history error:', err);
        tbody.innerHTML = '<tr class="empty-row"><td colspan="6" class="history-error">Could not load history. Sign in and check your connection.</td></tr>';
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

    toast.style.animation = 'none';
    toast.offsetHeight;
    toast.style.animation = '';

    setTimeout(() => {
        toast.style.animation = 'toastOut 0.4s ease-in forwards';
        setTimeout(() => {
            toast.classList.add('hidden');
        }, 400);
    }, 3000);
}

function setupHistoryDeleteHandlers() {
    document.getElementById('currency-history-body')?.addEventListener('click', (e) => {
        const btn = e.target.closest('[data-delete-currency]');
        if (btn) deleteCurrencyHistory(btn.dataset.deleteCurrency);
    });

    document.getElementById('temp-history-body')?.addEventListener('click', (e) => {
        const btn = e.target.closest('[data-delete-temp]');
        if (btn) deleteTempHistory(btn.dataset.deleteTemp);
    });
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
    setupHistoryDeleteHandlers();
    updateAuthBarUI();

    if (getAppToken()) {
        refreshCurrentUser();
        loadCurrencyRateAndStats();
    }
    loadCurrencyHistory();

    const params = new URLSearchParams(window.location.search);
    if (params.get('signin') === 'required') {
        showToast('Please sign in with Google to continue', 'error');
        promptSignInOnHome();
        params.delete('signin');
        const newUrl = params.toString() ? `/?${params}` : '/';
        window.history.replaceState({}, '', newUrl);
    }
});

// Expose for inline handlers
window.switchTab = switchTab;
window.setCurrencyDirection = setCurrencyDirection;
window.convertCurrency = convertCurrency;
window.convertTemperature = convertTemperature;
window.loadCurrencyHistory = loadCurrencyHistory;
window.loadTempHistory = loadTempHistory;
window.changeCurrencyPage = changeCurrencyPage;
window.changeTempPage = changeTempPage;
window.resetCurrencyHistoryPage = resetCurrencyHistoryPage;
window.resetTempHistoryPage = resetTempHistoryPage;
window.logout = logout;
window.copyAppToken = copyAppToken;
