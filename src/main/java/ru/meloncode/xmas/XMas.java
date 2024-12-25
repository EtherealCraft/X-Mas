package ru.meloncode.xmas;

import com.destroystokyo.paper.profile.PlayerProfile;
import com.destroystokyo.paper.profile.ProfileProperty;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.Skull;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;
import ru.meloncode.xmas.utils.TextUtils;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static ru.meloncode.xmas.Main.RANDOM;

class XMas {

    private static final ConcurrentHashMap<UUID, MagicTree> trees = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<Long, List<MagicTree>> trees_byChunk = new ConcurrentHashMap<>();
    public static ItemStack XMAS_CRYSTAL;

    public static void createMagicTree(Player player, Location loc) {
        MagicTree tree = new MagicTree(player.getUniqueId(), TreeLevel.SAPLING, loc);
        trees.put(tree.getTreeUID(), tree);
        trees_byChunk.computeIfAbsent(tree.getLocation().getChunk().getChunkKey(), aLong -> new ArrayList<>()).add(tree);
        tree.save();
    }

    public static void addMagicTree(MagicTree tree) {
        trees.put(tree.getTreeUID(), tree);
        long chunkId = tree.getLocation().getChunk().getChunkKey();
        if (trees_byChunk.containsKey(chunkId)) {
            trees_byChunk.get(chunkId).add(tree);
        } else {
            trees_byChunk.put(chunkId, new ArrayList<>(List.of(tree)));
        }
        tree.build();
    }

    public static void updateMagicTree(MagicTree tree, long id) {
        ListIterator<MagicTree> iterator = trees_byChunk.get(id).listIterator();
        while (iterator.hasNext()) {
            MagicTree next = iterator.next();
            if (next.getLocation().equals(tree.getLocation())) {
                iterator.set(tree);
            }
        }
    }

    public static void printTreeChunkIds() {
        Main.getInstance().getLogger().info("The chunks containing trees are:");
        for (Long id : trees_byChunk.keySet()) {
            Main.getInstance().getLogger().info("Id : " + id + " has " + trees_byChunk.get(id).size() + " trees, the first of which is located at " + trees_byChunk.get(id).get(0).getLocation());
        }
    }

    public static Collection<MagicTree> getAllTrees() {
        return trees.values();
    }

    public static boolean treesContainsId(long id) {
        return trees_byChunk.containsKey(id);
    }

    @Nullable
    public static Collection<MagicTree> getAllTreesInChunk(Chunk chunk) {
        return trees_byChunk.get(chunk.getChunkKey());
    }

    public static void removeTree(MagicTree tree) {
        tree.unbuild();
        TreeSerializer.removeTree(tree);
        trees.remove(tree.getTreeUID());
        trees_byChunk.remove(tree.getLocation().getChunk().getChunkKey());
    }

    public static void processPresent(Block block, Player player) {
        if (block.getType() == Material.PLAYER_HEAD) {
            Skull skull = (Skull) block.getState();
            PlayerProfile profile = skull.getPlayerProfile();
            if (profile == null) return;
            boolean isPresentHead = false;
            for (ProfileProperty property : profile.getProperties()) {
                if (property.getName().equals("textures") && Main.getHeads().contains(property.getValue()))
                {
                    isPresentHead = true;
                }
            }
            if (!isPresentHead) return;

            Location loc = block.getLocation();
            World world = loc.getWorld();
            if (world != null) {
                if (RANDOM.nextFloat() < Main.LUCK_CHANCE || !Main.LUCK_CHANCE_ENABLED) {
                    world.dropItemNaturally(loc, new ItemStack(Main.gifts.get(RANDOM.nextInt(Main.gifts.size()))));
                    Effects.TREE_SWAG.playEffect(loc);
                    TextUtils.sendMessage(player, LocaleManager.GIFT_LUCK);
                } else {
                    Effects.SMOKE.playEffect(loc);
                    world.dropItemNaturally(loc, new ItemStack(Material.COAL));
                    TextUtils.sendMessage(player, LocaleManager.GIFT_FAIL);
                }
            }
            block.setType(Material.AIR);
        }
    }

    public static List<MagicTree> getTreesPlayerOwn(Player player) {
        List<MagicTree> own = new ArrayList<>();
        for (MagicTree cTree : getAllTrees())
            if (cTree.getOwner().equals(player.getUniqueId()))
                own.add(cTree);
        return own;
    }

    public static MagicTree getTree(UUID treeUID) {
        return trees.get(treeUID);
    }
}
