
# 🎵 Idle Display

**A minimalist, OLED‑friendly music visualizer and always‑on display for Android.**

Idle Display transforms your phone or tablet into a beautiful, dedicated music station. Designed specifically for **OLED / AMOLED screens**, it combines burn‑in protection, dynamic ambient visuals, and fluid animations that react to your music in real time.

---

## ✨ Features

### 🖤 OLED‑First Design
- **Pixel Shifter Burn‑in Protection**  
  Imperceptibly shifts the entire UI every minute to prevent permanent burn‑in on OLED panels.
- **Pure Black UI**  
  True blacks ensure minimal power consumption on OLED displays.

### 🎶 Smart Idle Modes
- **Music Mode**  
  Automatically activates while music is playing:
  - High‑resolution album art
  - Circular progress indicator
  - Track title & artist metadata
- **Clock Mode**  
  Seamlessly transitions to a massive, elegant digital clock when playback stops.

### 🌈 Dynamic Ambience
- Uses the **Android Palette API** to extract colors from album art.
- Generates a soft, glowing background that *breathes* and adapts to every track.

### 👆 Interactive & Useful
- **Quick App Launch**  
  Tap the album art or source icon to instantly open the active music app (Spotify, YouTube Music, etc.).
- **Bottom Notification Drawer**  
  Incoming notifications slide up subtly from the bottom, blurring the background to maintain focus.

### 🔋 Battery Efficient
- Optimized **Jetpack Compose** rendering.
- Designed for long‑running, always‑on use with minimal battery drain.

### 🌍 Universal Compatibility
Works with **any media player** that posts standard Android media notifications, including:
- Spotify
- Apple Music
- YouTube / YouTube Music
- Tidal
- SoundCloud
- And more

---

## 📱 Screenshots

- **Music Mode**
(https://github.com/user-attachments/assets/93819c89-ff75-46ca-8f8c-f6e414e5f344)

- **Idle Clock Mode**
(https://github.com/user-attachments/assets/91677c2a-5798-4824-88ac-506e0ea2a507)

- **Notification Drawer**
(https://github.com/user-attachments/assets/64dccc6d-f790-4227-a11b-fe38b5beafbf)


---

## 📥 Installation

### Option 1: Direct APK Download
1. Go to the **Releases** page.
2. Download the latest `IdleDisplay.apk`.
3. Open the file on your Android device and tap **Install**.

> **Note:** You may need to enable **“Install from Unknown Sources”** in your browser or file manager settings.

### Option 2: Build from Source

```bash
git clone https://github.com/sujkrxsh/idle-display.git
```

1. Open the project in **Android Studio (Koala or newer recommended)**.
2. Sync Gradle files to download dependencies.
3. Build and run on your device or emulator.

---

## 🛠️ Requirements & Compatibility

- **Minimum Android Version:** Android 13 (API 33)
- **Recommended Hardware:** OLED / AMOLED devices  
  (Samsung Galaxy S‑Series, Google Pixel, etc.)
- **LCD Screens:** Supported, but without OLED battery‑saving benefits
- **Device Types:** Phones & Tablets (centered adaptive layout)

---

## 🔒 Permissions Explained

Idle Display is **privacy‑first** and works completely **offline**.

### Notification Access
- **Permission:** `BIND_NOTIFICATION_LISTENER_SERVICE`
- **Why it’s needed:**
  - Read song title, artist, and album art from media notifications
  - Display incoming notifications in the bottom drawer

> Notifications are **never stored, transmitted, or analyzed** — they are displayed locally only.

### Wake Lock
- Keeps the screen on while Idle Display is active.

---

## 🔧 Tech Stack

- **Language:** Kotlin
- **UI Toolkit:** Jetpack Compose (Material 3)
- **Architecture:** MVVM + StateFlow
- **Color Processing:** Android Palette API
- **Animations:**
  - `androidx.compose.animation`
  - Spring physics
  - Shared element transitions

---

## 🤝 Contributing

Contributions are welcome and encouraged!

1. Fork the repository
2. Create a new branch
   ```bash
   git checkout -b feature-name
   ```
3. Commit your changes
4. Push to your fork
5. Submit a Pull Request

Whether it’s a bug fix, UI polish, or a new idea — all contributions are appreciated.

---

## 📄 License

This project is licensed under the **MIT License**.  
See the `LICENSE` file for details.

---

<p align="center">
  Created with ❤️ by <b>sujkrxsh</b>
</p>
