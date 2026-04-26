/* NUST LMS Course Page */

let completedIds = new Set();

// TYPE CONFIG
const TYPE_CONFIG = {
    lecture:    { icon: "fa-solid fa-chalkboard-user", label: "Lecture",    cssClass: "type-lecture"    },
    pdf:        { icon: "fa-solid fa-file-pdf",        label: "PDF",        cssClass: "type-pdf"        },
    assignment: { icon: "fa-solid fa-pen-to-square",   label: "Assignment", cssClass: "type-assignment" },
    quiz:       { icon: "fa-solid fa-circle-question", label: "Quiz",       cssClass: "type-quiz"       },
    word:       { icon: "fa-solid fa-file-word",       label: "Word Doc",   cssClass: "type-word"       },
};
function getTypeConfig(type) {
    return TYPE_CONFIG[type] || TYPE_CONFIG["pdf"];
}

// THEME
const themeToggle = document.getElementById("themeToggle");
const themeIcon   = document.getElementById("themeIcon");
const html        = document.documentElement;

function applyTheme(dark) {
    html.setAttribute("data-bs-theme", dark ? "dark" : "light");
    themeIcon.className = dark ? "fa-solid fa-moon" : "fa-solid fa-sun";
    themeToggle.checked = dark;
    localStorage.setItem("lmsTheme", dark ? "dark" : "light");
}

themeToggle.addEventListener("change", () => applyTheme(themeToggle.checked));

// POPULATE HERO
function populateHero(data) {
    document.title = `${data.code} — NUST LMS`;
    document.getElementById("breadcrumbCourse").textContent = `${data.code}: ${data.title}`;
    document.getElementById("heroCourseCode").textContent   = data.code;
    document.getElementById("heroCourseTitle").textContent  = data.title;
    document.getElementById("heroCourseDesc").textContent   = data.description;

    // Stats
    const totalWeeks       = data.weeks.length;
    const totalResources   = data.weeks.reduce((s, w) => s + w.resources.length, 0);
    const totalAssignments = data.weeks.reduce((s, w) =>
        s + w.resources.filter(r => r.type === "assignment").length, 0);
    const dueSoon = data.weeks.reduce((s, w) =>
        s + w.resources.filter(r => r.dueDate).length, 0);

    document.getElementById("statWeeks").textContent       = totalWeeks;
    document.getElementById("statResources").textContent   = totalResources;
    document.getElementById("statAssignments").textContent = totalAssignments;
    document.getElementById("statDue").textContent         = dueSoon;

    // Progress ring
    const pct = Math.round((data.completedCount / data.totalCount) * 100);
    document.getElementById("progressPct").textContent = pct + "%";
    // circumference = 2π×34 ≈ 213.6
    const circumference = 2 * Math.PI * 34;
    const offset = circumference - (pct / 100) * circumference;
    // Animate after a tiny delay so transition fires
    setTimeout(() => {
        document.getElementById("progressRing").style.strokeDashoffset = offset;
    }, 120);
}

// BUILD WEEK SECTIONS
function buildWeeks(data) {
    const container = document.getElementById("weekSections");
    container.innerHTML = "";

    data.weeks.forEach((week, wi) => {
        const section = document.createElement("div");
        section.className = "week-section";
        section.dataset.weekIndex = wi;

        // Header
        const header = document.createElement("div");
        header.className = "week-header";
        header.innerHTML = `
            <span class="week-label">${week.label}</span>
            <div class="week-line"></div>
            <span class="week-count" style="font-size:0.75rem;color:#555">${week.resources.length} items</span>
            <i class="fa-solid fa-chevron-down week-toggle-icon"></i>
        `;
        header.addEventListener("click", () => {
            section.classList.toggle("collapsed");
        });

        // Items list
        const itemsList = document.createElement("div");
        itemsList.className = "week-items";

        week.resources.forEach(res => {
            const cfg  = getTypeConfig(res.type);
            const done = completedIds.has(res.id);
            const row  = document.createElement("div");
            row.className = `resource-row ${cfg.cssClass} ${done ? "done" : ""}`;
            row.dataset.type  = res.type;
            row.dataset.title = res.title.toLowerCase();
            row.dataset.id    = res.id;

            row.innerHTML = `
                <div class="resource-icon-wrap">
                    <i class="${cfg.icon}"></i>
                </div>
                <span class="resource-name">${res.title}</span>
                <span class="resource-type-label">${cfg.label}</span>
                <div class="resource-check">
                    ${done ? '<i class="fa-solid fa-check"></i>' : ''}
                </div>
            `;

            row.addEventListener("click", () => openModal(res, week.label, cfg));
            itemsList.appendChild(row);
        });

        section.appendChild(header);
        section.appendChild(itemsList);
        container.appendChild(section);
    });
}

