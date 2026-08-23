# REST API ডকুমেন্টেশন (API Documentation)

**Base URL:** `https://api.yourdomain.com/v1/index.php`

---

## 📡 এন্ডপয়েন্ট তালিকা (Endpoints)

### ১. লাইভ রেট লিস্ট
- **পদ্ধতি:** `GET`
- **এন্ডপয়েন্ট:** `?action=rates`
- **রেসপন্স:**
```json
{
  "success": true,
  "message": "Exchange rates retrieved successfully",
  "data": [
    {
      "currency_code": "SAR",
      "country_name_bn": "সৌদি আরব",
      "sending_rate": 32.45,
      "flat_fee": 5.0
    }
  ]
}
```

---

### ২. রেমিট্যান্স কোটেশন হিসাব
- **পদ্ধতি:** `POST`
- **এন্ডপয়েন্ট:** `?action=calculate-quote`
- **বডি (Body):**
```json
{
  "currency_code": "SAR",
  "send_amount": 2000.0,
  "promo_code": "PAISANEW"
}
```
- **রেসপন্স:**
```json
{
  "success": true,
  "data": {
    "send_amount": 2000.0,
    "send_currency": "SAR",
    "rate": 32.45,
    "service_fee": 5.0,
    "discount_amount": 5.0,
    "total_payable": 2000.0,
    "raw_bdt_amount": 64900.0,
    "govt_incentive_bdt": 1622.5,
    "recipient_bdt_amount": 66522.5
  }
}
```

---

### ৩. ওটিপি ও অথেনটিকেশন
- **পদ্ধতি:** `POST`
- **এন্ডপয়েন্ট:** `?action=send-otp` (বডি: `{"phone": "0501234567"}`)
- **এন্ডপয়েন্ট:** `?action=verify-otp` (বডি: `{"phone": "0501234567", "otp": "123456"}`)

---

### ৪. নতুন রেমিট্যান্স তৈরি
- **পদ্ধতি:** `POST`
- **এন্ডপয়েন্ট:** `?action=create-transaction`
- **হেডার:** `Authorization: Bearer <TOKEN>`
- **বডি:**
```json
{
  "recipient_name": "মোঃ রফিকুল ইসলাম",
  "recipient_account": "01712345678",
  "recipient_channel": "বিকাশ",
  "send_amount": 2000.0,
  "send_currency": "SAR",
  "payment_method_id": 1
}
```
