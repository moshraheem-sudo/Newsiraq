# نظام إدارة ونشر التحديثات | Release & Updates Workflow

تُخصص هذه الآلية لإصدارات البرمجيات والميزات الجديدة وإصلاحات الأخطاء فقط.
**تنبيه مهم:** عمليات جلب الأخبار التلقائية كل 10 دقائق تقوم بتحديث `data/news.json` فقط ولا تُنشئ Tag أو GitHub Release أو منشور تحديث.

---

## خطوات إطلاق أي تحديث برمجي جديد

1. **تحديد رقم الإصدار الجديد** وفق [Semantic Versioning](https://semver.org/):
   - `PATCH` (`v1.0.1`): إصلاح أخطاء أو تحسينات بسيطة.
   - `MINOR` (`v1.1.0`): إضافة ميزات جديدة أو تحسينات في الواجهة/المجمع.
   - `MAJOR` (`v2.0.0`): تغييرات جذرية في هيكلة البيانات أو الواجهة.
2. **تحديث رقم الإصدار** في `scraper/__init__.py` و `web/releases.json`.
3. **تحديث `CHANGELOG.md`** بملاحظات الإصدار الجديد وتاريخه.
4. **تشغيل الاختبارات والتحقق محلياً** (`python -m unittest discover -s tests -t . -v`).
5. **إنشاء Commit و Git Tag** (مثال: `git tag -a v1.1.0 -m "Release v1.1.0"`).
6. **دفع الكود والـ Tag وإنشاء GitHub Release:**
   ```bash
   git push origin main
   git push origin v1.1.0
   gh release create v1.1.0 --title "Iraq News Hub v1.1.0" --notes-file release_notes.md
   ```
7. **تحديث سجل المنشورات أدناه** لنسخ منشور التحديث ونشره يدوياً على Telegram و Facebook.

---

## قالب منشور تحديث جاهز (Telegram / Facebook)

```text
🆕 تحديث جديد لمشروع مركز أخبار العراق | Iraq News Hub (vX.Y.Z)

تم إطلاق الإصدار vX.Y.Z من المشروع، والذي يتضمن التحسينات والميزات التالية:

✨ الإضافات الجديدة:
• [اكتب الميزة الأولى]
• [اكتب الميزة الثانية]

🛠️ الإصلاحات والتحسينات:
• [اكتب الإصلاح الأول]

🔗 الروابط:
🌐 الموقع: https://USERNAME.github.io/iraq-news-hub/
📡 ملف البيانات: https://USERNAME.github.io/iraq-news-hub/news.json
💻 الإصدار على GitHub: https://github.com/USERNAME/iraq-news-hub/releases/tag/vX.Y.Z

#IraqNewsHub #تحديث #OpenSource
```

---

## سجل منشورات التحديثات المنشورة

### الإصدار v1.0.0 (2026-11-21)
- تم توثيق منشور الإطلاق الأول في `docs/ANNOUNCEMENTS.md`.
- لا توجد تحديثات لاحقة بعد الإصدار الأول حتى الآن.
