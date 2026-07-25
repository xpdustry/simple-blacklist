/*
 * This file is part of Simple Blacklist.
 *
 * MIT License
 *
 * Copyright (c) 2023-2026 Xpdustry
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

import arc.func.Cons;
import arc.struct.Seq;
import arc.util.CommandHandler;

import mindustry.Vars;
import mindustry.gen.Player;
import mindustry.mod.Plugin;


public class Main extends Plugin {
  public void importNameBanList() {
    Vars.netServer.admins.bannedNames.each(p -> {
      if (p.pattern().startsWith("\\Q") && p.pattern().endsWith("\\E"))
        Config.namesList.add(p.pattern().substring(2, p.pattern().length()-2), 0);
      else Config.regexList.add(p, 0);
    });
    Vars.netServer.admins.bannedNames.clear();
  }

  @Override
  public void init() {
    // Init logger
    Logger.init(this);

    // Load settings
    Config.init(getConfig());
    Config.load();

    // Import old settings, if necessary
    if (Config.needSettingsMigration()) {
      Logger.warn("Detected an old configuration in the server settings. Migrating...");
      Config.migrateOldSettings();
    }

    // Import existing name ban list, as we'll replace it
    importNameBanList();

    // Register plugin listeners
    Manager.registerListener();
  }

  @Override
  public void registerServerCommands(CommandHandler handler) {
    handler.register("name-ban", "[command] [args...]",
                     "Control the name blacklist. (use '&finame-ban help&fr' for usage)", args -> {
      if (args.length == 0) {
        // Print settings
        Logger.info("Settings:");
        Logger.info("&lk|&fr " + Config.mode.desc +": @", Config.mode.get().desc);
        Logger.info("&lk|&fr " + Config.message.desc +": @",
                    Config.message.get().isEmpty() ? "&fi(default)" : Config.message.get().replace("\n", "\\n"));
        Logger.info("&lk|&fr " + Config.ignoreAdmins.desc + ": @", Config.ignoreAdmins.get() ? "yes" : "no");
        Logger.info("&lk|&fr " + Config.caseSensitive.desc + ": @", Config.caseSensitive.get() ? "yes" : "no");

        // Format the lists
        Seq<String>
           left = Strings.lJust(Config.namesList.get().keys().toArray().map(s -> "&lk|&lw " + s),
                                Strings.max(Config.namesList.get(), e -> e.key.length() + 8)),
          right = Strings.lJust(Config.regexList.get().keys().toArray().map(s -> "&lk|&lw " + s),
                                Strings.max(Config.regexList.get(), e -> e.key.pattern().length() + 8));

        left = Strings.sJust(left, Config.namesList.get().values().toArray().map(t -> " &fi(uses: &lb"+t+"&lw)&fr"), 0);
        right = Strings.sJust(right, Config.regexList.get().values().toArray().map(t -> " &fi(uses: &lb"+t+"&lw)&fr"), 0);

        left.insert(0, Config.namesList.desc+": ["+
                       (Config.namesList.get().isEmpty() ? "&fb&lbempty&fr" : "total: &fb&lb"+
                         Config.namesList.get().size)+"&fr, "+
                       (Config.namesEnabled.get() ? "&fb&lgenabled&fr" : "&fb&lrdisabled&fr")+"]");
        right.insert(0, Config.regexList.desc+": ["+
                       (Config.regexList.get().isEmpty() ? "&fb&lbempty&fr" : "total: &fb&lb"+
                         Config.regexList.get().size)+"&fr, "+
                       (Config.regexEnabled.get() ? "&fb&lgenabled&fr" : "&fb&lrdisabled&fr")+"]");

        left = Strings.lJust(left, Strings.max(left, String::length)+2);

        // Print the lists
        Logger.ln();
        Strings.columnify(left, right).each(Logger::info);
      } else command(args, Logger::info, Logger::err);
    });
  }

  @Override
  public void registerClientCommands(CommandHandler handler){
    handler.<Player>register("name-ban", "[command] [args...]",
                             "Control the name blacklist. (use '/name-ban help' for usage)", (args, player) -> {
      if (!player.admin) player.sendMessage("[scarlet]You need admin permissions to use this command.");
      else if (args.length == 0) {
        player.sendMessage(Strings.format("""
          Settings:
          [lightgray]|[] @: [#1E90FF]@[]
          [lightgray]|[] @: [#1E90FF]@[white]
          [lightgray]|[] @: [#1E90FF]@[]
          [lightgray]|[] @: [#1E90FF]@[]
          """,
          Config.mode.desc, Config.mode.get().desc,
          Config.message.desc, Config.message.get().isEmpty() ? "(default)" : Config.message.get().replace("\n", "\\n"),
          Config.ignoreAdmins.desc, Config.ignoreAdmins.get() ? "yes" : "no",
          Config.caseSensitive.desc, Config.caseSensitive.get() ? "yes" : "no")
        );

        StringBuilder builder = new StringBuilder();

        builder.append(Config.namesList.desc).append(": [")
               .append(Config.namesList.get().isEmpty() ? "[#1E90FF]empty[]" : "total: [#1E90FF]"+
                       Config.namesList.get().size)
               .append("[], ").append(Config.namesEnabled.get() ? "[green]enabled[]" : "[scarlet]disabled[]")
               .append("]\n");
        Config.namesList.get().forEach(e ->
          builder.append("[lightgray]|[] ").append(e.key.replace("[", "[[")).append("  (uses: [#1E90FF]")
                 .append(e.value).append("[])\n"));

        player.sendMessage(builder.toString());
        builder.setLength(0);

        builder.append(Config.regexList.desc).append(": [")
               .append(Config.regexList.get().isEmpty() ? "[#1E90FF]&lbempty[]" : "total: [#1E90FF]"+
                       Config.regexList.get().size)
               .append("[], ").append(Config.regexEnabled.get() ? "[green]enabled[]" : "[scarlet]disabled[]")
               .append("]\n");
        Config.regexList.get().forEach(e ->
          builder.append("[lightgray]|[] ").append(e.key.pattern()).append("  (uses: [#1E90FF]").append(e.value)
                 .append("[])\n"));

        player.sendMessage(builder.toString());
      } else command(args, player::sendMessage, t -> player.sendMessage("[scarlet]" + t));
    });
  }

  private void command(String[] args, Cons<String> info, Cons<String> error) {
    switch (args[0]) {
      default:
        error.get("Invalid arguments. Use 'name-ban help' to see usage.");
        return;

      case "help":
        info.get("""
          Usage:  name-ban
             or:  name-ban help
             or:  name-ban reload
             or:  name-ban <names|regex> <add|del|clear> <value...>
             or:  name-ban <names|regex|ignore-admin|case-sensitive> <on|off>
             or:  name-ban mode <default|ban-ip|ban-uuid|kick>
             or:  name-ban message <default|off|text...>

          Description:
            Allows to filter player nicknames, which contain specific text or matches a regex.

            To create good regex, I recommend these websites:
              - https://regex101.com/
              - https://regex-generator.olafneumann.org/

          Notes:
            - Colors and glyphs are removed before nickname verification."""
        );
        return;

      case "reload":
        if (Config.load()) {
          importNameBanList();
          Manager.checkOnlinePlayers();
          info.get("Configuration reloaded.");
        } else error.get("Failed to reload configuration. See console logs for details.");
        return;

      case "names":
        if (args.length < 2) break;
        else if (args[1].startsWith("add ")) {
          String arg = args[1].substring(4).strip();
          if (arg.isEmpty()) break;

          if (Config.namesList.add(arg, 0) == null) {
            info.get("Nickname added to the list.");
            Manager.checkOnlinePlayers();
          } else error.get("Nickname already in the list.");

        } else if (args[1].startsWith("del ")) {
          String arg = args[1].substring(4).strip();
          if (arg.isEmpty()) break;

          if (Config.namesList.remove(arg) != null)
            info.get("Nickname removed from the list");
          else error.get("Nickname not in the list");

        } else if (args[1].startsWith("clear ")) {
          Config.namesList.clear();
          info.get("Nickname list emptied.");

        } else if (Strings.isTrue(args[1])) {
          Config.namesEnabled.set(true);
          info.get("Enabled nickname list.");

        } else if (Strings.isFalse(args[1])) {
          Config.namesEnabled.set(false);
          info.get("Disabled nickname list.");

        } else error.get("Invalid argument. Must be 'add', 'del', 'clear', 'on' or 'off'.");
        return;

      case "regex":
        if (args.length < 2) break;
        else if (args[1].startsWith("add ")) {
          String arg = args[1].substring(4).strip();
          if (arg.isEmpty()) break;

          Pattern pattern = null;
          // Check if regex is valid
          try {
            pattern = Pattern.compile(arg);
            if (pattern.matcher("test string") == null) pattern = null;
          } catch (Exception e) {}
          if (pattern == null) {
            error.get("Bad formatted regex.");
            return;
          }

          if (Config.regexList.add(pattern, 0) == null) {
            info.get("Regex added to the list.");
            Manager.checkOnlinePlayers();
          } else error.get("Regex already in the list.");

        } else if (args[1].startsWith("del ")) {
          String arg = args[1].substring(4).strip();
          if (arg.isEmpty()) break;

          Pattern pattern = Config.regexList.get().keys().toArray().find(p -> p.pattern().equals(arg));
          if (pattern != null) {
            Config.regexList.remove(pattern);
            info.get("Regex removed from the list");
          } else error.get("Regex not in the list");

        } else if (args[1].startsWith("clear ")) {
          Config.regexList.clear();
          info.get("Regex list emptied.");

        } else if (Strings.isTrue(args[1])) {
          Config.regexEnabled.set(true);
          info.get("Enabled regex list.");

        } else if (Strings.isFalse(args[1])) {
          Config.regexEnabled.set(false);
          info.get("Disabled regex list.");

        } else error.get("Invalid argument. Must be 'add', 'del', 'clear', 'on' or 'off'.");
        return;

      case "ignore-admin":
        if (args.length < 2) break;
        else if (Strings.isTrue(args[1])) {
          Config.regexEnabled.set(true);
          info.get("Blacklists will ignore admin players.");

        } else if (Strings.isFalse(args[1])) {
          Config.regexEnabled.set(false);
          info.get("Blacklists will check everyone.");

        } else error.get("Invalid argument. Must be 'on' or 'off'.");
        return;

      case "case-sensitive":
        if (args.length < 2) break;
        else if (Strings.isTrue(args[1])) {
          if (Config.caseSensitive.set(true) == Boolean.FALSE) Config.recompileRegexList();
          info.get("Case sensitivity enabled.");

        } else if (Strings.isFalse(args[1])) {
          if (Config.caseSensitive.set(false) == Boolean.TRUE) Config.recompileRegexList();
          info.get("Case sensitivity disabled.");

        } else error.get("Invalid argument. Must be 'on' or 'off'.");
        return;

      case "mode":
        if (args.length < 2) break;
        switch (args[1]) {
          case "ban-ip":
            Config.mode.set(WorkingMode.banip);
            info.get("Working mode set to ban the player IP.");
            return;

          case "ban-uuid":
            Config.mode.set(WorkingMode.banuuid);
            info.get("Working mode set to ban the player UUID.");
            return;

          case "kick":
            Config.mode.set(WorkingMode.kick);
            info.get("Working mode set to kick the player.");
            return;

          case "default":
            Config.mode.setDefault();
            info.get("Working mode set to default.");
            return;

          default:
            error.get("Invalid argument. Working mode must be 'ban-ip', 'ban-uuid', 'kick' or 'default'.");
            return;
        }

      case "message":
        if (args.length < 2) break;
        else if (args[1].equals("default")) {
          Config.message.setDefault();
          info.get("Kick message set to default.");
        } else if (args[1].equals("off")) {
          Config.message.set("");
          info.get("Kick message removed.");
        } else {
          Config.message.set(args[1]);
          info.get("Kick message modified.");
        }
        return;
    }

    error.get("Missing argument(s). Use 'name-ban help' to see usage.");
  }
}
