/*
 * GlobalTablist - get the global tablist back
 *
 * Copyright (C) 2014 Florian Stober
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package codecrafter47.globaltablist;

import net.md_5.bungee.UserConnection;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.connection.LoginResult;
import net.md_5.bungee.protocol.packet.PlayerListItem;
import net.md_5.bungee.protocol.packet.PlayerListItem.Item;
import net.md_5.bungee.tab.TabList;

/**
 * @author Florian Stober
 */
public class GlobalTablistHandler extends TabList {

    private final GlobalTablist plugin;

    private int lastPing = 0;

    public GlobalTablistHandler(ProxiedPlayer player, GlobalTablist plugin) {
        super(player);
        this.plugin = plugin;
    }

    protected ProxiedPlayer getPlayer() {
        return player;
    }

    @Override
    public void onServerChange() {
        // It's a global tablist. Nothing happens if a player switches the server
    }

    @Override
    public void onUpdate(PlayerListItem pli) {
        System.out.println("onUpdate");
        for (Item i : pli.getItems()) {
            if (i.getUuid().version() != 4) {
                // NPC
                PlayerListItem out = new PlayerListItem();
                out.setAction(pli.getAction());
                out.setItems(new Item[] { i });
                player.unsafe().sendPacket(out);
                System.out.println("Tablistdings (NPC): " + i.getUuid().version() + " Action: " + pli.getAction() + " Name: " + i.getUsername());
            } else {
                // System.out.println("Tablistdings: " + i.getUuid().version() + " Action: " + pli.getAction() + " Name: " + i.getUsername());
            }
        }
        // It's a global tablist - we don't pass packets from the server
    }

    @Override
    public void onPingChange(int i) {
        if (plugin.getConfig().updatePing) {
            if (lastPing - i > 50 || lastPing - i < 50) {
                PlayerListItem pli = new PlayerListItem();
                pli.setAction(PlayerListItem.Action.UPDATE_LATENCY);
                Item item = new Item();
                item.setUsername(getPlayer().getName());
                item.setUuid(getPlayer().getUniqueId());
                String text = player.getDisplayName();
                if (text.length() > 16) {
                    text = text.substring(0, 16);
                }
                item.setDisplayName(text);
                item.setPing(i);
                for (ProxiedPlayer p : plugin.getProxy().getPlayers()) {
                    p.unsafe().sendPacket(pli);
                }
            }
        }
    }

    @Override
    public void onConnect() {
        // System.out.println("onConnect " + getPlayer().getName());
        // send players
        for (ProxiedPlayer p : plugin.getProxy().getPlayers()) {
            sendPlayerSlot(p, getPlayer());
            if (p == getPlayer()) {
                continue;
            }
            sendPlayerSlot(getPlayer(), p);
        }

        // store ping
        lastPing = getPlayer().getPing();
    }

    @Override
    public void onDisconnect() {
        //  System.out.println("onDisconnect " + getPlayer().getName());
        //  System.out.println(plugin.getProxy().getPlayer(getPlayer().getUniqueId()) == getPlayer());
        // remove player
        if (plugin.getProxy().getPlayer(getPlayer().getUniqueId()) == getPlayer()) {
            for (ProxiedPlayer p : plugin.getProxy().getPlayers()) {
                removePlayerSlot(getPlayer(), p);
            }
        }
    }

    protected boolean is18Client(ProxiedPlayer player) {
        return player.getPendingConnection().getVersion() >= 47;
    }

    protected boolean isPremium(ProxiedPlayer player) {
        return player.getPendingConnection().isOnlineMode();
    }

    protected void sendPlayerSlot(ProxiedPlayer player, ProxiedPlayer receiver) {
        // System.out.println("Sending Add " + player.getName() + " to " + receiver.getName());
        String text = player.getDisplayName();

        if (!is18Client(receiver) && text.length() > 16) {
            text = text.substring(0, 16);
        }

        PlayerListItem pli = new PlayerListItem();
        pli.setAction(PlayerListItem.Action.ADD_PLAYER);
        Item item = new Item();
        item.setPing(player.getPing());
        if (!is18Client(receiver)) {
            item.setDisplayName(text);
        } else {
            item.setUsername(player.getName());
            item.setGamemode(((UserConnection) player).getGamemode());
            item.setUuid(player.getUniqueId());
            item.setProperties(new String[0][0]);
            if (isPremium(receiver)) {
                LoginResult loginResult = ((UserConnection) player).getPendingConnection().getLoginProfile();
                if (loginResult != null) {
                    for (LoginResult.Property s : loginResult.getProperties()) {
                        if (s.getName().equals("textures")) {
                            item.setProperties(new String[][] { { "textures", s.getValue(), s.getSignature() } });
                        }
                    }
                }
            }
        }
        pli.setItems(new Item[] { item });
        receiver.unsafe().sendPacket(pli);
    }

    private void removePlayerSlot(ProxiedPlayer player, ProxiedPlayer receiver) {
        // System.out.println("Sending Remove " + player.getName() + " to " + receiver.getName());
        PlayerListItem pli = new PlayerListItem();
        pli.setAction(PlayerListItem.Action.REMOVE_PLAYER);
        Item item = new Item();
        if (is18Client(receiver)) {
            item.setUsername(player.getName());
            item.setUuid(player.getUniqueId());
        } else {
            String text = player.getDisplayName();
            if (text.length() > 16) {
                text = text.substring(0, 16);
            }
            item.setDisplayName(text);
        }
        pli.setItems(new Item[] { item });
        receiver.unsafe().sendPacket(pli);
    }
}
