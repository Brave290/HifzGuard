# HifzGuard 🛡️📖
> A Premium, Secure, and Cinematic Digital Sanctuary for Quran Memorization & consistent spiritual focus.

**HifzGuard** is a beautifully crafted Android application built using Jetpack Compose and Material Design 3. It prioritizes your daily spiritual connection to the Holy Quran by tracking your recitation sessions, providing an interactive memorization matrix, and locking down your phone from digital distractions once your daily goals are carry-forward or past due.

---

## 🚀 Instant Download (Pre-built APK)
For convenience, a fully working compiled Android installation archive is prepared and placed directly in the repository explorer. You can download it directly using any of these links:

* 📥 **[Direct Download: app-debug.apk](./app-debug.apk)**
* 📦 **[Alternative Download: app-debug.apk](app-debug.apk)**

If you are using the **Google AI Studio IDE**, you can simply download the APK file by:
1. Locating `app-debug.apk` at the root of the file explorer on the left sidebar.
2. Clicking the **three dots (options menu)** next to `app-debug.apk` or right-clicking it.
3. Selecting **Download** to save it locally on your computer!

Or via the top/platform settings menu:
- You can generate and download APKs/AABs or export the entire project as a ZIP via the **settings menu** in the top-right corner of the interface.

---

## ✨ Features Checklist
- [x] **Cinematic Dashboard**: Dark luxury theme featuring consistent stats, streak highlights, and modern interactive elements.
- [x] **Adaptive Juz Memorization Grid**: Custom 30-Juz matrix to cycle statuses (Gray/Unsaved -> Orange/In-Progress -> Green/Memorized) and log personal milestones.
- [x] **Voice-Tracked Reading Session**: Utilizes the microphone to detect voice amplitude and wave frequencies in real time, pausing automatically on silent inactivity.
- [x] **Spiritual Overlay Blocker**: Draw-over-other-apps lock screen that shields your focus from social media once locking hours are reached on uncompleted goals.
- [x] **Emergency 30M Override**: Keeps life-essential operations reachable through safety bypass tags.
- [x] **Anti-Tamper & Security Verifications**: Integrated signature verifications, custom obfuscation frameworks, and security alerts.

---

## 🛠️ Performance & Display Optimizations
To resolve the slow loading and startup failures ("unrecoverably broken input channel"), the following critical fixes have been integrated:
1. **Disabled Screen Blocker (FLAG_SECURE) for Previews**: Commented out the window secure flag in `MainActivity` which was causing the browser-based streaming emulator framework to black out/crash when drawing frames. Now, the live preview renders beautifully!
2. **Offloaded DB Initialization to Dispatchers.IO**: Shifted startup database preparations and DataStore tasks out of the main thread in `HifzGuardApplication`. This eliminates cold launch wait times and prevents Android Not Responding (ANR) flags.
3. **Optimized Shared Preferences**: Handled Keystore decryption exceptions gracefully so the app auto-recovers into high-speed standard properties under standard debug setups.

---
*Made with 🤍 by Akanji Mus'ab*
