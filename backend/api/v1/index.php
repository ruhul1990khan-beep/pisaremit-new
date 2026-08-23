<?php
/**
 * Paisa Remit - Central REST API v1 Router & Dispatcher
 */

require_once __DIR__ . '/../../config/app.php';

$requestUri = $_SERVER['REQUEST_URI'] ?? '/';
$requestMethod = $_SERVER['REQUEST_METHOD'] ?? 'GET';

// Parse query params and path
$parsedUrl = parse_url($requestUri);
$path = trim($parsedUrl['path'], '/');
$body = json_decode(file_get_contents('php://input'), true) ?? $_POST;
$headers = getallheaders();
$authHeader = $headers['Authorization'] ?? $headers['authorization'] ?? null;

$db = Database::getConnection();

// Basic Router
$action = $_GET['action'] ?? '';

switch ($action) {
    // -------------------------------------------------------------
    // 1. HEALTH & METADATA
    // -------------------------------------------------------------
    case 'health':
        AppResponse::success([
            'service' => 'Paisa Remit API Gateway',
            'version' => '1.0.0',
            'status' => 'ONLINE',
            'timestamp' => date('c')
        ], 'API is running normally');
        break;

    // -------------------------------------------------------------
    // 2. EXCHANGE RATES
    // -------------------------------------------------------------
    case 'rates':
        $stmt = $db->query("SELECT * FROM exchange_rates WHERE is_active = 1 ORDER BY display_order ASC");
        $rates = $stmt->fetchAll();
        AppResponse::success($rates, 'Exchange rates retrieved successfully');
        break;

    case 'calculate-quote':
        $currencyCode = $body['currency_code'] ?? 'SAR';
        $sendAmount = floatval($body['send_amount'] ?? 1000.0);
        $promoCode = trim($body['promo_code'] ?? '');

        $stmt = $db->prepare("SELECT * FROM exchange_rates WHERE currency_code = ? AND is_active = 1 LIMIT 1");
        $stmt->execute([$currencyCode]);
        $rate = $stmt->fetch();

        if (!$rate) {
            AppResponse::error('Invalid currency code selected', 404);
        }

        $sendingRate = floatval($rate['sending_rate']);
        $serviceFee = floatval($rate['flat_fee']);
        $discountAmount = 0.00;

        if (!empty($promoCode)) {
            $pStmt = $db->prepare("SELECT * FROM offers WHERE promo_code = ? AND is_active = 1 AND valid_until >= CURDATE() LIMIT 1");
            $pStmt->execute([$promoCode]);
            $offer = $pStmt->fetch();
            if ($offer) {
                $discountAmount = ($serviceFee * floatval($offer['discount_percent'])) / 100.0;
                if ($discountAmount > floatval($offer['max_discount']) && floatval($offer['max_discount']) > 0) {
                    $discountAmount = floatval($offer['max_discount']);
                }
            }
        }

        $totalPayable = ($sendAmount + $serviceFee) - $discountAmount;
        $rawBdt = $sendAmount * $sendingRate;
        $govtIncentiveBdt = $rawBdt * 0.025; // 2.5% Bangladesh Govt incentive
        $recipientBdt = $rawBdt + $govtIncentiveBdt;

        AppResponse::success([
            'send_amount' => $sendAmount,
            'send_currency' => $currencyCode,
            'rate' => $sendingRate,
            'service_fee' => $serviceFee,
            'discount_amount' => $discountAmount,
            'total_payable' => $totalPayable,
            'raw_bdt_amount' => $rawBdt,
            'govt_incentive_bdt' => $govtIncentiveBdt,
            'recipient_bdt_amount' => $recipientBdt,
            'country_name' => $rate['country_name_bn'],
            'flag_emoji' => $rate['flag_emoji']
        ], 'Quote calculated successfully');
        break;

    // -------------------------------------------------------------
    // 3. AUTH & OTP
    // -------------------------------------------------------------
    case 'send-otp':
        $phone = trim($body['phone'] ?? '');
        if (empty($phone)) {
            AppResponse::error('Phone number is required');
        }
        $otp = '123456'; // Default demo / SMS gateway dispatcher
        $expiresAt = date('Y-m-d H:i:s', time() + 300);

        $stmt = $db->prepare("INSERT INTO otp_requests (phone, otp_code, purpose, expires_at, ip_address) VALUES (?, ?, 'LOGIN', ?, ?)");
        $stmt->execute([$phone, $otp, $expiresAt, $_SERVER['REMOTE_ADDR'] ?? '127.0.0.1']);

        AppResponse::success([
            'phone' => $phone,
            'expires_in_seconds' => 300,
            'test_otp_hint' => '123456'
        ], 'OTP sent successfully to ' . $phone);
        break;

    case 'verify-otp':
        $phone = trim($body['phone'] ?? '');
        $otp = trim($body['otp'] ?? '');
        $deviceId = trim($body['device_id'] ?? 'device-' . time());
        $modelName = trim($body['model_name'] ?? 'Android Device');

        if (empty($phone) || empty($otp)) {
            AppResponse::error('Phone and OTP are required');
        }

        // Verify OTP
        if ($otp !== '123456') {
            $stmt = $db->prepare("SELECT * FROM otp_requests WHERE phone = ? AND otp_code = ? AND is_verified = 0 AND expires_at >= NOW() ORDER BY id DESC LIMIT 1");
            $stmt->execute([$phone, $otp]);
            $req = $stmt->fetch();
            if (!$req) {
                AppResponse::error('Invalid or expired OTP code');
            }
            $db->prepare("UPDATE otp_requests SET is_verified = 1 WHERE id = ?")->execute([$req['id']]);
        }

        // Find or create user
        $uStmt = $db->prepare("SELECT * FROM users WHERE phone = ? LIMIT 1");
        $uStmt->execute([$phone]);
        $user = $uStmt->fetch();

        if (!$user) {
            $userUuid = 'USR-' . strtoupper(substr(md5(uniqid()), 0, 10));
            $inStmt = $db->prepare("INSERT INTO users (user_uuid, phone, kyc_status, account_status) VALUES (?, ?, 'VERIFIED', 'ACTIVE')");
            $inStmt->execute([$userUuid, $phone]);
            $userId = $db->lastInsertId();

            // Create profile
            $db->prepare("INSERT INTO user_profiles (user_id, full_name_en, full_name_bn, occupation) VALUES (?, 'Mohammed Tareq', 'মোহাম্মদ তারেক', 'প্রবাসী কর্মী')")->execute([$userId]);
        } else {
            $userId = $user['id'];
        }

        // Device Registration
        $devStmt = $db->prepare("INSERT INTO devices (device_id, user_id, model_name, os_version, app_version, ip_address, status) VALUES (?, ?, ?, 'Android 14', '1.0.0', ?, 'TRUSTED') ON DUPLICATE KEY UPDATE last_active = NOW(), status = 'TRUSTED'");
        $devStmt->execute([$deviceId, $userId, $modelName, $_SERVER['REMOTE_ADDR'] ?? '127.0.0.1']);

        $token = Security::generateToken((int)$userId, $phone, 'USER');

        AppResponse::success([
            'token' => $token,
            'user' => [
                'id' => $userId,
                'phone' => $phone,
                'name' => 'মোহাম্মদ তারেক',
                'kyc_status' => 'VERIFIED',
                'biometric_enabled' => true
            ]
        ], 'Login successful');
        break;

    // -------------------------------------------------------------
    // 4. TRANSACTIONS
    // -------------------------------------------------------------
    case 'create-transaction':
        $userPayload = Security::validateToken($authHeader);
        $userId = $userPayload ? $userPayload['user_id'] : 1;

        $recipientName = trim($body['recipient_name'] ?? '');
        $recipientAccount = trim($body['recipient_account'] ?? '');
        $recipientChannel = trim($body['recipient_channel'] ?? 'বিকাশ');
        $sendAmount = floatval($body['send_amount'] ?? 1000.0);
        $sendCurrency = trim($body['send_currency'] ?? 'SAR');
        $paymentMethodId = intval($body['payment_method_id'] ?? 1);

        if (empty($recipientName) || empty($recipientAccount) || $sendAmount <= 0) {
            AppResponse::error('Invalid transaction parameters');
        }

        // Generate Transaction Ref
        $txRef = 'RM-2026-' . rand(100000, 999999);
        $idempotency = 'IDEM-' . md5(uniqid() . microtime());

        // Get Rate
        $rStmt = $db->prepare("SELECT * FROM exchange_rates WHERE currency_code = ? LIMIT 1");
        $rStmt->execute([$sendCurrency]);
        $rateRow = $rStmt->fetch();
        $rate = $rateRow ? floatval($rateRow['sending_rate']) : 32.45;
        $fee = $rateRow ? floatval($rateRow['flat_fee']) : 5.0;

        $rawBdt = $sendAmount * $rate;
        $incentiveBdt = $rawBdt * 0.025;
        $totalRecipientBdt = $rawBdt + $incentiveBdt;
        $totalPayable = $sendAmount + $fee;

        // Insert Transaction
        $inTx = $db->prepare("INSERT INTO transactions (transaction_ref, user_id, beneficiary_id, payment_method_id, send_amount, send_currency, exchange_rate, service_fee, total_payable, raw_bdt_amount, govt_incentive_bdt, recipient_bdt_amount, status, idempotency_key) VALUES (?, ?, 1, ?, ?, ?, ?, ?, ?, ?, ?, ?, 'PROCESSING', ?)");
        $inTx->execute([$txRef, $userId, $paymentMethodId, $sendAmount, $sendCurrency, $rate, $fee, $totalPayable, $rawBdt, $incentiveBdt, $totalRecipientBdt, $idempotency]);
        $txId = $db->lastInsertId();

        // Status history
        $db->prepare("INSERT INTO transaction_status_history (transaction_id, previous_status, new_status, title_bn, description_bn) VALUES (?, 'PENDING', 'PROCESSING', 'রেমিট্যান্স গ্রহণ করা হয়েছে', 'টাকা প্রেরণের প্রক্রিয়া সফলভাবে শুরু হয়েছে')")->execute([$txId]);

        AppResponse::success([
            'transaction_ref' => $txRef,
            'status' => 'PROCESSING',
            'recipient_name' => $recipientName,
            'recipient_bdt_amount' => $totalRecipientBdt,
            'send_amount' => $sendAmount,
            'send_currency' => $sendCurrency
        ], 'Remittance transaction created successfully');
        break;

    case 'transactions':
        $userPayload = Security::validateToken($authHeader);
        $userId = $userPayload ? $userPayload['user_id'] : 1;

        $stmt = $db->prepare("SELECT t.*, pm.name_bn as payment_method_name FROM transactions t LEFT JOIN payment_methods pm ON t.payment_method_id = pm.id WHERE t.user_id = ? ORDER BY t.id DESC");
        $stmt->execute([$userId]);
        $txs = $stmt->fetchAll();
        AppResponse::success($txs, 'Transactions retrieved successfully');
        break;

    // -------------------------------------------------------------
    // 5. OFFERS & NOTICES
    // -------------------------------------------------------------
    case 'offers':
        $stmt = $db->query("SELECT * FROM offers WHERE is_active = 1 ORDER BY id DESC");
        AppResponse::success($stmt->fetchAll(), 'Offers retrieved');
        break;

    case 'notices':
        $stmt = $db->query("SELECT * FROM notices WHERE is_active = 1 ORDER BY is_priority DESC, id DESC");
        AppResponse::success($stmt->fetchAll(), 'Notices retrieved');
        break;

    // -------------------------------------------------------------
    // 6. DEFAULT FALLBACK
    // -------------------------------------------------------------
    default:
        AppResponse::error('Invalid API endpoint action', 404);
        break;
}
