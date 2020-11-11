package com.minelatino.pixelbuy.managers.database.types;

import com.google.gson.Gson;
import com.minelatino.pixelbuy.PixelBuy;
import com.minelatino.pixelbuy.managers.database.DatabaseType;
import com.minelatino.pixelbuy.managers.player.PlayerData;
import com.minelatino.pixelbuy.util.Utils;
import org.bukkit.Bukkit;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class MySQL implements DatabaseType {

    private final PixelBuy pl = PixelBuy.get();

    private boolean enabled = false;

    private boolean debug = false;

    public Connection con = null;

    public String getType() {
        return "MYSQL";
    }

    public boolean setup() {
        if (enabled) disable(false);
        debug = pl.getFiles().getConfig().getBoolean("Database.Debug");
        try {
            Class.forName("com.mysql.jdbc.Driver").newInstance();

            con = DriverManager.getConnection("jdbc:mysql://" +
                            pl.getFiles().getConfig().getString("Database.Host") + "/" +
                            pl.getFiles().getConfig().getString("Database.Database") +
                            pl.getFiles().getConfig().getString("Database.Flags"),
                            pl.getFiles().getConfig().getString("Database.User"),
                            pl.getFiles().getConfig().getString("Database.Password"));

        } catch (ClassNotFoundException e) {
            if (debug) Utils.info(pl.getFiles().getLang().getString("Debug.MySQL.Not-Found"));
            return false;
        } catch (SQLException e) {
            if (debug) Utils.info(pl.getFiles().getLang().getString("Debug.MySQL.Cant-Connect"));
            return false;
        } catch (Exception e) {
            if (debug) {
                Utils.info(pl.getFiles().getLang().getString("Debug.MySQL.Unknown"));
                Utils.info(e.getMessage());
            }
            return false;
        }
        enabled = true;
        query("CREATE TABLE IF NOT EXISTS `PlayerOrders` (`PLAYER` varchar(255) NOT NULL,`DATA` TEXT, PRIMARY KEY (`PLAYER`));", null);
        return true;
    }

    public void saveData(PlayerData data) {
        String player = data.getPlayer().toLowerCase();
        PlayerData oldData = getData(player);
        if (oldData != null) data.addOrders(oldData.getOrders());
        Gson gson = new Gson();
        String json = gson.toJson(data);
        query("INSERT INTO `PlayerOrders` (PLAYER, DATA) VALUES ('" + player + "','" + json + "') " + "ON DUPLICATE KEY UPDATE `DATA` = '" + json + "';", data);
    }

    public PlayerData getData(String player) {
        PlayerData data = null;
        try {
            Statement stmt = con.createStatement();
            ResultSet rS = stmt.executeQuery("SELECT `DATA` FROM `PlayerOrders` WHERE `PLAYER` = '" + player + "';");
            rS.last();
            Gson gson = new Gson();
            data = gson.fromJson(rS.getString("DATA"), PlayerData.class);
            stmt.close();
            rS.close();
        } catch (SQLException | NullPointerException e) {
            if (debug) {
                Utils.info(pl.getFiles().getLang().getString("Debug.MySQL.No-Data").replace("%player%", player));
            }
        }
        return data;
    }

    public List<PlayerData> getAllData() {
        List<PlayerData> datas = new ArrayList<>();
        try {
            PreparedStatement stmt = con.prepareStatement("SELECT `DATA` FROM `PlayerOrders`");
            ResultSet rS = stmt.executeQuery();
            Gson gson = new Gson();
            while (rS.next()) {
                datas.add(gson.fromJson(rS.getString("DATA"), PlayerData.class));
            }
        } catch (SQLException ignored) { }
        return datas;
    }

    public void deleteData(String player) {
        query("DELETE FROM `PlayerOrders` WHERE `PLAYER` = '" + player + "';", null);
    }

    public void query(String sql, PlayerData data) {
        Bukkit.getScheduler().runTaskAsynchronously(pl, () -> {
            try {
                Statement stmt = con.createStatement();
                stmt.executeUpdate(sql);
                stmt.close();
            } catch (SQLException e) {
                if (debug) {
                    Utils.info(pl.getFiles().getLang().getString("Debug.MySQL.Query-Error"));
                    Utils.info(e.getMessage());
                }
                if (data != null) pl.getDatabase().addCachedData(data);
                disable(true);
            } catch (NullPointerException e) {
                if (debug) Utils.info(e.getMessage());
                if (data != null) pl.getDatabase().addCachedData(data);
            }
        });
    }

    public void disable(boolean reconnect) {
        if (debug) Utils.info(pl.getFiles().getLang().getString("Debug.MySQL.Shutdown"));
        try {
            con.close();
        } catch (SQLException e) {
            if (debug) {
                Utils.info(pl.getFiles().getLang().getString("Debug.MySQL.Shut-Error"));
                Utils.info(e.getMessage());
            }
        }
        if (reconnect) {
            if (debug) Utils.info(pl.getFiles().getLang().getString("Debug.MySQL.Reconnect"));
            if (!setup()) {
                if (debug) Utils.info(pl.getFiles().getLang().getString("Debug.MySQL.Reco-Error"));
                pl.getDatabase().setDefault();
            }
        }
    }
}
