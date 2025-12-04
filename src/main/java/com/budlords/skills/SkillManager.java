package com.budlords.skills;

import com.budlords.BudLords;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages player skills and skill trees in BudLords v2.0.0.
 */
public class SkillManager implements InventoryHolder {

    private final BudLords plugin;
    
    // Player skills: UUID -> Set of unlocked skill names
    private final Map<UUID, Set<String>> playerSkills;
    
    // Player skill points: UUID -> available points
    private final Map<UUID, Integer> skillPoints;
    
    // Player XP per tree: UUID -> (SkillTree -> XP)
    private final Map<UUID, Map<String, Integer>> treeXP;
    
    // Data file
    private File skillsFile;
    private FileConfiguration skillsConfig;

    public SkillManager(BudLords plugin) {
        this.plugin = plugin;
        this.playerSkills = new ConcurrentHashMap<>();
        this.skillPoints = new ConcurrentHashMap<>();
        this.treeXP = new ConcurrentHashMap<>();
        
        loadSkills();
        
        plugin.getLogger().info("✦ Skill System initialized with " + Skill.values().length + " skills");
    }

    private void loadSkills() {
        skillsFile = new File(plugin.getDataFolder(), "skills.yml");
        if (!skillsFile.exists()) {
            try {
                skillsFile.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().warning("Failed to create skills file: " + e.getMessage());
            }
        }
        skillsConfig = YamlConfiguration.loadConfiguration(skillsFile);
        
        ConfigurationSection playersSection = skillsConfig.getConfigurationSection("players");
        if (playersSection != null) {
            for (String uuidStr : playersSection.getKeys(false)) {
                try {
                    UUID uuid = UUID.fromString(uuidStr);
                    
                    // Load unlocked skills
                    List<String> unlocked = playersSection.getStringList(uuidStr + ".skills");
                    playerSkills.put(uuid, new HashSet<>(unlocked));
                    
                    // Load skill points
                    int points = playersSection.getInt(uuidStr + ".points", 0);
                    skillPoints.put(uuid, points);
                    
                    // Load tree XP
                    ConfigurationSection xpSection = playersSection.getConfigurationSection(uuidStr + ".tree-xp");
                    if (xpSection != null) {
                        Map<String, Integer> xpMap = new HashMap<>();
                        for (String tree : xpSection.getKeys(false)) {
                            xpMap.put(tree, xpSection.getInt(tree));
                        }
                        treeXP.put(uuid, xpMap);
                    }
                } catch (Exception e) {
                    plugin.getLogger().warning("Failed to load skills for " + uuidStr);
                }
            }
        }
    }

    public void saveSkills() {
        skillsConfig.set("players", null);
        
        for (UUID uuid : playerSkills.keySet()) {
            String path = "players." + uuid.toString();
            
            // Save unlocked skills
            Set<String> skills = playerSkills.get(uuid);
            skillsConfig.set(path + ".skills", new ArrayList<>(skills));
            
            // Save skill points
            skillsConfig.set(path + ".points", skillPoints.getOrDefault(uuid, 0));
            
            // Save tree XP
            Map<String, Integer> xp = treeXP.get(uuid);
            if (xp != null) {
                for (Map.Entry<String, Integer> entry : xp.entrySet()) {
                    skillsConfig.set(path + ".tree-xp." + entry.getKey(), entry.getValue());
                }
            }
        }
        
        try {
            skillsConfig.save(skillsFile);
        } catch (IOException e) {
            plugin.getLogger().warning("Failed to save skills: " + e.getMessage());
        }
    }

    /**
     * Adds skill points to a player.
     */
    public void addSkillPoints(UUID uuid, int amount) {
        int current = skillPoints.getOrDefault(uuid, 0);
        skillPoints.put(uuid, current + amount);
    }

    /**
     * Gets available skill points for a player.
     */
    public int getSkillPoints(UUID uuid) {
        return skillPoints.getOrDefault(uuid, 0);
    }

    /**
     * Adds XP to a specific skill tree.
     */
    public void addTreeXP(UUID uuid, Skill.SkillTree tree, int amount) {
        Map<String, Integer> xp = treeXP.computeIfAbsent(uuid, k -> new HashMap<>());
        int current = xp.getOrDefault(tree.name(), 0);
        xp.put(tree.name(), current + amount);
    }

    /**
     * Gets XP for a specific skill tree.
     */
    public int getTreeXP(UUID uuid, Skill.SkillTree tree) {
        Map<String, Integer> xp = treeXP.get(uuid);
        return xp != null ? xp.getOrDefault(tree.name(), 0) : 0;
    }

