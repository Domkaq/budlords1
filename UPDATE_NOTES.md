# BudLords - Major Update Notes

## Version: 3.6.0 - Bug Fixes & Balance Update

### Release Date: December 2024

---

## 🐛 Critical Bug Fixes

### Sale GUI Shift-Click Bug
**Problem**: Items were disappearing when using shift-right-click in the sale GUI.

**Solution**: 
- Added proper shift-click handling in MobSaleGUI
- Items are now correctly returned to player inventory
- If inventory is full, items are dropped at player location

### Phone Not Updating for Joint Sales
**Problem**: When selling joints to buyers, the buyer's phone profile wasn't updating with purchase history.

**Solution**:
- Fixed item iteration in MobSaleGUI to properly handle both packages and joints
- Purchase tracking now works for all product types
- Buyer purchase history displays correctly in phone

### Phone GUI Buttons Not Working
**Problem**: Buttons in BuyerDetailGUI (purchase history, special requests, back to registry) were non-functional.

**Solution**:
- Implemented click handlers for purchase history (slot 14)
- Implemented click handler for special requests (slot 12)
- Fixed back button (slot 45) to properly open BuyerListGUI
- Added session tracking for viewed buyers

---

## 🏥 Disease System Overhaul

### Simplified Cure System
**Before**: 7 different cure types (Fungicide, Antibacterial Spray, Pesticide, Nutrient Flush, Neem Oil, Golden Elixir, Healing Salve)

**After**: Single Universal Plant Cure that works for all diseases
- 90% effectiveness across all disease types
- No need to match cure to disease category
- Cleaner inventory management
- More accessible for new players

### Private Disease Notifications
**Before**: Disease infections were broadcast to all nearby players in chat

**After**: Private notifications to plant owner only
- Owner receives notification with phone bell sound
- No public chat spam
- Plant deaths from disease only notify owner
- Better multiplayer experience

---

## ⚖️ Gameplay Balance

### Seed Star Progression
**Problem**: Seeds were getting 5-star ratings too easily, breaking progression.

**Solution**: Implemented farming XP-based gates
```
Farming XP     | Max Seed Stars | Skill Level
---------------|----------------|-------------
0-99 XP        | 2★             | Beginner
100-499 XP     | 3★             | Intermediate
500-999 XP     | 4★             | Advanced
1000+ XP       | 5★             | Expert
```

**Benefits**:
- Proper progression curve
- Rewards consistent farming
- Prevents early-game imbalance
- Encourages skill development

---

## 📊 System Architecture Notes

### Reputation vs Buyer Systems

The game uses TWO complementary systems (this is intentional):

1. **ReputationManager** (Buyer Type System)
   - Manages reputation with buyer TYPES (Market Joe, BlackMarket Joe, etc.)
   - Affects: Tips, price bonuses, success chances
   - Per-type progression

2. **BuyerRegistry + IndividualBuyer** (Individual System)
   - Tracks individual NPCs with unique personalities
   - Manages: Purchase history, preferences, special requests
   - Personal relationships

**These work together**:
- Type reputation sets baseline pricing
- Individual relationships add personalization
- Both properly integrated in sale transactions

---

## 🔧 Technical Details

### Files Modified
1. `MobSaleGUI.java` - Sale GUI fixes and buyer tracking
2. `PlantDisease.java` - Cure consolidation
3. `DiseaseManager.java` - Private notifications
4. `BuyerDetailGUI.java` - Button functionality
5. `FarmingListener.java` - Seed progression balance

### Backward Compatibility
- All changes are backward compatible
- No data migration required
- Existing saves work without changes

### Multiplayer Stability
- Thread-safe collections (ConcurrentHashMap)
- Proper synchronization
- No race conditions introduced
- Tested for concurrent player access

---

## 🎮 Gameplay Impact

### For New Players
- Simpler disease management (one cure)
- Less overwhelming notifications
- Clearer progression path for seeds
- Better phone GUI usability

### For Experienced Players
- More challenging seed progression
- Rewards farming XP investment
- Cleaner multiplayer experience
- No gameplay mechanics removed

### For Server Owners
- Reduced chat spam
- Better player retention (proper progression)
- More stable multiplayer
- No configuration changes needed

---

## 📝 Known Limitations

### Out of Scope (For Future Updates)
The following suggestions require major redesign and are deferred:
- Area damage/utility mechanics for joint effects
- Enhanced movement/exploration mechanics
- Dynamic joint effect system

These will be considered for future major updates with proper design and testing.

---

## 🚀 Installation

1. Stop your server
2. Replace old BudLords.jar with new version
3. Start server
4. All changes apply automatically

No configuration files need to be edited.

---

## 📞 Support

For issues or questions:
- Open an issue on GitHub
- Check the wiki for guides
- Join the Discord community

---

## 🙏 Credits

Special thanks to the community for bug reports and feedback that made this update possible.
