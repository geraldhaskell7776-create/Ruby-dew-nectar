import { readFileSync } from "node:fs";
import assert from "node:assert";

// Run the browser script with minimal DOM shims to reach window.FridgePhotos.
const code = readFileSync(new URL("../slideshow.js", import.meta.url), "utf8");
const stub = () => ({ className: "", style: {}, textContent: "" });
const win = {};
new Function("document", "window", "setTimeout", "setInterval", "Image", "XMLHttpRequest", code)(
  { getElementById: stub, body: {} }, win, () => 0, () => 0, function () {},
  function () { this.open = () => {}; this.send = () => {}; }
);
const { inRange, spanDays, pickAlbum, dayNum } = win.FridgePhotos;
const d = (md) => dayNum(md);

// Range that wraps past New Year
assert.equal(inRange(d("12-16"), "12-16", "01-02"), true);
assert.equal(inRange(d("12-31"), "12-16", "01-02"), true);
assert.equal(inRange(d("01-01"), "12-16", "01-02"), true);
assert.equal(inRange(d("01-02"), "12-16", "01-02"), true);
assert.equal(inRange(d("01-03"), "12-16", "01-02"), false);
assert.equal(inRange(d("12-15"), "12-16", "01-02"), false);
assert.equal(inRange(d("07-04"), "06-01", "08-31"), true);
assert.equal(spanDays("12-16", "01-02"), 17);

const photo = ["p.jpg"];
const albums = [
  { id: "ny", name: "New Years", start: "12-16", end: "01-02", photos: photo },
  { id: "xmas", name: "Christmas", start: "12-24", end: "12-26", photos: photo },
  { id: "every", name: "Everyday", start: null, end: null, photos: photo },
];
assert.equal(pickAlbum(albums, d("12-25")).id, "xmas"); // shortest overlapping range wins
assert.equal(pickAlbum(albums, d("12-30")).id, "ny");
assert.equal(pickAlbum(albums, d("07-04")).id, "every"); // fallback to no-date album
assert.equal(pickAlbum([albums[0]], d("03-01")), null);  // nothing covers today
assert.equal(pickAlbum([{ id: "x", start: "12-16", end: "01-02", photos: [] }, albums[2]], d("12-31")).id, "every"); // empty album skipped
console.log("All date-rule tests passed.");
