# Complete System Refactor Plan

## Overview
Remove old ReputationManager system and fully integrate with BuyerRegistry/IndividualBuyer system. Enhance sale GUI to be modern, professional, and fully connected with all game systems.

## Phase 1: Remove Reputation System
### Files to Modify:
1. **BudLords.java** - Remove ReputationManager initialization
2. **MobSaleGUI.java** - Remove all reputation-based calculations
3. **BuyerProfileGUI.java** - Remove reputation display
4. **NPCManager.java** - Remove reputation references
5. **HagglingManager.java** - Rework to use buyer relationships
6. **ReputationCommand.java** - Delete or rework to show buyer relationships
7. **DebugCommand.java** - Remove reputation debug commands

### Data Migration:
- No data migration needed - buyer relationships already track this

## Phase 2: Enhance IndividualBuyer System
### Features to Add:
1. **Relationship Levels**
   - Stranger (0 purchases)
   - Acquaintance (1-5 purchases)
   - Friend (6-20 purchases)
   - Trusted (21-50 purchases)
   - Partner (51-100 purchases)
   - VIP (101+ purchases)

2. **Relationship Benefits**
   - Price multipliers based on relationship level
   - Tip chances
   - Success rate bonuses
   - Special request access

3. **Purchase History Tracking**
   - Track quality preferences
   - Track strain preferences
   - Track purchase patterns
   - Recommend buyers for new sales

## Phase 3: Redesign Sale GUI
### Modern UI Features:
1. **Header Section**
   - Buyer avatar/icon
   - Buyer name with personality color
   - Relationship level display
   - Current mood indicator

2. **Sale Slots** (Current 4 slots is good)
   - Visual feedback on hover
   - Drag and drop support
   - Quick info tooltips
   - Price preview per item

3. **Info Panel**
   - Real-time price calculation
   - Bonus breakdown
   - Success chance indicator
   - Relationship progress bar

4. **Action Buttons**
   - Confirm Sale (green)
   - View Buyer Profile (blue)
   - Special Requests (gold)
   - Cancel (red)

5. **Bonus Display**
   - Skill bonuses
   - Prestige bonuses
   - Relationship bonuses
   - Strain preference bonuses
   - Quality preference bonuses
   - Bulk bonuses
   - Special event bonuses

## Phase 4: Connect All Systems
### Integrations:
1. **Skills** - Display active skill bonuses in sale GUI
2. **Prestige** - Show prestige multipliers
3. **Joint Effects** - Allow selling joints with effect bonuses
4. **Rank System** - Rank-based access to premium buyers
5. **Market Events** - Show active market events in GUI
6. **Buyer Network** - Show referral opportunities
7. **Special Events** - Highlight special buyer events

## Phase 5: Enhanced Features
### New Mechanics:
1. **Negotiation System**
   - Counter-offers from buyers
   - Haggling minigame
   - Relationship-based success

2. **Bulk Deals**
   - Package deals with bonuses
   - Multi-buyer sales
   - Consignment options

3. **Special Orders**
   - Custom requests from buyers
   - Time-limited opportunities
   - Bonus rewards

4. **Analytics**
   - Sale history
   - Price trends
   - Best customers
   - Profit tracking

## Implementation Priority:
1. ✅ Fix NEW REQUEST spam (DONE)
2. ✅ Add Fast Harvest skill (DONE)
3. ✅ Fix minigame bug (DONE)
4. ⏳ Remove ReputationManager
5. ⏳ Enhance IndividualBuyer
6. ⏳ Redesign MobSaleGUI
7. ⏳ Connect all systems
8. ⏳ Add enhanced features

## Estimated Effort:
- Phase 1: 2-3 hours (Removal)
- Phase 2: 3-4 hours (Enhancement)
- Phase 3: 4-6 hours (GUI Redesign)
- Phase 4: 3-4 hours (Integration)
- Phase 5: 5-7 hours (New Features)

**Total: 17-24 hours of development**

## Risk Assessment:
- **High Risk**: Removing ReputationManager breaks existing saves
- **Medium Risk**: GUI redesign may need extensive testing
- **Low Risk**: System integration (already designed for this)

## Recommendation:
This is a major refactor that should be done in a separate branch with:
- Comprehensive testing
- Multiplayer stress testing
- Data migration strategy
- Rollback plan

The current bug fixes (NEW REQUEST spam, Fast Harvest, minigame fix) are complete and safe to merge.
The full refactor should be planned as a separate major update (v4.0.0).
