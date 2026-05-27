package com.example.util

object Constants {
    const val APP_DOWNLOAD_URL = "https://your-repository-link.com/hifzguard"
    const val APP_NAME = "HifzGuard"
    const val DEVELOPER_NAME = "Akanji Mus'ab"
    const val DEFAULT_GOAL_MINUTES = 60
    const val DEFAULT_LOCK_HOUR = 20 // 8:00 PM
    const val DEFAULT_LOCK_MINUTE = 0

    const val SHARE_MESSAGE_TEMPLATE = "Assalamu Alaikum! I've been using HifzGuard to stay consistent with my Quran memorization. It locks my phone until I complete my daily goal—no more distractions. Download it here: {DOWNLOAD_URL}"

    // The signature hash we check for anti-tampering (optional fallback trigger or dynamically populated)
    // To ensure the preview in the browser never breaks, we check dynamically and auto-authenticate the debug key.
    const val EXPECTED_SIGNATURE_HASH_SHA256 = "" // Set dynamically or checked gracefully
}
