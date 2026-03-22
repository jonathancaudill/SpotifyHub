# ── Google Tink / Error Prone ─────────────────────────────────────────
# androidx.security:security-crypto depends on Google Tink, which
# references Error Prone annotations at compile time. These are not
# shipped at runtime, so R8 complains about missing classes.
-dontwarn com.google.errorprone.annotations.**
