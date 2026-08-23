# সিপ্যানেল ও হোস্টিং ডিপ্লয়মেন্ট গাইড (cPanel Deployment Guide)

এই নির্দেশিকা অনুসরণ করে আপনি খুব সহজেই যেকোনো শেয়ার্ড হোস্টিং বা cPanel-এ **Paisa Remit API ও Admin Panel** ডিপ্লয় করতে পারবেন।

---

## 🌐 ধাপ ১: MySQL ডাটাবেজ তৈরি

১. cPanel-এ লগইন করে **MySQL Database Wizard**-এ যান।
২. একটি নতুন ডাটাবেজ নাম দিন (যেমন: `cpaneluser_paisa_db`)।
৩. একটি ডাটাবেজ ইউজার তৈরি করুন এবং শক্তিশালী পাসওয়ার্ড সেট করুন।
৪. উক্ত ইউজারকে সব প্রিভিলেজ (ALL PRIVILEGES) প্রদান করুন।

---

## 📥 ধাপ ২: স্কিমা ইমপোর্ট করা

১. cPanel-এর **phpMyAdmin** ওপেন করুন।
২. সদ্য তৈরিকৃত ডাটাবেজে ক্লিক করুন।
৩. **Import** ট্যাবে গিয়ে `backend/database/schema.sql` এবং `backend/database/seed.sql` আপলোড করে রান করুন।

---

## 📁 ধাপ ৩: ফাইল আপলোড ও কনফিগারেশন

১. cPanel-এর **File Manager**-এ যান।
২. আপনার সাবডোমেন বা `public_html/api` ফোল্ডারে `backend/` ডিরেক্টরির সমস্ত ফাইল ও ফোল্ডার আপলোড করুন।
৩. `backend/config/database.php` ফাইলটি এডিট করে আপনার তৈরিকৃত ডাটাবেজ নাম, ইউজারনেম ও পাসওয়ার্ড বসান।
৪. ব্রাউজারে `https://your-domain.com/api/admin/index.php` ভিজিট করুন।
