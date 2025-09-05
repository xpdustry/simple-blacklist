/*
 * This file is part of Simple Blacklist.
 *
 * MIT License
 *
 * Copyright (c) 2023-2025 Xpdustry
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

import mindustry.gen.Player;

import com.xpdustry.simple_blacklist.util.Logger;
import com.xpdustry.simple_blacklist.util.Strings;


public class Main extends mindustry.mod.Plugin {
  private static final Logger logger = new Logger();
  
  @Override
  public void init() {
    // Init logger
    Logger.init(this);
    
    // Load settings
    Config.init(getConfig());
    Config.load();
    
    // Import old settings, in the server's config, if necessary
    if (Config.needSettingsMigration()) {
      logger.warn("Detected an old configuration, in the server settings. Migrating the config...");
      Config.migrateOldSettings();
    }
    
    // Register plugin listeners
    Manager.registerListeners();
  }

  @Override
  public void registerServerCommands(arc.util.CommandHandler handler) {
    handler.register("blacklist", "[command] [args...]", 
                     "Control the blacklist. (use '&fiblacklist help&fr' for usage)", args -> {
      if (args.length == 0) {
        // Print settings
        logger.info("Settings:");
        logger.info("&lk|&fr " + Config.mode.desc +": @", Config.mode.get().desc);
        logger.info("&lk|&fr " + Config.message.desc +": @", Config.message.get().isEmpty() ? "&fi(default)" : 
                                                                 Config.message.get());
        logger.info("&lk|&fr " + Config.ignoreAdmins.desc + ": @", Config.ignoreAdmins.get() ? "yes" : "no");
        logger.info("&lk|&fr " + Config.nameCaseSensitive.desc + ": @", Config.nameCaseSensitive.get() ? "yes" : "no");
  
        // Format the lists
        Seq<String> left = Strings.lJust(Config.namesList.get().keys().toSeq().map(s -> "&lk|&lw "+s), 
                                         Strings.max(Config.namesList.get(), e -> e.key.length()+8)),
                    right = Strings.lJust(Config.regexList.get().keys().toSeq().map(s -> "&lk|&lw "+s.toString()), 
                                          Strings.max(Config.regexList.get(), e -> e.key.pattern().length()+8));
  
        left = Strings.sJust(left, Config.namesList.get().values().toSeq().map(t -> " &fi(uses: &lb"+t+"&lw)&fr"), 0);
        right = Strings.sJust(right, Config.regexList.get().values().toSeq().map(t -> " &fi(uses: &lb"+t+"&lw)&fr"), 0);
  
        left.insert(0, Config.namesList.desc+": ["+
                       (Config.namesList.get().isEmpty() ? "&fb&lbempty&fr" : "total: &fb&lb"+
                         Config.namesList.get().size)+"&fr, "+
                       (Config.namesEnabled.get() ? "&fb&lgenabled&fr" : "&fb&lrdisabled&fr")+"]");
        right.insert(0, Config.regexList.desc+": ["+
                       (Config.regexList.get().isEmpty() ? "&fb&lbempty&fr" : "total: &fb&lb"+
                         Config.regexList.get().size)+"&fr, "+
                       (Config.regexEnabled.get() ? "&fb&lgenabled&fr" : "&fb&lrdisabled&fr")+"]");

        left = Strings.lJust(left, Strings.maxLength(left)+2);
        
        // Print the lists
        logger.none();
        Strings_columnify(left, right).each(logger::info);
      } else blacklistCommand(args, logger::info, logger::err);
    });
  }
  
  @Override
  public void registerClientCommands(CommandHandler handler){
    handler.<Player>register("blacklist", "[command] [args...]", 
                             "Control the blacklist. (use '/blacklist help' for usage)", (args, player) -> {
      if (!player.admin) player.sendMessage("[scarlet]You need admin permissions to use this command.");
      else if (args.length == 0) {
        player.sendMessage(
          Strings.format("Settings:\n"
                       + "[lightgray]|[] @: [#1E90FF]@[]\n"
                       + "[lightgray]|[] @: [#1E90FF]@[white]\n"
                       + "[lightgray]|[] @: [#1E90FF]@[]\n"
                       + "[lightgray]|[] @: [#1E90FF]@[]\n",
                         Config.mode.desc, Config.mode.get().desc, 
                         Config.message.desc, Config.message.get().isEmpty() ? "(default)" : Config.message.get(),
                         Config.ignoreAdmins.desc, Config.ignoreAdmins.get() ? "yes" : "no",
                         Config.nameCaseSensitive.desc, Config.nameCaseSensitive.get() ? "yes" : "no"));
        
        StringBuilder builder = new StringBuilder();
        
        builder.append(Config.namesList.desc).append(": [")
               .append(Config.namesList.get().isEmpty() ? "[#1E90FF]empty[]" : "total: [#1E90FF]"+ 
                       Config.namesList.get().size)
               .append("[], ").append(Config.namesEnabled.get() ? "[green]enabled[]" : "[scarlet]disabled[]")
               .append("]\n");
        Config.namesList.get().each((n, t) ->
          builder.append("[lightgray]|[] ").append(n.replace("[", "[[")).append("  (uses: [#1E90FF]").append(t)
                 .append("[])\n"));
        
        player.sendMessage(builder.toString());
        builder.setLength(0);
        
        builder.append(Config.regexList.desc).append(": [")
               .append(Config.regexList.get().isEmpty() ? "[#1E90FF]&lbempty[]" : "total: [#1E90FF]"+ 
                       Config.regexList.get().size)
               .append("[], ").append(Config.regexEnabled.get() ? "[green]enabled[]" : "[scarlet]disabled[]")
               .append("]\n");
        Config.regexList.get().each((p, t) -> 
          builder.append("[lightgray]|[] ").append(p.pattern()).append("  (uses: [#1E90FF]").append(t).append("[])\n"));
      
        player.sendMessage(builder.toString());
      } else blacklistCommand(args, player::sendMessage, t -> player.sendMessage("[scarlet]" + t));
    });
  }
  
  private void blacklistCommand(String[] args, Cons<String> info, Cons<String> error) {
    switch (args[0]) {
      default:
        error.get("Invalid arguments. Use 'blacklist help' to see usage.");
        return;

      case "help":
        info.get("Usage:  blacklist\n"
               + "   or:  blacklist help\n"
               + "   or:  blacklist reload\n"
               + "   or:  blacklist <names|regex> <add|del> <value...>\n"
               + "   or:  blacklist <names|regex|ignore-admin|case-sensitive> <on|off>\n"
               + "   or:  blacklist mode <ban-ip|ban-uuid|kick>\n"
               + "   or:  blacklist message <text...>\n\n"
               + "Description:\n"
               + "  Allows to filter player nicknames, which contain specific text or matches a regex.\n\n"
               + "  To create good regex, I recommend these websites:\n"
               + "    - https://regex101.com/\n"
               + "    - https://regex-generator.olafneumann.org/\n\n"
               + "Notes:\n"
               + "  - Colors and glyphs are removed before nickname verification.\n"
               + "  - The \"\" (double quotes) value can be used to specify an empty value.\n");
        return;

      case "reload":
        Config.load();
        info.get("Configuration reloaded.");
        return;
        
      case "names":
        if (args.length < 2) break;
        else if (args[1].startsWith("add")) {
          String arg = args[1].substring(3).trim();
          if (arg.isEmpty()) break;
          
          if (!Config.namesList.get().containsKey(arg)) {
            Config.namesList.put(arg, 0);
            info.get("Nickname added to the list.");
            Manager.checkOnlinePlayers();

          } else error.get("Nickname already in the list.");

        } else if (args[1].startsWith("del")) {
          String arg = args[1].substring(3).trim();
          if (arg.isEmpty()) break;
          
          if (Config.namesList.get().containsKey(arg)) {
            Config.namesList.remove(arg);
            info.get("Nickname removed from the list");

          } else error.get("Nickname not in the list");
          
        } else if (Strings.isTrue(args[1])) {
          Config.namesEnabled.set(true);
          info.get("Enabled nickname list.");
          
        } else if (Strings.isFalse(args[1])) {
          Config.namesEnabled.set(false);
          info.get("Disabled nickname list.");
          
        } else error.get("Invalid argument. Must be 'add', 'del', 'on' or 'off'.");
        return;
          
      case "regex":
        if (args.length < 2) break;
        else if (args[1].startsWith("add")) {
          String arg = args[1].substring(3).trim();
          if (arg.isEmpty()) break;

          if (Structs_find(Config.regexList.get().keys(), p -> p.pattern().equals(arg)) == null) {
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

            Config.regexList.put(pattern, 0);
            info.get("Regex added to the list.");
            Manager.checkOnlinePlayers();

          } else error.get("Regex already in the list.");
          
        } else if (args[1].startsWith("del")) {
          String arg = args[1].substring(3).trim();
          if (arg.isEmpty()) break;

          Pattern pattern = Structs_find(Config.regexList.get().keys(), p -> p.pattern().equals(arg));
          if (pattern != null) {
            Config.regexList.remove(pattern);
            info.get("Regex removed from the list");

          } else error.get("Regex not in the list");

        } else if (Strings.isTrue(args[1])) {
          Config.regexEnabled.set(true);
          info.get("Enabled regex list.");
          
        } else if (Strings.isFalse(args[1])) {
          Config.regexEnabled.set(false);
          info.get("Disabled regex list.");
          
        } else error.get("Invalid argument. Must be 'add', 'del', 'on' or 'off'.");
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
          Config.nameCaseSensitive.set(true);
          info.get("Nickname list is now case sensitive.");
          
        } else if (Strings.isFalse(args[1])) {
          Config.nameCaseSensitive.set(false);
          info.get("Nickname list will now ignore the case.");
          
        } else error.get("Invalid argument. Must be 'on' or 'off'.");
        return;
        
      case "mode":
        if (args.length < 2) break;
        switch (args[1]) {
          case "ban-ip":
            Config.mode.set(Config.WorkingMode.banip);
            info.get("Working mode set to ban the player IP.");
            return;
            
          case "ban-uuid":
            Config.mode.set(Config.WorkingMode.banuuid);
            info.get("Working mode set to ban the player UUID.");
            return;
            
          case "kick":
            Config.mode.set(Config.WorkingMode.kick);
            info.get("Working mode set to kick the player.");
            return;
            
          default:
            error.get("Invalid argument. Working mode must be 'ban-ip', 'ban-uuid' or 'kick'.");
            return;
        }
        
      case "message":
        if (args.length < 2) break;
        else if (args[1].equals("\"\"")) {
          Config.message.set("");
          info.get("Kick message for blacklisted nickname set to default.");
          
        } else {
          Config.message.set(args[1]);
          info.get("Kick message for blacklisted nickname modified.");
        }
        return;
    }
    
    error.get("Missing argument(s). Use 'blacklist help' to see usage.");
  }
  
  /** {@link arc.util.Structs#find(T[], Boolf)}, but with {@code Iterable<T>} instead of {@code T[]}. */
  private static <T> T Structs_find(Iterable<T> array, arc.func.Boolf<T> value){
    for(T t : array) {
      if (value.get(t)) return t;
    }
    return null;
  }
  
  /** {@link Strings#columnify(Seq[])} for only two columns and ignores logging color codes. */
  private static Seq<String> Strings_columnify(Seq<String> left, Seq<String> right) {
    Seq<Integer> sl = left.map(l -> arc.util.Log.removeColors(l).length()),
                 sr = right.map(l -> arc.util.Log.removeColors(l).length());
    String lf = Strings.repeat(" ", Strings.max(sl, e -> e)),
           rf = Strings.repeat(" ", Strings.max(sr, e -> e));
    Seq<String> arr = left;
    int i = 0;

    for (; i<Integer.min(left.size, right.size); i++) arr.set(i, left.get(i)+right.get(i));
    // Fill the rest
    for (; i<left.size; i++) arr.set(i, left.get(i)+rf);
    for (; i<right.size; i++) arr.add(lf+right.get(i));
    
    return arr;
  }
}
