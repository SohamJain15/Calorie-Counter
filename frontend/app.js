/* =========================================================
   Calorie Counter — frontend (plain JS, no build step)
   Talks to the Spring Boot API under /api.
   ========================================================= */

// When Spring Boot serves this page (port 8081) use same-origin URLs; otherwise call the backend directly.
const API = location.port === '8081' ? '/api' : 'http://localhost:8081/api';

const MEALS = ['breakfast', 'lunch', 'dinner', 'snack'];
const MEAL_LABEL = { breakfast: 'Breakfast', lunch: 'Lunch', dinner: 'Dinner', snack: 'Snack' };
const CATEGORY_LABEL = {
    indian: 'Indian Food', fruits: 'Fruits', vegetables: 'Vegetables', grains: 'Grains',
    dairy: 'Dairy', meat: 'Meat', other: 'Other',
};
const ROUTES = {
    overview: { title: 'Overview', render: renderOverview },
    log: { title: 'Food log', render: renderLog },
    progress: { title: 'Progress', render: renderProgress },
    foods: { title: 'Food database', render: renderFoods },
    profile: { title: 'Profile & goals', render: renderProfile },
    settings: { title: 'Settings', render: renderSettings },
};

const ICON = {
    plus: '<svg viewBox="0 0 24 24" aria-hidden="true"><path d="M12 5v14M5 12h14"/></svg>',
    arrow: '<svg viewBox="0 0 24 24" aria-hidden="true"><path d="M5 12h14M13 6l6 6-6 6"/></svg>',
    chevUp: '<svg viewBox="0 0 24 24" aria-hidden="true"><path d="m6 15 6-6 6 6"/></svg>',
    chevDown: '<svg viewBox="0 0 24 24" aria-hidden="true"><path d="m6 9 6 6 6-6"/></svg>',
    left: '<svg viewBox="0 0 24 24" aria-hidden="true"><path d="m15 6-6 6 6 6"/></svg>',
    right: '<svg viewBox="0 0 24 24" aria-hidden="true"><path d="m9 6 6 6-6 6"/></svg>',
    check: '<svg viewBox="0 0 24 24" aria-hidden="true"><path d="m5 12 5 5L20 7"/></svg>',
    alert: '<svg viewBox="0 0 24 24" aria-hidden="true"><path d="M12 8v5M12 16.5v.5"/></svg>',
    search: '<svg viewBox="0 0 24 24" aria-hidden="true"><circle cx="11" cy="11" r="7"/><path d="m20 20-3.5-3.5"/></svg>',
};

const state = {
    route: 'overview',
    logDate: todayISO(),
    progressDays: 7,
    foodQuery: '',
    foodCategory: 'all',
    openMeals: new Set(MEALS),
    openItems: new Set(),
    entries: new Map(),      // id -> entry currently on screen (for Edit)
    exercises: new Map(),
    editingExercise: null,
    profile: null,
};

const view = document.getElementById('view');

// =========================================================
// Helpers
// =========================================================

async function api(path, options = {}) {
    const res = await fetch(API + path, {
        headers: { 'Content-Type': 'application/json' },
        ...options,
    });
    if (res.status === 204) return null;
    const data = await res.json().catch(() => null);
    if (!res.ok) {
        const err = new Error((data && data.error) || `Request failed (${res.status})`);
        err.details = (data && data.details) || [];
        err.status = res.status;
        throw err;
    }
    return data;
}

const send = (path, method, body) => api(path, { method, body: body === undefined ? undefined : JSON.stringify(body) });

