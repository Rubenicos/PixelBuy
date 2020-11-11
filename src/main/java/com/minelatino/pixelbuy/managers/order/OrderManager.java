package com.minelatino.pixelbuy.managers.order;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.minelatino.pixelbuy.PixelBuy;
import com.minelatino.pixelbuy.managers.order.objects.WebOrder;
import com.minelatino.pixelbuy.managers.order.objects.SavedOrders;
import com.minelatino.pixelbuy.managers.order.objects.WebString;
import com.minelatino.pixelbuy.managers.player.Order;
import com.minelatino.pixelbuy.managers.player.PlayerData;
import com.minelatino.pixelbuy.util.Utils;
import okhttp3.*;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class OrderManager {

    private final PixelBuy pl = PixelBuy.get();

    private int checker;

    private boolean on = false;

    public OrderManager() {
        reload(true);
    }

    public void reload(boolean init) {
        if (!init) {
            on = false;
            Bukkit.getScheduler().cancelTask(checker);
        }
        checker = Bukkit.getScheduler().runTaskTimerAsynchronously(pl, () -> {
            if (!on) {
                on = true;
                checkWebData(null);
            }
        }, pl.configInt("Web-Data.Check-Interval") * 20, pl.configInt("Web-Data.Check-Interval") * 20).getTaskId();
    }

    public URL getURL() throws Exception {
        return new URL(pl.configString("Web-Data.URL").replace("https:", "http:") + "/wp-json/wmc/v1/server/" + pl.configString("Web-Data.Key"));
    }

    /**
     * Checks WebData String
     */
    public void checkWebData(CommandSender sender) {
        // First of all the plugin will check player data stored on database
        //pl.getPlayerManager().processPlayers();
        boolean debug = pl.configBoolean("Web-Data.Debug");

        // Check if plugin is correctly configured
        if (pl.configString("Web-Data.URL").isEmpty()) {
            on = false;
            if (debug) Utils.info(pl.langString("Debug.WebData.Empty-URL"));
            return;
        }
        if (pl.configString("Web-Data.Key").isEmpty()) {
            on = false;
            if (debug) Utils.info(pl.langString("Debug.WebData.Empty-Key"));
            return;
        }

        // First debug message
        if (debug) Utils.info(pl.langString("Debug.WebData.Check-URL"));

        // Check URL connection
        BufferedReader in;
        try {
            in = new BufferedReader(new InputStreamReader(getURL().openStream()));
        } catch (FileNotFoundException e) {
            // Error about connection
            on = false;
            if (debug) Utils.info(e.getMessage().replace(pl.configString( "Web-Data.Key"), "privateKey"));
            return;
        } catch (Exception e) {
            // Error about bad configured URL
            on = false;
            if (debug) Utils.info(pl.langString("Debug.WebData.Invalid-URL"));
            return;
        }

        // WebData string builder
        StringBuilder buffer = new StringBuilder();
        String line;
        try {
            while ((line = in.readLine()) != null) {
                buffer.append(line);
            }
            in.close();
        } catch (IOException e) {
            on = false;
            e.printStackTrace();
            return;
        }

        // Final checker
        if (buffer.toString().isEmpty()) {
            on = false;
            if (debug) Utils.info(pl.langString("Debug.WebData.Empty-Page"));
        } else {
            processData(sender, buffer.toString(), debug);
        }
    }

    /**
     * Read JSON string and process them
     */
    public void processData(CommandSender sender, String webData, boolean debug) {
        // First debug message
        if (debug) Utils.info(pl.langString("Debug.WebData.Check-Data"));

        // Read web data
        Gson gson = new GsonBuilder().create();
        WebString webString = gson.fromJson(webData, WebString.class);

        // Check if Wordpress plugin have no errors
        if (webString.getData() != null) {
            on = false;
            if (debug) Utils.info(webString.getCode());
            return;
        }

        // Create a list of available orders
        List<WebOrder> webOrderList = webString.getOrders();

        // Check if order list have orders
        if (webOrderList == null || webOrderList.isEmpty()) {
            on = false;
            if (debug) Utils.info(pl.langString("Debug.WebData.Empty-Orders"));
            return;
        }

        List<Integer> savedOrders = new ArrayList<>();
        for (WebOrder webOrder : webOrderList) {
            Player player = Utils.getPlayer(webOrder.getPlayer());
            List<String> cmds = new ArrayList<>();
            for (String cmd : webOrder.getCommands()) {
                cmds.add(cmd.replace("%orderID%", String.valueOf(webOrder.getOrderId())));
            }

            if (player != null) {
                for (String cmd : cmds) {
                    Bukkit.getScheduler().runTaskAsynchronously(pl, () -> Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), cmd));
                }
            } else {
                String p = (pl.configBoolean("Database.UUID") ? String.valueOf(Bukkit.getOfflinePlayer(webOrder.getPlayer()).getUniqueId()) : webOrder.getPlayer());
                Order order = new Order(webOrder.getOrderId(), cmds);
                PlayerData playerData = new PlayerData(p, Collections.singletonList(order));
                pl.getDatabase().saveData(playerData);
            }
            savedOrders.add(webOrder.getOrderId());
        }
        sendProcessedData(sender, savedOrders, debug);
    }

    /**
     * Sends the processed orders to wordpress.
     */
    public void sendProcessedData(CommandSender sender, List<Integer> orders, boolean debug) {
        // Build saved orders data to send
        Gson gson = new Gson();
        SavedOrders savedOrders = new SavedOrders(orders);
        String ordersIds = gson.toJson(savedOrders);

        // Build a request and get the response
        RequestBody body = RequestBody.create(MediaType.parse("application/json; charset=utf-8"), ordersIds);
        Request request;
        Response response;
        try {
            request = new Request.Builder().url(getURL()).post(body).build();
            response = new OkHttpClient().newCall(request).execute();
        } catch (Exception e) {
            on = false;
            return;
        }

        // Check if server has a connection
        if (response.body() == null) {
            on = false;
            if (debug) Utils.info("Received empty response from your server, check connections.");
            return;
        }

        // Check any existing errors from Wordpress
        WebString webString = gson.fromJson(Objects.requireNonNull(response.body()).toString(), WebString.class);
        if (webString.getCode() != null) {
            if (debug) Utils.info("Received error when trying to send post data:" + webString.getCode());
        }
        on = false;
    }
}
