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

import arc.util.Log;
import arc.util.Log.LogHandler;
import arc.util.Log.LogLevel;

import mindustry.Vars;
import mindustry.mod.Mods;
import mindustry.mod.Mod;


public class Logger {
  protected static final Object[] empty = {};
  protected static final String[] tagsColors = {"&lc&fb", "&lb&fb", "&ly&fb", "&lr&fb", ""};
  /** Will use slf4j when slf4md plugin is present */
  protected static boolean slf4mdPresent;

  public static String mainTopic;
  public static String topicFormat = "&fb&ly[@&fb&ly]&fr ";
  public static LogHandler logger;

  /** Sets the main topic using the mod. */
  public static void init(Mod mod) {
    init(mod.getClass());
  }

  /** Sets the main topic using the mod class. */
  public static void init(Class<? extends Mod> mod) {
    Mods.LoadedMod load = Vars.mods.getMod(mod);
    if (load == null) throw new IllegalArgumentException("Mod is not loaded yet (or missing)!");
    mainTopic = load.meta.displayName;
    slf4mdPresent = Vars.mods.locateMod("slf4md") != null;
    if (slf4mdPresent) {
      org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(mod);
      logger = (l, t) -> {switch (l) {
        case debug: log.debug(t); break;
        case info: log.info(t); break;
        case warn: log.warn(t); break;
        case err: log.error(t); break;
        default: break;
      }};
    } else logger = Log.logger;
  }

  /** Sets the main topic */
  public static void init(String mainTopic) {
    Logger.mainTopic = mainTopic;
    slf4mdPresent = Vars.mods.locateMod("slf4md") != null;
    if (slf4mdPresent) {
      org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(mainTopic);
      logger = (l, t) -> {switch (l) {
        case debug: log.debug(t); break;
        case info: log.info(t); break;
        case warn: log.warn(t); break;
        case err: log.error(t); break;
        default: break;
      }};
    } else logger = (l, t) -> Log.logger.log(l, t);
  }


  protected static void logImpl(LogLevel level, String text, Throwable th, Object... args) {
    if(Log.level.ordinal() > level.ordinal()) return;

    if (text != null) {
      text = Log.format(text, args);
      if (th != null) text += ": " + Strings.getStackTrace(th);
    } else if (th != null) text = Strings.getStackTrace(th);

    String tag = slf4mdPresent || mainTopic == null ? "" :
                 Log.format(Strings.format("@[@]&fr ", tagsColors[level.ordinal()], mainTopic), empty);

    if (text == null || text.isEmpty()) {
      logger.log(level, tag);
      return;
    }

    int i = 0, nl = text.indexOf('\n');
    while (nl >= 0) {
      logger.log(level, tag + text.substring(i, nl));
      i = nl + 1;
      nl = text.indexOf('\n', i);
    }
    logger.log(level, tag + (i == 0 ? text : text.substring(i)));
  }

  public static void log(LogLevel level, String text, Object... args) { logImpl(level, text, null, args); }
  public static void log(LogLevel level, String text) { log(level, text, empty); }

  public static void debug(String text, Object... args) { log(LogLevel.debug, text, args); }
  public static void debug(Object object) { debug(String.valueOf(object), empty); }

  public static void info(String text, Object... args) { log(LogLevel.info, text, args); }
  public static void info(Object object) { info(String.valueOf(object), empty); }

  public static void warn(String text, Object... args) { log(LogLevel.warn, text, args); }
  public static void warn(String text) { warn(text, empty); }

  public static void err(String text, Throwable th, Object... args) { logImpl(LogLevel.err, text, th, args); }
  public static void err(String text, Object... args) { err(text, null, args); }
  public static void err(String text, Throwable th) { err(text, th, empty); }
  public static void err(String text) { err(text, null, empty); }
  public static void err(Throwable th) { err(null, th, empty); }

  /** Log an empty "info" line. */
  public static void ln() { log(LogLevel.info, null, empty); }
}
