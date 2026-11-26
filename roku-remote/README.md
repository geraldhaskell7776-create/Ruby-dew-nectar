# Roku Remote

An Android app that turns your phone into a Roku remote over your home Wi-Fi.
It uses the Roku's built-in External Control Protocol (port 8060): no account, no pairing, no internet needed.

- **Finds your Roku automatically** on the Wi-Fi, or you can type its IP address
- **Remote buttons:** Power, Back, Home, D-pad + OK, Instant replay, Options (*), Rewind, Play/Pause, Fast forward, Volume and Mute
- **Hold to repeat** on the arrows and volume
- **Your phone's own volume buttons** change the TV volume
- **Keyboard:** type into TV search boxes from your phone
- **Channels tab:** tap a channel icon to open it
- Remembers your Roku for next time

This is its own project. It is not part of the Ruby Dew Nectar app.

## Build it in Android Studio

1. Pull this repo onto your computer.
2. In Android Studio choose **File > Open** and pick the **`roku-remote`** folder (not the repo's top folder).
3. Wait for Gradle sync to finish. If Android Studio asks, let it install the Android SDK 35 platform.
4. Plug in your phone with USB debugging on (or pair it over Wi-Fi), pick it at the top, and press **Run ▶**.

To get an APK file instead: **Build > Build App Bundle(s) / APK(s) > Build APK(s)**.
It ends up at `roku-remote/app/build/outputs/apk/debug/app-debug.apk`.

## If it can't find or control the Roku

- The phone and the Roku must be on the same Wi-Fi network. Guest networks often block this.
- On the Roku go to **Settings > System > Advanced system settings > Control by mobile apps** and make sure it is on.
- Search not finding it? Type the IP from **Settings > Network > About** on the Roku.
