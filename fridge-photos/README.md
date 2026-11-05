# Fridge Photos

Shows photos from your Google Photos on a Samsung Family Hub fridge screen, with a different album for each time of year
(for example **New Years** from Dec 16 to Jan 2).

## How it works

1. In the app you make albums and give each one dates (or no dates, for every other day).
2. **Add photos from Google** opens Google's photo picker. You pick photos and tap **Done**, and the app saves copies on the phone.
   Google only lets apps see photos you pick yourself, so the app can't browse your whole library.
3. Turn on **Slideshow**. The phone shares a full-screen slideshow over your home Wi-Fi.
4. On the fridge, open the **Browser** app and go to the address the app shows (like `http://192.168.1.23:8080`). Bookmark it.
   The fridge switches albums on its own when the dates change.

Family Hub fridges run Samsung's Tizen system and can't install Android apps, so the phone does the work and the fridge just shows the page.
Use a phone or an old Android tablet that stays at home, on Wi-Fi and plugged in.

## One-time Google setup (about 10 minutes, free)

1. Go to <https://console.cloud.google.com/> and create a project, for example "Fridge Photos".
2. **APIs & Services → Library**: search for **Photos Picker API** and click **Enable**.
3. **APIs & Services → OAuth consent screen**: choose **External**, fill in the app name and your email.
   Leave it in **Testing**, and under **Test users** add your Gmail address.
4. Get your computer's signing fingerprint: in Android Studio open the **Gradle** panel →
   **FridgePhotos → Tasks → android → signingReport** (or run `./gradlew signingReport`). Copy the **SHA1** for `debug`.
5. **APIs & Services → Credentials → Create credentials → OAuth client ID**: type **Android**,
   package name `com.fridgephotos`, and paste the SHA1. No key goes into the code.

## Build and install from Android Studio

1. **File → Open** and choose this `fridge-photos` folder (not the repo root).
2. Plug in the phone with USB debugging on, then press **Run ▶**.
3. In the app, tap **Let it keep running in the background**. Samsung phones close background apps otherwise.

## Code map

- `data/DateRules.kt`: which album shows today (handles ranges that cross New Year's)
- `data/AlbumStore.kt`: albums and saved photos on the phone
- `google/`: Google sign-in and the Photos Picker API
- `server/`: the slideshow web server and the background service that keeps it running
- `assets/slideshow.html`: the page the fridge shows
- `ui/`: the app screens
