# BudLords - Complete Documentation

## 🎉 VERSION 2.0.0 - MAJOR UPDATE 🎉

## Table of Contents
1. [Installation Guide](#installation-guide)
2. [Feature Usage Guide](#feature-usage-guide)
3. [Star Quality System](#star-quality-system)
4. [Pot-Based Growing](#pot-based-growing)
5. [v2.0.0 New Features](#v200-new-features)
   - [Season System](#season-system)
   - [Weather System](#weather-system)
   - [Disease System](#disease-system)
   - [Achievement System](#achievement-system)
   - [Skill Tree System](#skill-tree-system)
   - [Collection Book](#collection-book)
   - [New Seed Types](#new-seed-types)
6. [Admin Commands Reference](#admin-commands-reference)
7. [Configuration Guide](#configuration-guide)
8. [Troubleshooting](#troubleshooting)

---

## Installation Guide

### Requirements
- Minecraft Server: Paper or Spigot 1.20.4
- Java: 17 or higher

### Step-by-Step Installation

1. **Download the Plugin**
   - Download `BudLords-2.0.0.jar` from the releases

2. **Install the Plugin**
   ```
   1. Stop your Minecraft server
   2. Copy BudLords-2.0.0.jar to your server's plugins/ folder
   3. Start your server
   ```

3. **Verify Installation**
   - Check console for: `[BudLords] BudLords has been enabled successfully!`
   - Check console for: `[BudLords] ★ Star Quality System enabled!`
   - Check console for v2.0.0 features being enabled
   - Run `/budlords` in-game to see the help menu

4. **Configure (Optional)**
   - Edit `plugins/BudLords/config.yml` to customize settings
   - Run `/budlords reload` to apply changes

---

## Feature Usage Guide

### Getting Started

#### 1. Obtaining Seeds
As an admin, use the Strain Creator GUI:
```
/straincreator
```
This opens a modern GUI where you can:
- Click the name tag to rename the strain (type name in chat, auto-returns to GUI)
- Click rarity icon to cycle through: Common → Uncommon → Rare → Legendary
- Click the star icon to select seed quality (★1-5)
- Use +/- buttons to adjust Potency, Yield, and Packaging Quality
- Shift-click for larger adjustments (+10 instead of +5)
- Drag an item onto the icon slot to set the strain's appearance
- Click the emerald block to save and receive 5 seeds

#### 2. Planting Seeds (Pot-Based - Recommended)

**New Pot-Based Growing System:**
1. Obtain a **Growing Pot** ★ (via admin commands or crafting)
2. Place the pot on any solid surface (right-click)
3. Hold seeds in your main hand
4. Right-click the pot to plant

**Legacy Farmland Growing:**
1. Create farmland using a hoe on dirt/grass
2. Hold seeds in your main hand
3. Right-click on the farmland

**Growth Tips:**
- Use higher ★ pots for faster growth and better quality
- Water your plants regularly with a bucket
- Apply fertilizer for nutrient boost
- Place grow lamps nearby for quality bonus
- Surround plants with walls for enclosed growing bonus

#### 3. Caring for Plants

**Watering:**
- Right-click on a plant with a water bucket
- Water level slowly decreases over time
- Keep water level above 70% for quality bonus

**Fertilizing:**
- Craft or obtain fertilizer (★1-5 quality)
- Right-click on a plant with fertilizer
- Higher ★ fertilizer gives better nutrient boost

**Lighting:**
- Place Grow Lamps (★1-5) near your plants
- Better lamps provide more light and quality bonus
- Special glow particles show lamp effect

#### 4. Harvesting

**Without Scissors:**
- Wait for plants to reach the mature stage (4th stage)
- Right-click or break the plant to harvest
- You'll receive buds based on the strain's yield and quality bonuses

**With Harvest Scissors (Recommended):**
- Obtain Harvest Scissors (★1-5 quality)
- Right-click on mature plant with scissors
- Benefits:
  - Yield bonus based on scissors quality
  - Chance to upgrade bud quality
  - Chance for rare bonus seeds
  - Better final bud ★ rating

#### 5. Packaging
Convert raw buds into sellable packages:
```
/package 1   - Creates 1g package (×1.0 multiplier)
/package 3   - Creates 3g package (×1.25 multiplier)
/package 5   - Creates 5g package (×1.5 multiplier)
/package 10  - Creates 10g package (×2.0 multiplier)
```

#### 6. Selling
1. Find or spawn a trader:
   - **Market Joe** - Regular prices (admin: `/spawnmarket`)
   - **BlackMarket Joe** - Better prices for rare strains (admin: `/spawnblackmarket`)
   - **Village Vendors** - Any unemployed villager, lower prices

2. Hold your packaged product and right-click the trader
3. If the deal succeeds, you receive money!
4. If the deal fails, you'll have a cooldown before trying again

### Checking Progress

#### View Balance and Rank
```
/bal
```
Shows:
- Current balance
- Total lifetime earnings
- Current rank
- Progress to next rank

#### Ranks
| Rank | Required Earnings | Success Bonus |
|------|-------------------|---------------|
| Novice | $0 | 70% |
| Dealer | $1,000 | 75% |
| Supplier | $5,000 | 80% |
| Distributor | $15,000 | 85% |
| Kingpin | $50,000 | 90% |
| Cartel Boss | $150,000 | 95% |
| BudLord | $500,000 | 100% |

---

## Star Quality System

The ★ Star Quality System (1-5 stars) applies to all growing equipment and affects the final bud quality.

### Star Ratings

| Rating | Display | Color | Quality Mult | Growth Speed |
|--------|---------|-------|--------------|--------------|
| ★☆☆☆☆ | 1 Star | Gray | ×1.0 | 0.8x |
| ★★☆☆☆ | 2 Star | Yellow | ×1.15 | 0.9x |
| ★★★☆☆ | 3 Star | Green | ×1.35 | 1.0x |
| ★★★★☆ | 4 Star | Blue | ×1.6 | 1.15x |
| ★★★★★ | 5 Star | Gold | ×2.0 | 1.35x |

### Items with Star Ratings

#### Growing Pot ★
- Base for pot-based growing
- Affects overall growth speed
- Higher ★ = faster growth, better base quality
- Place on any solid surface

#### Seeds ★
- Set during strain creation
- Affects final bud potential
- Better seeds = better maximum quality

#### Grow Lamp ★
- Provides light to plants
- Affects quality bonus
- Higher ★ = more light, better efficiency

#### Fertilizer ★
- Boosts nutrient levels
- Affects quality and growth
- Higher ★ = more nutrients, longer duration

#### Harvest Scissors ★
- Used for harvesting
- Affects yield and quality
- Higher ★ = bonus yield, quality upgrade chance, rare drops

### Final Bud Rating Calculation

The final bud ★ rating is calculated from a weighted combination:
- **Pot**: 20%
- **Seed**: 25%
- **Lamp**: 20%
- **Fertilizer**: 15%
- **Scissors**: 10%
- **Care Quality**: 10%

**Example:**
- 5★ Pot + 4★ Seed + 3★ Lamp + 4★ Fertilizer + 5★ Scissors + 90% care
- = (5×0.2) + (4×0.25) + (3×0.2) + (4×0.15) + (5×0.1) + (4.5×0.1)
- = 1.0 + 1.0 + 0.6 + 0.6 + 0.5 + 0.45 = 4.15 → **4★ Bud**

---

## Pot-Based Growing

### Overview
The new pot-based growing system replaces farmland planting with a more interactive and rewarding experience.

### Growing Pot Types
Growing pots are placed on any solid surface and act as containers for your plants.

| Star | Growth Speed | Quality Bonus |
|------|--------------|---------------|
| ★☆☆☆☆ | -20% slower | +0% |
| ★★☆☆☆ | -10% slower | +15% |
| ★★★☆☆ | Normal | +35% |
| ★★★★☆ | +15% faster | +60% |
| ★★★★★ | +35% faster | +100% |

### Care System

**Water Level (0-100%)**
- Starts at 70% in new pots
- Decreases slowly over time
- Water with bucket to refill to 100%
- Below 30%: Quality penalty
- Above 70%: Quality bonus

**Nutrient Level (0-100%)**
- Starts at 50% in new pots
- Decreases slowly over time
- Apply fertilizer to boost
- Affects growth speed and quality

### Plant Status Display
Right-click a growing plant to see status:
```
━━━━ OG Kush ━━━━
Stage: Sprout (2/4)
Pot: ★★★☆☆
Seed: ★★★★☆
Water: 85%
Nutrients: 60%
Lamp: ★★★★★
Quality: ★★★☆☆ Good
```

---

## Admin Commands Reference

### Economy Commands

#### `/addmoney <player> <amount>`
Add money to a player's balance.
```
/addmoney Steve 1000
```
- Permission: `budlords.admin`
- Tab-completes player names
- Tab-suggests amounts: 100, 500, 1000, 5000, 10000

### NPC Commands

#### `/spawnmarket`
Spawn Market Joe at your location.
- Creates a farmer villager NPC
- NPC is invulnerable and stationary
- Permission: `budlords.admin`

**Market Joe Shop:**
When you interact with Market Joe without holding a packaged product, a shop GUI opens where you can purchase:
- **Growing Pots** (★1-5) - Essential for growing plants
- **Watering Cans** (★1-5) - Water your plants efficiently
- **Harvest Scissors** (★1-5) - Better harvests and bonus drops

Prices scale with star rating (higher ★ = better quality but more expensive).

#### `/spawnblackmarket`
Spawn BlackMarket Joe at your location.
- Creates a wandering trader NPC
- Pays premium for rare strains
- **Does NOT buy seeds** - Only accepts packaged buds
- NPC is invulnerable and stationary
- Permission: `budlords.admin`

### Strain Management

#### `/straincreator`
Open the Strain Creator GUI.
- Permission: `budlords.admin`

**Modernized GUI Features:**
- Visual progress bars for stats
- Sound effects on interactions
- Shift-click for larger adjustments
- Seed star rating selector
- Strain preview display
- Gradient border styling

**Name Entry Flow:**
1. Click the name tag button
2. GUI closes, type name in chat
3. Name is validated (2-32 chars, alphanumeric + spaces)
4. GUI reopens automatically with new name
5. Type "cancel" to return without changing

### Configuration

#### `/budlords reload`
Reload the plugin configuration.
- Permission: `budlords.admin`
- Reloads config.yml, strains.yml, players.yml, plants.yml

---

## Configuration Guide

### config.yml

```yaml
# Autosave interval in seconds
autosave-interval-seconds: 300

economy:
  currency-symbol: "$"
  starting-balance: 0.0

farming:
  growth-check-interval-seconds: 60
  growth-interval-seconds: 300
  particle-interval-ticks: 40
  # New pot-based growing system
  pot-based-growing: true
  # Legacy farmland support
  allow-farmland-growing: true

# Star Quality System settings
quality:
  enabled: true
  star-weights:
    one-star: 40
    two-star: 30
    three-star: 18
    four-star: 9
    five-star: 3

packaging:
  multipliers:
    1g: 1.0
    3g: 1.25
    5g: 1.5
    10g: 2.0

trading:
  failed-deal-cooldown-seconds: 30
```

### strains.yml (Auto-generated)

```yaml
strains:
  og_kush:
    name: "OG Kush"
    rarity: COMMON
    potency: 40
    yield: 3
    packaging-quality: 50
    icon: GREEN_DYE
```

### Custom Ranks (Optional)

Add to config.yml:
```yaml
ranks:
  rank1:
    name: "Beginner"
    required-earnings: 0
    success-chance-bonus: 0.6
    unlocked-strains: []
```

---

## Troubleshooting

### Common Issues

#### "Seeds can only be planted on farmland or in Growing Pots!"
- Use a Growing Pot (right-click on solid surface with pot)
- Or use a hoe on dirt/grass to create farmland (legacy mode)

#### Plants Not Growing Fast Enough
- Use higher ★ rated pots
- Keep water level above 70%
- Apply fertilizer for nutrient boost
- Add a grow lamp nearby

#### Harvest Scissors Not Working
- Ensure they are BudLords Harvest Scissors (with ★ rating in lore)
- Regular shears won't provide bonuses

#### Low Bud Quality
- Use better ★ rated equipment (pot, seed, lamp, fertilizer, scissors)
- Maintain high water and nutrient levels
- Higher star ratings across all components = higher final rating

### Data Files

All data is stored in `plugins/BudLords/`:
- `config.yml` - Main configuration
- `strains.yml` - Strain definitions
- `players.yml` - Player balances and stats
- `plants.yml` - Active plant locations and star ratings

### Getting Help

1. Check the server console for errors
2. Verify all permissions are set correctly
3. Ensure Java 17+ and Paper/Spigot 1.20.4

---

## Building from Source

### Requirements
- Java JDK 17+
- Maven 3.6+

### Build Steps
```bash
git clone https://github.com/your-repo/budlords1.git
cd budlords1
mvn clean package
```

### Output
The compiled JAR will be at:
```
target/BudLords-2.0.0.jar
```

---

## v2.0.0 New Features

### Season System

BudLords now features a dynamic 4-season system that affects all aspects of plant growth!

#### Seasons
| Season | Growth | Quality | Potency | Special |
|--------|--------|---------|---------|---------|
| 🌸 Spring | +10% | Normal | Normal | Best for Rainforest/Rainbow seeds |
| ☀️ Summer | +15% | Normal | +15% | Best for Tropical/Solar/Golden seeds |
| 🍂 Autumn | -5% | +15% | +10% | Best for Mountain/Phantom seeds |
| ❄️ Winter | -15% | -10% | +15% | Best for Arctic/Lunar seeds |

#### Commands
- `/season` - View current season and time remaining
- `/season info` - View all season bonuses
- `/season set <season>` - (Admin) Force change season

---

### Weather System

Real-time weather affects your plants automatically!

| Weather | Growth | Quality | Water |
|---------|--------|---------|-------|
| ☀️ Clear | Normal | Normal | - |
| 🌤️ Sunny | +15% | +10% | - |
| ☁️ Cloudy | -5% | Normal | - |
| 🌧️ Rain | +10% | +5% | Auto-water +5%/min |
| ⛈️ Thunderstorm | +20% | +15% | Auto-water +8%/min |
| 🌙 Night | -10% | +5% | - |

**Tip:** Plants exposed to the sky benefit from rain automatically!

---

### Disease System

Plants can now contract diseases that affect their growth and quality!

#### Disease Categories
1. **🍄 Fungal** - Root Rot, Powdery Mildew, Botrytis, Fusarium
2. **🦠 Bacterial** - Leaf Blight
3. **🐛 Pest** - Spider Mites, Aphids, Thrips
4. **🌡️ Environmental** - Heat Stress, Nutrient Burn, Light Burn
5. **✨ Mystical** - Zombie Fungus, Crystal Virus (rare!)

#### Prevention & Treatment
- Keep water/nutrient levels balanced (not too high or low)
- Use appropriate cures:
  - **Fungicide** - For fungal diseases
  - **Antibacterial Spray** - For bacterial infections
  - **Pesticide** - For pest infestations
  - **Nutrient Flush** - For environmental issues
  - **Golden Elixir** - For mystical diseases

#### Fun Fact
The rare **Zombie Fungus** actually increases mutation chance!

---

### Achievement System

40+ achievements across 9 categories provide goals and rewards!

#### Categories
| Category | Examples |
|----------|----------|
| 🌿 Farming | First Harvest, Master Gardener, Star Collector |
| 🧬 Strains | Crossbreeder, Geneticist, Strain Master |
| 💰 Economy | Entrepreneur, Millionaire, BudLord |
| 🤝 Trading | Trader, Black Market Master, Lucky Streak |
| 🚬 Rolling | First Roll, Master Roller, Perfect Roll |
| 📋 Challenges | Challenger, Daily Devotee, Challenge Master |
| ✨ Prestige | First Prestige, Max Prestige |
| ⭐ Special | Night Owl, Storm Farmer, Disease Doctor |
| 👑 Legendary | Completionist, Ultimate BudLord |

#### Commands
- `/achievements` - Open achievements GUI (default: Farming)
- `/achievements <category>` - View specific category

---

### Skill Tree System

35+ skills across 5 skill trees provide permanent passive bonuses!

#### Skill Trees
| Tree | Focus | Example Skills |
|------|-------|----------------|
| 🌿 Farming | Growth & Harvest | Quick Hands, Master Farmer, Disease Immunity |
| ⭐ Quality | Product Quality | Quality Focus, Star Master, Premium Quality |
| 💰 Trading | Sales & Prices | Smooth Talker, Master Dealer, Black Market VIP |
| 🧬 Genetics | Crossbreeding | Mutation Affinity, Evolution Master, Master Geneticist |
| ✨ Effects | Strain Effects | Effect Duration, Effect Master, Powerful Effects |

#### How to Unlock
1. Earn skill points through prestige and gameplay
2. Spend XP in specific trees to unlock higher tiers
3. Choose your playstyle!

#### Commands
- `/skills` - Open skill tree GUI (default: Farming)
- `/skills <tree>` - View specific tree

---

### Collection Book

Discover and collect all strains in your personal Collection Book!

#### Features
- Track every strain you've harvested
- See harvest counts per strain
- View strain stats and special effects
- Earn rewards for collection milestones

#### Milestones
| Strains | Reward |
|---------|--------|
| 5 | $500 |
| 10 | $1,500 |
| 25 | $5,000 |
| 50 | $15,000 |
| 100 | $50,000 |

#### Commands
- `/collection` - Open collection book
- `/collection <page>` - Go to specific page

---

### New Seed Types

25+ new seed types with unique bonuses!

#### Climate Seeds
| Seed | Yield | Potency | Growth | Best Season |
|------|-------|---------|--------|-------------|
| Arctic ❄️ | ×1.0 | ×0.9 | ×1.2 | Winter |
| Tropical 🌴 | ×1.1 | ×1.1 | ×1.0 | Summer |
| Desert 🏜️ | ×0.9 | ×0.85 | ×1.1 | Summer |
| Rainforest 🌧️ | ×1.3 | ×1.0 | ×1.0 | Spring |
| Mountain ⛰️ | ×1.0 | ×1.15 | ×0.95 | Autumn |

#### Special Seeds
| Seed | Effect |
|------|--------|
| Quick Bloom ⚡ | 50% faster growth, lower yield |
| Slow Burn 🔥 | Slower growth, high quality |
| Mega Yield 💰 | Extra buds per harvest |
| Potency Plus 💪 | Enhanced THC production |

#### Legendary Seeds
| Seed | Special Properties |
|------|-------------------|
| Celestial ⭐ | Cosmic genetics, high all stats |
| Dragon Scale 🐉 | Mythical, extreme effects |
| Phoenix 🔥 | Self-healing properties |
| Void Walker 🕳️ | Dimension-crossing genetics |
| Time Warp ⏰ | Unpredictable growth |

---

*BudLords v2.0.0 - A weed farming economy plugin for Minecraft with ★ Star Quality System*
