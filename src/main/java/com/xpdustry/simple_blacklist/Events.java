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

import java.util.regex.Pattern;

import arc.util.Nullable;

import mindustry.net.NetConnection;
import mindustry.net.Packets.ConnectPacket;


public class Events {
  public static class CheckingNicknameEvent {
    public final String name, uuid;
    public final NetConnection con;
    public final @Nullable ConnectPacket packet;


    public CheckingNicknameEvent(String name, String uuid, NetConnection con, ConnectPacket packet) {
      this.name = name;
      this.uuid = uuid;
      this.con = con;
      this.packet = packet;
    }
  }

  public static class BlacklistedNicknameEvent {
    public final String name, uuid;
    public final NetConnection con;
    public final @Nullable ConnectPacket packet;


    public BlacklistedNicknameEvent(String name, String uuid, NetConnection con, ConnectPacket packet) {
      this.name = name;
      this.uuid = uuid;
      this.con = con;
      this.packet = packet;
    }
  }


  public static class NicknameListUpdatedEvent {
    public final String name;
    public final int uses;

    public NicknameListUpdatedEvent(String name, int uses) {
      this.name = name;
      this.uses = uses;
    }
  }

  public static class RegexListUpdatedEvent {
    public final Pattern regex;
    public final int uses;

    public RegexListUpdatedEvent(Pattern regex, int uses) {
      this.regex = regex;
      this.uses = uses;
    }
  }
}
