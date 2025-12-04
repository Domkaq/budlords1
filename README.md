# BudLords - Minecraft Weed Economy Plugin

A comprehensive weed farming RPG + black market economy system for Minecraft Paper/Spigot 1.20.4.

## Features

### 🌱 Pot-Based Growing System (NEW!)
- Plant seeds in **Growing Pots** instead of farmland
- Every item has a **★ Star Quality Rating (1-5★)**:
  - **Pots** ★ - Base growth speed and quality
  - **Seeds** ★ - Affects final bud potential
  - **Lamps** ★ - Light level and quality bonus
  - **Fertilizers** ★ - Nutrient boost and duration
  - **Harvest Scissors** ★ - Better drops and quality chance
- Final **Weed Bud ★** calculated from all component ratings
- 📈 Higher star ratings = faster growth, better yields, rarer quality

### ✨ Visual & Animation Updates
- Modern particle effects for all growth stages
- Animated growth transitions (seed → sprout → growing → mature)
- Lamp glow effects based on ★ rating
- Special legendary bud animations and effects
- Harvest celebration particles
- **Ambient Effects:**
  - Fireflies around mature plants at night
  - Pollen particles on healthy plants
  - Weather-based visual effects
  - Dynamic lighting for grow rooms

### 🧩 Modern GUI & UX
- Redesigned **Strain Creator GUI** with visual feedback
- Progress bars and modern styling
- Sound effects on interactions
- Plant status display with care indicators
- Drag-and-drop item handling

### 🌿 Strain System
Each strain has:
- **Name** - Custom strain name
- **Rarity** - Common, Uncommon, Rare, or Legendary
- **Potency** - THC strength (1-100%)
- **Yield** - Number of buds produced (1-20)
- **Packaging Quality** - Affects final sale value
- **Seed Star Rating** - Quality of seeds created
- **Special Effects** - Up to 5 unique visual and gameplay effects!

### ✨ Strain Effects System (NEW!)
Each strain can have up to 5 unique special effects that activate when consumed:

**60+ Available Effects in 11 Categories:**
- **🔥 Transformation**: Ghost Rider (flaming head!), Shadow Walker, Angel Wings, Demon Horns
- **🌈 Visual**: Rainbow Aura, Galaxy Portal, Sparkling Eyes, Frost Aura
- **💨 Movement**: Speed Demon, Bunny Hop, Moon Gravity, Dolphin Swim, Fire Trail, Rocket Boost
- **👁 Perception**: Third Eye, Matrix Vision, Thermal Vision, Eagle Sight, Drunk Vision
- **🎮 Gameplay**: Lucky Charm, Midas Touch, Green Thumb, Iron Lungs, Munchies
- **⚔ Combat**: Berserker, Tank Mode, Ninja Mode, Vampire (lifesteal!), Thorns
- **🎉 Fun**: Disco Fever, Confetti, Bubble Aura, Heart Trail, Music Notes
- **🌿 Nature**: Flower Power, Storm Caller, Aurora Borealis, Wind Walker
- **🔮 Mystical**: Astral Projection, Time Warp, Dream State, Meditation, Enlightenment
- **⭐ Legendary**: Phoenix Rebirth, Dragon Breath, Void Walker, Celestial Being, Reality Bender

**Effect Features:**
- Visual particle effects (fire, sparkles, auras, trails)
- Gameplay modifiers (speed, strength, regeneration, etc.)
- Each effect has 1-5 intensity levels
- Effects are inherited and combined during crossbreeding!
- Mutations can create new random effects

### 🧬 Crossbreeding Lab (NEW!)
Create unique hybrid strains by combining existing ones:
- Combine two parent strains to create hybrids
- Hybrid traits are calculated from both parents
- **Effects are inherited from both parents!**
- Small chance for rare **mutations** that boost stats and add new effects
- Discover new legendary strains!
- Command: `/crossbreed`

### 🏆 Prestige System (NEW!)
Reset your progress for permanent bonuses:
- 10 prestige levels available
- Each level grants:
  - +10% Earnings bonus
  - +5% Growth speed
  - +8% Quality bonus
  - +2% Trade success
