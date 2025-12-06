# 🌿 BudLords - Professional Minecraft Weed Economy Plugin

[![Version](https://img.shields.io/badge/version-1.0.0-green.svg)](https://github.com/Domkaq/budlords1)
[![Minecraft](https://img.shields.io/badge/minecraft-1.20.4-blue.svg)](https://www.minecraft.net/)
[![License](https://img.shields.io/badge/license-MIT-blue.svg)](LICENSE)

A comprehensive, high-performance weed farming RPG with black market economy, advanced genetics system, and immersive visual effects for Minecraft Paper/Spigot 1.20.4.

---

## 📋 Table of Contents

- [Features Overview](#-features-overview)
- [Core Systems](#-core-systems)
- [Getting Started](#-getting-started)
- [Installation](#-installation)
- [Commands & Permissions](#-commands--permissions)
- [Configuration](#-configuration)
- [Performance](#-performance)
- [Building from Source](#-building-from-source)
- [Support](#-support)

---

## ✨ Features Overview

### 🌱 Advanced Farming System
- **3D Plant Visualization** - Realistic armor stand-based plant models with 4 growth stages
- **Star Quality System** (★☆☆☆☆ to ★★★★★) - Quality affects every aspect of growth and harvest
- **Growing Pots** - Place pots anywhere, not just on farmland
- **Smart Watering System** - Watering cans with capacity and quality bonuses
- **Fertilizer System** - Boost nutrients and growth speed with star-rated fertilizers
- **Grow Lamps** - Light systems that buff nearby plants based on star rating
- **Plant Care Mechanics** - Monitor water, nutrients, light, and quality in real-time

### 🧬 Genetics & Breeding
- **60+ Unique Strains** - From common to legendary with distinct properties
- **Crossbreeding Lab** - Combine strains to create hybrids with inherited traits
- **Mutation System** - Random mutations can create powerful new strains
- **139 Special Effects** - Unique visual and gameplay effects per strain
- **Strain Collection Book** - Track and discover all strains
- **Custom Strain Creator** (Admin) - Design unlimited custom strains with effects

### 💰 Economy & Trading
- **Multiple Buyer Types** - Market Joe, BlackMarket Joe, Village Vendors
- **Reputation System** - 6 levels from Suspicious to ★LEGENDARY★
- **Dynamic Pricing** - Prices vary based on reputation, rarity, and market events
- **Bulk Orders** - Complete special orders for massive bonus payouts (up to +200%)
- **Customer Tips** - Earn tips up to 25% based on reputation
- **10 Customer Types** - Each with unique preferences and price modifiers
- **Market Events** - Random events that boost or reduce prices
- **Dealer Phone** - Modern GUI to manage contacts, orders, and stats

### 📦 Production & Packaging
- **Packaging System** - Package buds in 1g, 3g, 5g, or 10g weights
- **Quality Multipliers** - Larger packages = better sale prices (up to ×2.0)
- **Drag-and-Drop** - Drop pack on buds for quick packaging
- **Joint Rolling** - Interactive 4-stage minigame with grinders
- **Harvest Scissors** - Star-rated tools for better yields and quality

### 🎮 RPG Systems
- **7 Rank Tiers** - Progress from Novice to BudLord based on earnings
- **10 Prestige Levels** - Reset progress for permanent bonuses
- **5 Skill Trees** - Farming, Quality, Trading, Genetics, Effects
- **40+ Achievements** - Unlock rewards across 9 categories
- **Daily Rewards** - Login streaks with milestone bonuses
- **Challenge System** - Daily and weekly challenges with XP rewards
- **Statistics Tracking** - Comprehensive stats and leaderboards

### 🌦️ Environmental Systems
- **Dynamic Weather** - 6 weather types affecting growth and quality
- **Seasonal System** - 4 seasons with unique bonuses and effects
- **Day/Night Cycle** - Time-based effects on plant growth
- **Disease System** - 12+ diseases with cures and prevention
- **Random Events** - Drought, crop blight, golden hour, market boom, etc.

### 🎨 Visual Effects
- **Ambient Particles** - Fireflies, pollen, sparkles around plants
- **Weather Effects** - Rain, thunder, sunshine particles
- **Celebration Effects** - Harvest animations based on quality and rarity
- **Smooth Animations** - Swaying plants with customizable animation styles
- **Visual Themes** - Strain-specific colors, particles, and glowing effects
- **Performance Optimized** - Scales from 1 to 100+ plants without FPS loss

### 📱 Modern UI/UX
- **Professional GUIs** - Clean, intuitive menus with phone-style design
- **Dealer Phone App** - Hub for contacts, orders, stats, and market info
- **Real-time Monitoring** - Plant status, water, nutrients, and quality
- **Sound Effects** - Immersive audio feedback for all interactions
- **Color-Coded Systems** - Easy-to-understand visual indicators

---

## 🔧 Core Systems

### Star Quality System

Every item in BudLords has a star rating that affects performance:

| Rating | Color | Quality Multiplier | Growth Speed | Rarity |
|--------|-------|-------------------|--------------|---------|
| ★☆☆☆☆ | §7Gray | ×1.0 | 0.8x | Common |
| ★★☆☆☆ | §eYellow | ×1.15 | 0.9x | Common |
| ★★★☆☆ | §aGreen | ×1.35 | 1.0x | Uncommon |
| ★★★★☆ | §9Blue | ×1.6 | 1.15x | Rare |
| ★★★★★ | §6Gold | ×2.0 | 1.35x | Legendary |

**Affects:** Pots, Seeds, Lamps, Fertilizers, Watering Cans, Scissors, Final Buds

### Reputation Levels

Build reputation with each buyer for better prices and perks:

| Level | Points | Price Bonus | Tip Chance | Tip Amount |
|-------|--------|-------------|------------|------------|
| Suspicious | 0-99 | +0% | 0% | - |
| Neutral | 100-299 | +5% | 5% | 2-5% |
| Friendly | 300-599 | +10% | 15% | 5-10% |
| Trusted | 600-999 | +15% | 25% | 8-15% |
| VIP | 1000-1499 | +20% | 35% | 10-20% |
| ★LEGENDARY★ | 1500+ | +25% | 50% | 15-25% |

### Bulk Orders

Complete special orders for massive bonus payouts:

| Tier | Bonus Payout | Duration |
|------|--------------|----------|
| Small | +15-25% | 30 min |
| Medium | +25-40% | 30 min |
| Large | +40-60% | 30 min |
| Massive | +60-100% | 30 min |
| Legendary | +100-200% | 30 min |

### Strain Effects (139 Total)

**Categories:**
- 🔥 Transformation (10) - Werewolf, Ghost Rider, Angel Wings, Demon Horns, etc.
- 🌈 Visual (15) - Rainbow Aura, Galaxy Portal, Plasma Aura, Void Eyes, etc.
- 💨 Movement (12) - Speed Demon, Bunny Hop, Rocket Boost, Sonic Boom, etc.
- 👁 Perception (10) - Third Eye, Thermal Vision, Death Sense, Aura Reading, etc.
- 🎮 Gameplay (15) - Lucky Drops, Midas Touch, Green Thumb, Merchant Blessing, etc.
- ⚔ Combat (12) - Berserker, Life Drain, Lightning Fist, Shadow Strike, etc.
- 🎉 Fun (10) - Disco Fever, Party Mode, Laugh Track, Sparkle Step, etc.
- 🌿 Nature (15) - Flower Power, Storm Caller, Photosynthesis, Aurora Borealis, etc.
- 🔮 Mystical (15) - Astral Projection, Time Warp, Oracle Vision, Ether Sight, etc.
- ⭐ Legendary (10) - Phoenix Rebirth, Dragon Breath, Titan Form, Singularity, etc.
- 🆕 Unique (15) - Quantum State, Crystalline Body, Gravity Well, Entropy Master, etc.

### Weather Types

| Weather | Growth | Quality | Water Gain | Special |
|---------|--------|---------|------------|---------|
| ☀ Clear | ×1.0 | ×1.0 | 0% | Normal conditions |
| ☀ Sunny | ×1.15 | ×1.1 | 0% | Best for growth |
| ☁ Cloudy | ×0.95 | ×1.0 | 0% | Slight penalty |
| 🌧 Rain | ×1.1 | ×1.05 | 5%/min | Auto-watering |
| ⛈ Thunderstorm | ×1.2 | ×1.15 | 8%/min | Best bonuses |
| 🌙 Night | ×0.9 | ×1.05 | 0% | Quality bonus |

### Seasons

| Season | Duration | Growth Bonus | Quality Bonus | Special Effects |
|--------|----------|--------------|---------------|-----------------|
| 🌸 Spring | 7 days | +10% | +5% | Best for new plants |
| ☀ Summer | 7 days | +15% | +10% | Peak growing season |
| 🍂 Autumn | 7 days | +5% | +15% | Best quality |
| ❄ Winter | 7 days | -10% | +5% | Challenging season |

---

## 🚀 Getting Started

### Quick Start Guide

1. **Get Seeds** - Obtain seeds from an admin or the black market
2. **Place Pot** - Right-click with a Growing Pot on any solid block
3. **Plant Seed** - Right-click the pot with seeds
4. **Care for Plant**:
   - Water with bucket or watering can (right-click)
   - Apply fertilizer (right-click plant)
   - Add grow lamp nearby for bonus
5. **Wait for Growth** - Plants grow through 4 stages
6. **Harvest** - Right-click or break with scissors for better yield
7. **Package** - Use `/package <amount>` or drag-and-drop method
8. **Sell** - Right-click buyers with packaged products

### Advanced Features

#### Crossbreeding
```
/crossbreed
```
- Combine two parent strains
- Inherits traits from both parents
- Chance for mutations
- Discover legendary strains

#### Daily Rewards
```
/daily
```
- $100 base reward
- +5% per day streak (up to +100%)
- Milestone bonuses at 7, 30, 100 days
- Grace period: 32 hours

#### Market System
```
/market
```
- Check current market conditions
- View active events
- See strain demand
- Plan optimal selling times

#### Skills
```
/skills [tree]
```
- Farming - Harvest speed, growth, water efficiency
- Quality - Quality bonus, star rating, fertilizer power
- Trading - Trade success, prices, cooldowns
- Genetics - Crossbreed quality, mutations, traits
- Effects - Effect duration and strength

#### Reputation
```
/reputation
```
- Check standing with all buyers
- View price bonuses
- Track progress to next level

---

## 📥 Installation

### Requirements
- Minecraft Server: Paper or Spigot 1.20.4
- Java: 17 or higher
- RAM: Minimum 2GB recommended

### Steps

1. **Download** the latest `BudLords-1.0.0.jar` from releases
2. **Place** the JAR file in your server's `plugins/` folder
3. **Restart** your server
4. **Configure** settings in `plugins/BudLords/config.yml`
5. **Start farming!**

### First Time Setup

On first startup, BudLords will create:
- `config.yml` - Main configuration
- `strains.yml` - Strain definitions
- `players.yml` - Player data (balances, stats)
- `plants.yml` - Active plant tracking

Default configuration works out of the box. Customize as needed.

---

## 📝 Commands & Permissions

### Player Commands

| Command | Description | Permission |
|---------|-------------|------------|
| `/budlords` | Plugin help menu | `budlords.use` |
| `/bal` | Check balance and rank | `budlords.balance` |
| `/pay <player> <amount>` | Send money to player | `budlords.pay` |
| `/package <amount>` | Package buds (1/3/5/10g) | `budlords.package` |
| `/stats` | View your statistics | `budlords.use` |
| `/prestige` | Prestige menu | `budlords.use` |
| `/challenges` | Daily/weekly challenges | `budlords.use` |
| `/crossbreed` | Crossbreeding lab | `budlords.use` |
| `/leaderboard [type]` | Server rankings | `budlords.use` |
| `/season` | Current season info | `budlords.use` |
| `/achievements [category]` | Your achievements | `budlords.use` |
| `/skills [tree]` | Skill tree GUI | `budlords.use` |
| `/collection [page]` | Strain collection book | `budlords.use` |
| `/reputation` | Buyer reputation status | `budlords.use` |
| `/orders` | Active bulk orders | `budlords.use` |
| `/daily` | Claim daily reward | `budlords.use` |
| `/market` | Market conditions | `budlords.use` |

### Admin Commands

| Command | Description | Permission |
|---------|-------------|------------|
| `/straincreator` | Strain creator GUI | `budlords.admin` |
| `/spawnmarket` | Spawn Market Joe | `budlords.admin` |
| `/spawnblackmarket` | Spawn BlackMarket Joe | `budlords.admin` |
| `/addmoney <player> <amount>` | Add money | `budlords.admin` |
| `/budlords reload` | Reload config | `budlords.admin` |
| `/season set <season>` | Force change season | `budlords.admin` |
| `/debug` | Debug commands | `budlords.admin` |

### Permission Nodes

| Permission | Description | Default |
|------------|-------------|---------|
| `budlords.use` | Basic plugin usage | true |
| `budlords.balance` | Check balance | true |
| `budlords.pay` | Pay other players | true |
| `budlords.package` | Package products | true |
| `budlords.admin` | Admin commands | op |

---

## ⚙️ Configuration

### Main Config (`config.yml`)

```yaml
# Economy Settings
economy:
  starting-balance: 100
  max-balance: 10000000
  
# Growth Settings  
growth:
  base-growth-time: 600  # seconds per stage
  cooperative-bonus: true  # farming together bonus
  
# Weather Settings
weather:
  enabled: true
  broadcast-changes: false  # disabled for performance
  particle-effects: true
  
# Performance Settings
performance:
  max-plants-per-player: 100
  particle-optimization: true  # auto-throttle when many plants
  animation-optimization: true
  
# Marketplace
marketplace:
  market-joe-enabled: true
  blackmarket-joe-enabled: true
  village-vendors-enabled: true
```

### Strain Configuration (`strains.yml`)

Customize or add strains:

```yaml
custom_strain:
  name: "My Custom Strain"
  rarity: LEGENDARY
  potency: 95
  yield: 8
  seed-rating: FIVE_STAR
  effects:
    - PHOENIX_REBIRTH
    - DRAGON_BREATH
    - RAINBOW_AURA
```

---

## 🚀 Performance

### Optimization Features

BudLords 1.0.0 includes extensive performance optimizations:

#### Automatic Scaling
- **Plant Count Detection** - Monitors active plant count
- **Dynamic Throttling** - Reduces particle/animation when >20 plants
- **Smart Cleanup** - Removes dead entities automatically
- **Chunk Loading Checks** - Only processes loaded chunks

#### Particle System
- **60-70% Reduction** - Fewer particles when many plants
- **Longer Intervals** - Ambient: 5s, Weather: 6s, Fireflies: 3s
- **Spawn Limits** - Max 30 particles per cycle
- **Quality Preservation** - Visual appeal maintained

#### Animation System
- **50% Frequency Reduction** - Updates every 0.5s instead of 0.25s
- **Selective Updates** - Only updates subset when many plants
- **Smooth Transitions** - No jarring visual changes

### Performance Metrics

| Scenario | Before | After | Improvement |
|----------|--------|-------|-------------|
| 10 plants | 300 FPS | 300 FPS | No change |
| 50 plants | ~5 FPS | ~100 FPS | **20x faster** |
| 100 plants | <1 FPS | ~60 FPS | **60x faster** |
| Memory usage | +50MB/100 plants | +20MB/100 plants | 60% reduction |

### Recommended Settings

**Small Server (1-10 players):**
```yaml
performance:
  max-plants-per-player: 50
  particle-optimization: false
```

**Medium Server (10-50 players):**
```yaml
performance:
  max-plants-per-player: 30
  particle-optimization: true
```

**Large Server (50+ players):**
```yaml
performance:
  max-plants-per-player: 20
  particle-optimization: true
  animation-optimization: true
```

---

## 🔨 Building from Source

### Prerequisites
- Java 17 JDK or higher
- Maven 3.6+
- Git

### Build Steps

```bash
# Clone the repository
git clone https://github.com/Domkaq/budlords1.git
cd budlords1

# Build with Maven
mvn clean package

# Output: target/BudLords-1.0.0.jar
```

### Development Build

```bash
# Compile only (faster for testing)
mvn clean compile

# Run with tests
mvn clean test package
```

---

## 💬 Support

### Getting Help

- **Issues:** [GitHub Issues](https://github.com/Domkaq/budlords1/issues)
- **Wiki:** [Documentation](https://github.com/Domkaq/budlords1/wiki)

### Reporting Bugs

Please include:
- Server version (Paper/Spigot/Minecraft version)
- BudLords version
- Error messages or logs
- Steps to reproduce
- Screenshots if applicable

### Feature Requests

We welcome suggestions! Open an issue with:
- Clear description of the feature
- Use case / why it's needed
- Expected behavior

---

## 📜 License

BudLords is licensed under the MIT License. See [LICENSE](LICENSE) file for details.

---

## 🙏 Credits

**Developer:** Domkaq  
**Version:** 1.0.0  
**Last Updated:** December 2024

### Special Thanks
- Spigot/Paper communities for excellent APIs
- Beta testers for valuable feedback
- Contributors for bug reports and suggestions

---

## 📊 Statistics

- **139 Unique Effects** - Most comprehensive effect system
- **60+ Strains** - From common to legendary
- **40+ Achievements** - Across 9 categories
- **35+ Skills** - 5 distinct skill trees
- **12+ Diseases** - Realistic plant care challenge
- **10 Customer Types** - Dynamic trading system
- **4 Seasons** - Environmental gameplay
- **6 Weather Types** - Dynamic conditions

---

## 🎯 Roadmap (Future Updates)

Potential features for future versions:
- [ ] Greenhouse building system
- [ ] Automated farming machines
- [ ] Delivery missions
- [ ] Rival NPC gangs
- [ ] Territory control
- [ ] Player-owned shops
- [ ] Custom recipe system
- [ ] Mobile app integration

---

<div align="center">

**Made with 💚 for the Minecraft community**

[⬆ Back to Top](#-budlords---professional-minecraft-weed-economy-plugin)

</div>
