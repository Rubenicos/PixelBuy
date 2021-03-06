package com.minelatino.pixelbuy.api.action.type;

import com.minelatino.pixelbuy.PixelBuy;
import com.minelatino.pixelbuy.api.action.ActionType;
import com.minelatino.pixelbuy.util.PixelUtils;

import java.util.Map;

public class BroadcastAction extends ActionType {

    public BroadcastAction(String regex) {
        super(regex);
    }

    @Override
    public String getName() {
        return "BROADCAST";
    }

    @Override
    public boolean execute(Map<String, String> keys, Map<String, String> content) {
        String type = keys.getOrDefault("type", "CHAT").toUpperCase();
        boolean single = content.size() == 1 && content.containsKey("content");
        if (type.equals("CHAT")) {
            for (String s : (single ? content.get("content") : content.getOrDefault("text", "")).split("\\n")) {
                PixelBuy.LOCALE.broadcast(s);
            }
            return true;
        }

        Map<String, String> subKeys = (single ? PixelUtils.getKeys(content.get("content"), ";", ":") : content);
        if (type.equals("TITLE")) {
            String title = subKeys.getOrDefault("title", "");
            String subtitle = subKeys.getOrDefault("subtitle", "");
            if (title.isEmpty() && subtitle.isEmpty()) return false;
            PixelBuy.LOCALE.broadcastTile(title, subtitle, PixelUtils.parseInt(subKeys.getOrDefault("fadein", "20"), 20), PixelUtils.parseInt(subKeys.getOrDefault("stay", "20"), 20), PixelUtils.parseInt(subKeys.getOrDefault("fadeout", "20"), 20));
        } else if (type.equals("ACTIONBAR")) {
            String text = subKeys.getOrDefault("text", "");
            if (text.isEmpty()) return false;
            PixelBuy.LOCALE.broadcastActionbar(text, PixelUtils.parseInt(subKeys.getOrDefault("pulses", "1"), 1));
        } else {
            return false;
        }
        return true;
    }
}
