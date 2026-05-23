-- ─────────────────────────────────────────────────────────────────────────────
-- LabChain schema migrations — safe to re-run on every boot (IF NOT EXISTS)
-- ─────────────────────────────────────────────────────────────────────────────

-- v1: Forgot-password OTP columns
ALTER TABLE users ADD COLUMN IF NOT EXISTS email                  VARCHAR(255);
ALTER TABLE users ADD COLUMN IF NOT EXISTS reset_otp              VARCHAR(10);
ALTER TABLE users ADD COLUMN IF NOT EXISTS reset_otp_expires_at   TIMESTAMP;

-- v2: Phone column (used for booking ownership and JWT claim)
ALTER TABLE users ADD COLUMN IF NOT EXISTS phone                  VARCHAR(20);

-- v2: Unique index on email — enforces one account per email.
--     PostgreSQL treats NULLs as distinct in unique indexes, so existing
--     admin/superadmin rows that have NULL email are unaffected.
CREATE UNIQUE INDEX IF NOT EXISTS uq_users_email
    ON users(email)
    WHERE email IS NOT NULL;