package com.example.client;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

public class HarvesterSettingsScreen extends Screen {

    private int listeningRow = -1;

    public HarvesterSettingsScreen() {
        super(Component.literal("Harvester Settings"));
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor extractor, int mouseX, int mouseY, float partialTick) {
        // Transparent dark overlay background
        extractor.fill(0, 0, this.width, this.height, 0x60000000);

        int width = 300;
        int height = 180;
        int x = (this.width - width) / 2;
        int y = (this.height - height) / 2;

        // Dark glassmorphism box panel
        extractor.fill(x, y, x + width, y + height, 0xD5101015);
        
        // Glowing border outline
        extractor.outline(x, y, x + width, y + height, 0xFF10B981);
        
        // Settings title
        extractor.centeredText(this.font, "HARVESTER SETTINGS", x + width / 2, y + 10, 0xFF10B981);
        
        String[] names = { "Speedmine", "Fast Place", "Farming Assist", "Auto-Bridge", "Auto Feed" };
        
        for (int i = 0; i < 5; i++) {
            int rowY = y + 35 + i * 26;
            
            // Feature Name
            extractor.text(this.font, names[i], x + 15, rowY + 5, 0xFFFFFFFF);
            
            // Toggle Button
            boolean enabled = false;
            String statusText = "DISABLED";
            int statusColor = 0xFFEF4444; // red
            
            if (i == 0) {
                enabled = SpeedmineState.enabled;
                statusText = SpeedmineState.getLabel();
                statusColor = enabled ? 0xFF10B981 : 0xFFEF4444;
            } else if (i == 1) {
                enabled = SpeedmineState.fastPlaceEnabled;
                statusText = enabled ? "ACTIVE" : "INACTIVE";
                statusColor = enabled ? 0xFF10B981 : 0xFFEF4444;
            } else if (i == 2) {
                enabled = SpeedmineState.farmingAssistEnabled;
                statusText = enabled ? "ACTIVE" : "INACTIVE";
                statusColor = enabled ? 0xFF10B981 : 0xFFEF4444;
            } else if (i == 3) {
                enabled = SpeedmineState.bridgingEnabled;
                statusText = enabled ? "ACTIVE" : "INACTIVE";
                statusColor = enabled ? 0xFF10B981 : 0xFFEF4444;
            } else if (i == 4) {
                enabled = SpeedmineState.autoFeedEnabled;
                statusText = enabled ? "ACTIVE" : "INACTIVE";
                statusColor = enabled ? 0xFF10B981 : 0xFFEF4444;
            }
            
            int btnX = x + 120;
            int btnW = (i == 0) ? 40 : 65;
            
            boolean hoverToggle = mouseX >= btnX && mouseX <= btnX + btnW && mouseY >= rowY && mouseY <= rowY + 18;
            int toggleBg = hoverToggle ? 0x60FFFFFF : 0x30FFFFFF;
            extractor.fill(btnX, rowY, btnX + btnW, rowY + 18, toggleBg);
            extractor.outline(btnX, rowY, btnX + btnW, rowY + 18, statusColor);
            extractor.centeredText(this.font, i == 0 && !enabled ? "OFF" : (i == 0 ? "ON" : (enabled ? "ON" : "OFF")), btnX + btnW / 2, rowY + 5, statusColor);
            
            if (i == 0) {
                // Draw extra speed selector button
                int spdX = x + 165;
                int spdW = 40;
                boolean hoverSpd = mouseX >= spdX && mouseX <= spdX + spdW && mouseY >= rowY && mouseY <= rowY + 18;
                int spdBg = hoverSpd ? 0x60FFFFFF : 0x30FFFFFF;
                extractor.fill(spdX, rowY, spdX + spdW, rowY + 18, spdBg);
                extractor.outline(spdX, rowY, spdX + spdW, rowY + 18, 0xFF3B82F6);
                extractor.centeredText(this.font, SpeedmineState.getLabel(), spdX + spdW / 2, rowY + 5, 0xFF3B82F6);
            }
            
            // Draw Rebind Button
            int bindX = x + 215;
            int bindW = 70;
            boolean isListening = (listeningRow == i);
            String bindText = isListening ? "???" : getKeyName(getKeycodeForIndex(i));
            boolean hoverBind = mouseX >= bindX && mouseX <= bindX + bindW && mouseY >= rowY && mouseY <= rowY + 18;
            int bindBg = hoverBind || isListening ? 0x60FFFFFF : 0x30FFFFFF;
            int bindOutline = isListening ? 0xFFF59E0B : 0xFF9CA3AF;
            
            extractor.fill(bindX, rowY, bindX + bindW, rowY + 18, bindBg);
            extractor.outline(bindX, rowY, bindX + bindW, rowY + 18, bindOutline);
            extractor.centeredText(this.font, bindText, bindX + bindW / 2, rowY + 5, bindOutline);
        }
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean isSecondary) {
        if (event.button() != 0) return super.mouseClicked(event, isSecondary);

        double mouseX = event.x();
        double mouseY = event.y();

        int width = 300;
        int height = 180;
        int x = (this.width - width) / 2;
        int y = (this.height - height) / 2;

        for (int i = 0; i < 5; i++) {
            int rowY = y + 35 + i * 26;

            int btnX = x + 120;
            int btnW = (i == 0) ? 40 : 65;
            if (mouseX >= btnX && mouseX <= btnX + btnW && mouseY >= rowY && mouseY <= rowY + 18) {
                if (i == 0) {
                    if (SpeedmineState.enabled) {
                        SpeedmineState.enabled = false;
                        SpeedmineState.speedIndex = 0;
                        SpeedmineState.multiplier = 1.0f;
                    } else {
                        SpeedmineState.cycle();
                    }
                } else if (i == 1) {
                    SpeedmineState.fastPlaceEnabled = !SpeedmineState.fastPlaceEnabled;
                } else if (i == 2) {
                    SpeedmineState.farmingAssistEnabled = !SpeedmineState.farmingAssistEnabled;
                } else if (i == 3) {
                    SpeedmineState.bridgingEnabled = !SpeedmineState.bridgingEnabled;
                } else if (i == 4) {
                    SpeedmineState.autoFeedEnabled = !SpeedmineState.autoFeedEnabled;
                }
                return true;
            }

            if (i == 0) {
                int spdX = x + 165;
                int spdW = 40;
                if (mouseX >= spdX && mouseX <= spdX + spdW && mouseY >= rowY && mouseY <= rowY + 18) {
                    SpeedmineState.cycle();
                    return true;
                }
            }

            int bindX = x + 215;
            int bindW = 70;
            if (mouseX >= bindX && mouseX <= bindX + bindW && mouseY >= rowY && mouseY <= rowY + 18) {
                listeningRow = i;
                return true;
            }
        }

        return super.mouseClicked(event, isSecondary);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (listeningRow != -1) {
            int keycode = event.key();
            if (keycode != 256) { // escape cancels
                setKeycodeForIndex(listeningRow, keycode);
            }
            listeningRow = -1;
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    public void onClose() {
        SpeedmineState.saveConfig();
        super.onClose();
    }

    private int getKeycodeForIndex(int index) {
        if (index == 0) return SpeedmineState.speedmineKey;
        if (index == 1) return SpeedmineState.fastPlaceKey;
        if (index == 2) return SpeedmineState.farmingAssistKey;
        if (index == 3) return SpeedmineState.bridgingKey;
        if (index == 4) return SpeedmineState.autoFeedKey;
        return 0;
    }

    private void setKeycodeForIndex(int index, int keycode) {
        if (index == 0) SpeedmineState.speedmineKey = keycode;
        if (index == 1) SpeedmineState.fastPlaceKey = keycode;
        if (index == 2) SpeedmineState.farmingAssistKey = keycode;
        if (index == 3) SpeedmineState.bridgingKey = keycode;
        if (index == 4) SpeedmineState.autoFeedKey = keycode;
    }

    private String getKeyName(int key) {
        if (key >= 65 && key <= 90) return String.valueOf((char) key);
        if (key >= 48 && key <= 57) return String.valueOf((char) key);
        switch (key) {
            case 344: return "RSHIFT";
            case 340: return "LSHIFT";
            case 341: return "LCTRL";
            case 345: return "RCTRL";
            case 342: return "LALT";
            case 346: return "RALT";
            case 256: return "ESC";
            case 257: return "ENTER";
            case 32: return "SPACE";
            case 258: return "TAB";
            case 259: return "BACKSPACE";
            case 262: return "RIGHT";
            case 263: return "LEFT";
            case 264: return "DOWN";
            case 265: return "UP";
            default: return "KEY_" + key;
        }
    }
}