    /**
     * Attempts to unlock a skill for a player.
     */
    public boolean unlockSkill(Player player, Skill skill) {
        UUID uuid = player.getUniqueId();
        Set<String> skills = playerSkills.computeIfAbsent(uuid, k -> new HashSet<>());
        
        // Check if already unlocked
        if (skills.contains(skill.name())) {
            player.sendMessage("§cYou already have this skill!");
            return false;
        }
        
        // Check skill point cost
        int cost = skill.getSkillPointCost();
        int available = skillPoints.getOrDefault(uuid, 0);
        if (available < cost) {
            player.sendMessage("§cYou need §e" + cost + " §cskill points! (You have: §e" + available + "§c)");
            return false;
        }
        
        // Check XP requirement
        int requiredXP = skill.getRequiredXP();
        int currentXP = getTreeXP(uuid, skill.getTree());
        if (currentXP < requiredXP) {
            player.sendMessage("§cYou need §e" + requiredXP + " §c" + skill.getTree().getDisplayName() + 
                              " XP! (You have: §e" + currentXP + "§c)");
            return false;
        }
        
        // Check tier prerequisites
        if (skill.getTier() > 1) {
            boolean hasPreviousTier = false;
            for (Skill s : Skill.values()) {
                if (s.getTree() == skill.getTree() && s.getTier() == skill.getTier() - 1) {
                    if (skills.contains(s.name())) {
                        hasPreviousTier = true;
                        break;
                    }
                }
            }
            if (!hasPreviousTier) {
                player.sendMessage("§cYou must unlock a Tier " + (skill.getTier() - 1) + " skill first!");
                return false;
            }
        }
        
        // Unlock skill
        skills.add(skill.name());
        skillPoints.put(uuid, available - cost);
        
        // Send notification
        sendSkillUnlockNotification(player, skill);
        
        // Save
        saveSkills();
        
        return true;
    }

