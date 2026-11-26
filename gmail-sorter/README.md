# Gmail Sender Sorter

Automatically moves email from senders you choose into a Gmail folder
(Gmail calls folders **labels**) and takes it out of your Inbox.
It runs inside your own Google account every 10 minutes, so you don't need a server and
nothing leaves your account.

## Set it up (about 5 minutes)

1. Go to **https://script.google.com** and click **New project**.
2. Delete the sample code, then paste in everything from [`Code.gs`](Code.gs).
3. Edit the `RULES` list at the top with your senders and folder names, for example:
   ```js
   const RULES = [
     { folder: 'Bills',  senders: ['billing@power.com', '@mybank.com'], markRead: false },
     { folder: 'Promos', senders: ['deals@store.com'],                  markRead: true  },
   ];
   ```
   * `"name@site.com"` matches one address; `"@site.com"` matches anyone at that domain.
   * Use `"Parent/Child"` for a nested folder. Missing folders are created for you.
4. Click **Save** (💾).
5. In the function drop-down choose **`sortNow`**, then click **Run**.
   Google asks for permission the first time. Choose your account, click **Advanced → Go to
   project (unsafe)**, then **Allow**. You see the "unsafe" warning because you wrote the
   script yourself and Google hasn't reviewed it. It only runs in your account.
6. Choose **`install`** and click **Run**. From then on it sorts automatically every 10 minutes.

To stop it, run **`uninstall`**.

## Options (in `Code.gs`)

| Setting | Default | What it does |
|---|---|---|
| `RUN_EVERY_MINUTES` | `10` | How often it runs (1, 5, 10, 15 or 30). |
| `KEEP_IN_INBOX` | `false` | `true` adds the folder label but leaves the mail in the Inbox too. |
| `markRead` (per rule) | | Also marks the moved mail as read. |

## Good to know

* It only looks at mail that's **in the Inbox**, so it never touches anything you've already filed.
* The first run also sorts your existing Inbox mail from those senders.
* To see what it did, open **Executions** in the left sidebar of the script editor.
