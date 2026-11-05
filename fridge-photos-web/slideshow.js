/* Fridge Photos slideshow. Plain ES5 so it runs in the fridge's built-in browser. */
(function () {
  "use strict";

  var SECONDS = 15;            // seconds per photo
  var RECHECK_MINUTES = 30;    // how often to look for a new album / new photos

  var layers = [document.getElementById("a"), document.getElementById("b")];
  var msg = document.getElementById("msg");
  var cap = document.getElementById("cap");
  var front = 0, list = [], idx = 0, currentKey = null, timer = null;

  function shuffle(a) {
    for (var i = a.length - 1; i > 0; i--) {
      var j = Math.floor(Math.random() * (i + 1));
      var t = a[i]; a[i] = a[j]; a[j] = t;
    }
    return a;
  }

  // "MM-DD" -> day number 1..366 using a leap year so Feb 29 fits.
  function dayNum(md) {
    var p = md.split("-");
    return new Date(2024, parseInt(p[0], 10) - 1, parseInt(p[1], 10)).valueOf();
  }
  function todayNum() {
    var d = new Date();
    return new Date(2024, d.getMonth(), d.getDate()).valueOf();
  }
  function inRange(today, start, end) {
    var s = dayNum(start), e = dayNum(end), t = today;
    return s <= e ? (t >= s && t <= e) : (t >= s || t <= e);
  }
  function spanDays(start, end) {
    var s = dayNum(start), e = dayNum(end);
    var day = 24 * 3600 * 1000;
    return ((e - s) / day % 366 + 366) % 366;
  }

  // Which album shows today: shortest dated range that covers today, else an
  // album with no dates. Empty albums are skipped.
  function pickAlbum(albums, today) {
    if (today == null) today = todayNum();
    var best = null, bestSpan = Infinity, fallback = null, i, a;
    for (i = 0; i < albums.length; i++) {
      a = albums[i];
      if (!a.photos || !a.photos.length) continue;
      if (a.start && a.end) {
        if (inRange(today, a.start, a.end)) {
          var span = spanDays(a.start, a.end);
          if (span < bestSpan) { best = a; bestSpan = span; }
        }
      } else if (!fallback) {
        fallback = a;
      }
    }
    return best || fallback;
  }

  function showMessage(text) {
    clearTimeout(timer);
    layers[0].className = "slide";
    layers[1].className = "slide";
    cap.textContent = "";
    msg.textContent = text;
    msg.style.display = "block";
  }

  function next() {
    clearTimeout(timer);
    if (!list.length) return;
    if (idx >= list.length) { shuffle(list); idx = 0; }
    var url = list[idx++];
    var img = new Image();
    img.onload = function () {
      var back = layers[1 - front];
      back.style.backgroundImage = 'url("' + url + '")';
      back.className = "slide on";
      layers[front].className = "slide";
      front = 1 - front;
      timer = setTimeout(next, SECONDS * 1000);
    };
    img.onerror = function () { timer = setTimeout(next, 2000); };
    img.src = url;
  }

  function load() {
    var x = new XMLHttpRequest();
    x.open("GET", "manifest.json?t=" + new Date().getTime());
    x.onload = function () {
      var albums;
      try { albums = JSON.parse(x.responseText); } catch (e) { return; }
      var album = pickAlbum(albums);
      var key = album ? album.id + ":" + album.photos.length : "none";
      if (key === currentKey) return;   // nothing changed, keep playing
      currentKey = key;
      if (!album) {
        showMessage("No photos yet. Add some to an album folder to get started.");
        list = [];
        return;
      }
      cap.textContent = album.name;
      list = shuffle(album.photos.slice());
      idx = 0;
      msg.style.display = "none";
      next();
    };
    x.onerror = function () {
      if (currentKey === null) showMessage("Can't load the photos. Check the web address and the connection.");
    };
    x.send();
  }

  document.body.onclick = next;                       // tap the screen to skip
  load();
  setInterval(load, RECHECK_MINUTES * 60 * 1000);     // switch albums by date on its own
  setTimeout(function () { location.reload(); }, 12 * 3600 * 1000);

  window.FridgePhotos = { inRange: inRange, spanDays: spanDays, pickAlbum: pickAlbum, dayNum: dayNum };
})();
