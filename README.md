# مركز أخبار العراق | Iraq News Hub

منصة مفتوحة المصدر لتجميع الأخبار العراقية تلقائياً من قنوات Telegram العامة عبر صفحات الويب (`https://t.me/s/` فقط)، وعرضها عبر موقع ويب عربي سريع بتصميم RTL قابل للعرض داخل WebView، مع توفير واجهة بيانات `JSON` منظمة للربط المباشر مع تطبيقات Android و Windows.

Automated Iraqi News Aggregator using Telegram Public Web Channels (`t.me/s/`), Python, GitHub Actions, and GitHub Pages.

---

## المميزات الرئيسية | Key Features

- **استخراج آمن ومستقل بدون Bot API:** يعتمد المجمع على صفحات العرض العام `https://t.me/s/<username>` فقط دون توكنات بوت أو تسجيل دخول أو تجاوز لأي قيود تقنية.
- **قابلية توسع غير محدودة:** أضف مئات القنوات العراقية بسهولة عبر ملف `config/sources.json` دون تعديل أي سطر برمجي.
- **عزل الأعطال (Fault Isolation):** تعطل أي مصدر لا يوقف بقية المصادر، مع تسجيل آخر تحديث ناجح وآخر خطأ لكل مصدر في `data/sources_status.json`.
- **منع التكرار وتوثيق التعديلات:** يعتمد على `(source_username, message_id)` وبصمة المحتوى `content_hash` (SHA-256) للاحتفاظ بالأرشيف التاريخي وتحديث المنشورات المعدلة دون تكرار.
- **أتمتة كل 10 دقائق عبر GitHub Actions:** سحب تلقائي مجدول + تشغيل يدوي + منع التعارض عبر `concurrency` + نشر تلقائي إلى GitHub Pages دون إنشاء إصدارات برمجية عند تحديث الأخبار.
- **واجهة ويب عربية RTL احترافية:** بحث فوري، فلترة حسب المصدر والتصنيف، صفحة تفاصيل كاملة للخبر، مؤشر آخر تحديث ناجح، وضع داكن وفاتح، وصفحة سجل الإصدارات ومعلومات الربط البرمجي.

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
