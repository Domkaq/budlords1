# BudLords - Complete Documentation

## Table of Contents
1. [Installation Guide](#installation-guide)
2. [Feature Usage Guide](#feature-usage-guide)
3. [Admin Commands Reference](#admin-commands-reference)
4. [Configuration Guide](#configuration-guide)
5. [Troubleshooting](#troubleshooting)

---

## Installation Guide

### Requirements
- Minecraft Server: Paper or Spigot 1.20.4
- Java: 17 or higher

### Step-by-Step Installation

1. **Download the Plugin**
   - Download `BudLords-1.0.0.jar` from the releases

2. **Install the Plugin**
   ```
   1. Stop your Minecraft server
   2. Copy BudLords-1.0.0.jar to your server's plugins/ folder
   3. Start your server
   ```

3. **Verify Installation**
   - Check console for: `[BudLords] BudLords has been enabled successfully!`
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
This opens a GUI where you can:
- Click the name tag to rename the strain
- Click rarity icon to cycle through: Common → Uncommon → Rare → Legendary
- Use +/- buttons to adjust Potency, Yield, and Packaging Quality
- Drag an item onto the icon slot to set the strain's appearance
- Click the emerald block to save and receive 5 seeds

#### 2. Planting Seeds
1. Create farmland using a hoe on dirt/grass
2. Hold seeds in your main hand
3. Right-click on the farmland

**Growth Tips:**
- Ensure light level 12+ for quality bonus
- Keep farmland hydrated (near water) for bonus
- Surround plants with walls for enclosed growing bonus

#### 3. Harvesting
- Wait for plants to reach the mature stage (4th stage)
- Right-click or break the plant to harvest
- You'll receive buds based on the strain's yield and quality bonuses

#### 4. Packaging
Convert raw buds into sellable packages:
```
/package 1   - Creates 1g package (×1.0 multiplier)
/package 3   - Creates 3g package (×1.25 multiplier)
/package 5   - Creates 5g package (×1.5 multiplier)
/package 10  - Creates 10g package (×2.0 multiplier)
```

#### 5. Selling
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

### Trading System

#### Success Chance Factors
- **Rank Bonus**: Higher ranks = better success rate
- **Potency**: Very high potency slightly reduces success
- **Rarity**: Rarer strains are riskier to sell
- **Weight**: Larger packages are riskier
- **Trader Type**: BlackMarket Joe gives +10% success bonus

#### Failed Deals
- No product lost
- Cooldown applied (default 30 seconds)
- Try again after cooldown expires

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

#### `/spawnblackmarket`
Spawn BlackMarket Joe at your location.
- Creates a wandering trader NPC
- Pays premium for rare strains
- NPC is invulnerable and stationary
- Permission: `budlords.admin`

### Strain Management

#### `/straincreator`
Open the Strain Creator GUI.
- Permission: `budlords.admin`

**GUI Layout:**
```
[Border]  [Border]  [Border]  [Border]  [Name Tag] [Border]  [Border]  [Border]  [Border]
[Border]  [        ] [Rarity] [- Pot] [Potency] [+ Pot] [        ] [Icon]   [Border]
[Border]  [        ] [       ] [- Yld] [Yield  ] [+ Yld] [        ] [Display] [Border]
[Border]  [        ] [       ] [- Qlt] [Quality] [+ Qlt] [        ] [        ] [Border]
[Cancel]  [Border]  [Border]  [Border]  [SAVE]   [Border]  [Border]  [Border]  [Border]
```

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
  # Currency symbol shown before amounts
  currency-symbol: "$"
  # Starting balance for new players
  starting-balance: 0.0

farming:
  # How often to check for plant growth (seconds)
  growth-check-interval-seconds: 60
  # Time between growth stages (seconds)
  growth-interval-seconds: 300
  # Particle effect interval (ticks, 20 = 1 second)
  particle-interval-ticks: 40

packaging:
  multipliers:
    1g: 1.0
    3g: 1.25
    5g: 1.5
    10g: 2.0

trading:
  # Cooldown after failed deal (seconds)
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
  rank2:
    name: "Amateur"
    required-earnings: 500
    success-chance-bonus: 0.7
    unlocked-strains:
      - "og_kush"
```

---

## Troubleshooting

### Common Issues

#### "Seeds can only be planted on farmland!"
- Use a hoe on dirt/grass to create farmland
- Seeds must be placed on farmland blocks

#### "You need at least X buds to package!"
- Ensure you have enough buds of the same strain
- Check that you're holding bud items, not seeds

#### "This trader doesn't buy weed."
- Interact with Market Joe, BlackMarket Joe, or unemployed villagers
- Other villager types don't participate in trading

#### "You're too suspicious! Wait X seconds."
- A previous deal failed, triggering a cooldown
- Wait for the cooldown to expire

#### Plants Not Growing
- Ensure chunks are loaded
- Check light levels (12+ optimal)
- Verify farmland is hydrated
- Wait for growth interval (default 5 minutes per stage)

### Data Files

All data is stored in `plugins/BudLords/`:
- `config.yml` - Main configuration
- `strains.yml` - Strain definitions
- `players.yml` - Player balances and stats
- `plants.yml` - Active plant locations

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
target/BudLords-1.0.0.jar
```

---

*BudLords v1.0.0 - A weed farming economy plugin for Minecraft*