- Keep your strains, stats, and achievements
- Epic celebration effects on prestige

### 📋 Daily & Weekly Challenges (NEW!)
Stay engaged with rotating challenges:
- 3 daily challenges (Easy, Medium, Hard)
- 3 weekly challenges (Hard/Legendary difficulty)
- Earn bonus money and XP
- Variety of challenge types:
  - Harvest plants, Roll joints
  - Earn money, Get perfect harvests
  - Complete trades, Crossbreed strains
- Command: `/challenges`

### 🎲 Random Events (NEW!)
Exciting events that affect gameplay:
- **Drought** - Water drains faster from plants
- **Crop Blight** - Some plants lose quality
- **Golden Hour** - All plants gain bonus quality
- **Nutrient Rain** - Free water and nutrient boost
- **Police Activity** - Trading is riskier
- **Market Boom** - All products sell for 50% more!

### 📊 Statistics & Leaderboards (NEW!)
Track your BudLords journey:
- Comprehensive stat tracking:
  - Plants grown/harvested
  - Trading success rate
  - Joints rolled
  - Crossbreeds created
- BudLord Score ranking system
- Server leaderboards for:
  - Score, Earnings, Harvests
  - Prestige level, Daily streak
- Commands: `/stats`, `/leaderboard`

### 🧪 Strain Creator GUI (Admin)
- Command: `/straincreator`
- Permission: `budlords.admin`
- Rename strain with chat input → automatically returns to GUI
- Adjust stats with visual feedback
- Select seed star rating
- **Select up to 5 special effects from 60+ options!**
- Browse effects by category (Visual, Movement, Combat, etc.)
- Adjust effect intensity levels
- Save and register new strains with all effects

### 💰 Custom Economy System
- No Vault dependency required
- Commands: `/bal`, `/addmoney`, `/pay`
- Full tab-completion

### 🧑‍🌾 NPC Trading System
- **Market Joe** - Farmer villager NPC (`/spawnmarket`)
  - Opens a **Shop GUI** to buy farming equipment (pots, watering cans, scissors)
  - Also accepts packaged products for sale
- **BlackMarket Joe** - Wandering trader NPC (`/spawnblackmarket`)
  - Pays premium for rare strains
  - **Does NOT buy seeds** - Only accepts packaged buds
- Village vendors (any unemployed villager)
- Dynamic pricing based on strain value and ★ rating

### 📦 Packaging System
Two ways to package your buds:

#### Drag-and-Drop Packaging (NEW!)
1. Drop buds on the ground
2. Drop a pack (1g, 3g, 5g, or 10g) on the buds
3. Pick up your packaged product!

Buy packs from Market Joe's Rolling & Packaging Shop.

#### Command Packaging
Use `/package <amount>` to package buds from your inventory.

| Weight | Sell Multiplier |
|--------|-----------------|
| 1g | ×1.0 |
| 3g | ×1.25 |
| 5g | ×1.5 |
| 10g | ×2.0 |

### 🚬 Joint Rolling System (NEW!)
Roll joints through an interactive 4-stage minigame:

1. **Paper Pull** - Timing game: Click the paper when it reaches the green zone
2. **Tobacco Roll** - Click rapidly to fill the progress bar before time runs out
3. **Ganja Grinding** - Follow the moving target to grind the bud
4. **Final Roll** - Time your clicks when the power meter is in the perfect zone

**How to Roll:**
1. Buy a **Grinder** and **Tobacco** from Market Joe
2. Grind your buds (right-click with grinder while holding bud)
3. Right-click with grinded bud (with rolling paper and tobacco in inventory)
4. Complete the 4-stage minigame!

Better performance = higher quality joints!

### 🎲 Deal Success System
Success chance depends on:
- Player rank
- Strain potency and rarity
- Package weight
- Trader type (black market bonus)
- Active random events
- Prestige bonuses
- Failed deals result in a cooldown

