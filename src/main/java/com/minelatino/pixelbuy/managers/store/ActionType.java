package com.minelatino.pixelbuy.managers.store;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public abstract class ActionType {

    private String executable;
    private String price;

    public abstract String getType();

    public abstract boolean isRefundable();

    @SuppressWarnings("deprecation")
    public void executeBuy(String player, Integer orderID) {
        executeBuy((Player) Bukkit.getOfflinePlayer(player), orderID);
    }

    public void executeBuy(Player player, Integer orderID) {}

    public void executeRefund(Player player, Integer orderID) {}

    public void setParts(String executable, String price) {
        this.executable = executable;
        this.price = price;
    }

    public String getExecutable() {
        return executable;
    }

    public String getExecutable(String player, Integer orderID) {
        return executable.replace("%player%", player).replace("%orderID%", String.valueOf(orderID)).replace("%itemPrice%", price);
    }
}
