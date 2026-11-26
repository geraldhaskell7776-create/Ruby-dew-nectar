/**
 * Gmail Sender Sorter
 *
 * Moves email from specific senders into a folder (Gmail calls folders "labels")
 * and takes it out of the Inbox. Runs automatically on a timer once installed.
 *
 * HOW TO USE
 *   1. Edit RULES below.
 *   2. Run `sortNow` once (approve the Gmail permission prompt).
 *   3. Run `install` once to make it run automatically every 10 minutes.
 *   Run `uninstall` to stop it.
 */

// ---------------------------------------------------------------------------
// 1. YOUR RULES — edit this list
// ---------------------------------------------------------------------------
// folder:   the Gmail folder/label to move mail into (created if missing).
//           Use "Parent/Child" for a nested folder.
// senders:  full addresses ("news@shop.com") or whole domains ("@shop.com").
// markRead: true to also mark the moved mail as read.
const RULES = [
  {
    folder: 'Shopping',
    senders: ['orders@example-store.com', '@another-shop.com'],
    markRead: false,
  },
  {
    folder: 'Newsletters',
    senders: ['newsletter@example.com'],
    markRead: true,
  },
];

// ---------------------------------------------------------------------------
// 2. SETTINGS
// ---------------------------------------------------------------------------
const RUN_EVERY_MINUTES = 10;   // allowed: 1, 5, 10, 15 or 30
const KEEP_IN_INBOX = false;    // true = add the folder but leave mail in the Inbox
const BATCH_SIZE = 100;         // Gmail's maximum per call
const MAX_RUNTIME_MS = 4.5 * 60 * 1000; // stop before Apps Script's 6-minute limit

// ---------------------------------------------------------------------------
// Main job
// ---------------------------------------------------------------------------
function sortNow() {
  const started = Date.now();
  let total = 0;

  for (const rule of RULES) {
    if (!rule.folder || !rule.senders || rule.senders.length === 0) continue;

    const label = getOrCreateLabel_(rule.folder);
    const query = buildQuery_(rule.senders, rule.folder);
    let moved = 0;

    // Sorted threads stop matching the search (they leave the Inbox, or get
    // the label), so keep searching from the start until nothing matches.
    while (Date.now() - started < MAX_RUNTIME_MS) {
      const threads = GmailApp.search(query, 0, BATCH_SIZE);
      if (threads.length === 0) break;

      label.addToThreads(threads);
      if (rule.markRead) GmailApp.markThreadsRead(threads);
      if (!KEEP_IN_INBOX) GmailApp.moveThreadsToArchive(threads);
      moved += threads.length;
      if (threads.length < BATCH_SIZE) break;
    }

    if (moved > 0) console.log(`Moved ${moved} conversation(s) to "${rule.folder}"`);
    total += moved;
  }

  console.log(`Done. ${total} conversation(s) sorted.`);
}

// ---------------------------------------------------------------------------
// Turn automatic running on / off
// ---------------------------------------------------------------------------
function install() {
  uninstall();
  ScriptApp.newTrigger('sortNow')
    .timeBased()
    .everyMinutes(RUN_EVERY_MINUTES)
    .create();
  console.log(`Installed: sorting every ${RUN_EVERY_MINUTES} minutes.`);
}

function uninstall() {
  for (const t of ScriptApp.getProjectTriggers()) {
    if (t.getHandlerFunction() === 'sortNow') ScriptApp.deleteTrigger(t);
  }
  console.log('Automatic sorting is off.');
}

// ---------------------------------------------------------------------------
// Helpers
// ---------------------------------------------------------------------------
function buildQuery_(senders, folder) {
  const parts = senders
    .map(s => String(s).trim())
    .filter(Boolean)
    .map(s => (s.startsWith('@') ? s.slice(1) : s)); // Gmail matches domains without "@"
  // In search, Gmail writes "My Stuff/Bills" as label:my-stuff-bills
  const labelTerm = folder.toLowerCase().replace(/[\s\/]+/g, '-');
  const where = KEEP_IN_INBOX ? `in:inbox -label:${labelTerm}` : 'in:inbox';
  return `${where} from:(${parts.join(' OR ')})`;
}

function getOrCreateLabel_(name) {
  return GmailApp.getUserLabelByName(name) || GmailApp.createLabel(name);
}
