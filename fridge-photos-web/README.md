# Fridge Photos (web)

A photo slideshow for a Samsung Family Hub fridge screen. A different album shows at different times of year
(for example **New Years** from Dec 16 to Jan 2). The fridge just opens a web address in its **Browser** app.

Family Hub fridges run Samsung's Tizen system and can't install apps, so this is a web page, hosted on **Hostinger**.

## ⚠️ These photos are public

Anyone who has the web address can see the photos. There's no password. Don't put anything private here.
(Ask me if you'd like a password on it — Hostinger can do that.)

## Put it on Hostinger (once)

1. Sign in to Hostinger and open **hPanel → Files → File Manager**.
2. Open the **`public_html`** folder.
3. Upload **`fridge-photos.zip`** (I sent it to you), then right-click it and choose **Extract**.
   The files should sit directly in `public_html` (`index.html`, `manifest.php`, the `photos` folder, and so on).
   If Hostinger extracted them into a subfolder, move them up into `public_html`.
4. Visit your domain in a browser to check it. You should see the slideshow (or a "no photos yet" note).
5. On the fridge, open the **Browser** app, go to your domain, and add it to favorites. Tap the screen to skip a photo.

## Add your photos

Each album is a folder under `photos/`. In Hostinger's **File Manager**:

1. Open `public_html/photos/new-years` (or another album folder).
2. Click **Upload**, choose your JPGs or PNGs, and let them finish.
3. That's it. The slideshow shows them the next time the page checks (within 30 minutes, or reopen the page).

The `manifest.php` file lists whatever photos are in each folder, so there's nothing to rebuild.

## Change the dates or add an album

Edit **`albums.json`** (right-click → Edit in File Manager). Each album has:

- `name` — shown in the corner on the fridge
- `folder` — the folder under `photos/` that holds its pictures
- `start` and `end` — `"MM-DD"`, or `null` and `null` for an album that fills every other day

Example — a summer album in June–August:

```json
{ "id": "summer", "name": "Summer", "folder": "summer", "start": "06-01", "end": "08-31" }
```

Then make a `photos/summer` folder and upload photos into it.

**Rules:** an album only shows if it has photos. If two dated albums overlap, the shorter range wins
(a 3-day "Christmas" beats a 3-week "Holidays"). An album with no dates shows on all other days.

## For developers

- `index.html` + `slideshow.js` — the slideshow and the date logic
- `manifest.php` — lists each album's photos at request time (used on Hostinger)
- `albums.json` — album names, folders and dates
- `manifest.json` + `tools/build-manifest.mjs` — a static fallback for hosts with no PHP; the page tries
  `manifest.php` first and falls back to `manifest.json`
- `tools/daterules.test.mjs` — tests for the date logic (`node tools/daterules.test.mjs`)