### 🏆 Rank Progression
Ranks based on total earnings:
1. **Novice** - $0+
2. **Dealer** - $1,000+
3. **Supplier** - $5,000+
4. **Distributor** - $15,000+
5. **Kingpin** - $50,000+
6. **Cartel Boss** - $150,000+
7. **BudLord** - $500,000+

## Installation

1. Download `BudLords-1.0.0.jar`
2. Place in your server's `plugins/` folder
3. Restart/reload the server
4. Configure `plugins/BudLords/config.yml` as needed

## Commands

### Player Commands
| Command | Description | Permission |
|---------|-------------|------------|
| `/bal` | Check your balance and rank | `budlords.balance` |
| `/pay <player> <amount>` | Pay another player | `budlords.pay` |
| `/package <amount>` | Package buds (1, 3, 5, or 10g) | `budlords.package` |
| `/budlords` | View plugin help | `budlords.use` |
| `/stats` | View your statistics | `budlords.use` |
| `/prestige` | Open prestige menu | `budlords.use` |
| `/challenges` | View daily/weekly challenges | `budlords.use` |
| `/crossbreed` | Open crossbreeding lab | `budlords.use` |
| `/leaderboard [type]` | View server leaderboards | `budlords.use` |

### Admin Commands
| Command | Description | Permission |
|---------|-------------|------------|
| `/addmoney <player> <amount>` | Add money to a player | `budlords.admin` |
| `/straincreator` | Open strain creator GUI | `budlords.admin` |
| `/spawnmarket` | Spawn Market Joe NPC | `budlords.admin` |
| `/spawnblackmarket` | Spawn BlackMarket Joe NPC | `budlords.admin` |
| `/budlords reload` | Reload configuration | `budlords.admin` |

## Permissions

| Permission | Description | Default |
|------------|-------------|---------|
| `budlords.use` | Basic plugin usage | true |
| `budlords.balance` | Check balance | true |
| `budlords.pay` | Pay other players | true |
| `budlords.package` | Package weed | true |
| `budlords.admin` | Admin commands | op |

## Configuration Files

The plugin auto-generates these files:
- `config.yml` - Main configuration
- `strains.yml` - Strain definitions
- `players.yml` - Player balances and earnings
- `plants.yml` - Active plant data

## Building from Source

Requirements:
- Java 17+
- Maven 3.6+

```bash
mvn clean package
```

The compiled JAR will be in `target/BudLords-1.0.0.jar`

## Default Strains

| Strain | Rarity | Potency | Yield |
|--------|--------|---------|-------|
| OG Kush | Common | 40% | 3 |
| Purple Haze | Uncommon | 60% | 4 |
| White Widow | Rare | 75% | 5 |
| Northern Lights | Legendary | 95% | 7 |

## How to Play

### New Pot-Based Growing (Recommended)
1. Obtain a **Growing Pot** ★ (from admin or crafting)
2. Place the pot on any solid surface
3. Get seeds from `/straincreator` or find them
4. Right-click the pot with seeds to plant
5. **Care for your plant:**
   - Water with bucket → increases water level
   - Apply fertilizer → boosts nutrients
   - Add grow lamp nearby → improves light and quality
6. Wait for the plant to grow through 4 stages
7. Harvest with **Harvest Scissors** ★ for best results
8. Package and sell for profit!

### Legacy Farmland Growing
1. Get seeds from an admin using the Strain Creator
2. Plant seeds on farmland (right-click with seed on farmland)
3. Wait for plants to grow through 4 stages
4. Harvest mature plants (right-click or break)
5. Package harvested buds using `/package <amount>`
6. Sell packaged products to NPCs
7. Earn money and rank up!

## Star Quality Guide

| Rating | Color | Quality Mult | Growth Speed |
|--------|-------|--------------|--------------|
| ★☆☆☆☆ | Gray | ×1.0 | 0.8x |
| ★★☆☆☆ | Yellow | ×1.15 | 0.9x |
| ★★★☆☆ | Green | ×1.35 | 1.0x |
| ★★★★☆ | Blue | ×1.6 | 1.15x |
| ★★★★★ | Gold | ×2.0 | 1.35x |

## License

MIT License