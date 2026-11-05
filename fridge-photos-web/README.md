# Fridge Photos (web)

A photo slideshow for a Samsung Family Hub fridge screen. A different album shows at different times of year
(for example **New Years** from Dec 16 to Jan 2). The fridge just opens a web address in its **Browser** app.

Family Hub fridges run Samsung's Tizen system and can't install apps, so this is a web page instead of an installed app.

## ⚠️ These photos are public

This is hosted on GitHub Pages, so **anyone who has the link can see the photos**. There's no password.
Don't put anything private here. (If that's a problem, the phone version in `../fridge-photos` keeps photos on your home Wi-Fi only.)

## Turn it on once

1. On GitHub, open this repo's **Settings → Pages**.
2. Under **Build and deployment → Source**, choose **GitHub Actions**.
3. Push the `fridge-photos-web` folder to the **main** branch (merge this branch, or ask and I'll help).
   The **Deploy Fridge Photos web** action runs and gives you a web address like
   `https://geraldhaskell7776-create.github.io/Ruby-dew-nectar/`.
4. On the fridge, open the **Browser** app, go to that address and add it to favorites. Tap the screen to skip a photo.

## Add your photos

Each album is a folder under `photos/`. To add pictures:

1. On github.com open `fridge-photos-web/photos/new-years` (or another album folder).
2. Click **Add file → Upload files**, drag your JPGs or PNGs in, and **Commit changes**.
3. That's it. GitHub rebuilds the slideshow and the fridge picks it up within 30 minutes (or reopen the page).

## Change the dates or add an album

Edit `albums.json`. Each album has:

- `name` — shown in the corner on the fridge
- `folder` — the folder under `photos/` that holds its pictures
- `start` and `end` — `"MM-DD"`, or `null` `null` for an album that fills every other day

Example: a summer album in June–August.

```json
{ "id": "summer", "name": "Summer", "folder": "summer", "start": "06-01", "end": "08-31" }
```

Then make a `photos/summer` folder and upload photos into it.

**Rules:** an album only shows if it has photos. If two dated albums overlap, the shorter range wins
(a 3-day "Christmas" beats a 3-week "Holidays"). An album with no dates shows on all other days.

## For developers

- `index.html` + `slideshow.js` — the slideshow and the date logic
- `albums.json` — album names, folders and dates (you edit this)
- `tools/build-manifest.mjs` — lists the photos in each folder into `manifest.json`
- `tools/daterules.test.mjs` — tests for the date logic (`node tools/daterules.test.mjs`)

The GitHub Action runs the tests, builds `manifest.json` and deploys, so you never generate it by hand.