// MODAL
const bsModal = new bootstrap.Modal(document.getElementById("resourceModal"));

function openModal(res, weekLabel, cfg) {
    document.getElementById("modalIcon").className = `cal-modal-icon`;
    document.getElementById("modalIcon").style.background = getIconBg(res.type);
    document.getElementById("modalIcon").style.color = getIconColor(res.type);
    document.getElementById("modalIcon").innerHTML = `<i class="${cfg.icon}"></i>`;
    document.getElementById("modalTitle").textContent = res.title;
    document.getElementById("modalWeek").textContent  = weekLabel;
    document.getElementById("modalType").textContent  = cfg.label;
    document.getElementById("modalSize").textContent  = res.size || "—";

    const dueDateRow = document.getElementById("modalDueDateRow");
    if (res.dueDate) {
        dueDateRow.style.display = "";
        document.getElementById("modalDueDate").textContent = res.dueDate;
    } else {
        dueDateRow.style.display = "none";
    }

    document.getElementById("modalOpenBtn").href = res.url;
    bsModal.show();
}

function getIconBg(type) {
    const map = {
        lecture: "rgba(59,130,246,0.15)", pdf: "rgba(231,76,60,0.15)",
        assignment: "rgba(167,139,250,0.15)", quiz: "rgba(245,158,11,0.15)", word: "rgba(34,197,94,0.15)"
    };
    return map[type] || "rgba(255,255,255,0.08)";
}
function getIconColor(type) {
    const map = {
        lecture: "#3b82f6", pdf: "#e74c3c",
        assignment: "#a78bfa", quiz: "#f59e0b", word: "#22c55e"
    };
    return map[type] || "#ccc";
}

// FILTER & SEARCH
let activeFilter = "all";

document.querySelectorAll(".cal-filter-btn").forEach(btn => {
    btn.addEventListener("click", () => {
        document.querySelectorAll(".cal-filter-btn").forEach(b => b.classList.remove("active"));
        btn.classList.add("active");
        activeFilter = btn.dataset.filter;
        applyFilterSearch();
    });
});

document.getElementById("searchInput").addEventListener("input", applyFilterSearch);

function applyFilterSearch() {
    const query = document.getElementById("searchInput").value.toLowerCase().trim();

    document.querySelectorAll(".week-section").forEach(section => {
        let visibleCount = 0;
        section.querySelectorAll(".resource-row").forEach(row => {
            const typeMatch  = activeFilter === "all" || row.dataset.type === activeFilter;
            const queryMatch = !query || row.dataset.title.includes(query);
            const visible    = typeMatch && queryMatch;
            row.classList.toggle("hidden-by-filter", !visible);
            if (visible) visibleCount++;
        });
        section.classList.toggle("all-hidden", visibleCount === 0);
    });
}

// PUBLIC RENDER FUNCTION
function renderCourse(data, doneIds = new Set()) {
    completedIds = doneIds;
    populateHero(data);
    buildWeeks(data);
}

// INIT
const savedTheme2 = localStorage.getItem("lmsTheme") || "dark";
applyTheme(savedTheme2 === "dark");