<?php
/**
 * Paisa Remit - Application Utilities & Security Middleware
 */

require_once __DIR__ . '/database.php';

// Enable CORS
header("Access-Control-Allow-Origin: *");
header("Access-Control-Allow-Methods: GET, POST, PUT, DELETE, OPTIONS");
header("Access-Control-Allow-Headers: Content-Type, Authorization, X-Requested-With, X-Device-ID");
header("Content-Type: application/json; charset=UTF-8");

if ($_SERVER['REQUEST_METHOD'] === 'OPTIONS') {
    http_response_code(200);
    exit;
}

class AppResponse {
    public static function success($data = null, string $message = 'Success', int $statusCode = 200): void {
        http_response_code($statusCode);
        echo json_encode([
            'success' => true,
            'message' => $message,
            'data' => $data,
            'timestamp' => date('Y-m-d H:i:s')
        ], JSON_UNESCAPED_UNICODE | JSON_PRETTY_PRINT);
        exit;
    }

    public static function error(string $message = 'Error', int $statusCode = 400, $errors = null): void {
        http_response_code($statusCode);
        echo json_encode([
            'success' => false,
            'message' => $message,
            'errors' => $errors,
            'timestamp' => date('Y-m-d H:i:s')
        ], JSON_UNESCAPED_UNICODE | JSON_PRETTY_PRINT);
        exit;
    }
}

class Security {
    private static string $secretKey = 'paisa_remit_super_secret_jwt_encryption_key_2026';

    public static function generateToken(int $userId, string $phone, string $role = 'USER'): string {
        $payload = [
            'user_id' => $userId,
            'phone' => $phone,
            'role' => $role,
            'iat' => time(),
            'exp' => time() + (86400 * 30) // 30 days
        ];
        $base64Payload = base64_encode(json_encode($payload));
        $signature = hash_hmac('sha256', $base64Payload, self::$secretKey);
        return $base64Payload . '.' . $signature;
    }

    public static function validateToken(?string $authHeader): ?array {
        if (!$authHeader || !preg_match('/Bearer\s+(.*)$/i', $authHeader, $matches)) {
            return null;
        }

        $token = $matches[1];
        $parts = explode('.', $token);
        if (count($parts) !== 2) return null;

        [$base64Payload, $signature] = $parts;
        $expectedSignature = hash_hmac('sha256', $base64Payload, self::$secretKey);
        if (!hash_equals($expectedSignature, $signature)) {
            return null;
        }

        $payload = json_decode(base64_decode($base64Payload), true);
        if (!$payload || !isset($payload['exp']) || $payload['exp'] < time()) {
            return null;
        }

        return $payload;
    }

    public static function logAudit(int $adminId, string $adminName, string $action, string $targetType, string $targetId, ?string $notes = null): void {
        try {
            $db = Database::getConnection();
            $stmt = $db->prepare("INSERT INTO admin_audit_logs (admin_id, admin_name, action, target_type, target_id, notes, ip_address) VALUES (?, ?, ?, ?, ?, ?, ?)");
            $ip = $_SERVER['REMOTE_ADDR'] ?? '127.0.0.1';
            $stmt->execute([$adminId, $adminName, $action, $targetType, $targetId, $notes, $ip]);
        } catch (Exception $e) {
            // Non-blocking
        }
    }
}