    private void sendSkillUnlockNotification(Player player, Skill skill) {
        player.sendMessage("");
        player.sendMessage("§8§l═══════════════════════════════════════════");
        player.sendMessage("§a§l   ✦ SKILL UNLOCKED! ✦");
        player.sendMessage("");
        player.sendMessage("   " + skill.getColoredName());
        player.sendMessage("   §7" + skill.getDescription());
        player.sendMessage("");
        player.sendMessage("   §7Tree: " + skill.getTree().getColor() + skill.getTree().getDisplayName());
        player.sendMessage("§8§l═══════════════════════════════════════════");
        player.sendMessage("");
        
        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.2f);
        player.spawnParticle(Particle.VILLAGER_HAPPY, player.getLocation().add(0, 1, 0), 
            30, 0.5, 0.5, 0.5, 0.05);
    }

    /**
     * Checks if a player has a specific skill.
     */
    public boolean hasSkill(UUID uuid, Skill skill) {
        Set<String> skills = playerSkills.get(uuid);
        return skills != null && skills.contains(skill.name());
    }

    /**
     * Gets the total bonus for a specific bonus type for a player.
     */
    public double getTotalBonus(UUID uuid, Skill.BonusType bonusType) {
        Set<String> skills = playerSkills.get(uuid);
        if (skills == null) return 0;
        
        double total = 0;
        for (String skillName : skills) {
            try {
                Skill skill = Skill.valueOf(skillName);
                for (Skill.SkillBonus bonus : skill.getBonuses()) {
                    if (bonus.getType() == bonusType) {
                        total += bonus.getValue();
                    }
                }
            } catch (IllegalArgumentException ignored) {}
        }
        return total;
    }

    /**
     * Gets the total bonus multiplier (1.0 + bonus).
     */
    public double getBonusMultiplier(UUID uuid, Skill.BonusType bonusType) {
        return 1.0 + getTotalBonus(uuid, bonusType);
    }

    /**
     * Opens the skill tree GUI for a player.
     */
    @SuppressWarnings("deprecation")
    public void openSkillTreeGUI(Player player, Skill.SkillTree selectedTree) {
        Inventory inv = Bukkit.createInventory(this, 54, 
            "§a§l✦ Skill Tree - " + selectedTree.getDisplayName() + " ✦");
        
        UUID uuid = player.getUniqueId();
        Set<String> unlockedSkills = playerSkills.getOrDefault(uuid, new HashSet<>());
        int points = skillPoints.getOrDefault(uuid, 0);
        int treeXPAmount = getTreeXP(uuid, selectedTree);
        
        // Border
        ItemStack border = createItem(Material.BLACK_STAINED_GLASS_PANE, " ", null);
        for (int i = 0; i < 9; i++) {
            inv.setItem(i, border);
            inv.setItem(45 + i, border);
        }
        for (int i = 9; i < 45; i += 9) {
            inv.setItem(i, border);
            inv.setItem(i + 8, border);
        }
        
        // Tree selection at bottom
        int treeSlot = 46;
        for (Skill.SkillTree tree : Skill.SkillTree.values()) {
            Material mat = tree == selectedTree ? Material.EMERALD : Material.COAL;
            ItemStack treeItem = createItem(mat, tree.getColor() + tree.getDisplayName(),
                Arrays.asList("", "§7" + tree.getDescription(), "", "§eClick to view"));
            inv.setItem(treeSlot++, treeItem);
        }
        
        // Player info
        inv.setItem(4, createItem(Material.PLAYER_HEAD, 
            "§e" + player.getName() + "'s Skills",
            Arrays.asList(
                "",
                "§7Available Points: §a" + points,
                "§7" + selectedTree.getDisplayName() + " XP: §e" + treeXPAmount,
                ""
            )));
        
        // Display skills in tier layout
        // Tier 1: slots 19, 22, 25
        // Tier 2: slots 28, 31, 34
        // Tier 3: slots 37, 40 (centered)
        int[][] tierSlots = {
            {19, 22, 25},  // Tier 1
            {28, 31, 34},  // Tier 2
            {37, 40, -1}   // Tier 3 (only 2 skills usually)
        };
        
        Map<Integer, List<Skill>> skillsByTier = new HashMap<>();
        for (Skill skill : Skill.values()) {
            if (skill.getTree() == selectedTree) {
                skillsByTier.computeIfAbsent(skill.getTier(), k -> new ArrayList<>()).add(skill);
            }
        }
        
        for (int tier = 1; tier <= 3; tier++) {
            List<Skill> tierSkills = skillsByTier.getOrDefault(tier, new ArrayList<>());
            int[] slots = tierSlots[tier - 1];
            
            for (int i = 0; i < tierSkills.size() && i < slots.length && slots[i] >= 0; i++) {
                Skill skill = tierSkills.get(i);
                boolean unlocked = unlockedSkills.contains(skill.name());
                boolean canUnlock = canUnlockSkill(uuid, skill);
                
                Material material;
                String statusLine;
                
                if (unlocked) {
                    material = skill.getIconMaterial();
                    statusLine = "§a✓ UNLOCKED";
                } else if (canUnlock) {
                    material = Material.LIME_DYE;
                    statusLine = "§eClick to unlock (" + skill.getSkillPointCost() + " points)";
                } else {
                    material = Material.GRAY_DYE;
                    statusLine = "§cLocked";
                }
                
                List<String> lore = new ArrayList<>();
                lore.add("");
                lore.add("§7" + skill.getDescription());
                lore.add("");
                lore.add("§7Tier: §e" + skill.getTier());
                lore.add("§7Cost: §e" + skill.getSkillPointCost() + " points");
                if (skill.getRequiredXP() > 0) {
                    lore.add("§7Required XP: §e" + skill.getRequiredXP());
                }
                lore.add("");
                
                // Show bonuses
                for (Skill.SkillBonus bonus : skill.getBonuses()) {
                    String bonusText = formatBonus(bonus);
                    lore.add("§a+ " + bonusText);
                }
                
                lore.add("");
                lore.add(statusLine);
                
                String name = unlocked ? skill.getColoredName() : "§7" + skill.getDisplayName();
                inv.setItem(slots[i], createItem(material, name, lore));
            }
        }
        
        // Tier labels
        inv.setItem(10, createItem(Material.PAPER, "§eTier 1", 
            Arrays.asList("", "§7Basic skills", "§7No prerequisites")));
        inv.setItem(11, createItem(Material.ARROW, "§7→", null));
        
        inv.setItem(27, createItem(Material.PAPER, "§6Tier 2", 
            Arrays.asList("", "§7Advanced skills", "§7Requires Tier 1 + XP")));
        
        inv.setItem(36, createItem(Material.PAPER, "§c§lTier 3", 
            Arrays.asList("", "§7Master skills", "§7Requires Tier 2 + XP")));
        
        player.openInventory(inv);
        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_CHIME, 0.5f, 1.0f);
    }

    private boolean canUnlockSkill(UUID uuid, Skill skill) {
        Set<String> skills = playerSkills.getOrDefault(uuid, new HashSet<>());
        
        // Already unlocked
        if (skills.contains(skill.name())) return false;
        
        // Check points
        if (skillPoints.getOrDefault(uuid, 0) < skill.getSkillPointCost()) return false;
        
        // Check XP
        if (getTreeXP(uuid, skill.getTree()) < skill.getRequiredXP()) return false;
        
        // Check tier prerequisites
        if (skill.getTier() > 1) {
            boolean hasPreviousTier = false;
            for (Skill s : Skill.values()) {
                if (s.getTree() == skill.getTree() && s.getTier() == skill.getTier() - 1) {
                    if (skills.contains(s.name())) {
                        hasPreviousTier = true;
                        break;
                    }
                }
            }
            if (!hasPreviousTier) return false;
        }
        
        return true;
    }

    private String formatBonus(Skill.SkillBonus bonus) {
        String name = bonus.getType().name().replace("_", " ").toLowerCase();
        // Capitalize first letter of each word
        String[] words = name.split(" ");
        StringBuilder formatted = new StringBuilder();
        for (String word : words) {
            if (!formatted.isEmpty()) formatted.append(" ");
            formatted.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1));
        }
        
        if (bonus.getValue() < 1) {
            return formatted + " +" + (int)(bonus.getValue() * 100) + "%";
        } else {
            return formatted + " +" + (int)bonus.getValue();
        }
    }

    /**
     * Gets the count of unlocked skills for a player.
     */
    public int getUnlockedCount(UUID uuid) {
        Set<String> skills = playerSkills.get(uuid);
        return skills != null ? skills.size() : 0;
    }

    private ItemStack createItem(Material material, String name, List<String> lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            if (lore != null) meta.setLore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }

    @Override
    public Inventory getInventory() {
        return null;
    }
}
