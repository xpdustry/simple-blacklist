/*
 * This file is part of Simple Blacklist.
 *
 * MIT License
 *
 * Copyright (c) 2025 Xpdustry
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

import arc.Events;
import arc.func.Cons;
import arc.struct.ObjectMap;
import arc.struct.Seq;
import arc.util.Reflect;

import mindustry.Vars;
import mindustry.gen.Groups;
import mindustry.net.Administration.PlayerInfo;
import mindustry.net.Packets.KickReason;

import com.xpdustry.simple_blacklist.util.Logger;
import com.xpdustry.simple_blacklist.util.Strings;

import static mindustry.game.EventType.*;

import static com.xpdustry.simple_blacklist.Events.*;


public class Manager {
  private static final Logger logger = new Logger();
  
  public static void registerListeners() {
    // Name blacklist listener
    Cons<ConnectPacketEvent> listener = e -> {
      e.connection.uuid = e.packet.uuid; // For console visual 

      // Handle case of multiple connection of client
      if (e.connection.hasBegunConnecting) {
        e.connection.kick(KickReason.idInUse, 0);
        return;
          
      // Check client validity
      } else if (e.packet.uuid == null || e.packet.usid == null) {
        e.connection.kick(KickReason.idInUse, 0);
        return;
        
      // Check if the nickname is valid
      } else if (e.packet.name == null || Vars.netServer.fixName(e.packet.name).trim().length() <= 0) {
        e.connection.kick(KickReason.nameEmpty, 0);
        return;
      }
      
      Events.fire(new CheckingNicknameEvent(e.packet.name, e.packet.uuid, e.connection, e.packet));
      
      // Ignore if it's an admin and the 'ignore-admins' option is enabled
      PlayerInfo pInfo = Vars.netServer.admins.getInfoOptional(e.packet.uuid);
      if (Config.ignoreAdmins.get() && pInfo != null && 
          pInfo.admin && e.packet.usid.equals(pInfo.adminUsid)) 
        return;

      // Check if the nickname is blacklisted
      if (!isValidName(e.packet.name)) {
        if (Config.mode.get() == Config.WorkingMode.banuuid) {
          /* The player UUID will be banned.
           * So we need to manually create an account
           * and filling it with as much informations as possible, if not already.
           * 
           * This avoids to create empty accounts BUT not filling the server settings.
           */
          if (pInfo == null) {
            Vars.netServer.admins.updatePlayerJoined(e.packet.uuid, e.connection.address, e.packet.name);
            pInfo = Vars.netServer.admins.getInfo(e.packet.uuid);
            pInfo.adminUsid = e.packet.usid;
            // the client never joined the server, this value can be used as a filter, to know all invalid accounts
            pInfo.timesJoined = 0; 
          }
          
          Vars.netServer.admins.banPlayerID(e.packet.uuid);
          
        } else if (Config.mode.get() == Config.WorkingMode.banip)
          Vars.netServer.admins.banPlayerIP(e.connection.address);

        logger.info("Kicking player '@' [@, @] for a blacklisted nickname.", e.packet.name, e.connection.address,
                    e.packet.uuid);
        if (Config.message.get().isEmpty()) 
          e.connection.kick(Config.mode.get() == Config.WorkingMode.kick ? KickReason.kick : KickReason.banned, 
                            pInfo != null ? 30*1000 : 0);
        else e.connection.kick(Config.message.get(), pInfo != null ? 30*1000 : 0);
        Events.fire(new BlacklistedNicknameEvent(e.packet.name, e.packet.uuid, e.connection, e.packet));
      }
    };

    
    // Try to move listeners at top of lists
    try {
      ObjectMap<Object, Seq<Cons<?>>> events = Reflect.get(Events.class, "events");
      events.get(ConnectPacketEvent.class, () -> new Seq<>(Cons.class)).insert(0, listener);

    } catch (RuntimeException err) {
      logger.warn("Unable to get access of Events class, because of a security manager!");
      logger.warn("Falling back to a normal event...");
      Events.on(ConnectPacketEvent.class, listener);
    }
  }

  /** 
   * @return {@code true} if the {@code name} is valid. If it's not in the name list and doesn't match with any regex.
   * @apiNote this will returns {@code true} if lists are both disabled.
   */
  public static boolean isValidName(String name) {
    name = Strings.normalise(name);
    
    if (Config.namesEnabled.get()) {
      String n = Config.nameCaseSensitive.get() ? name : name.toLowerCase();
      for (ObjectMap.Entry<String, Integer> e : Config.namesList.get()) {
        if (Config.nameCaseSensitive.get() ? n.contains(e.key) : n.contains(e.key.toLowerCase())) {
          int uses = Config.namesList.get(e.key)+1;
          Config.namesList.put(e.key, uses);
          Events.fire(new NicknameListUpdatedEvent(e.key, uses));
          return false;
        }
      }
    }
    
    if (Config.regexEnabled.get()) {
      for (ObjectMap.Entry<java.util.regex.Pattern, Integer> e : Config.regexList.get()) {
        if (e.key.matcher(name).matches()) {
          int uses = Config.regexList.get(e.key)+1;
          Config.regexList.put(e.key, uses);
          Events.fire(new RegexListUpdatedEvent(e.key, uses));
          return false;
        }
      }
    }
    
    return true;
  }

  public static void checkOnlinePlayers() {
    Groups.player.each(p -> {
      // Ignore admins if enabled
      if (Config.ignoreAdmins.get() && p.admin) return;

      Events.fire(new CheckingNicknameEvent(p.name, p.uuid(), p.con, null));
      if (!isValidName(p.name)) {
        logger.info("Kicking player '@' [@, @] for a blacklisted nickname.", p.name, p.con.address, p.uuid());
        if (Config.mode.get() == Config.WorkingMode.banip) Vars.netServer.admins.banPlayerIP(p.con.address);
        else if (Config.mode.get() == Config.WorkingMode.banuuid) Vars.netServer.admins.banPlayerID(p.uuid());
        if (Config.message.get().isEmpty()) 
             p.kick(Config.mode.get() == Config.WorkingMode.kick ? KickReason.kick : KickReason.banned);
        else p.kick(Config.message.get());
        Events.fire(new BlacklistedNicknameEvent(p.name, p.uuid(), p.con, null));
      }      
    });
  }
}