function esc(str) {
    return String(str ?? '').replace(/[&<>"']/g, (c) => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c]));
}

const fmt = (n) => Math.round(Number(n) || 0).toLocaleString('en-IN');
const grams = (n) => `${Number(n || 0).toFixed(1).replace(/\.0$/, '')}g`;
function pad(n) {
    return String(n).padStart(2, '0');
}

function toISO(d) {
    return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`;
}

function todayISO() {
    return toISO(new Date());
}

function parseISO(iso) {
    const [y, m, d] = iso.split('-').map(Number);
    return new Date(y, m - 1, d);
}

function addDays(iso, n) {
    const d = parseISO(iso);
    d.setDate(d.getDate() + n);
    return toISO(d);
}

const longDate = (iso) => parseISO(iso).toLocaleDateString('en-IN', { weekday: 'long', day: 'numeric', month: 'long' });

function shortDayLabel(iso) {
    if (iso === todayISO()) return 'Today';
    if (iso === addDays(todayISO(), -1)) return 'Yesterday';
    return parseISO(iso).toLocaleDateString('en-IN', { weekday: 'short', day: 'numeric', month: 'short' });
}

function fmtTime(t) {
    if (!t) return '';
    const [h, m] = t.split(':').map(Number);
    return `${((h + 11) % 12) + 1}:${pad(m)} ${h < 12 ? 'am' : 'pm'}`;
}

const nowHHMM = () => { const d = new Date(); return `${pad(d.getHours())}:${pad(d.getMinutes())}`; };

function mealForNow() {
    const h = new Date().getHours();
    if (h < 11) return 'breakfast';
    if (h < 16) return 'lunch';
    if (h < 19) return 'snack';
    return 'dinner';
}

const DEFAULT_TIME = { breakfast: '08:30', lunch: '13:15', snack: '17:00', dinner: '20:30' };

const NO_PLURAL = ['medium', 'large', 'small'];

function servingText(e) {
    const q = Number(e.quantity);
    const qty = String(q).replace(/\.0$/, '');
    if (q === 1) return e.servingSize;
    // "1 piece" × 2 -> "2 pieces", "1 bowl" × 1.5 -> "1.5 bowls"; otherwise "2 × 100g"
    const m = /^1 (\w+)(.*)$/.exec(e.servingSize);
    if (m) {
        const unit = NO_PLURAL.includes(m[1]) || m[1].endsWith('s') ? m[1] : `${m[1]}s`;
        return `${qty} ${unit}${m[2]}`;
    }
    return `${qty} × ${e.servingSize}`;
}

let toastTimer;
function toast(message, isError = false) {
    const el = document.getElementById('toast');
    el.textContent = message;
    el.className = `toast show${isError ? ' error' : ''}`;
    clearTimeout(toastTimer);
    toastTimer = setTimeout(() => { el.className = 'toast'; }, 2600);
}

function errorText(err) {
    return [err.message, ...(err.details || [])].join(' ');
}

function serverDown() {
    view.innerHTML = `
      <div class="server-down">
        <h2>Can’t reach the server</h2>
        <p>Start the backend, then reload this page:</p>
        <p><code>cd backend</code><br /><code>.\\mvnw.cmd spring-boot:run</code></p>
      </div>`;
}

function pageHead({ eyebrow, title, sub, actions = '' }) {
    return `
      <div class="page-head">
        <div>
          <p class="eyebrow">${esc(eyebrow)}</p>
          <h1 class="page-title">${esc(title)}</h1>
          ${sub ? `<p class="page-sub">${esc(sub)}</p>` : ''}
        </div>
        ${actions ? `<div class="head-actions">${actions}</div>` : ''}
      </div>`;
}

const addFoodButton = (label = 'Add food') =>
    `<button type="button" class="btn primary" data-open-add>${ICON.plus}<span>${label}</span></button>`;

function updateAvatar(profile) {
    if (!profile) return;
    state.profile = profile;
    const initial = (profile.name || '').trim().charAt(0).toUpperCase();
    document.getElementById('avatar').textContent = initial || '?';
}

// =========================================================
// Shared components
// =========================================================

function rememberEntries(entries) {
    state.entries = new Map(entries.map((e) => [String(e.id), e]));
}

function mealsHtml(entries) {
    return `<div class="meals">${MEALS.map((meal) => {
        const items = entries.filter((e) => e.mealType === meal);
        const total = items.reduce((s, e) => s + e.calories, 0);
        const open = state.openMeals.has(meal);
        return `
          <div class="meal">
            <button type="button" class="meal-row" data-toggle-meal="${meal}" aria-expanded="${open}">
              <span class="dot"></span>
              <span class="meal-name">${MEAL_LABEL[meal]}
                <span class="meal-count">${items.length} ${items.length === 1 ? 'item' : 'items'}</span>
              </span>
              <span class="kcal">${fmt(total)} kcal</span>
              <span class="chev">${ICON.chevUp}</span>
            </button>
            <div class="meal-items" ${open ? '' : 'hidden'}>
              ${items.length ? items.map(itemHtml).join('') : `<p class="empty-meal">Nothing logged for ${MEAL_LABEL[meal].toLowerCase()} yet.</p>`}
              <div class="meal-add">
                <button type="button" class="link" data-open-add data-meal="${meal}">${ICON.plus}Add to ${MEAL_LABEL[meal].toLowerCase()}</button>
              </div>
            </div>
          </div>`;
    }).join('')}</div>`;
}

function itemHtml(e) {
    const open = state.openItems.has(String(e.id));
    return `
      <div class="item">
        <button type="button" class="item-row" data-toggle-item="${e.id}" aria-expanded="${open}">
          <span class="item-time">${fmtTime(e.loggedTime)}</span>
          <span class="item-name">${esc(e.foodName)}
            <span class="item-meta">${esc(servingText(e))} · ${MEAL_LABEL[e.mealType]}</span>
          </span>
          <span class="kcal">${fmt(e.calories)} kcal</span>
          <span class="chev">${ICON.chevDown}</span>
        </button>
        <div class="item-detail" ${open ? '' : 'hidden'}>
          <div class="nutri">
            <span>Protein <b>${grams(e.protein)}</b></span>
            <span>Carbs <b>${grams(e.carbs)}</b></span>
            <span>Fat <b>${grams(e.fat)}</b></span>
          </div>
          <div class="item-actions">
            <button type="button" class="link" data-edit-entry="${e.id}">Edit</button>
            <button type="button" class="link danger" data-delete-entry="${e.id}">Delete</button>
          </div>
        </div>
      </div>`;
}

function kLabel(v) {
    if (v === 0) return '0k';
    return v >= 1000 || v % 100 === 0 ? `${(v / 1000).toFixed(1).replace(/\.0$/, '')}k` : String(v);
}

/** Bar chart built from divs: gridlines, dashed goal line, one column per day with a hover tooltip. */
function chartHtml(points, { goal, height = 260, axis = true, forecast = [] }) {
    const maxVal = Math.max(goal || 0, ...points.map((p) => p.consumed), ...forecast.map((f) => f.high), 400);
    const top = Math.ceil((maxVal * 1.15) / 400) * 400;
    const ticks = [0, 1, 2, 3, 4].map((i) => (top / 4) * i);
    const every = points.length <= 7 ? 1 : points.length <= 31 ? 5 : 15;
    const last = points.length - 1;

    const cols = points.map((p, i) => {
        const h = (p.consumed / top) * 100;
        const over = goal && p.consumed > goal + p.burned;
        const d = parseISO(p.date);
        const label = points.length <= 7
            ? d.toLocaleDateString('en-IN', { weekday: 'short' })
            : (i % every === 0 || i === last) ? d.toLocaleDateString('en-IN', { day: 'numeric', month: 'short' }) : '';
        const tipTitle = points.length <= 7
            ? d.toLocaleDateString('en-IN', { weekday: 'short' })
            : d.toLocaleDateString('en-IN', { weekday: 'short', day: 'numeric', month: 'short' });
        const tipBody = p.logged ? `Consumed : ${fmt(p.consumed)} kcal` : 'Nothing logged';
        return `
          <button type="button" class="col" style="--y:${h}%" aria-label="${esc(tipTitle)}: ${esc(tipBody)}">
            <span class="b${p.logged ? '' : ' empty'}${over ? ' over' : ''}" style="height:${h}%"></span>
            <span class="x">${label}</span>
            <span class="tip"><b>${esc(tipTitle)}</b><span>${tipBody}</span></span>
          </button>`;
    }).join('') + forecast.map((f, i) => {
        const h = (f.predicted / top) * 100;
        const d = parseISO(f.date);
        const show = points.length <= 7 || i === 0 || i === forecast.length - 1;
        const label = show ? d.toLocaleDateString('en-IN', points.length <= 7 ? { weekday: 'short' } : { day: 'numeric', month: 'short' }) : '';
        const title = d.toLocaleDateString('en-IN', { weekday: 'short', day: 'numeric', month: 'short' });
        const body = `Forecast : ~${fmt(f.predicted)} kcal (${fmt(f.low)}–${fmt(f.high)})`;
        return `
          <button type="button" class="col fc${i === 0 ? ' fc-start' : ''}" style="--y:${h}%" aria-label="${esc(title)}: ${esc(body)}">
            <span class="b forecast${goal && f.predicted > goal ? ' over' : ''}" style="height:${h}%"></span>
            <span class="x">${label}</span>
            <span class="tip"><b>${esc(title)}</b><span>${body}</span></span>
          </button>`;
    }).join('');

    return `
      <div class="chart" style="--h:${height}px${axis ? '' : ';margin-left:0'}">
        <div class="chart-grid">
          ${ticks.map((v) => `<div style="top:${100 - (v / top) * 100}%">${axis ? `<span>${kLabel(v)}</span>` : ''}</div>`).join('')}
        </div>
        ${goal ? `<div class="goal-line" style="top:${100 - (goal / top) * 100}%" title="Daily goal"></div>` : ''}
        <div class="chart-cols">${cols}</div>
      </div>`;
}

function ringHtml(percent, over) {
    const r = 44;
    const c = 2 * Math.PI * r;
    const p = Math.min(Math.max(percent, 0), 100);
    return `
      <div class="ring${over ? ' over' : ''}" role="img" aria-label="${percent}% of daily goal consumed">
        <svg viewBox="0 0 104 104" aria-hidden="true">
          <circle class="track" cx="52" cy="52" r="${r}"></circle>
          <circle class="value" cx="52" cy="52" r="${r}" stroke-dasharray="${c}" stroke-dashoffset="${c * (1 - p / 100)}"></circle>
        </svg>
        <div class="ring-label">${percent}<small>%</small></div>
      </div>`;
}

function suggestionsHtml(s) {
    state.suggestion = s;
    if (!s || !s.suggestions.length) return '';
    const meal = MEAL_LABEL[s.mealType].toLowerCase();
    return `
      <section class="card" aria-labelledby="suggest-title">
        <div class="card-head" style="margin-bottom:10px">
          <div>
            <p class="card-label">Smart suggestions</p>
            <h2 class="card-title" id="suggest-title">What to eat next</h2>
          </div>
        </div>
        <p class="hint">${esc(s.message)}</p>
        <ul class="suggest-list">
          ${s.suggestions.map((g, i) => `
            <li>
              <div>
                <b>${esc(g.title)}</b>
                <span class="item-meta">${fmt(g.calories)} kcal · P ${grams(g.protein)} · C ${grams(g.carbs)} · F ${grams(g.fat)}</span>
                <p class="suggest-reason">${esc(g.reason)}</p>
              </div>
              <button type="button" class="btn ghost small" data-add-suggestion="${i}">Add to ${meal}</button>
            </li>`).join('')}
        </ul>
      </section>`;
}

function macroRow(name, value, goal, color) {
    const pct = goal > 0 ? Math.min(value / goal, 1) * 100 : 0;
    return `
      <div>
        <div class="macro-top">
          <span class="macro-name"><span class="dot" style="background:${color}"></span>${name}</span>
          <span class="macro-amount">${grams(value).replace('g', '')}g <small>/ ${fmt(goal)}g</small></span>
        </div>
        <div class="bar"><span style="width:${pct}%;background:${color}"></span></div>
      </div>`;
}

// =========================================================
// Overview
// =========================================================

async function renderOverview() {
    const today = todayISO();
    const [summary, day, progress, profile, suggestions] = await Promise.all([
        api(`/summary?date=${today}`), api(`/log?date=${today}`), api('/progress?days=7'), api('/profile'),
        api(`/suggestions?date=${today}`),
    ]);
    updateAvatar(profile);
    rememberEntries(day.entries);

    const h = new Date().getHours();
    const greeting = h < 12 ? 'Good morning' : h < 17 ? 'Good afternoon' : 'Good evening';
    const over = summary.remaining < 0;
    const chip = summary.consumed === 0
        ? '<span class="chip idle">Nothing logged</span>'
        : over ? '<span class="chip over">Over goal</span>' : '<span class="chip">On track</span>';
    const sub = over
        ? `${fmt(-summary.remaining)} kcal over your goal`
        : `${fmt(summary.remaining)} kcal remaining${summary.burned ? `, including ${fmt(summary.burned)} burned` : ''}`;

    view.innerHTML = `
      ${pageHead({
          eyebrow: longDate(today),
          title: profile.name ? `${greeting}, ${profile.name}` : greeting,
          sub: 'A clear view of your nutrition today.',
          actions: addFoodButton('Log food'),
      })}

      <div class="grid-2">
        <section class="card" aria-labelledby="cal-label">
          <div class="card-head">
            <p class="card-label" id="cal-label">Today’s calories</p>
            ${chip}
          </div>
          <div class="cal-main">
            <div>
              <p class="cal-figure">${fmt(summary.consumed)} <small>/ ${fmt(summary.goal)} kcal</small></p>
              <p class="cal-sub${over ? ' over' : ''}">${sub}</p>
            </div>
            ${ringHtml(summary.percentConsumed, over)}
          </div>
          <div class="card-foot">
            <span>Daily goal<strong>${fmt(summary.goal)} kcal</strong></span>
            <span>Burned<strong>${fmt(summary.burned)} kcal</strong></span>
            <span>${summary.percentConsumed}% consumed</span>
          </div>
        </section>

        <section class="card" aria-labelledby="macro-title">
          <div class="card-head">
            <div>
              <p class="card-label">Nutrition split</p>
              <h2 class="card-title" id="macro-title">Macros today</h2>
            </div>
            <a class="link" href="#/log">Details${ICON.arrow}</a>
          </div>
          <div class="macros">
            ${macroRow('Protein', summary.protein, summary.proteinGoal, 'var(--sage)')}
            ${macroRow('Carbohydrates', summary.carbs, summary.carbsGoal, 'var(--carbs)')}
            ${macroRow('Fat', summary.fat, summary.fatGoal, 'var(--fat)')}
          </div>
        </section>
      </div>

      <div class="grid-side mt">
        <section aria-labelledby="meals-title">
          <div class="section-head">
            <div>
              <p class="card-label">Your meals</p>
              <h2 class="card-title" id="meals-title">Today’s food log</h2>
            </div>
            <a class="link" href="#/log">View all${ICON.arrow}</a>
          </div>
          ${mealsHtml(day.entries)}
        </section>

        <div class="stack">
          <section class="card" aria-labelledby="rhythm-title">
            <div class="card-head">
              <div>
                <p class="card-label">This week</p>
                <h2 class="card-title" id="rhythm-title">Calorie rhythm</h2>
              </div>
              <a class="link" href="#/progress">See trends${ICON.arrow}</a>
            </div>
            ${chartHtml(progress.points, { goal: progress.goal, height: 150, axis: false })}
          </section>
          ${suggestionsHtml(suggestions)}
        </div>
      </div>`;
}

// =========================================================
// Food log
// =========================================================

async function renderLog() {
    const date = state.logDate;
    const isToday = date === todayISO();
    const [day, summary, suggestions] = await Promise.all([
        api(`/log?date=${date}`), api(`/summary?date=${date}`),
        isToday ? api(`/suggestions?date=${date}`) : Promise.resolve(null),
    ]);
    rememberEntries(day.entries);
    state.exercises = new Map(day.exercises.map((x) => [String(x.id), x]));

    const editing = state.editingExercise && state.exercises.get(String(state.editingExercise));
    const status = summary.status.toLowerCase();

    view.innerHTML = `
      ${pageHead({
          eyebrow: longDate(date),
          title: 'Food log',
          sub: isToday ? 'Review and adjust everything you’ve logged today.' : `Everything you logged on ${longDate(date)}.`,
          actions: `
            <div class="date-nav" role="group" aria-label="Choose day">
              <button type="button" data-day="-1" aria-label="Previous day">${ICON.left}</button>
              <span class="date-label">${shortDayLabel(date)}</span>
              <button type="button" data-day="1" aria-label="Next day" ${isToday ? 'disabled' : ''}>${ICON.right}</button>
            </div>
            ${addFoodButton()}`,
      })}

      <div class="stats">
        <div class="stat"><p class="card-label">${isToday ? 'Consumed today' : 'Consumed'}</p><p class="stat-value">${fmt(summary.consumed)} <small>kcal</small></p></div>
        <div class="stat"><p class="card-label">Protein</p><p class="stat-value">${grams(summary.protein).replace('g', '')}<small>g</small></p></div>
        <div class="stat"><p class="card-label">Carbs</p><p class="stat-value">${grams(summary.carbs).replace('g', '')}<small>g</small></p></div>
        <div class="stat"><p class="card-label">Fat</p><p class="stat-value">${grams(summary.fat).replace('g', '')}<small>g</small></p></div>
      </div>

      <div class="grid-side">
        <section aria-labelledby="by-meal-title">
          <div class="section-head">
            <div>
              <p class="card-label">By meal</p>
              <h2 class="card-title" id="by-meal-title">${isToday ? 'Today’s meals' : 'Meals'}</h2>
            </div>
            <p class="hint">Tap an item to see nutrition</p>
          </div>
          ${mealsHtml(day.entries)}
        </section>

        <div class="stack">
          <section class="card" aria-labelledby="balance-title">
            <p class="card-label" id="balance-title">Day balance</p>
            <div class="balance-list" style="margin-top:16px">
              <div>Calories budgeted <b>${fmt(summary.goal)}</b></div>
              <div>Calories consumed <b>${fmt(summary.consumed)}</b></div>
              <div>Calories burned <b>${fmt(summary.burned)}</b></div>
            </div>
            <p class="balance-result ${status}">
              ${fmt(Math.abs(summary.remaining))} Calorie ${summary.status}
              <small>${status === 'deficit' ? 'Still within your budget' : 'More than your budget'} (budget − consumed + burned)</small>
            </p>
          </section>

          <section class="card" aria-labelledby="ex-title">
            <p class="card-label">Activity</p>
            <h2 class="card-title" id="ex-title" style="margin-bottom:14px">Exercise</h2>
            ${day.exercises.length ? `
              <ul class="ex-list">
                ${day.exercises.map((x) => `
                  <li>
                    <span><b>${esc(x.name)}</b><span class="muted">${x.durationMinutes ? `${x.durationMinutes} min · ` : ''}${fmtTime(x.loggedTime)}</span></span>
                    <span class="kcal">−${fmt(x.caloriesBurned)} kcal</span>
                    <span class="item-actions">
                      <button type="button" class="link" data-edit-ex="${x.id}">Edit</button>
                      <button type="button" class="link danger" data-delete-ex="${x.id}">Delete</button>
                    </span>
                  </li>`).join('')}
              </ul>` : '<p class="hint" style="margin-bottom:14px">No exercise logged. Calories you burn are added back to your budget.</p>'}
            <form class="ex-form" id="ex-form" novalidate>
              <label class="field"><span>Activity</span>
                <input type="text" id="ex-name" maxlength="100" placeholder="e.g. Evening run" value="${esc(editing ? editing.name : '')}" /></label>
              <div class="field-row">
                <label class="field"><span>Minutes</span>
                  <input type="number" id="ex-min" min="1" max="1440" inputmode="numeric" placeholder="30" value="${editing && editing.durationMinutes ? editing.durationMinutes : ''}" /></label>
                <label class="field"><span>Calories burned</span>
                  <input type="number" id="ex-kcal" min="0" max="5000" inputmode="numeric" placeholder="200" value="${editing ? editing.caloriesBurned : ''}" /></label>
              </div>
              <p class="form-error" id="ex-error" role="alert"></p>
              <div class="head-actions">
                <button type="submit" class="btn primary small">${editing ? 'Save exercise' : 'Add exercise'}</button>
                ${editing ? '<button type="button" class="btn ghost small" data-cancel-ex>Cancel</button>' : ''}
              </div>
            </form>
          </section>
          ${isToday ? suggestionsHtml(suggestions) : ''}
        </div>
      </div>`;

    document.getElementById('ex-form').addEventListener('submit', saveExercise);
}

async function saveExercise(e) {
    e.preventDefault();
    const name = document.getElementById('ex-name').value.trim();
    const minutes = document.getElementById('ex-min').value.trim();
    const kcal = document.getElementById('ex-kcal').value.trim();
    const errorEl = document.getElementById('ex-error');

    if (!name) { errorEl.textContent = 'Enter the activity name.'; return; }
    if (kcal === '' || Number(kcal) < 0) { errorEl.textContent = 'Enter the calories burned (0 or more).'; return; }

    const body = {
        date: state.logDate,
        name,
        durationMinutes: minutes === '' ? null : Number(minutes),
        caloriesBurned: Number(kcal),
    };
    try {
        if (state.editingExercise) {
            await send(`/log/exercises/${state.editingExercise}`, 'PUT', body);
            toast('Exercise updated.');
        } else {
            await send('/log/exercises', 'POST', body);
            toast(`Added ${name}.`);
        }
        state.editingExercise = null;
        render();
    } catch (err) {
        errorEl.textContent = errorText(err);
    }
}

// =========================================================
// Progress
// =========================================================

async function renderProgress() {
    const days = state.progressDays;
    const [p, fc] = await Promise.all([api(`/progress?days=${days}`), api('/forecast?days=7')]);
    const periodWord = days === 7 ? 'week' : `${days} days`;

    const range = `
      <div class="segmented" role="group" aria-label="Range">
        ${[7, 30, 90].map((d) => `<button type="button" data-range="${d}" aria-pressed="${d === days}">${d} days</button>`).join('')}
      </div>`;

    if (!p.loggedDays) {
        view.innerHTML = `
          ${pageHead({ eyebrow: 'Your patterns', title: 'Progress', sub: 'Small patterns become useful when you can see them clearly.', actions: range })}
          <div class="card empty">
            <h3>No history in the last ${days} days</h3>
            <p>Log your meals for a few days and your trends will appear here. Want to see how it looks first?</p>
            <button type="button" class="btn primary" data-sample-data>Load sample data</button>
          </div>`;
        return;
    }

    let changeNote = 'No earlier data to compare';
    let changeClass = '';
    if (p.changePercent !== null && p.changePercent !== undefined) {
        const up = p.changePercent > 0;
        changeNote = `${up ? '↑' : '↓'} ${Math.abs(p.changePercent)}% from ${days === 7 ? 'last week' : `previous ${days} days`}`;
        changeClass = up ? 'bad' : 'good';
    }
    const ratio = p.withinGoalDays / days;
    const consistency = ratio >= 0.7 ? `Good consistency this ${days === 7 ? 'week' : 'period'}`
        : ratio >= 0.4 ? 'Room to tighten up' : 'Mostly over goal or not logged';

    view.innerHTML = `
      ${pageHead({ eyebrow: 'Your patterns', title: 'Progress', sub: 'Small patterns become useful when you can see them clearly.', actions: range })}

      <div class="stats flat" style="--cols: 3">
        <div class="stat">
          <p class="card-label">${days === 7 ? 'Weekly average' : `${days}-day average`}</p>
          <p class="stat-value">${fmt(p.averageConsumed)} <small>kcal</small></p>
          <p class="stat-note ${changeClass}">${changeNote}</p>
        </div>
        <div class="stat">
          <p class="card-label">Within goal</p>
          <p class="stat-value">${p.withinGoalDays} <small>/ ${days} days</small></p>
          <p class="stat-note">${consistency}</p>
        </div>
        <div class="stat">
          <p class="card-label">Protein target</p>
          <p class="stat-value">${p.proteinCompletion ?? 0}<small>%</small></p>
          <p class="stat-note">Average completion</p>
        </div>
      </div>

      <section class="card">
        <div class="card-head">
          <div>
            <p class="card-label">Calories consumed</p>
            <h2 class="card-title">${days} days overview</h2>
          </div>
          <span class="legend-goal">
            <span><span class="dot"></span>Daily goal : ${fmt(p.goal)} kcal</span>
            ${fc.available ? '<span><span class="swatch-fc"></span>Forecast</span>' : ''}
          </span>
        </div>
        ${chartHtml(p.points, { goal: p.goal, height: 260, forecast: fc.available ? fc.points : [] })}
      </section>

      ${forecastHtml(fc)}

      <section class="mt" aria-labelledby="insights-title">
        <p class="card-label">Observations</p>
        <h2 class="card-title" id="insights-title" style="margin-bottom:16px">Helpful insights</h2>
        <div class="insights">${insights(p, periodWord).map((i) => `
          <div class="insight">
            <span class="tick${i.warn ? ' warn' : ''}">${i.warn ? ICON.alert : ICON.check}</span>
            <div><h3>${esc(i.title)}</h3><p>${esc(i.text)}</p></div>
          </div>`).join('')}
        </div>
      </section>`;
}

function forecastHtml(fc) {
    if (!fc.available) {
        return `
          <section class="card mt">
            <p class="card-label">Prediction</p>
            <h2 class="card-title">Next 7 days forecast</h2>
            <p class="hint" style="margin-top:8px">${esc(fc.message)}</p>
          </section>`;
    }
    const trend = Math.abs(fc.slopePerDay) < 3 ? 'Steady' : fc.slopePerDay > 0 ? 'Rising' : 'Falling';
    const trendNote = `${fc.slopePerDay > 0 ? '+' : ''}${fc.slopePerDay} kcal per day`;
    return `
      <section class="card mt" aria-labelledby="fc-title">
        <div class="card-head">
          <div>
            <p class="card-label">Prediction</p>
            <h2 class="card-title" id="fc-title">Next 7 days forecast</h2>
          </div>
        </div>
        <div class="stats flat fc-stats" style="--cols: 3">
          <div class="stat">
            <p class="card-label">Predicted daily average</p>
            <p class="stat-value">${fmt(fc.averagePredicted)} <small>kcal</small></p>
            <p class="stat-note ${fc.averagePredicted > fc.goal ? 'bad' : 'good'}">${fc.averagePredicted > fc.goal ? `${fmt(fc.averagePredicted - fc.goal)} over` : `${fmt(fc.goal - fc.averagePredicted)} under`} your goal</p>
          </div>
          <div class="stat">
            <p class="card-label">Trend</p>
            <p class="stat-value">${trend}</p>
            <p class="stat-note">${trendNote}</p>
          </div>
          <div class="stat">
            <p class="card-label">Days likely over goal</p>
            <p class="stat-value">${fc.daysOverGoal} <small>/ ${fc.points.length} days</small></p>
            <p class="stat-note">${esc(fc.message)}</p>
          </div>
        </div>
        <p class="hint fc-method">
          How it works: a least-squares linear regression on your last ${fc.basedOnDays} logged days, plus a
          day-of-week adjustment (for example, heavier weekends). The dashed bars on the chart are the predictions;
          hover one to see its likely range. Model fit R² = ${fc.rSquared}.
        </p>
      </section>`;
}

function insights(p, periodWord) {
    const list = [];
    const diff = p.averageConsumed - p.goal;
    if (Math.abs(diff) <= p.goal * 0.1) {
        list.push({ title: 'Your average is steady', text: `Your intake sits close to your daily goal across the last ${p.days} days.` });
    } else if (diff > 0) {
        list.push({ warn: true, title: 'You’re averaging above goal', text: `About ${fmt(diff)} kcal over your daily goal on a typical day this ${periodWord}.` });
    } else {
        list.push({ warn: true, title: 'You’re well under goal', text: `About ${fmt(-diff)} kcal below goal on average. Make sure you’re eating enough.` });
    }

    const lowProtein = p.points.filter((d) => d.logged && d.protein < p.proteinGoal).length;
    list.push(lowProtein
        ? { warn: lowProtein > p.loggedDays / 2, title: `Protein was below target ${lowProtein} ${lowProtein === 1 ? 'day' : 'days'}`, text: 'Adding a protein-rich snack like curd, sprouts, eggs or paneer could help close the gap.' }
        : { title: 'Protein target met every day', text: `You reached ${p.proteinGoal}g of protein on every logged day.` });

    list.push(p.withinGoalDays
        ? { title: `You stayed within goal ${p.withinGoalDays} ${p.withinGoalDays === 1 ? 'day' : 'days'}`, text: 'That consistency is more useful than chasing a perfect day.' }
        : { warn: true, title: 'No days within goal yet', text: 'Logging meals as you eat them makes it easier to stay on top of the day.' });
    return list;
}

// =========================================================
// Food database
// =========================================================

async function renderFoods() {
    const chips = ['all', ...Object.keys(CATEGORY_LABEL)].map((c) =>
        `<button type="button" data-category="${c}" aria-pressed="${state.foodCategory === c}">${c === 'all' ? 'All foods' : CATEGORY_LABEL[c]}</button>`).join('');

    view.innerHTML = `
      ${pageHead({
          eyebrow: 'Explore nutrition',
          title: 'Food database',
          sub: 'Indian-friendly foods with clear, estimated nutrition per serving.',
          actions: `<button type="button" class="btn primary" data-open-add data-custom>${ICON.plus}<span>Log a food</span></button>`,
      })}
      <div class="toolbar">
        <label class="search">
          ${ICON.search}
          <input type="search" id="food-search" placeholder="Search foods, like paneer or rice" value="${esc(state.foodQuery)}" aria-label="Search foods" />
        </label>
        <div class="chips" role="group" aria-label="Category">${chips}</div>
      </div>
      <div class="table-wrap">
        <table>
          <thead><tr><th>Food</th><th>Serving</th><th>Calories</th><th>Macros</th><th><span class="visually-hidden">Actions</span></th></tr></thead>
          <tbody id="food-rows"><tr><td colspan="5" class="loading">Loading…</td></tr></tbody>
        </table>
      </div>`;

    const input = document.getElementById('food-search');
    let timer;
    input.addEventListener('input', () => {
        state.foodQuery = input.value;
        clearTimeout(timer);
        timer = setTimeout(loadFoodRows, 180);
    });
    await loadFoodRows();
}

async function loadFoodRows() {
    const params = new URLSearchParams();
    if (state.foodQuery.trim()) params.set('q', state.foodQuery.trim());
    if (state.foodCategory !== 'all') params.set('category', state.foodCategory);
    const foods = await api(`/foods?${params}`);
    const body = document.getElementById('food-rows');
    if (!body) return;

    body.innerHTML = foods.length ? foods.map((f) => `
      <tr>
        <td><span class="food-name">${esc(f.name)}</span><span class="food-sub">${CATEGORY_LABEL[f.category]} · ${f.custom ? 'custom' : 'estimate'}</span></td>
        <td>${esc(f.servingSize)}</td>
        <td class="food-kcal">${fmt(f.calories)} kcal</td>
        <td class="food-macros">P ${grams(f.protein)} · C ${grams(f.carbs)} · F ${grams(f.fat)}</td>
        <td class="actions">
          ${f.custom ? `<button type="button" class="link muted" data-delete-food="${f.id}" style="margin-right:14px">Remove</button>` : ''}
          <button type="button" class="link" data-open-add data-food="${f.id}" style="color:var(--text)">Add ${ICON.plus}</button>
        </td>
      </tr>`).join('')
        : `<tr><td colspan="5"><div class="empty"><h3>No foods match “${esc(state.foodQuery)}”</h3><p>Try a different spelling, or add it yourself.</p><button type="button" class="btn ghost" data-open-add data-custom>Add a custom food</button></div></td></tr>`;
}

// =========================================================
// Profile & goals
// =========================================================

async function renderProfile() {
    const p = await api('/profile');
    updateAvatar(p);
    const opt = (value, label, current) => `<option value="${value}" ${current === value ? 'selected' : ''}>${label}</option>`;

    view.innerHTML = `
      ${pageHead({ eyebrow: 'About you', title: 'Profile & goals', sub: 'Your daily targets drive every number on the dashboard.' })}
      <form id="profile-form" class="grid-2" novalidate>
        <section class="card form-grid">
          <div><p class="card-label">Profile</p><h2 class="card-title">Personal details</h2></div>
          <label class="field"><span>Name</span><input id="pf-name" maxlength="60" value="${esc(p.name || '')}" placeholder="Your first name" /></label>
          <div class="field-row">
            <label class="field"><span>Age</span><input id="pf-age" type="number" min="10" max="120" value="${p.age ?? ''}" /></label>
            <label class="field"><span>Sex</span>
              <select id="pf-sex">${opt('', 'Prefer not to say', p.sex || '')}${opt('female', 'Female', p.sex)}${opt('male', 'Male', p.sex)}</select></label>
          </div>
          <div class="field-row">
            <label class="field"><span>Height (cm)</span><input id="pf-height" type="number" min="50" max="260" step="0.1" value="${p.heightCm ?? ''}" /></label>
            <label class="field"><span>Weight (kg)</span><input id="pf-weight" type="number" min="20" max="400" step="0.1" value="${p.weightKg ?? ''}" /></label>
          </div>
          <label class="field"><span>Activity level</span>
            <select id="pf-activity">
              ${opt('', 'Not set', p.activityLevel || '')}
              ${opt('sedentary', 'Sedentary (little exercise)', p.activityLevel)}
              ${opt('light', 'Light (1–3 days a week)', p.activityLevel)}
              ${opt('moderate', 'Moderate (3–5 days a week)', p.activityLevel)}
              ${opt('active', 'Active (6–7 days a week)', p.activityLevel)}
              ${opt('very_active', 'Very active (physical job or twice a day)', p.activityLevel)}
            </select></label>
        </section>

        <section class="card form-grid">
          <div><p class="card-label">Targets</p><h2 class="card-title">Daily goals</h2></div>
          <label class="field"><span>Calories (kcal)</span><input id="pf-cal" type="number" min="800" max="10000" value="${p.calorieGoal}" /></label>
          <div class="field-row three">
            <label class="field"><span>Protein (g)</span><input id="pf-protein" type="number" min="0" max="1000" value="${p.proteinGoal}" /></label>
            <label class="field"><span>Carbs (g)</span><input id="pf-carbs" type="number" min="0" max="2000" value="${p.carbsGoal}" /></label>
            <label class="field"><span>Fat (g)</span><input id="pf-fat" type="number" min="0" max="1000" value="${p.fatGoal}" /></label>
          </div>
          <div>
            <button type="button" class="link" id="pf-suggest">Suggest goals from my details</button>
            <p class="hint" id="pf-suggest-note">Uses the Mifflin–St Jeor formula with your age, sex, height, weight and activity.</p>
          </div>
          <p class="form-error" id="pf-error" role="alert"></p>
          <div class="head-actions"><button type="submit" class="btn primary">Save changes</button></div>
        </section>
      </form>`;

    document.getElementById('pf-suggest').addEventListener('click', suggestGoals);
    document.getElementById('profile-form').addEventListener('submit', saveProfile);
}

const val = (id) => document.getElementById(id).value.trim();
const numOrNull = (id) => (val(id) === '' ? null : Number(val(id)));

function suggestGoals() {
    const age = numOrNull('pf-age'); const h = numOrNull('pf-height'); const w = numOrNull('pf-weight');
    const sex = val('pf-sex'); const activity = val('pf-activity');
    const note = document.getElementById('pf-suggest-note');
    if (!age || !h || !w || !sex || !activity) {
        note.textContent = 'Fill in age, sex, height, weight and activity level first.';
        note.style.color = 'var(--danger)';
        return;
    }
    const factor = { sedentary: 1.2, light: 1.375, moderate: 1.55, active: 1.725, very_active: 1.9 }[activity];
    const bmr = 10 * w + 6.25 * h - 5 * age + (sex === 'male' ? 5 : -161);
    const kcal = Math.max(1200, Math.round((bmr * factor) / 10) * 10);
    const protein = Math.round(w * 1.6);
    const fat = Math.round((kcal * 0.25) / 9);
    const carbs = Math.max(0, Math.round((kcal - protein * 4 - fat * 9) / 4));
    document.getElementById('pf-cal').value = kcal;
    document.getElementById('pf-protein').value = protein;
    document.getElementById('pf-carbs').value = carbs;
    document.getElementById('pf-fat').value = fat;
    note.style.color = '';
    note.textContent = `Suggested ${fmt(kcal)} kcal to maintain your weight. Save to apply.`;
}

async function saveProfile(e) {
    e.preventDefault();
    const body = {
        name: val('pf-name') || null,
        age: numOrNull('pf-age'),
        sex: val('pf-sex') || null,
        heightCm: numOrNull('pf-height'),
        weightKg: numOrNull('pf-weight'),
        activityLevel: val('pf-activity') || null,
        calorieGoal: numOrNull('pf-cal'),
        proteinGoal: numOrNull('pf-protein'),
        carbsGoal: numOrNull('pf-carbs'),
        fatGoal: numOrNull('pf-fat'),
    };
    try {
        updateAvatar(await send('/profile', 'PUT', body));
        document.getElementById('pf-error').textContent = '';
        toast('Changes saved.');
    } catch (err) {
        document.getElementById('pf-error').textContent = errorText(err);
    }
}

// =========================================================
// Settings
// =========================================================

function currentTheme() {
    try { return localStorage.getItem('cc-theme') || 'system'; } catch (e) { return 'system'; }
}

function applyTheme(theme) {
    try { localStorage.setItem('cc-theme', theme); } catch (e) { /* ignore */ }
    if (theme === 'system') delete document.documentElement.dataset.theme;
    else document.documentElement.dataset.theme = theme;
}

async function renderSettings() {
    const theme = currentTheme();
    view.innerHTML = `
      ${pageHead({ eyebrow: 'Preferences', title: 'Settings', sub: 'Appearance and your saved data.' })}
      <div class="settings">
        <section class="card">
          <div class="setting">
            <div><h3>Appearance</h3><p>Follow your system, or pick light or dark.</p></div>
            <div class="segmented" role="group" aria-label="Theme">
              ${['system', 'light', 'dark'].map((t) => `<button type="button" data-theme-pick="${t}" aria-pressed="${t === theme}">${t[0].toUpperCase() + t.slice(1)}</button>`).join('')}
            </div>
          </div>
        </section>

        <section class="card">
          <div class="setting">
            <div><h3>Sample data</h3><p>Fill the last 30 days (and today’s meals so far) with example Indian meals and workouts. Days you already logged are left alone.</p></div>
            <button type="button" class="btn ghost" data-sample-data>Load sample data</button>
          </div>
        </section>

        <section class="card">
          <div class="setting">
            <div><h3>Clear today’s log</h3><p>Removes every food and exercise entry logged today.</p></div>
            <button type="button" class="btn danger" data-confirm="clear-today">Clear today</button>
          </div>
          <div class="setting">
            <div><h3>Delete all history</h3><p>Removes your whole food and exercise history. Your profile, goals and food database stay.</p></div>
            <button type="button" class="btn danger" data-confirm="clear-all">Delete history</button>
          </div>
        </section>
      </div>`;
}

// =========================================================
// Add / edit food dialog
// =========================================================

const dlg = {
    el: document.getElementById('entry-dialog'),
    form: document.getElementById('entry-form'),
    mode: 'add',
    entryId: null,
    date: null,
    tab: 'db',
    food: null,
    searchTimer: null,
};

const $d = (id) => document.getElementById(id);

function setTab(tab) {
    dlg.tab = tab;
    dlg.form.querySelectorAll('[data-tab]').forEach((b) => b.setAttribute('aria-selected', String(b.dataset.tab === tab)));
    dlg.form.querySelector('[data-panel="db"]').hidden = tab !== 'db';
    dlg.form.querySelector('[data-panel="custom"]').hidden = tab !== 'custom';
    $d('dlg-error').textContent = '';
    updateTotal();
}

function pickFood(food) {
    dlg.food = food;
    const picked = $d('dlg-picked');
    if (!food) {
        picked.hidden = true;
        $d('dlg-results').hidden = false;
        updateTotal();
        return;
    }
    picked.innerHTML = `
      <div><b>${esc(food.name)}</b><small>${esc(food.servingSize)} · ${fmt(food.calories)} kcal per serving</small></div>
      <button type="button" class="link" data-change-food>Change</button>`;
    picked.hidden = false;
    $d('dlg-results').hidden = true;
    updateTotal();
}

async function searchDialogFoods() {
    const q = $d('dlg-search').value.trim();
    try {
        const foods = await api(`/foods${q ? `?q=${encodeURIComponent(q)}` : ''}`);
        const list = $d('dlg-results');
        list.innerHTML = foods.slice(0, 8).map((f) => `
          <li><button type="button" data-pick="${f.id}">
            <span>${esc(f.name)}<br /><small>${esc(f.servingSize)}</small></span>
            <small>${fmt(f.calories)} kcal</small>
          </button></li>`).join('')
            || '<li><p class="hint" style="padding:12px 14px">No match. Try the Custom food tab.</p></li>';
        list._foods = new Map(foods.map((f) => [String(f.id), f]));
        if (!dlg.food) list.hidden = false;
    } catch (err) {
        $d('dlg-error').textContent = errorText(err);
    }
}

function perServing() {
    if (dlg.tab === 'db') {
        return dlg.food ? { kcal: dlg.food.calories, p: dlg.food.protein, c: dlg.food.carbs, f: dlg.food.fat } : null;
    }
    const kcal = $d('dlg-calories').value;
    if (kcal === '') return null;
    return {
        kcal: Number(kcal),
        p: Number($d('dlg-protein').value || 0),
        c: Number($d('dlg-carbs').value || 0),
        f: Number($d('dlg-fat').value || 0),
    };
}

function updateTotal() {
    const base = perServing();
    const q = Number($d('dlg-qty').value) || 0;
    $d('dlg-total').innerHTML = base && q > 0
        ? `Total <b>${fmt(base.kcal * q)} kcal</b> · P ${grams(base.p * q)} · C ${grams(base.c * q)} · F ${grams(base.f * q)}`
        : '';
}

async function openEntryDialog({ meal, foodId, entry, custom } = {}) {
    dlg.form.reset();
    $d('dlg-error').textContent = '';
    $d('dlg-results').innerHTML = '';
    pickFood(null);

    dlg.mode = entry ? 'edit' : 'add';
    dlg.entryId = entry ? entry.id : null;
    dlg.date = entry ? entry.logDate : (state.route === 'log' ? state.logDate : todayISO());

    const mealType = entry ? entry.mealType : meal || mealForNow();
    $d('entry-title').textContent = entry ? 'Edit entry' : 'Add food';
    $d('dlg-submit').textContent = entry ? 'Save changes' : 'Add to log';
    $d('dlg-meal').value = mealType;
    $d('dlg-qty').value = entry ? entry.quantity : 1;
    $d('dlg-time').value = entry ? entry.loggedTime.slice(0, 5)
        : (dlg.date === todayISO() ? nowHHMM() : DEFAULT_TIME[mealType]);
    $d('dlg-save-row').hidden = Boolean(entry);

    if (entry && !entry.foodId) {
        setTab('custom');
        const q = entry.quantity || 1;
        $d('dlg-name').value = entry.foodName;
        $d('dlg-serving').value = entry.servingSize;
        $d('dlg-calories').value = Math.round(entry.calories / q);
        $d('dlg-protein').value = +(entry.protein / q).toFixed(1);
        $d('dlg-carbs').value = +(entry.carbs / q).toFixed(1);
        $d('dlg-fat').value = +(entry.fat / q).toFixed(1);
    } else if (custom) {
        setTab('custom');
    } else {
        setTab('db');
        const id = entry ? entry.foodId : foodId;
        if (id) {
            try { pickFood(await api(`/foods/${id}`)); } catch (e) { /* food was removed; user can pick again */ }
        }
        searchDialogFoods();
    }

    updateTotal();
    if (!dlg.el.open) dlg.el.showModal();
    (dlg.tab === 'custom' ? $d('dlg-name') : dlg.food ? $d('dlg-qty') : $d('dlg-search')).focus();
}

async function submitEntry(e) {
    e.preventDefault();
    const err = $d('dlg-error');
    const qty = Number($d('dlg-qty').value);
    const mealType = $d('dlg-meal').value;
    if (!(qty >= 0.1 && qty <= 50)) { err.textContent = 'Servings must be between 0.1 and 50.'; return; }

    const body = { date: dlg.date, mealType, time: $d('dlg-time').value || null, quantity: qty };
    let name;

    if (dlg.tab === 'db') {
        if (!dlg.food) { err.textContent = 'Pick a food from the list, or switch to Custom food.'; return; }
        body.foodId = dlg.food.id;
        name = dlg.food.name;
    } else {
        name = $d('dlg-name').value.trim();
        const kcal = $d('dlg-calories').value;
        if (!name) { err.textContent = 'Enter a food name.'; $d('dlg-name').focus(); return; }
        if (kcal === '' || Number(kcal) < 0) { err.textContent = 'Enter calories per serving (0 or more).'; $d('dlg-calories').focus(); return; }
        const custom = {
            name,
            servingSize: $d('dlg-serving').value.trim() || '1 serving',
            calories: Math.round(Number(kcal)),
            protein: Number($d('dlg-protein').value || 0),
            carbs: Number($d('dlg-carbs').value || 0),
            fat: Number($d('dlg-fat').value || 0),
        };
        if (dlg.mode === 'add' && $d('dlg-save-db').checked) {
            try {
                const food = await send('/foods', 'POST', { ...custom, category: $d('dlg-category').value });
                body.foodId = food.id;
            } catch (ex) { err.textContent = errorText(ex); return; }
        } else {
            Object.assign(body, custom);
        }
    }

    const submit = $d('dlg-submit');
    submit.disabled = true;
    try {
        if (dlg.mode === 'edit') {
            await send(`/log/entries/${dlg.entryId}`, 'PUT', body);
            toast('Entry updated.');
        } else {
            await send('/log/entries', 'POST', body);
            toast(`Added ${name} to ${MEAL_LABEL[mealType].toLowerCase()}.`);
        }
        dlg.el.close();
        render();
    } catch (ex) {
        err.textContent = errorText(ex);
    } finally {
        submit.disabled = false;
    }
}

dlg.form.addEventListener('submit', submitEntry);
dlg.form.addEventListener('input', (e) => {
    if (e.target.id === 'dlg-search') {
        clearTimeout(dlg.searchTimer);
        dlg.searchTimer = setTimeout(searchDialogFoods, 160);
        if (dlg.food) pickFood(null);
    }
    updateTotal();
});
dlg.form.addEventListener('click', (e) => {
    const tab = e.target.closest('[data-tab]');
    if (tab) setTab(tab.dataset.tab);

    const pick = e.target.closest('[data-pick]');
    if (pick) {
        pickFood($d('dlg-results')._foods.get(pick.dataset.pick));
        $d('dlg-qty').focus();
    }

    if (e.target.closest('[data-change-food]')) {
        pickFood(null);
        $d('dlg-search').focus();
    }
    if (e.target.closest('[data-close]')) dlg.el.close();
});
// Click on the backdrop closes the dialog.
dlg.el.addEventListener('click', (e) => { if (e.target === dlg.el) dlg.el.close(); });

// =========================================================
// Global actions (event delegation)
// =========================================================

const armed = new WeakMap();

document.addEventListener('click', async (e) => {
    const t = e.target;

    const add = t.closest('[data-open-add]');
    if (add && !dlg.el.contains(add)) {
        openEntryDialog({ meal: add.dataset.meal, foodId: add.dataset.food, custom: 'custom' in add.dataset });
        return;
    }

    const mealBtn = t.closest('[data-toggle-meal]');
    if (mealBtn) {
        const meal = mealBtn.dataset.toggleMeal;
        const open = mealBtn.getAttribute('aria-expanded') !== 'true';
        open ? state.openMeals.add(meal) : state.openMeals.delete(meal);
        mealBtn.setAttribute('aria-expanded', String(open));
        mealBtn.nextElementSibling.hidden = !open;
        return;
    }

    const itemBtn = t.closest('[data-toggle-item]');
    if (itemBtn) {
        const id = itemBtn.dataset.toggleItem;
        const open = itemBtn.getAttribute('aria-expanded') !== 'true';
        open ? state.openItems.add(id) : state.openItems.delete(id);
        itemBtn.setAttribute('aria-expanded', String(open));
        itemBtn.nextElementSibling.hidden = !open;
        return;
    }

    const edit = t.closest('[data-edit-entry]');
    if (edit) { openEntryDialog({ entry: state.entries.get(edit.dataset.editEntry) }); return; }

    const del = t.closest('[data-delete-entry]');
    if (del) {
        const entry = state.entries.get(del.dataset.deleteEntry);
        try {
            await send(`/log/entries/${del.dataset.deleteEntry}`, 'DELETE');
            toast(`Removed ${entry ? entry.foodName : 'entry'}.`);
            render();
        } catch (err) { toast(errorText(err), true); }
        return;
    }

    const day = t.closest('[data-day]');
    if (day && !day.disabled) {
        const next = addDays(state.logDate, Number(day.dataset.day));
        if (next <= todayISO()) { state.logDate = next; state.editingExercise = null; render(); }
        return;
    }

    const editEx = t.closest('[data-edit-ex]');
    if (editEx) {
        state.editingExercise = editEx.dataset.editEx;
        await render();
        document.getElementById('ex-name').focus();
        return;
    }
    if (t.closest('[data-cancel-ex]')) { state.editingExercise = null; render(); return; }

    const delEx = t.closest('[data-delete-ex]');
    if (delEx) {
        try {
            await send(`/log/exercises/${delEx.dataset.deleteEx}`, 'DELETE');
            if (state.editingExercise === delEx.dataset.deleteEx) state.editingExercise = null;
            toast('Exercise removed.');
            render();
        } catch (err) { toast(errorText(err), true); }
        return;
    }

    const range = t.closest('[data-range]');
    if (range) { state.progressDays = Number(range.dataset.range); render(); return; }

    const cat = t.closest('[data-category]');
    if (cat) {
        state.foodCategory = cat.dataset.category;
        document.querySelectorAll('[data-category]').forEach((b) => b.setAttribute('aria-pressed', String(b === cat)));
        loadFoodRows();
        return;
    }

    const delFood = t.closest('[data-delete-food]');
    if (delFood) {
        try {
            await send(`/foods/${delFood.dataset.deleteFood}`, 'DELETE');
            toast('Food removed from the database.');
            loadFoodRows();
        } catch (err) { toast(errorText(err), true); }
        return;
    }

    const themeBtn = t.closest('[data-theme-pick]');
    if (themeBtn) {
        applyTheme(themeBtn.dataset.themePick);
        document.querySelectorAll('[data-theme-pick]').forEach((b) => b.setAttribute('aria-pressed', String(b === themeBtn)));
        return;
    }

    const sample = t.closest('[data-sample-data]');
    if (sample) {
        sample.disabled = true;
        try {
            const res = await send('/sample-data?days=30', 'POST');
            toast(res.daysFilled ? `Added sample meals for ${res.daysFilled} days.` : 'Those days already have entries.');
            if (state.route === 'progress') render();
        } catch (err) { toast(errorText(err), true); } finally { sample.disabled = false; }
        return;
    }

    const sug = t.closest('[data-add-suggestion]');
    if (sug && state.suggestion) {
        const s = state.suggestion;
        const g = s.suggestions[Number(sug.dataset.addSuggestion)];
        sug.disabled = true;
        try {
            for (const item of g.items) {
                await send('/log/entries', 'POST', { date: s.date, mealType: s.mealType, foodId: item.foodId, quantity: item.quantity });
            }
            toast(`Added ${g.title} to ${MEAL_LABEL[s.mealType].toLowerCase()}.`);
            render();
        } catch (err) { toast(errorText(err), true); sug.disabled = false; }
        return;
    }

    // Two-step confirm for destructive buttons: first click arms, second click acts.
    const confirmBtn = t.closest('[data-confirm]');
    if (confirmBtn) {
        if (!armed.get(confirmBtn)) {
            const original = confirmBtn.textContent;
            confirmBtn.textContent = 'Click again to confirm';
            confirmBtn.classList.add('armed');
            armed.set(confirmBtn, setTimeout(() => {
                confirmBtn.textContent = original;
                confirmBtn.classList.remove('armed');
                armed.delete(confirmBtn);
            }, 4000));
            return;
        }
        clearTimeout(armed.get(confirmBtn));
        armed.delete(confirmBtn);
        try {
            if (confirmBtn.dataset.confirm === 'clear-today') {
                await send(`/log/day/${todayISO()}`, 'DELETE');
                toast('Today’s log cleared.');
            } else {
                await send('/log/all', 'DELETE');
                toast('All history deleted.');
            }
        } catch (err) { toast(errorText(err), true); }
        render();
    }
});

// =========================================================
// Router + shell
// =========================================================

const sidebar = document.getElementById('sidebar');
const scrim = document.getElementById('scrim');
const menuBtn = document.getElementById('menu-btn');

function setMenu(open) {
    sidebar.classList.toggle('open', open);
    scrim.hidden = !open;
    menuBtn.setAttribute('aria-expanded', String(open));
}

menuBtn.addEventListener('click', () => setMenu(!sidebar.classList.contains('open')));
scrim.addEventListener('click', () => setMenu(false));

document.getElementById('search-btn').addEventListener('click', () => {
    if (state.route === 'foods') document.getElementById('food-search').focus();
    else location.hash = '#/foods';
});

let renderSeq = 0;

async function render() {
    const seq = ++renderSeq;
    const route = ROUTES[state.route] ? state.route : 'overview';
    try {
        await ROUTES[route].render();
    } catch (err) {
        if (seq !== renderSeq) return;
        if (err instanceof TypeError) serverDown();          // fetch failed: server not running
        else view.innerHTML = `<div class="server-down"><h2>Something went wrong</h2><p>${esc(errorText(err))}</p></div>`;
    }
}

function onRoute() {
    const name = (location.hash.replace(/^#\/?/, '') || 'overview').split('?')[0];
    const route = ROUTES[name] ? name : 'overview';
    const changed = route !== state.route;
    state.route = route;

    document.querySelectorAll('.nav a').forEach((a) => {
        const active = a.dataset.route === route;
        a.classList.toggle('active', active);
        if (active) a.setAttribute('aria-current', 'page'); else a.removeAttribute('aria-current');
    });
    document.getElementById('crumb').textContent = ROUTES[route].title;
    document.title = `${ROUTES[route].title} · Calorie Counter`;
    setMenu(false);

    if (changed) {
        view.innerHTML = '<p class="loading">Loading…</p>';
        window.scrollTo(0, 0);
    }
    render().then(() => {
        if (route === 'foods' && name === 'foods' && document.activeElement === document.getElementById('search-btn')) {
            document.getElementById('food-search').focus();
        }
    });
}

window.addEventListener('hashchange', onRoute);
state.route = '';
onRoute();
api('/profile').then(updateAvatar).catch(() => {});
