<?php
/**
 * Paisa Remit - Web Administration Console
 * Single-file responsive administrative management panel
 */

session_start();
require_once __DIR__ . '/../config/app.php';

$db = Database::getConnection();

// Handle Login
if (isset($_POST['admin_login'])) {
    $username = trim($_POST['username'] ?? '');
    $password = trim($_POST['password'] ?? '');

    $stmt = $db->prepare("SELECT * FROM admin_users WHERE username = ? AND is_active = 1 LIMIT 1");
    $stmt->execute([$username]);
    $admin = $stmt->fetch();

    if ($admin && password_verify($password, $admin['password_hash'])) {
        $_SESSION['admin_user'] = $admin;
        Security::logAudit($admin['id'], $admin['full_name'], 'LOGIN', 'ADMIN_PORTAL', (string)$admin['id'], 'Admin logged in');
        header('Location: index.php');
        exit;
    } else {
        $loginError = "ভুল ইউজারনেম অথবা পাসওয়ার্ড!";
    }
}

// Handle Logout
if (isset($_GET['logout'])) {
    session_destroy();
    header('Location: index.php');
    exit;
}

$isAdmin = isset($_SESSION['admin_user']);
$currentAdmin = $_SESSION['admin_user'] ?? null;

// Admin Actions
if ($isAdmin && $_SERVER['REQUEST_METHOD'] === 'POST') {
    // 1. Update Transaction Status
    if (isset($_POST['update_tx_status'])) {
        $txId = intval($_POST['tx_id']);
        $newStatus = trim($_POST['new_status']);
        $note = trim($_POST['admin_note']);

        $stmt = $db->prepare("UPDATE transactions SET status = ?, admin_note = ? WHERE id = ?");
        $stmt->execute([$newStatus, $note, $txId]);

        $db->prepare("INSERT INTO transaction_status_history (transaction_id, new_status, title_bn, description_bn, changed_by_admin_id) VALUES (?, ?, ?, ?, ?)")
           ->execute([$txId, $newStatus, "স্ট্যাটাস পরিবর্তন: $newStatus", $note, $currentAdmin['id']]);

        Security::logAudit($currentAdmin['id'], $currentAdmin['full_name'], 'UPDATE_TX_STATUS', 'TRANSACTION', (string)$txId, "Status set to $newStatus: $note");
        $actionSuccess = "লেনদেন স্ট্যাটাস সফলভাবে আপডেট করা হয়েছে!";
    }

    // 2. Update Exchange Rate
    if (isset($_POST['update_rate'])) {
        $currencyCode = trim($_POST['currency_code']);
        $newRate = floatval($_POST['sending_rate']);
        $newFee = floatval($_POST['flat_fee']);

        $stmt = $db->prepare("UPDATE exchange_rates SET sending_rate = ?, flat_fee = ? WHERE currency_code = ?");
        $stmt->execute([$newRate, $newFee, $currencyCode]);

        Security::logAudit($currentAdmin['id'], $currentAdmin['full_name'], 'UPDATE_EXCHANGE_RATE', 'RATE', $currencyCode, "Rate set to $newRate, Fee: $newFee");
        $actionSuccess = "এক্সচেঞ্জ রেট সফলভাবে আপডেট করা হয়েছে!";
    }

    // 3. Toggle Maintenance Mode
    if (isset($_POST['toggle_maintenance'])) {
        $mMode = intval($_POST['maintenance_val']);
        $db->prepare("UPDATE app_settings SET setting_value = ? WHERE setting_key = 'maintenance_mode'")->execute([(string)$mMode]);
        Security::logAudit($currentAdmin['id'], $currentAdmin['full_name'], 'MAINTENANCE_TOGGLE', 'SYSTEM', 'MAINTENANCE', "Maintenance set to $mMode");
        $actionSuccess = "মেইনটেন্যান্স সেটিংস পরিবর্তন সফল হয়েছে!";
    }
}

// Fetch Admin Data
$stats = [
    'total_users' => $db->query("SELECT COUNT(*) FROM users")->fetchColumn(),
    'total_tx_volume' => $db->query("SELECT COALESCE(SUM(recipient_bdt_amount), 0) FROM transactions")->fetchColumn(),
    'pending_tx' => $db->query("SELECT COUNT(*) FROM transactions WHERE status = 'PENDING' OR status = 'PROCESSING'")->fetchColumn(),
    'verified_kyc' => $db->query("SELECT COUNT(*) FROM users WHERE kyc_status = 'VERIFIED'")->fetchColumn(),
];

