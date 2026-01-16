# LocalGlobalChat (Hytale Plugin)

```md

Chat plugin for Hytale featuring:
- **Global** and **Local** chat modes
- Local chat based on a **distance radius** (configurable)
- **Private messages** (/msg) fully in **pink**
- Chat targets debug (admin)

---

## Installation

1. Build the plugin and grab the generated `.jar`.
2. Put the plugin `.jar` into:
   - `mods/`
3. (Optional, recommended) Install TinyMessage/TinyMsg:
   - put `tinymessage-*.jar` into `mods/`
4. Restart the server.

> **Tip:** With TinyMessage/TinyMsg installed, colors and formatting work better.

---

## How it works (quick overview)

- Player default chat mode on join is **LOCAL**.
- Use **/g** to switch to **GLOBAL**.
- Use **/l** to switch to **LOCAL**.
- In **LOCAL**, only players in the same world and within the configured radius will receive the message.

---

## Commands

### `/g`
Switches to **GLOBAL** chat.

**Usage:**
```

/g

```

**Permission:** None (free)

---

### `/l`
Switches to **LOCAL** chat (distance-based).

**Usage:**
```

/l

```

**Permission:** None (free)

---

### `/msg <player> <message...>`
Sends a **private message** to another player (entire message is **pink**).

**Usage:**
```

/msg player2 hi

```

**What shows up:**
- Sender sees:
```

[To player2] hi

```
- Receiver sees:
```

[from player1] hi

```

**Permission:** None (free)

**Note (messages with spaces):**
- On most builds, this works normally:
```

/chatdebug

```

**Permission:** `hytale.command.chatdebug` (Admin)

---

### `/localradius <number>`
Sets the local chat radius (in blocks).

**Usage:**
```

/localradius 80

```

**Permission:** `hytale.command.localradius` (Admin)

**Persistence:**
- The value is saved to a file and remains after server restarts.
- Default fallback file path:
  - `./plugins/LocalGlobalChat/localglobalchat.properties`
  - key: `localRadius=<number>`

```

## Permissions Summary

| Command | Permission |
|--------|------------|
| `/g` | *(none)* |
| `/l` | *(none)* |
| `/msg` | *(none)* |
| `/chatdebug` | `hytale.command.chatdebug` |
| `/localradius` | `hytale.command.localradius` |

> Assign the permissions above only to the Admin group in your permissions system.

---

## Quick Examples

```
- Switch to global:
```

/g

```
- Switch back to local:
```

/l

```
- Send a DM:
```

/msg FoToom hi

```
- Admin: set local radius to 120:
```

/localradius 120

```
- Admin: toggle chat targets debug:
```

/chatdebug


## Notes

- Local chat is filtered by:
- Same world
- Distance <= configured radius
- The plugin automatically uses TinyMsg if installed.

