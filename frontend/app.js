/* ===================================================
   ConvertHub — Application Logic
   =================================================== */

// API Endpoints
const isLocalRuntime = ['localhost', '127.0.0.1'].includes(window.location.hostname);
const AUTH_API = isLocalRuntime
    ? 'http://localhost:8081/auth/google'
    : 'https://temperature-converter.vikumkodikara123.workers.dev/auth/google';
const CURRENCY_API = isLocalRuntime
    ? 'http://localhost:8082/api/currency'
    : 'https://currency-converter.vikumkodikara123.workers.dev/api/currency';
const TEMP_API = isLocalRuntime
    ? 'http://localhost:8081/api/temperatures'
    : 'https://temperature-converter.vikumkodikara123.workers.dev/api/temperatures';

const APP_TOKEN_KEY = 'converthub_app_jwt';
const USER_KEY = 'converthub_user';

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

async function handleCredential(response) {
    try {
        const data = await exchangeGoogleToken(response.credential);
        localStorage.removeItem('converthub_google_id_token');
        saveAuthSession(data.token, data.user);
        const displayName = data.user?.name || data.user?.email || 'user';
        showToast(`Signed in as ${displayName}`, 'success');
        loadCurrencyHistory();
    } catch (err) {
        console.error('Auth exchange failed:', err);
        showToast(`Sign-in failed: ${err.message}`, 'error');
    }
}

window.handleCredential = handleCredential;

async function exchangeGoogleToken(idToken) {
    const res = await fetch(AUTH_API, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ idToken })
    });

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

    if (!requireAuthOrToast()) return;

    btn.classList.add('loading');
    btn.innerHTML = `
        <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" class="spin"><circle cx="12" cy="12" r="10" stroke-dasharray="30 60"/></svg>
        Converting...
    `;

    try {
        const res = await authFetch(`${CURRENCY_API}/convert?usdAmount=${amount}`, {
            method: 'POST'
        });

        if (!res.ok) {
            const errText = await res.text();
            throw new Error(errText || `HTTP ${res.status}`);
        }

        const data = await res.json();

        const resultPanel = document.getElementById('currency-result');
        resultPanel.classList.remove('hidden');

        document.getElementById('currency-input-val').textContent = `$ ${formatNumber(data.inputAmount)}`;
        document.getElementById('currency-output-val').textContent = `Rs ${formatNumber(data.outputAmount)}`;
        document.getElementById('currency-rate-info').textContent = `Rate: 1 USD = ${data.exchangeRate} LKR`;
        document.getElementById('currency-time-info').textContent = formatTimestamp(data.timestamp);

        showToast('Conversion successful! ✨', 'success');
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

async function loadCurrencyHistory() {
    const tbody = document.getElementById('currency-history-body');

    try {
        const res = await authFetch(`${CURRENCY_API}/history`);
        if (!res.ok) throw new Error(`HTTP ${res.status}`);
        const data = await res.json();

        if (!data.length) {
            tbody.innerHTML = '<tr class="empty-row"><td colspan="5">No conversion history yet</td></tr>';
            return;
        }

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
        if (err.message === 'Unauthorized') return;
        console.error('Load currency history error:', err);
        tbody.innerHTML = '<tr class="empty-row"><td colspan="5" class="history-error">Could not load history. Sign in and check your connection.</td></tr>';
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
                errText || 'Safety check unavailable. Ensure the temperature API is running on port 8081.',
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
        showSafetyAlert(
            'Could not load safety warning. Check that the temperature API is reachable.',
            'warning'
        );
    }
}

function showSafetyAlert(message, forceType) {
    const alert = document.getElementById('temp-safety-alert');
    const msg = document.getElementById('temp-safety-msg');
    if (!alert || !msg) {
        console.warn('Safety alert element missing — rebuild frontend container.');
        return;
    }

    const isSafe = forceType === 'safe'
        || (!forceType && message.toLowerCase().includes('comfortable and safe'));
    alert.className = `safety-alert ${isSafe ? 'safety-alert-safe' : 'safety-alert-warning'}`;
    msg.textContent = message;
    alert.classList.remove('hidden');
}

function renderTempHistoryRows(data) {
    const tbody = document.getElementById('temp-history-body');
    const unitSymbols = { Celsius: '°C', Fahrenheit: '°F', Kelvin: 'K' };

    if (!data.length) {
        tbody.innerHTML = '<tr class="empty-row"><td colspan="5">No conversion history yet</td></tr>';
        return;
    }

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
}

async function loadTempHistory() {
    const tbody = document.getElementById('temp-history-body');
    const filterEl = document.getElementById('temp-history-filter');
    const filter = filterEl ? filterEl.value : 'all';
    const url = filter === 'all'
        ? `${TEMP_API}/history`
        : `${TEMP_API}/history/filter?unit=${encodeURIComponent(filter)}`;

    try {
        const res = await authFetch(url);
        if (!res.ok) throw new Error(`HTTP ${res.status}`);
        const data = await res.json();
        renderTempHistoryRows(data);

    } catch (err) {
        if (err.message === 'Unauthorized') return;
        console.error('Load temp history error:', err);
        tbody.innerHTML = '<tr class="empty-row"><td colspan="5" class="history-error">Could not load history. Sign in and check your connection.</td></tr>';
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
    updateAuthBarUI();
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