$transactions = $db->query("SELECT t.*, u.phone as sender_phone FROM transactions t LEFT JOIN users u ON t.user_id = u.id ORDER BY t.id DESC LIMIT 50")->fetchAll();
$rates = $db->query("SELECT * FROM exchange_rates ORDER BY display_order ASC")->fetchAll();
$auditLogs = $db->query("SELECT * FROM admin_audit_logs ORDER BY id DESC LIMIT 30")->fetchAll();
?>
<!DOCTYPE html>
<html lang="bn">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>পয়সা রেমিট • সেন্ট্রাল অ্যাডমিন কনসোল</title>
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css" rel="stylesheet">
    <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.5.1/css/all.min.css">
    <style>
        :root { --magenta: #E2136E; --purple: #4A154B; --bg: #FDF4F8; }
        body { background: var(--bg); font-family: 'Segoe UI', system-ui, -apple-system, sans-serif; }
        .sidebar { background: linear-gradient(180deg, var(--purple) 0%, #2A082C 100%); min-height: 100vh; color: #fff; }
        .sidebar .nav-link { color: #e9d5ff; font-weight: 500; border-radius: 8px; margin: 4px 12px; }
        .sidebar .nav-link.active, .sidebar .nav-link:hover { background: rgba(226, 19, 110, 0.2); color: #fff; }
        .brand-header { padding: 20px; border-bottom: 1px solid rgba(255,255,255,0.1); }
        .card-stat { border-radius: 16px; border: none; box-shadow: 0 4px 15px rgba(0,0,0,0.04); }
        .btn-magenta { background: var(--magenta); color: white; border: none; font-weight: 600; }
        .btn-magenta:hover { background: #c00f5c; color: white; }
    </style>
</head>
<body>

<?php if (!$isAdmin): ?>
    <div class="d-flex align-items-center justify-content-center vh-100">
        <div class="card p-4 shadow-lg border-0" style="max-width: 420px; width: 100%; border-radius: 20px;">
            <div class="text-center mb-4">
                <div class="d-inline-flex p-3 rounded-circle" style="background: rgba(226,19,110,0.1);">
                    <i class="fa-solid fa-building-columns fa-2x" style="color: var(--magenta);"></i>
                </div>
                <h4 class="mt-3 fw-bold text-dark">পয়সা রেমিট অ্যাডমিন</h4>
                <p class="text-muted small">এডমিনিস্ট্রেটিভ সিস্টেমে প্রবেশ করুন</p>
            </div>
            <?php if (isset($loginError)): ?>
                <div class="alert alert-danger py-2 small"><?= htmlspecialchars($loginError) ?></div>
            <?php endif; ?>
            <form method="POST">
                <div class="mb-3">
                    <label class="form-label small fw-semibold">ইউজারনেম</label>
                    <input type="text" name="username" class="form-control" value="admin" required>
                </div>
                <div class="mb-3">
                    <label class="form-label small fw-semibold">পাসওয়ার্ড</label>
                    <input type="password" name="password" class="form-control" value="Admin@Paisa2026!" required>
                </div>
                <button type="submit" name="admin_login" class="btn btn-magenta w-100 py-2">লগইন করুন</button>
            </form>
        </div>
    </div>
<?php else: ?>
    <div class="container-fluid">
        <div class="row">
            <!-- Sidebar -->
            <div class="col-md-2 p-0 sidebar">
                <div class="brand-header text-center">
                    <h5 class="fw-bold mb-0 text-white"><i class="fa-solid fa-paper-plane me-2" style="color: var(--magenta);"></i>পয়সা রেমিট</h5>
                    <small class="text-white-50">Admin Console v1.0</small>
                </div>
                <div class="p-3">
                    <div class="small text-white-50 mb-2">ইউজার: <strong><?= htmlspecialchars($currentAdmin['full_name']) ?></strong></div>
                    <span class="badge bg-success small"><?= htmlspecialchars($currentAdmin['role']) ?></span>
                </div>
                <ul class="nav flex-column mt-2">
                    <li class="nav-item"><a class="nav-link active" href="#dashboard"><i class="fa-solid fa-chart-pie me-2"></i>ড্যাশবোর্ড</a></li>
                    <li class="nav-item"><a class="nav-link" href="#transactions"><i class="fa-solid fa-receipt me-2"></i>লেনদেনসমূহ</a></li>
                    <li class="nav-item"><a class="nav-link" href="#rates"><i class="fa-solid fa-coins me-2"></i>লাইভ রেট</a></li>
                    <li class="nav-item"><a class="nav-link" href="#audit"><i class="fa-solid fa-shield-halved me-2"></i>অডিট লগ</a></li>
                    <li class="nav-item mt-4"><a class="nav-link text-danger" href="?logout=1"><i class="fa-solid fa-right-from-bracket me-2"></i>লগআউট</a></li>
                </ul>
            </div>

            <!-- Main Content -->
            <div class="col-md-10 p-4">
                <?php if (isset($actionSuccess)): ?>
                    <div class="alert alert-success alert-dismissible fade show"><?= htmlspecialchars($actionSuccess) ?><button type="button" class="btn-close" data-bs-dismiss="alert"></button></div>
                <?php endif; ?>

                <!-- Stats Overview -->
                <div class="row g-3 mb-4">
                    <div class="col-md-3">
                        <div class="card card-stat p-3 bg-white">
                            <div class="text-muted small">মোট নিবন্ধিত গ্রাহক</div>
                            <h3 class="fw-bold text-dark mt-1"><?= number_format($stats['total_users']) ?></h3>
                        </div>
                    </div>
                    <div class="col-md-3">
                        <div class="card card-stat p-3 bg-white">
                            <div class="text-muted small">যাচাইকৃত কেওয়াইসি (KYC)</div>
                            <h3 class="fw-bold text-success mt-1"><?= number_format($stats['verified_kyc']) ?></h3>
                        </div>
                    </div>
                    <div class="col-md-3">
                        <div class="card card-stat p-3 bg-white">
                            <div class="text-muted small">মোট প্রেরিত ভলিউম</div>
                            <h3 class="fw-bold text-primary mt-1">৳ <?= number_format($stats['total_tx_volume'], 2) ?></h3>
                        </div>
                    </div>
                    <div class="col-md-3">
                        <div class="card card-stat p-3 bg-white">
                            <div class="text-muted small">প্রক্রিয়াধীন লেনদেন</div>
                            <h3 class="fw-bold text-warning mt-1"><?= number_format($stats['pending_tx']) ?></h3>
                        </div>
                    </div>
                </div>

                <!-- Transactions Table -->
                <div class="card card-stat p-4 bg-white mb-4">
                    <div class="d-flex justify-content-between align-items-center mb-3">
                        <h5 class="fw-bold text-dark mb-0"><i class="fa-solid fa-money-bill-transfer me-2 text-danger"></i>সর্বশেষ রেমিট্যান্স লেনদেন</h5>
                    </div>
                    <div class="table-responsive">
                        <table class="table table-hover align-middle">
                            <thead class="table-light">
                                <tr>
                                    <th>রেফারেন্স নং</th>
                                    <th>প্রেরক</th>
                                    <th>প্রেরিত মুদ্রা</th>
                                    <th>বাংলাদেশে প্রাপকের টাকা</th>
                                    <th>স্ট্যাটাস</th>
                                    <th>তারিখ</th>
                                    <th>অ্যাকশন</th>
                                </tr>
                            </thead>
                            <tbody>
                                <?php foreach ($transactions as $tx): ?>
                                <tr>
                                    <td><strong class="text-magenta"><?= htmlspecialchars($tx['transaction_ref']) ?></strong></td>
                                    <td><?= htmlspecialchars($tx['sender_phone'] ?? 'User') ?></td>
                                    <td><?= htmlspecialchars($tx['send_amount'] . ' ' . $tx['send_currency']) ?></td>
                                    <td><strong class="text-success">৳ <?= number_format($tx['recipient_bdt_amount'], 2) ?></strong></td>
                                    <td>
                                        <span class="badge bg-<?= $tx['status'] === 'COMPLETED' ? 'success' : ($tx['status'] === 'PENDING' ? 'warning' : 'primary') ?>">
                                            <?= htmlspecialchars($tx['status']) ?>
                                        </span>
                                    </td>
                                    <td class="small text-muted"><?= htmlspecialchars($tx['created_at']) ?></td>
                                    <td>
                                        <button class="btn btn-sm btn-outline-primary" data-bs-toggle="modal" data-bs-target="#modalTx<?= $tx['id'] ?>">স্ট্যাটাস</button>
                                    </td>
                                </tr>

                                <!-- Status Modal -->
                                <div class="modal fade" id="modalTx<?= $tx['id'] ?>" tabindex="-1">
                                    <div class="modal-dialog">
                                        <form method="POST" class="modal-content">
                                            <input type="hidden" name="tx_id" value="<?= $tx['id'] ?>">
                                            <div class="modal-header">
                                                <h6 class="modal-title fw-bold">লেনদেন আপডেট: <?= htmlspecialchars($tx['transaction_ref']) ?></h6>
                                                <button type="button" class="btn-close" data-bs-dismiss="modal"></button>
                                            </div>
                                            <div class="modal-body">
                                                <div class="mb-3">
                                                    <label class="form-label small">নতুন স্ট্যাটাস</label>
                                                    <select name="new_status" class="form-select">
                                                        <option value="PROCESSING" <?= $tx['status'] === 'PROCESSING' ? 'selected' : '' ?>>PROCESSING (প্রক্রিয়াধীন)</option>
                                                        <option value="COMPLETED" <?= $tx['status'] === 'COMPLETED' ? 'selected' : '' ?>>COMPLETED (সম্পন্ন)</option>
                                                        <option value="REFUNDED" <?= $tx['status'] === 'REFUNDED' ? 'selected' : '' ?>>REFUNDED (রিফান্ড)</option>
                                                        <option value="REJECTED" <?= $tx['status'] === 'REJECTED' ? 'selected' : '' ?>>REJECTED (বাতিল)</option>
                                                    </select>
                                                </div>
                                                <div class="mb-3">
                                                    <label class="form-label small">অ্যাডমিন নোট</label>
                                                    <textarea name="admin_note" class="form-control" placeholder="মন্তব্য লিখুন..."><?= htmlspecialchars($tx['admin_note'] ?? '') ?></textarea>
                                                </div>
                                            </div>
                                            <div class="modal-footer">
                                                <button type="submit" name="update_tx_status" class="btn btn-magenta">সংরক্ষণ করুন</button>
                                            </div>
                                        </form>
                                    </div>
                                </div>
                                <?php endforeach; ?>
                            </tbody>
                        </table>
                    </div>
                </div>

                <!-- Live Rates Control -->
                <div class="card card-stat p-4 bg-white mb-4">
                    <h5 class="fw-bold text-dark mb-3"><i class="fa-solid fa-arrow-trend-up me-2 text-success"></i>মুদ্রা ও এক্সচেঞ্জ রেট নিয়ন্ত্রণ</h5>
                    <div class="row g-3">
                        <?php foreach ($rates as $r): ?>
                        <div class="col-md-4">
                            <form method="POST" class="border rounded-3 p-3 bg-light">
                                <input type="hidden" name="currency_code" value="<?= htmlspecialchars($r['currency_code']) ?>">
                                <div class="d-flex justify-content-between align-items-center mb-2">
                                    <span class="fw-bold"><?= $r['flag_emoji'] ?> <?= htmlspecialchars($r['country_name_bn']) ?> (<?= htmlspecialchars($r['currency_code']) ?>)</span>
                                </div>
                                <div class="mb-2">
                                    <label class="form-label small">রেট (১ <?= $r['currency_code'] ?> = ? BDT)</label>
                                    <input type="number" step="0.01" name="sending_rate" value="<?= htmlspecialchars($r['sending_rate']) ?>" class="form-control form-control-sm">
                                </div>
                                <div class="mb-2">
                                    <label class="form-label small">সার্ভিস ফি</label>
                                    <input type="number" step="0.5" name="flat_fee" value="<?= htmlspecialchars($r['flat_fee']) ?>" class="form-control form-control-sm">
                                </div>
                                <button type="submit" name="update_rate" class="btn btn-sm btn-outline-success w-100">রেট আপডেট</button>
                            </form>
                        </div>
                        <?php endforeach; ?>
                    </div>
                </div>

            </div>
        </div>
    </div>
<?php endif; ?>

<script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/js/bootstrap.bundle.min.js"></script>
</body>
</html>
