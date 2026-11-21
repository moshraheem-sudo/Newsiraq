# أخبار العالم | World News

تطبيق أندرويد ومنظومة مفتوحة المصدر لتجميع الأخبار العراقية والعربية والعالمية مباشرةً على مدار الساعة من 25 مصدراً (قنوات تلغرام العامة، خلاصات RSS العالمية، وقوائم X)، مع تحديث تلقائي عبر GitHub Releases وواجهة ويب سريعة على GitHub Pages.

[![GitHub Repo](https://img.shields.io/badge/GitHub-moshraheem--sudo%2FNewsiraq-0f3d2e?logo=github)](https://github.com/moshraheem-sudo/Newsiraq)
[![Live Portal](https://img.shields.io/badge/GitHub%20Pages-Live%20Feed-1b6b4f)](https://moshraheem-sudo.github.io/Newsiraq/)

---

## المميزات الرئيسية | Key Features

- **اسم وهوية موحّدة (أخبار العالم / World News):** واجهة عربية RTL وأخرى إنجليزية LTR قابلة للتبديل الفوري مع ثيم تلقائي وفاتح وداكن.
- **التحديث عبر GitHub داخل التطبيق (GitHub Self-Updater):**
  - فحص تلقائي للإصدار الجديد عند فتح التطبيق وتوفر الاتصال بالإنترنت عبر مقارنة الإصدار المنصّب مع أحدث إصدار على `github.com/moshraheem-sudo/Newsiraq`.
  - زر مخصص داخل **الإعدادات ← تحديث التطبيق عبر GitHub**.
  - تنزيل حزمة التحديث `.apk` داخل التطبيق مع شريط تقدم دقيق بالميغابايت (`MB`) وزر **إيقاف وإلغاء التنزيل** في أي لحظة.
  - زر **تثبيت التحديث (Install)** مع التحقق التلقائي من موافقة النظام لتثبيت التطبيقات (`REQUEST_INSTALL_PACKAGES`).
- **سحب الشاشة للأسفل للتحديث (Pull-to-Refresh):** سحب سلس في واجهة الأخبار لتحديث ملفات المصادر (`config/sources.json`) والمنشورات والبيانات الحية بدقة، مع تحديث دوري كل 10 دقائق.
- **مجمع أخبار حي ومستقر:** سحب مباشر بالتوازي من 25 مصدراً (`LiveFeedFetcher`) مع دمج ذكي مع `news.json` المخزن على GitHub Pages ونسخة محلية سريعة للفتح الفوري بدون اتصال.
- **تصفح وحفظ ووسائط متكاملة:** المفضلة الدائمة عبر Room، مشغل فيديو مدمج وبملء الشاشة، تنزيل الصور والفيديوهات، مشاركة ونسخ الأخبار، وعرض الروابط المذكورة داخل المنشور.

---

## الأتمتة على GitHub Actions

يحتوي المستودع على مسارات عمل جاهزة في `.github/workflows/`:

1. **`update-news.yml` (تحديث الأخبار والبيانات كل 10 دقائق):**
   - يشغّل اختبارات المجمع (`scraper`)، ويجلب أحدث المنشورات من تلغرام وRSS وX.
   - يحدّث ملفات `data/news.json` و`data/sources_status.json` ويرفعها تلقائياً إلى `main`.
   - يبني بوابة الويب `_site` وينشرها تلقائياً على **GitHub Pages** (`https://moshraheem-sudo.github.io/Newsiraq/`).

2. **`release-apk.yml` (بناء ونشر تحديثات التطبيق تلقائياً على Releases):**
   - يعمل تلقائياً عند دفع وسم إصدار جديد مثل `v1.8.0` أو يدوياً عبر **Actions → Release Android APK → Run workflow**.
   - يبني ملف `WorldNews-<tag>.apk` ويرفعه كمرفق رسمي في **GitHub Releases** بحيث يكتشفه تطبيق **أخبار العالم** تلقائياً ويبدأ بتنزيله وتثبيته للمستخدمين.

### خطوات نشر تحديث جديد لمستخدمي التطبيق
1. ارفع `versionCode` و`versionName` في `app/build.gradle.kts` (وحدّث `iraq-news-hub/web/releases.json` و`CHANGELOG.md`).
2. ادفع التغييرات إلى فرع `main`.
3. أنشئ وادفع وسماً جديداً (مثلاً `v1.8.0`):
   ```bash
   git tag v1.8.0
   git push origin v1.8.0
   ```
   سيقوم مسار `release-apk.yml` ببناء ملف APK وإرفاقه بإصدار GitHub تلقائياً، وسيظهر التحديث لجميع المستخدمين فور فتح التطبيق.

---

## روابط البيانات والواجهة | API & Endpoints

- **مستودع GitHub:** `https://github.com/moshraheem-sudo/Newsiraq`
- **بوابة الويب:** `https://moshraheem-sudo.github.io/Newsiraq/`
- **واجهة أخبار JSON:** `https://moshraheem-sudo.github.io/Newsiraq/news.json`
- **حالة المصادر:** `https://moshraheem-sudo.github.io/Newsiraq/sources_status.json`
- **سجل الإصدارات:** `https://moshraheem-sudo.github.io/Newsiraq/releases.json`

---

## هيكل المشروع | Repository Structure

```text
.
├── .github/workflows/
│   ├── update-news.yml          # سحب الأخبار كل 10 دقائق وتحديث GitHub Pages
│   └── release-apk.yml          # بناء APK ونشر GitHub Release تلقائياً للتحديث الذاتي
├── app/                         # تطبيق أندرويد (Kotlin + Jetpack Compose + Material 3)
├── iraq-news-hub/               # المجمع البرمجي Python وبوابة الويب وملفات البيانات
│   ├── config/sources.json      # إعدادات المصادر الـ 25
│   ├── data/                    # news.json و sources_status.json
│   ├── scraper/                 # محرك الجلب والتحليل ومنع التكرار
│   ├── web/                     # واجهة الويب + releases.json
│   └── docs/                    # وثائق الاستخدام والتحديثات
└── README.md
```

---

## الترخيص | License

هذا المشروع مرخص بموجب رخصة [MIT](iraq-news-hub/LICENSE).
