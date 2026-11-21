(function () {
  "use strict";

  const state = {
    newsData: null,
    statusData: null,
    releasesData: null,
    searchQuery: "",
    selectedSource: "all",
    selectedCategory: "all",
    loading: true,
    error: null,
  };

  const appRoot = document.getElementById("appRoot");
  const lastSuccessText = document.getElementById("lastSuccessText");
  const totalNewsBadge = document.getElementById("totalNewsBadge");
  const activeSourcesBadge = document.getElementById("activeSourcesBadge");
  const statusDot = document.getElementById("statusDot");
  const footerVersion = document.getElementById("footerVersion");

  function initTheme() {
    const saved = localStorage.getItem("inh_theme");
    const prefersDark =
      window.matchMedia && window.matchMedia("(prefers-color-scheme: dark)").matches;
    const theme = saved || (prefersDark ? "dark" : "light");
    document.documentElement.setAttribute("data-theme", theme);
    updateThemeButton(theme);
  }

  function updateThemeButton(theme) {
    const btn = document.getElementById("themeToggleBtn");
    if (btn) btn.textContent = theme === "dark" ? "☀" : "☾";
  }

  function toggleTheme() {
    const current = document.documentElement.getAttribute("data-theme") || "light";
    const next = current === "dark" ? "light" : "dark";
    document.documentElement.setAttribute("data-theme", next);
    localStorage.setItem("inh_theme", next);
    updateThemeButton(next);
  }

  function escapeHtml(str) {
    return String(str || "")
      .replace(/&/g, "&amp;")
      .replace(/</g, "&lt;")
      .replace(/>/g, "&gt;")
      .replace(/"/g, "&quot;")
      .replace(/'/g, "&#39;");
  }

  function formatArabicDate(isoString) {
    if (!isoString) return "غير متوفر";
    const date = new Date(isoString);
    if (Number.isNaN(date.getTime())) return isoString;
    try {
      return new Intl.DateTimeFormat("ar-IQ", {
        dateStyle: "medium",
        timeStyle: "short",
        timeZone: "Asia/Baghdad",
      }).format(date);
    } catch (_) {
      return date.toLocaleString("ar");
    }
  }

  function formatRelativeArabic(isoString) {
    if (!isoString) return "";
    const diffMs = Date.now() - new Date(isoString).getTime();
    if (Number.isNaN(diffMs)) return "";
    const mins = Math.floor(diffMs / 60000);
    if (mins < 1) return "الآن";
    if (mins < 60) return `منذ ${mins} دقيقة`;
    const hours = Math.floor(mins / 60);
    if (hours < 24) return `منذ ${hours} ساعة`;
    const days = Math.floor(hours / 24);
    return `منذ ${days} يوم`;
  }

  async function fetchJsonWithFallback(urls) {
    for (const url of urls) {
      try {
        const res = await fetch(url + "?t=" + Date.now(), { cache: "no-store" });
        if (res.ok) {
          return await res.json();
        }
      } catch (_) {
        // try next fallback URL
      }
    }
    return null;
  }

  async function loadAllData() {
    state.loading = true;
    state.error = null;
    render();

    const [news, status, releases] = await Promise.all([
      fetchJsonWithFallback(["./news.json", "./data/news.json", "../data/news.json"]),
      fetchJsonWithFallback([
        "./sources_status.json",
        "./data/sources_status.json",
        "../data/sources_status.json",
      ]),
      fetchJsonWithFallback(["./releases.json"]),
    ]);

    if (!news) {
      state.loading = false;
      state.error = "تعذر تحميل ملف الأخبار (news.json). تأكد من تشغيل المجمع أو توفر الاتصال.";
      render();
      return;
    }

    state.newsData = Array.isArray(news) ? { items: news, sources: [] } : news;
    state.statusData = status;
    state.releasesData = releases;
    state.loading = false;

    updateHeaderStatus();
    render();
  }

  function updateHeaderStatus() {
    const items = (state.newsData && state.newsData.items) || [];
    const lastSuccess =
      (state.statusData && state.statusData.last_successful_update) ||
      (state.newsData && state.newsData.last_successful_update) ||
      "";

    lastSuccessText.textContent = lastSuccess
      ? `${formatArabicDate(lastSuccess)} (${formatRelativeArabic(lastSuccess)})`
      : "غير متاح بعد";

    totalNewsBadge.textContent = `${items.length} خبر محفوظ`;

    const statusSources = (state.statusData && state.statusData.sources) || {};
    const sourceValues = Object.values(statusSources);
    const okCount = sourceValues.filter((s) => s.status === "ok").length;
    const enabledCount = sourceValues.filter((s) => s.enabled !== false).length;
    activeSourcesBadge.textContent =
      enabledCount > 0 ? `${okCount}/${enabledCount} مصدر نشط` : `${(state.newsData.sources || []).length} مصادر`;

    if (enabledCount > 0 && okCount < enabledCount) {
      statusDot.classList.add("warn");
    } else {
      statusDot.classList.remove("warn");
    }

    if (state.releasesData && state.releasesData.current_version) {
      footerVersion.textContent = "v" + state.releasesData.current_version;
    }
  }

  function getRoute() {
    const hash = window.location.hash || "#/";
    if (hash.startsWith("#/news/")) {
      return { name: "detail", id: decodeURIComponent(hash.slice("#/news/".length)) };
    }
    if (hash === "#/about") {
      return { name: "about" };
    }
    if (hash === "#/updates") {
      return { name: "updates" };
    }
    return { name: "home" };
  }

  function updateActiveNav(routeName) {
    document.querySelectorAll(".nav-link").forEach((el) => {
      const target = el.getAttribute("data-route");
      el.classList.toggle("active", target === routeName);
    });
  }

  function getFilteredItems() {
    const items = ((state.newsData && state.newsData.items) || []).slice();
    items.sort((a, b) => {
      const da = new Date(a.published_at || 0).getTime();
      const db = new Date(b.published_at || 0).getTime();
      if (db !== da) return db - da;
      return (b.message_id || 0) - (a.message_id || 0);
    });

    const q = state.searchQuery.trim().toLowerCase();
    return items.filter((item) => {
      if (state.selectedSource !== "all" && item.source_username !== state.selectedSource) {
        return false;
      }
      if (state.selectedCategory !== "all" && item.category !== state.selectedCategory) {
        return false;
      }
      if (q) {
        const haystack = [item.title, item.content, item.source_name, item.category]
          .join(" ")
          .toLowerCase();
        if (!haystack.includes(q)) return false;
      }
      return true;
    });
  }

  function renderHome() {
    const allItems = (state.newsData && state.newsData.items) || [];
    const filtered = getFilteredItems();

    const sourcesMap = new Map();
    allItems.forEach((it) => {
      if (it.source_username && !sourcesMap.has(it.source_username)) {
        sourcesMap.set(it.source_username, it.source_name || it.source_username);
      }
    });

    const categories = Array.from(
      new Set(allItems.map((it) => it.category).filter(Boolean))
    );

    const sourceChips = [
      `<button class="chip ${state.selectedSource === "all" ? "active" : ""}" data-source="all">جميع المصادر (${allItems.length})</button>`,
      ...Array.from(sourcesMap.entries()).map(([username, name]) => {
        const count = allItems.filter((i) => i.source_username === username).length;
        return `<button class="chip ${state.selectedSource === username ? "active" : ""}" data-source="${escapeHtml(username)}">${escapeHtml(name)} (${count})</button>`;
      }),
    ].join("");

    const categoryChips = [
      `<button class="chip ${state.selectedCategory === "all" ? "active" : ""}" data-category="all">كل التصنيفات</button>`,
      ...categories.map(
        (cat) =>
          `<button class="chip ${state.selectedCategory === cat ? "active" : ""}" data-category="${escapeHtml(cat)}">${escapeHtml(cat)}</button>`
      ),
    ].join("");

    const cardsHtml =
      filtered.length === 0
        ? `<div class="empty-box"><h3>لا توجد نتائج مطابقة</h3><p>جرّب تعديل كلمات البحث أو اختيار مصدر آخر.</p></div>`
        : `<section class="news-grid">${filtered.map(renderNewsCard).join("")}</section>`;

    appRoot.innerHTML = `
      <section class="controls-panel" aria-label="البحث والفلترة">
        <div class="search-row">
          <input
            id="searchInput"
            class="search-input"
            type="search"
            placeholder="ابحث في عناوين الأخبار ونصوصها والمصادر..."
            value="${escapeHtml(state.searchQuery)}"
            aria-label="البحث عن الأخبار"
          />
          <button id="clearFiltersBtn" class="clear-btn" type="button">إعادة ضبط</button>
        </div>

        <div class="filter-group">
          <span class="filter-label">تصفية حسب المصدر:</span>
          <div class="chips-row">${sourceChips}</div>
        </div>

        <div class="filter-group">
          <span class="filter-label">التصنيف:</span>
          <div class="chips-row">${categoryChips}</div>
        </div>
      </section>

      ${cardsHtml}
    `;

    const searchInput = document.getElementById("searchInput");
    if (searchInput) {
      searchInput.addEventListener("input", (e) => {
        state.searchQuery = e.target.value;
        const caret = e.target.selectionStart;
        renderHome();
        const fresh = document.getElementById("searchInput");
        if (fresh) {
          fresh.focus();
          try {
            fresh.setSelectionRange(caret, caret);
          } catch (_) {}
        }
      });
    }

    const clearBtn = document.getElementById("clearFiltersBtn");
    if (clearBtn) {
      clearBtn.addEventListener("click", () => {
        state.searchQuery = "";
        state.selectedSource = "all";
        state.selectedCategory = "all";
        renderHome();
      });
    }

    document.querySelectorAll("[data-source]").forEach((btn) => {
      btn.addEventListener("click", () => {
        state.selectedSource = btn.getAttribute("data-source");
        renderHome();
      });
    });

    document.querySelectorAll("[data-category]").forEach((btn) => {
      btn.addEventListener("click", () => {
        state.selectedCategory = btn.getAttribute("data-category");
        renderHome();
      });
    });
  }

  function renderNewsCard(item) {
    const images = item.images || [];
    const media = item.media || [];
    const videos = media.filter((m) => m.type === "video" && m.url);
    const firstVideo = videos[0] || null;
    const firstImage = images[0] || "";
    const titleText = item.title || "منشور وسائط بدون نص";
    const contentText = item.content || "";
    const editedBadge = item.is_edited ? `<span class="edited-badge">معدّل</span>` : "";

    let mediaHtml = "";
    if (firstVideo) {
      const posterAttr = firstVideo.thumbnail ? `poster="${escapeHtml(firstVideo.thumbnail)}"` : "";
      mediaHtml = `
        <div class="card-media">
          <video controls preload="metadata" playsinline ${posterAttr} style="width:100%;height:100%;object-fit:cover;display:block;">
            <source src="${escapeHtml(firstVideo.url)}" />
          </video>
          <span class="media-count-badge">${videos.length > 1 ? `▶ ${videos.length} فيديو` : "▶ فيديو"}</span>
        </div>`;
    } else if (firstImage) {
      mediaHtml = `
        <a href="#/news/${encodeURIComponent(item.id)}" class="card-media">
          <img src="${escapeHtml(firstImage)}" alt="${escapeHtml(titleText)}" loading="lazy" />
          ${images.length > 1 ? `<span class="media-count-badge">+${images.length} صور</span>` : ""}
        </a>`;
    }

    return `
      <article class="news-card">
        ${mediaHtml}
        <div class="card-body">
          <div class="card-meta-top">
            <span class="source-tag">${escapeHtml(item.source_name)}</span>
            <span class="category-tag">${escapeHtml(item.category || "")}</span>
            ${editedBadge}
          </div>
          <h2 class="card-title">
            <a href="#/news/${encodeURIComponent(item.id)}" style="color: inherit;">
              ${escapeHtml(titleText)}
            </a>
          </h2>
          ${contentText ? `<p class="card-excerpt">${escapeHtml(contentText)}</p>` : ""}
          <div class="card-footer">
            <time datetime="${escapeHtml(item.published_at)}" title="${escapeHtml(formatArabicDate(item.published_at))}">
              ${escapeHtml(formatArabicDate(item.published_at))}
            </time>
            <div class="card-actions">
              <a class="btn btn-primary" href="#/news/${encodeURIComponent(item.id)}">التفاصيل</a>
              <a class="btn btn-outline" href="${escapeHtml(item.original_url)}" target="_blank" rel="noopener noreferrer">الأصل ↗</a>
            </div>
          </div>
        </div>
      </article>
    `;
  }

  function renderDetail(itemId) {
    const items = (state.newsData && state.newsData.items) || [];
    const item = items.find((i) => i.id === itemId);

    if (!item) {
      appRoot.innerHTML = `
        <div class="empty-box">
          <h2>الخبر غير موجود</h2>
          <p>ربما تم تغيير الرابط أو لم يتم تحميل هذا الخبر بعد.</p>
          <a class="btn btn-primary" href="#/">العودة إلى الأخبار</a>
        </div>
      `;
      return;
    }

    const images = item.images || [];
    const media = item.media || [];
    const links = item.links || [];
    const videos = media.filter((m) => m.type === "video" && m.url);

    const galleryHtml =
      images.length > 0
        ? `<div class="gallery-grid">${images
            .map(
              (src) =>
                `<a href="${escapeHtml(src)}" target="_blank" rel="noopener noreferrer"><img src="${escapeHtml(src)}" alt="صورة مرفقة بالخبر" loading="lazy" /></a>`
            )
            .join("")}</div>`
        : "";

    const videosHtml =
      videos.length > 0
        ? videos
            .map(
              (v) => `
          <div class="media-box">
            <strong>مقطع فيديو مرفق</strong>
            <video controls preload="metadata" ${v.thumbnail ? `poster="${escapeHtml(v.thumbnail)}"` : ""}>
              <source src="${escapeHtml(v.url)}" />
            </video>
          </div>`
            )
            .join("")
        : "";

    const linksHtml =
      links.length > 0
        ? `<div class="media-box">
            <strong>روابط مذكورة في المنشور:</strong>
            <ul>
              ${links
                .map(
                  (lnk) =>
                    `<li><a href="${escapeHtml(lnk)}" target="_blank" rel="noopener noreferrer">${escapeHtml(lnk)}</a></li>`
                )
                .join("")}
            </ul>
          </div>`
        : "";

    appRoot.innerHTML = `
      <div style="margin-bottom: 14px;">
        <a href="#/" class="btn btn-outline">→ العودة إلى قائمة الأخبار</a>
      </div>

      <article class="detail-card">
        <div class="detail-header">
          <div class="card-meta-top">
            <span class="source-tag">${escapeHtml(item.source_name)} (@${escapeHtml(item.source_username)})</span>
            <span class="category-tag">${escapeHtml(item.category || "")}</span>
            ${item.is_edited ? `<span class="edited-badge">منشور معدّل</span>` : ""}
          </div>
          <h2 class="detail-title">${escapeHtml(item.title || "منشور وسائط")}</h2>
          <div style="color: var(--text-muted); font-size: 0.9rem;">
            تاريخ النشر: <strong>${escapeHtml(formatArabicDate(item.published_at))}</strong>
            • رقم المنشور: <code>${escapeHtml(item.message_id)}</code>
          </div>
        </div>

        ${galleryHtml}
        ${videosHtml}

        <div class="detail-content">${escapeHtml(item.content || "لا يوجد نص مرافق في المصدر الأصلي.")}</div>

        ${linksHtml}

        <div style="margin-top: 20px; display: flex; gap: 10px; flex-wrap: wrap;">
          <a class="btn btn-primary" href="${escapeHtml(item.original_url)}" target="_blank" rel="noopener noreferrer">
            فتح المنشور الأصلي على تلغرام ↗
          </a>
          <a class="btn btn-outline" href="#/">تصفح بقية الأخبار</a>
        </div>
      </article>

      <section class="info-card">
        <h3 style="margin-top: 0;">البيانات الوصفية للخبر</h3>
        <table class="meta-table">
          <tr><th>المعرّف الفريد</th><td><code>${escapeHtml(item.id)}</code></td></tr>
          <tr><th>وقت السحب الأول</th><td>${escapeHtml(formatArabicDate(item.fetched_at))}</td></tr>
          <tr><th>آخر تحديث</th><td>${escapeHtml(formatArabicDate(item.updated_at))}</td></tr>
          <tr><th>بصمة المحتوى (SHA-256)</th><td><code style="font-size:0.75rem;">${escapeHtml(item.content_hash)}</code></td></tr>
        </table>
      </section>
    `;
  }

  function renderAbout() {
    const baseUrl = window.location.origin + window.location.pathname.replace(/\/[^/]*$/, "/");
    const jsonUrl = baseUrl + "news.json";
    const statusUrl = baseUrl + "sources_status.json";

    const statusSources = Object.values((state.statusData && state.statusData.sources) || {});
    const statusRows =
      statusSources.length > 0
        ? statusSources
            .map(
              (s) => `
          <tr>
            <td>${escapeHtml(s.name)}</td>
            <td><code>@${escapeHtml(s.username)}</code></td>
            <td>${s.status === "ok" ? `<span class="badge-ok">يعمل</span>` : `<span class="badge-err">خطأ</span>`}</td>
            <td>${escapeHtml(formatArabicDate(s.last_success_at))}</td>
            <td>${escapeHtml(s.posts_extracted ?? 0)}</td>
            <td>${escapeHtml(s.last_error || "—")}</td>
          </tr>`
            )
            .join("")
        : `<tr><td colspan="6">لا توجد بيانات حالة متاحة حالياً.</td></tr>`;

    appRoot.innerHTML = `
      <section class="info-card">
        <h2 style="margin-top: 0;">عن مشروع مركز أخبار العراق (Iraq News Hub)</h2>
        <p>
          مشروع مفتوح المصدر لجمع الأخبار العراقية تلقائياً من صفحات العرض العام لقنوات تلغرام
          (<code>t.me/s/</code>) باستخدام Python و GitHub Actions دون الحاجة إلى Bot API أو مفاتيح سرية،
          مع تقديم واجهة ويب عربية سريعة متوافقة مع WebView وواجهة JSON جاهزة للتكامل مع تطبيقات أندرويد وويندوز.
        </p>
      </section>

      <section class="info-card">
        <h3 style="margin-top: 0;">روابط التكامل البرمجي (API / WebView)</h3>
        <p>يمكن لتطبيقاتك قراءة الأخبار مباشرة عبر HTTP GET:</p>
        <div class="code-block">Web / WebView URL: ${escapeHtml(baseUrl)}
JSON Feed URL:     ${escapeHtml(jsonUrl)}
Sources Status:    ${escapeHtml(statusUrl)}</div>
      </section>

      <section class="info-card">
        <h3 style="margin-top: 0;">حالة مصادر الأخبار</h3>
        <div style="overflow-x: auto;">
          <table class="meta-table">
            <thead>
              <tr>
                <th>المصدر</th>
                <th>المعرّف</th>
                <th>الحالة</th>
                <th>آخر تحديث ناجح</th>
                <th>المنشورات المستخرجة</th>
                <th>آخر خطأ</th>
              </tr>
            </thead>
            <tbody>${statusRows}</tbody>
          </table>
        </div>
      </section>
    `;
  }

  function renderUpdates() {
    const releases = (state.releasesData && state.releasesData.releases) || [];
    const cards =
      releases.length > 0
        ? releases
            .map(
              (rel) => `
          <article class="info-card">
            <div class="card-meta-top" style="margin-bottom: 10px;">
              <span class="source-tag">الإصدار v${escapeHtml(rel.version)}</span>
              <span style="color: var(--text-muted); font-size: 0.88rem;">${escapeHtml(rel.date)}</span>
            </div>
            <h3 style="margin: 0 0 8px;">${escapeHtml(rel.title)}</h3>
            <p style="margin: 0 0 12px; color: var(--text-muted);">${escapeHtml(rel.summary)}</p>
            <ul>
              ${(rel.highlights || []).map((h) => `<li>${escapeHtml(h)}</li>`).join("")}
            </ul>
          </article>`
            )
            .join("")
        : `<div class="empty-box"><p>لا توجد إصدارات مسجلة بعد.</p></div>`;

    appRoot.innerHTML = `
      <section class="info-card">
        <h2 style="margin-top: 0;">سجل إصدارات وتحديثات النظام</h2>
        <p style="margin-bottom: 0; color: var(--text-muted);">
          يُوثّق هذا السجل تحديثات البرمجيات والميزات الجديدة فقط (عمليات سحب الأخبار التلقائية كل 10 دقائق لا تُنشئ إصداراً جديداً).
        </p>
      </section>
      ${cards}
    `;
  }

  function render() {
    const route = getRoute();
    updateActiveNav(route.name === "detail" ? "home" : route.name);

    if (state.loading) {
      appRoot.innerHTML = `
        <div class="loading-box">
          <div class="spinner" aria-hidden="true"></div>
          <p>جاري تحميل أحدث الأخبار العراقية...</p>
        </div>
      `;
      return;
    }

    if (state.error) {
      appRoot.innerHTML = `
        <div class="empty-box">
          <h3>تعذر تحميل البيانات</h3>
          <p>${escapeHtml(state.error)}</p>
          <button id="retryBtn" class="btn btn-primary" type="button">إعادة المحاولة</button>
        </div>
      `;
      const retry = document.getElementById("retryBtn");
      if (retry) retry.addEventListener("click", loadAllData);
      return;
    }

    if (route.name === "detail") {
      renderDetail(route.id);
    } else if (route.name === "about") {
      renderAbout();
    } else if (route.name === "updates") {
      renderUpdates();
    } else {
      renderHome();
    }
  }

  document.getElementById("themeToggleBtn").addEventListener("click", toggleTheme);
  document.getElementById("refreshBtn").addEventListener("click", loadAllData);
  window.addEventListener("hashchange", render);

  initTheme();
  loadAllData();
})();
