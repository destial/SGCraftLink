package xyz.destiall.sgcraftlink.items;

import xyz.destiall.sgcraftlink.modules.hooks.utils.Font;

public class FontUserSettings {
    private Font font;
    private boolean convertChat = true;
    private boolean convertEntityMetadata = true;
    private boolean convertBossBar = true;
    private boolean convertActionBar = true;
    private boolean convertTitle = true;

    public FontUserSettings(Font font) {
        this.font = font;
    }

    public Font getFont() {
        return font;
    }

    public boolean isConvertActionBar() {
        return convertActionBar;
    }

    public boolean isConvertBossBar() {
        return convertBossBar;
    }

    public boolean isConvertChat() {
        return convertChat;
    }

    public boolean isConvertEntityMetadata() {
        return convertEntityMetadata;
    }

    public boolean isConvertTitle() {
        return convertTitle;
    }

    public void setConvertActionBar(boolean convertActionBar) {
        this.convertActionBar = convertActionBar;
    }

    public void setFont(Font font) {
        this.font = font;
    }

    public void setConvertBossBar(boolean convertBossBar) {
        this.convertBossBar = convertBossBar;
    }

    public void setConvertChat(boolean convertChat) {
        this.convertChat = convertChat;
    }

    public void setConvertEntityMetadata(boolean convertEntityMetadata) {
        this.convertEntityMetadata = convertEntityMetadata;
    }

    public void setConvertTitle(boolean convertTitle) {
        this.convertTitle = convertTitle;
    }
}
