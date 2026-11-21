# أخبار العالم | World News (Newsiraq Hub)

منصة وتطبيق أندرويد مفتوح المصدر لتجميع الأخبار العراقية والعربية والعالمية تلقائياً على مدار الساعة من 25 مصدراً (قنوات Telegram العامة، خلاصات RSS، وقوائم X)، وعرضها عبر تطبيق أندرويد حديث وبوابة ويب عربية سريعة بتصميم RTL على GitHub Pages، مع نظام تحديث تلقائي للتطبيق عبر GitHub Releases.

Automated Iraqi, Arab & World News Aggregator using Telegram Public Web Channels (`t.me/s/`), RSS, X Lists, Python, GitHub Actions, GitHub Pages, and Android Jetpack Compose.

---

## المميزات الرئيسية | Key Features

- **تطبيق أندرويد (أخبار العالم / World News):** واجهة Material 3 متجاوبة تدعم العربية RTL والإنجليزية LTR، وسحب الشاشة للأسفل للتحديث (`Pull-to-Refresh`) لتحديث ملفات المصادر والمنشورات والأخبار، مع تحديث دوري تلقائي كل 10 دقائق.
- **التحديث الذاتي عبر GitHub داخل التطبيق:** فحص تلقائي عند فتح التطبيق مع زر مخصص في الإعدادات لمقارنة الإصدار المنصّب مع أحدث إصدار على `github.com/moshraheem-sudo/Newsiraq`، وتنزيل ملف `.apk` داخل التطبيق بشريط تقدم بالميغابايت وزر إيقاف وإلغاء، ثم التثبيت المباشر بموافقات النظام.
- **استخراج آمن ومستقل بدون Bot API:** يعتمد المجمع على صفحات العرض العام `https://t.me/s/<username>` وخلاصات RSS وقوائم X دون توكنات بوت أو مفاتيح مدفوعة.
- **قابلية توسع غير محدودة:** أضف مئات القنوات بسهولة عبر ملف `config/sources.json` دون تعديل أي سطر برمجي (ويتزامن التطبيق تلقائياً مع التعديلات عند التحديث).
- **عزل الأعطال ومنع التكرار:** تعطل أي مصدر لا يوقف بقية المصادر، مع تسجيل حالة كل مصدر في `data/sources_status.json` وبصمة المحتوى `content_hash` (SHA-256).
- **أتمتة كل 10 دقائق عبر GitHub Actions:** سحب تلقائي مجدول + تشغيل يدوي + نشر تلقائي إلى GitHub Pages + مسار عمل لبناء وإصدار تحديثات APK على GitHub Releases.

---

## هيكل المشروع | Project Structure

```text
iraq-news-hub/
├── .github/
│   └── workflows/
│       ├── update-news.yml      # جلب الأخبار كل 10 دقائق + تحديث Pages
│       └── deploy-pages.yml     # نشر الواجهة عند تعديل web أو data
├── config/
│   └── sources.json             # قائمة مصادر الأخبار القابلة للتوسع
├── data/
│   ├── news.json                # قاعدة بيانات الأخبار المجمعة
│   └── sources_status.json      # حالة كل مصدر وآخر نجاح/خطأ
├── docs/
│   ├── ANNOUNCEMENTS.md         # منشور إطلاق المشروع لوسائل التواصل
│   ├── UPDATES.md               # آلية وقالب منشورات التحديثات المستقبلية
│   └── USAGE.md                 # دليل التشغيل والربط مع التطبيقات
├── scraper/
│   ├── __init__.py
│   ├── config.py                # تحميل الإعدادات والتحقق من روابط t.me/s/
│   ├── fetcher.py               # سحب HTML بشكل مستقل وآمن
│   ├── parser.py                # استخراج النصوص والصور والوسائط والروابط
│   ├── storage.py               # الدمج ومنع التكرار وحساب content_hash
│   └── main.py                  # نقطة التشغيل الرئيسية
├── scripts/
│   └── build_site.py            # تجهيز مجلد _site للنشر على GitHub Pages
├── tests/
│   └── test_scraper.py          # اختبارات الوحدة للمحلل والدمج
├── web/
│   ├── index.html               # الواجهة العربية RTL
│   ├── styles.css               # التصميم المتجاوب والداعم لـ WebView
│   ├── app.js                   # منطق العرض والبحث والفلترة والتفاصيل
│   └── releases.json            # سجل الإصدارات المعروض داخل الموقع
├── CHANGELOG.md
├── LICENSE
├── README.md
└── requirements.txt
```

---

## التشغيل المحلي | Local Setup

```bash
python3 -m venv .venv
source .venv/bin/activate
pip install -r requirements.txt

# تشغيل اختبارات الوحدة
python -m unittest discover -s tests -t . -v

# تشغيل سحب الأخبار
python -m scraper.main

# تجهيز وتشغيل الموقع محلياً
python scripts/build_site.py
python -m http.server 8080 --directory _site
```

---

## إضافة قناة جديدة | Adding a New Source

افتح ملف `config/sources.json` وأضف كائناً جديداً داخل مصفوفة `"sources"` بصيغة `https://t.me/s/<username>`:

```json
{
  "id": "new_channel",
  "name": "اسم القناة بالعربية",
  "username": "ChannelUsername",
  "url": "https://t.me/s/ChannelUsername",
  "category": "أخبار عامة",
  "language": "ar",
  "enabled": true,
  "notes": "وصف مختصر للمصدر"
}
```

---

## هيكل عنصر الخبر في `data/news.json` | JSON Schema

```json
{
  "id": "inainaiq_162706",
  "source_id": "ina",
  "source_name": "وكالة الأنباء العراقية (واع)",
  "source_username": "inainaiq",
  "message_id": 162706,
  "category": "أخبار رسمية",
  "title": "العنوان المستخرج مباشرة من بداية النص",
  "content": "النص الكامل للمنشور...",
  "published_at": "2026-11-16T18:58:05Z",
  "fetched_at": "2026-11-21T04:17:20Z",
  "updated_at": "2026-11-21T04:17:20Z",
  "original_url": "https://t.me/inainaiq/162706",
  "images": ["https://..."],
  "media": [{"type": "photo", "url": "https://...", "thumbnail": "https://...", "title": ""}],
  "links": ["https://..."],
  "is_edited": false,
  "content_hash": "sha256..."
}
```

---

## الربط مع تطبيقات أندرويد وويندوز

- **رابط الموقع (WebView):** `https://moshraheem-sudo.github.io/Newsiraq/`
- **رابط البيانات (HTTP JSON):** `https://moshraheem-sudo.github.io/Newsiraq/news.json`
- راجع ملف `docs/USAGE.md` للحصول على أمثلة جاهزة بلغة Kotlin (Android) و C# (Windows).

---

## الترخيص | License

هذا المشروع مرخص بموجب رخصة [MIT](LICENSE).
