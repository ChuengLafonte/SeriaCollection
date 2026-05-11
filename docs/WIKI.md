# SeriaCollection Wiki

Detailed documentation for configuring and managing the SeriaCollection system.

## 🛠 Commands
| Command | Alias | Permission | Description |
|---------|-------|------------|-------------|
| `/collect` | `/collection` | None | Open the collection GUI |
| `/scollect reload` | | `seriacollection.admin` | Reload all configurations |
| `/scollect set <player> <id> <amount>` | | `seriacollection.admin` | Manually set player collection amount |

## 🔑 Permissions
- `seriacollection.admin`: Access to all administrative commands.

## 📊 Placeholders
Integrated with PlaceholderAPI:
- `%seriacollection_level_<id>%`: Returns the current tier level of a specific collection.
- `%seriacollection_amount_<id>%`: Returns the total amount collected for a specific ID.

Example: `%seriacollection_level_OAK_LOG%`

## ⚙️ Configuration

### collections.yml
Define your collections and their tiers here.
```yaml
collections:
  OAK_LOG:
    name: "Oak Wood"
    material: OAK_LOG
    category: FORAGING
    tiers:
      1:
        requirement: 100
        rewards:
          - "COMMAND: give <player> oak_sapling 5"
          - "MESSAGE: <green>You unlocked Oak Collection I!"
      2:
        requirement: 500
        rewards:
          - "ITEM: MMOITEMS:TOOL:OAK_AXE:1"
```

### guis.yml
Customize the collection menu layout and icons.
```yaml
gui:
  main_menu:
    title: "Item Collections"
    size: 54
    categories:
      FORAGING:
        slot: 10
        icon: OAK_SAPLING
        name: "<green>Foraging Collections"

## 💬 Messages & Notifications

### tier-up-broadcast
This message is sent to the player when they level up a collection. Despite the name, it is a private message but follows a "broadcast-style" layout for aesthetics.

**Available Placeholders:**
- `%collection%`: The name of the collection.
- `%old_roman%`: Previous tier level (Roman numerals).
- `%new_roman%`: New tier level (Roman numerals).
- `%rewards%`: A dynamically generated list of rewards.

**Reward Formatting:**
The system automatically pulls from `display-rewards` (preferred) or `rewards` in the collection file. Multiple rewards are automatically aligned with proper indentation for a "clean" look.

Example `messages.yml`:
```yaml
tier-up-broadcast:
  - "<yellow>▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬"
  - " <gold><bold>COLLECTION LEVEL UP </bold><yellow>%collection% <gray>%old_roman%<gray><bold>➜</bold><yellow>%new_roman%"
  - ""
  - " <green><bold>HADIAH</bold>"
  - "   %rewards%"
  - "<yellow>▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬"
```
```
