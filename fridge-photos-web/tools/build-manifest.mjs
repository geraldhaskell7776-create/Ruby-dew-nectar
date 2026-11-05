// Scans each album's photo folder and writes manifest.json for the slideshow.
// Runs automatically on GitHub whenever you add or remove photos.
import { readFileSync, readdirSync, writeFileSync, existsSync } from "node:fs";
import { join, dirname } from "node:path";
import { fileURLToPath } from "node:url";

const root = join(dirname(fileURLToPath(import.meta.url)), "..");
const IMAGE = /\.(jpe?g|png|gif|webp)$/i;

const albums = JSON.parse(readFileSync(join(root, "albums.json"), "utf8"));

const manifest = albums.map((album) => {
  const dir = join(root, "photos", album.folder);
  const photos = existsSync(dir)
    ? readdirSync(dir).filter((f) => IMAGE.test(f)).sort()
        .map((f) => `photos/${album.folder}/${encodeURIComponent(f)}`)
    : [];
  return { ...album, photos };
});

writeFileSync(join(root, "manifest.json"), JSON.stringify(manifest, null, 2) + "\n");

const total = manifest.reduce((n, a) => n + a.photos.length, 0);
console.log(`Wrote manifest.json: ${manifest.length} albums, ${total} photos.`);
for (const a of manifest) console.log(`  ${a.name}: ${a.photos.length}`);
