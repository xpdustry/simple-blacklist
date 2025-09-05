/*
 * This file is part of Anti-VPN-Service (AVS). The plugin securing your server against VPNs.
 *
 * MIT License
 *
 * Copyright (c) 2024-2025 Xpdustry
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

package com.xpdustry.simple_blacklist.util;

import arc.util.Log;
import arc.util.Log.LogLevel;

import mindustry.Vars;
import mindustry.mod.Mods;
import mindustry.mod.Mod;


/** Log messages to console with topics */
public class Logger {
  protected static final Object[] empty = {};
  protected static String[] tagsColors = {"&lc&fb", "&lb&fb", "&ly&fb", "&lr&fb", ""};
  /** Will use slf4j when slf4md plugin is present */
  protected static boolean slf4mdPresent;
  protected static Object slf4jLogger;
  
  public static String mainTopic;
  public static String topicFormat = "&fb&ly[@&fb&ly]&fr ";
  /** 
   * Force debug log level of all logger instances, even {@link LogLevel} is not to {@code debug}. <br>
   * Note that if enabled, {@link Log} will be always used.
   */
  public static boolean forceDebug = false;
  
  /** Sets the main topic using the mod. */
  public static void init(Mod mod) {
    init(mod.getClass());
  }
  
  /** Sets the main topic using the mod class. */
  public static void init(Class<? extends Mod> mod) {
    Mods.LoadedMod load = Vars.mods.getMod(mod);
    if (load == null) throw new IllegalArgumentException("Mod is not loaded yet (or missing)!");
    mainTopic = load.meta.displayName;
    slf4mdPresent = mindustry.Vars.mods.locateMod("slf4md") != null;
    if (slf4mdPresent) slf4jLogger = org.slf4j.LoggerFactory.getLogger(mod);
  }
  
  /** Sets the main topic */
  public static void init(String mainTopic) {
    Logger.mainTopic = mainTopic;
    slf4mdPresent = mindustry.Vars.mods.locateMod("slf4md") != null;
    if (slf4mdPresent) slf4jLogger = org.slf4j.LoggerFactory.getLogger(mainTopic);
  }

  public final String topic;
  
  /** Will only use the {@link #mainTopic}. */
  public Logger() { 
    topic = null;
  }
  
  public Logger(Class<?> clazz) {
    this(Strings.insertSpaces(clazz.getSimpleName()));
  }
  
  public Logger(String topic) {
    topic = topic.trim();
    this.topic = topic.isEmpty() ? null : topic;
  }

  /** 
   * Return {@code true} if the log level is not enough to log the message. <br>
   * Can be used like that: {@code if (cannotLog(level)) return;}
   */
  public boolean cannotLog(LogLevel level) {
    return Log.level == LogLevel.none || (!forceDebug && Log.level.ordinal() > level.ordinal());
  }

  public void log(LogLevel level, String text, Throwable th, Object... args) {
    if(cannotLog(level)) return;
   
    if (text != null) {
      text = Log.format(text, args);
      if (th != null) text += ": " + Strings.getStackTrace(th);
    } else if (th != null) text = Strings.getStackTrace(th);

    if (!forceDebug && slf4mdPresent && slf4jLogger != null) {
      synchronized (slf4jLogger) {
        String tag = topic == null ? "" : Log.format(Strings.format(topicFormat, topic), empty);
        org.slf4j.Logger l = (org.slf4j.Logger)slf4jLogger;
        arc.func.Cons<String> printer;
        
        switch (level) {
          case debug: printer = l::debug; break;
          case info: printer = l::info; break;
          case warn: printer = l::warn; break;
          case err: printer = l::error; break;
          default: return;
        }
  
        if (text == null || text.isEmpty()) {
          printer.get(tag);
          return;
        }
        
        int i = 0, nl = text.indexOf('\n');
        while (nl >= 0) {
          printer.get(tag + text.substring(i, nl));
          i = nl + 1;
          nl = text.indexOf('\n', i);
        } 
        printer.get(tag + (i == 0 ? text : text.substring(i)));
        return;        
      }
    }
    
    synchronized (Log.logger) {
      String tag = "";
      if (mainTopic != null) tag += Log.format(Strings.format("@[@]&fr ", tagsColors[level.ordinal()], mainTopic), empty);
      if (topic != null) tag += Log.format(Strings.format(topicFormat, topic), empty);
  
      if (text == null || text.isEmpty()) {
        Log.logger.log(level, tag);
        return;
      }
        
      int i = 0, nl = text.indexOf('\n');
      while (nl >= 0) {
        Log.logger.log(level, tag + text.substring(i, nl));
        i = nl + 1;
        nl = text.indexOf('\n', i);
      } 
      Log.logger.log(level, tag + (i == 0 ? text : text.substring(i)));      
    }
  }
  public void log(LogLevel level, String text, Object... args) { log(level, text, null, args); }
  public void log(LogLevel level, String text) { log(level, text, empty); }

  public void debug(String text, Object... args) { log(LogLevel.debug, text, args); }
  public void debug(Object object) { debug(String.valueOf(object), empty); }

  public void info(String text, Object... args) { log(LogLevel.info, text, args); }
  public void info(Object object) { info(String.valueOf(object), empty); }

  public void warn(String text, Object... args) { log(LogLevel.warn, text, args); }
  public void warn(String text) { warn(text, empty); }  
  
  public void err(String text, Throwable th, Object... args) { log(LogLevel.err, text, th, args); }
  public void err(String text, Object... args) { err(text, null, args); }
  public void err(String text) { err(text, null, empty); }
  public void err(String text, Throwable th) { err(text, th, empty); }
  public void err(Throwable th) { err(null, th, empty); } 
  
  /** Log an empty "info" line */
  public void none() { log(LogLevel.info, null, empty); }
}
