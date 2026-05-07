# 👷 Minion Crafting Collection System

The Minion Collection system in **SeriaCollection** is inspired by Hypixel Skyblock. It allows players to track their progress in crafting and upgrading unique minions to unlock additional minion slots on their private islands.

## 🚀 Key Features

*   **Unique Craft Tracking**: Tracks every unique minion type and tier you have crafted.
*   **Slot Rewards**: Automatically grants additional minion slots based on milestone achievements.
*   **Immersive GUI**: A beautiful tracker menu to view all tier progressions.
*   **Centralized Database**: All progress is saved across the server network.

---

## 📖 Player Commands

| Command | Description |
| :--- | :--- |
| `/collect minions` | Opens the Crafted Minions tracker menu. |
| `/collection` | Opens the main collection menu (Minions is a category here). |

---

## 🛠️ Configuration

The system uses a dedicated milestone configuration file: `minion_milestones.yml`.

### `minion_milestones.yml`
```yaml
# unique_crafts_required: bonus_slots
milestones:
  5: 1    # At 5 unique crafts, total slots = 5 (base) + 1 = 6
  15: 2   # At 15 unique crafts, total slots = 5 (base) + 2 = 7
  30: 3
  50: 4
  100: 6
```

---

## 🖥️ Graphical User Interface (GUI)

The tracker menu (`/collect minions`) displays:
1.  **Minion Icons**: Every registered minion type.
2.  **Tier Progress**: Detailed lore showing Tier I through XII with status indicators:
    *   §a✔ Tier X: Already crafted and counted.
    *   §c✖ Tier X: Not yet crafted.
3.  **Milestone Info**: A central book showing your total unique crafts and how many more are needed for the next slot.

---

## 🔧 Admin Integration

The plugin integrates with **TopMinions** and **LuckPerms**:
*   **Slot Granting**: When a player reaches a milestone, the plugin automatically executes:
    `/lp user <player> permission set topminion.slots.<total> true`
*   **Automatic Hook**: Automatically detects TopMinions upon startup.

---

## 📚 Recipe Book System
The Recipe Book is a centralized system that allows players to view crafting recipes for all items in the game, including Minions, Enchanted Materials, and Special Gear.

### 🌟 Features
*   **Automatic Categorization**: Recipes are automatically mapped to their respective collection categories (Farming, Mining, etc.) based on their requirements.
*   **Unlock Security**: Players can only view the full details of recipes they have already unlocked through their collection level.
*   **Intelligent Caching**: High-performance caching system ensures that opening the menu is instant and does not cause server lag.
*   **Minion Navigation**: Easy navigation between different tiers of the same minion within the recipe detail view.

### 📖 Recipe Commands
| Command | Description |
| :--- | :--- |
| `/recipes` | Opens the main Recipe Book menu. |

---

## 🖥️ Graphical User Interface (GUI)

### 1. Main Menu
*   **Progress Overview**: A central book showing your total recipe completion percentage.
*   **Category Access**: Quick access to Farming, Mining, Combat, Foraging, Fishing, and Minion recipes.

### 2. Recipe Detail View
*   **Crafting Grid**: Shows the 3x3 layout of required items.
*   **Interactive Ingredients**: Click on any ingredient in the grid to immediately view *its* recipe (if available and unlocked).
*   **Result Info**: Shows the output item with a "Next/Previous" navigation for items with multiple recipe variations.

---

## 📊 Placeholders (PAPI)
*   `%seriacollection_minion_unique_crafts%`: Returns total unique minion crafts.
*   `%topminion_maxminions%`: Current max minions (from TopMinion).
*   `%seriacollection_recipe_percentage%`: Returns overall recipe unlock progress percentage.

---
*Created with ❤️ by Seria Development Team*
