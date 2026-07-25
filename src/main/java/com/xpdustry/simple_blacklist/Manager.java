/*
 * This file is part of Simple Blacklist.
 *
 * MIT License
 *
 * Copyright (c) 2026 Xpdustry
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.xpdustry.simple_blacklist;

import java.util.regex.Pattern;

import arc.Events;
import arc.func.Cons;
import arc.struct.ObjectMap;
import arc.struct.Seq;
import arc.util.Reflect;

import mindustry.Vars;
import mindustry.game.EventType.ConnectPacketEvent;
import mindustry.gen.Groups;
import mindustry.net.Administration.PlayerInfo;
import mindustry.net.Packets.KickReason;

import static com.xpdustry.simple_blacklist.Events.*;


public class Manager {
  public static void registerListener() {
    // Name blacklist listener
    Cons<ConnectPacketEvent> listener = e -> {
      String name = e.packet.name, address = e.connection.address, uuid = e.packet.uuid, usid = e.packet.usid;

      // Validation part re-implementation
      e.connection.uuid = uuid; // For console visual
      if (e.connection.hasBegunConnecting) {
        e.connection.kick(KickReason.idInUse, 0);
        return;
      } else if (uuid == null || usid == null) {
        e.connection.kick(KickReason.idInUse, 0);
        return;
      } else if (name == null || Vars.netServer.fixName(name).isBlank()) {
        e.connection.kick(KickReason.nameEmpty, 0);
        return;
      }

      Events.fire(new CheckingNicknameEvent(name, uuid, e.connection, e.packet));

      // Ignore if it's an admin and the 'ignore-admins' option is enabled
      PlayerInfo pInfo = Vars.netServer.admins.getInfoOptional(uuid);
      if (Config.ignoreAdmins.get() && pInfo != null && pInfo.admin && usid.equals(pInfo.adminUsid))
        return;

      // Check if the nickname is blacklisted
      if (isValidName(name)) return;

      WorkingMode mode = Config.mode.get();
      if (mode == WorkingMode.banuuid) {
        /* The player UUID will be banned, so we need to manually create an account,
         * and filling it with as much informations as possible, if not already.
         * This avoids creating empty accounts but not filling the server settings.
         */
        if (pInfo == null) {
          Vars.netServer.admins.updatePlayerJoined(uuid, address, name);
          pInfo = Vars.netServer.admins.getInfo(uuid);
          pInfo.adminUsid = usid;
          pInfo.timesJoined = 0; // Never joined, can be used as a filter, to know invalid accounts
        }
        Vars.netServer.admins.banPlayerID(uuid);
      } else if (mode == WorkingMode.banip) {
        Vars.netServer.admins.banPlayerIP(address);
      }

      Logger.info("Kicking player '@' [@, @] for a blacklisted nickname.", name, address, uuid);
      if (!Config.message.get().isEmpty()) e.connection.kick(Config.message.get(), pInfo != null ? 30*1000 : 0);
      else e.connection.kick(mode == WorkingMode.kick ? KickReason.kick : KickReason.banned,
                             pInfo != null ? 30*1000 : 0);
      Events.fire(new BlacklistedNicknameEvent(name, uuid, e.connection, e.packet));
    };


    // Try to move listeners at top of list
    try {
      ObjectMap<Object, Seq<Cons<?>>> events = Reflect.get(Events.class, "events");
      events.get(ConnectPacketEvent.class, () -> new Seq<>(Cons.class)).insert(0, listener);
    } catch (RuntimeException ignored) {
      Logger.warn("Unable to edit ConnectPacketEvent list. Falling back to a normal event...");
      Events.on(ConnectPacketEvent.class, listener);
    }
  }

  /**
   * @return {@code true} if the {@code name} is valid. If it's not in the name list and doesn't match with any regex.<br>
   *         this will returns {@code true} if all lists are disabled.
   */
  public static boolean isValidName(String name) {
    name = Strings.normalise(name);

    if (Config.namesEnabled.get()) {
      String n = Config.caseSensitive.get() ? name : name.toLowerCase();
      for (int i=0; i<Config.namesList.get().size; i++) {
        String nn = Config.namesList.get().getKeyAt(i);
        if (!(Config.caseSensitive.get() ? n.contains(nn) : n.contains(nn.toLowerCase()))) continue;
        int uses = Config.namesList.get().getValueAt(i)+1;
        Config.namesList.get().setValue(i, uses);
        Events.fire(new NicknameListUpdatedEvent(nn, uses));
        return false;
      }
    }

    if (Config.regexEnabled.get()) {
      for (int i=0, n=Config.regexList.get().size; i<n; i++) {
        Pattern p = Config.regexList.get().getKeyAt(i);
        if (!p.matcher(name).find()) continue;
        int uses = Config.regexList.get().getValueAt(i)+1;
        Config.regexList.put(p, uses);
        Events.fire(new RegexListUpdatedEvent(p, uses));
        return false;
      }
    }

    return true;
  }

  public static void checkOnlinePlayers() {
    if (Groups.player.isEmpty()) return;
    Groups.player.each(p -> !Config.ignoreAdmins.get() || !p.admin, p -> {
      Events.fire(new CheckingNicknameEvent(p.name, p.uuid(), p.con, null));
      if (isValidName(p.name)) return;

      Logger.info("Kicking player '@' [@, @] for a blacklisted nickname.", p.name, p.con.address, p.uuid());
      WorkingMode mode = Config.mode.get();
      if (mode == WorkingMode.banip) Vars.netServer.admins.banPlayerIP(p.con.address);
      else if (mode == WorkingMode.banuuid) Vars.netServer.admins.banPlayerID(p.uuid());
      if (!Config.message.get().isEmpty()) p.kick(Config.message.get());
      else p.kick(mode == WorkingMode.kick ? KickReason.kick : KickReason.banned);
      Events.fire(new BlacklistedNicknameEvent(p.name, p.uuid(), p.con, null));
    });
  }
}
