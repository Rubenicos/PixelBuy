package com.minelatino.pixelbuy.module.locale.user;

import com.minelatino.pixelbuy.PixelBuy;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.UUID;

public class BukkitUser extends UserType<CommandSender> {

    @Override
    public String getName(CommandSender user) {
        if (user instanceof Player) {
            return user.getName();
        } else {
            return null;
        }
    }

    @Override
    public UUID getUniqueId(CommandSender user) {
        if (user instanceof Player) {
            return ((Player) user).getUniqueId();
        } else {
            return CONSOLE_UUID;
        }
    }

    @Override
    public boolean hasPermission(CommandSender user, String permission) {
        return user.hasPermission(permission);
    }

    @Override
    void sendMessage(CommandSender user, String msg, String... args) {
        user.sendMessage(PixelBuy.LOCALE.replaceArgs(msg, args));
    }
}
