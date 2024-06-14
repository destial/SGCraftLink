package xyz.destiall.sgcraftlink.items.gui;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class GUIItem {
    private final GUIViewer viewer;
    private final List<String> clickActions;
    private final int slot;
    private final ItemStack item;

    public GUIItem(GUIViewer guiViewer, int slot, ConfigurationSection section) {
        this.viewer = guiViewer;
        this.slot = slot;
        clickActions = new ArrayList<>();
        item = guiViewer.parseItem(section, "item");

        if (section.contains("on-click")) {
            clickActions.addAll(section.getStringList("on-click"));
        }
    }

    public GUIItem(GUIViewer guiViewer, int slot, ItemStack item) {
        this.viewer = guiViewer;
        this.slot = slot;
        this.item = item;
        clickActions = new ArrayList<>();
    }

    public ItemStack getItem() {
        return item;
    }

    public int getSlot() {
        return slot;
    }

    public GUIViewer getViewer() {
        return viewer;
    }

    public void onClick(InventoryClickEvent e, Player viewer) {
        for (String action : clickActions) {
            if (action.startsWith("playercmd=")) {
                String source = action.substring("playercmd=".length());
                viewer.performCommand(source);
            }

            if (action.startsWith("close")) {
                this.viewer.close(viewer);
            }
        }
    }
}
