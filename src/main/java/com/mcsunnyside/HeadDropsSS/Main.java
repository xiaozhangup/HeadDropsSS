package com.mcsunnyside.HeadDropsSS;

import com.destroystokyo.paper.profile.PlayerProfile;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Material;
import org.bukkit.block.BlockState;
import org.bukkit.block.Skull;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.ItemFrame;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.plugin.java.JavaPlugin;

public class Main extends JavaPlugin implements Listener {
    private boolean dropSkull = false;
    private boolean scanChunk = true;
    private boolean scanItemFrame = false;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        this.dropSkull = getConfig().getBoolean("drop-skull");
        this.scanItemFrame = getConfig().getBoolean("scan-itemframe");
        this.scanChunk = getConfig().getBoolean("scan-chunk",true);
        Bukkit.getPluginManager().registerEvents(this, this);
        Bukkit.getWorlds().forEach((world -> {
            if (scanItemFrame) {
                for (Entity entity : world.getEntities()) {
                    checkSkullInItemFrame(entity);
                }
            }
            if(scanChunk){
                for (Chunk chunk : world.getLoadedChunks()) {
                    for (BlockState state : chunk.getTileEntities()) {
                        checkAndRemoveSkull(state);
                    }
                }
            }
        }));
    }

    private void checkSkullInItemFrame(Entity entity) {
        if (entity.getType() == EntityType.ITEM_FRAME) {
            ItemFrame itemFrame = (ItemFrame) entity;
            ItemStack stack = itemFrame.getItem();
            if (stack.getType() != Material.PLAYER_HEAD || stack.getType() != Material.PLAYER_WALL_HEAD) {
                SkullMeta skull = (SkullMeta) itemFrame.getItem().getItemMeta();
                if (skull.getPlayerProfile() != null) {
                    PlayerProfile playerProfile = skull.getPlayerProfile();
                    if (!playerProfile.hasTextures()) {
                        itemFrame.setItem(new ItemStack(Material.AIR));
                        getLogger().info("Removed skull at " + itemFrame.getLocation() + " in the ItemFrame cause this skull will client laggy");
                    }
                }
            }
        }
    }

    private void checkAndRemoveSkull(BlockState state) {
        Material blockType = state.getType();
        if (blockType == Material.PLAYER_HEAD || blockType == Material.PLAYER_WALL_HEAD) {
            Skull skull = (Skull) state;
            if (skull.getPlayerProfile() != null) {
                PlayerProfile playerProfile = skull.getPlayerProfile();
                if (!playerProfile.hasTextures()) {
                    skull.getBlock().setType(Material.AIR);
                    getLogger().info("Removed skull at " + skull.getLocation() + " cause this skull will client laggy");
                }
            }
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void playerDeath(PlayerDeathEvent e) {
        if (!dropSkull) {
            return;
        }
        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        if (!e.getEntity().getPlayerProfile().hasTextures()) {
            getLogger().info("Cancel drop skull for player " + e.getEntity().getName() + " cause this player no skin");
            return;
        }
        ItemMeta meta = head.getItemMeta();
        SkullMeta skullMeta = (SkullMeta) meta;
        skullMeta.setPlayerProfile(e.getEntity().getPlayerProfile());
        head.setItemMeta(skullMeta);
        e.getEntity().getLocation().getWorld().dropItem(e.getEntity().getLocation(), head);
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onChunkLoad(ChunkLoadEvent event) {
        if(scanChunk) {
            for (BlockState state : event.getChunk().getTileEntities()) {
                checkAndRemoveSkull(state);
            }
        }
        if (scanItemFrame) {
            for (Entity entity : event.getChunk().getEntities()) {
                checkSkullInItemFrame(entity);
            }
        }
    }

}
