# অ্যান্ড্রয়েড অ্যাপ বিল্ড গাইড (Android Build Guide)

এই নির্দেশিকা অনুসরণ করে **Paisa Remit** অ্যান্ড্রয়েড অ্যাপ্লিকেশনের Debug এবং Signed Release APK / AAB তৈরি করতে পারবেন।

---

## 🛠️ বিল্ড নির্দেশিকা

### ১. ডিবাগ বিল্ড রান করা
```bash
# ইউনিট টেস্ট রান করা
gradle :app:testDebugUnitTest

# ডিবাগ APK বিল্ড করা
gradle :app:assembleDebug
```
তৈরিকৃত APK পাওয়া যাবে: `app/build/outputs/apk/debug/app-debug.apk`

---

### ২. প্রোডাকশন রিলিজ বিল্ড (Signed Release Bundle)
১. Android Studio-এর মেনু থেকে **Build > Generate Signed Bundle / APK** নির্বাচন করুন।
২. **Android App Bundle** অথবা **APK** নির্বাচন করুন।
৩. আপনার Keystore ফাইল ও পাসওয়ার্ড প্রদান করুন।
৪. **Release** বিল্ড ভেরিয়েন্ট নির্বাচন করে **Finish** বাটনে ক্লিক করুন।
৫. গুগল প্লে কনসোলে আপলোড করার জন্য প্রস্তুত `.aab` ফাইল তৈরি হয়ে যাবে।
