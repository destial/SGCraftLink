package xyz.destiall.sgcraftlink.modules;

import xyz.destiall.sgcraftlink.SGCraftLinkSpigot;
import xyz.destiall.sgcraftlink.items.Tag;

import java.util.ArrayList;
import java.util.List;

public class Tags extends Module {
    private final List<Tag> allTags;

    public Tags(SGCraftLinkSpigot plugin) {
        super(plugin, "tags.yml");
        allTags = new ArrayList<>();

        for (String key : config.getKeys(false)) {
            Tag tag = new Tag(key, config.getString(key));
            allTags.add(tag);
        }
    }

    @Override
    public void loadDefaultConfig() {
        config.set("vip", "&dVIP");
    }

    @Override
    public void disable() {
        allTags.clear();
    }

    public List<Tag> getAllTags() {
        return allTags;
    }
}
