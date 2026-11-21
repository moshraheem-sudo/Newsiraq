# دليل التشغيل والربط مع التطبيقات | Usage & Integration Guide

## 1. تشغيل المجمع محلياً

```bash
pip install -r requirements.txt
python -m unittest discover -s tests -t . -v
python -m scraper.main
```

خيارات سطر الأوامر المتاحة:
- `--config`: مسار ملف المصادر (الافتراضي: `config/sources.json`).
- `--output`: مسار ملف الأخبار (الافتراضي: `data/news.json`).
- `--status`: مسار ملف حالة المصادر (الافتراضي: `data/sources_status.json`).
- `--limit-per-source`: تحديد الحد الأقصى للمنشورات المستخرجة لكل مصدر.
- `--dry-run`: اختبار الجلب والدمج دون الكتابة على القرص.

## 2. إضافة مصادر جديدة

أضف القناة داخل `config/sources.json`:
- تأكد من أن القناة **عامة** وتستخدم الرابط بصيغة `https://t.me/s/<username>`.
- يمكنك إيقاف أي قناة مؤقتاً بتغيير `"enabled": false` دون حذفها.

## 3. الربط مع تطبيق أندرويد (Kotlin / Jetpack Compose)

### أ. عبر WebView:
```kotlin
webView.settings.javaScriptEnabled = true
webView.settings.domStorageEnabled = true
webView.loadUrl("https://moshraheem-sudo.github.io/Newsiraq/")
```

### ب. عبر HTTP لقراءة `news.json`:
```kotlin
// GET https://moshraheem-sudo.github.io/Newsiraq/news.json
// يحتوي الجذر على الحقول:
// schema_version, last_successful_update, total_count, sources, items
```

## 4. الربط مع تطبيق ويندوز (C# / .NET)

```csharp
using var http = new HttpClient();
var json = await http.GetStringAsync("https://moshraheem-sudo.github.io/Newsiraq/news.json");
// تحليل json عبر System.Text.Json لقراءة مصفوفة items
```

أو استخدام `WebView2` لعرض الواجهة مباشرة على الرابط:
`https://moshraheem-sudo.github.io/Newsiraq/`

## 5. التحديث الذاتي لتطبيق أندرويد عبر GitHub Releases

- يفحص التطبيق تلقائياً عند التشغيل (وعبر زر **تحديث التطبيق** في الإعدادات) نقطة API:
  `https://api.github.com/repos/moshraheem-sudo/Newsiraq/releases/latest`
- عند وجود إصدار أحدث مرفق بملف `.apk`، يعرض التطبيق شريط التنزيل بالميغابايت مع خيار الإيقاف/الإلغاء ثم التثبيت عبر موافقة النظام.
- لإصدار تحديث جديد تلقائياً عبر GitHub Actions، ادفع وسماً يبدأ بـ `v` (مثل `v1.8.0`) أو شغّل مسار `release-apk.yml` يدوياً من تبويب **Actions**.

