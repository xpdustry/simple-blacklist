/*
 * This file is part of Simple Blacklist.
 *
 * MIT License
 *
 * Copyright (c) 2025-2026 Xpdustry
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

import java.io.*;
import java.util.regex.Pattern;

import arc.ApplicationListener;
import arc.Core;
import arc.files.Fi;
import arc.func.Func;
import arc.func.Prov;
import arc.struct.*;
import arc.util.Log;
import arc.util.Timer;
import arc.util.serialization.*;


@SuppressWarnings({ "unchecked" })
public class Config {
  public static final Seq<Field<?>> all = new Seq<>();
  protected static Fi file;
  protected static Jval values;
  protected static boolean modified;

  public static void init(Fi settingsFile) {
    file = settingsFile;

    // Add an autosave task for every minutes
    Timer.schedule(Config::save, 60, 60);
    // Add a listener when exiting the server
    Core.app.addListener(new ApplicationListener() { public void dispose() { forceSave(); } });
  }

  protected static void check() {
    if (file == null) throw new IllegalStateException("Config#init() must be called before.");
  }

  public static boolean load() {
    check();
    boolean success = false;
    if (file.exists()) {
      try (Reader reader = file.reader()) {
        Jval parsed = Jval.read(reader);
        parsed.asObject(); // To be sure it's a map
        values = parsed;
        success = true;
      } catch (Exception e) {
        moveCorrupted();
        Log.err("Failed to read settings file '" + file + "'", e);
        values = Jval.newObject();
      }
    } else values = Jval.newObject();

    modified = false;
    try { all.each(Field::load); }
    catch (Exception e) {
      success = false;
      Log.err("Failed to load settings", e);
    }
    return success;
  }

  public static boolean save() {
    if (!modified) return false;
    return forceSave();
  }

  public static boolean forceSave() {
    check();
    try { all.each(Field::save); }
    catch (Exception e) {
      Log.err("Failed to save settings", e);
      return false;
    }
    return write();
  }

  protected static boolean write() {
    try (Writer writer = file.writer(false)) {
      values.writeTo(writer, Jval.Jformat.formatted);
      modified = false;
      return true;
    } catch (Exception e) {
      moveCorrupted();
      Log.err("Failed to write settings file '" + file + "'", e);
      return false;
    }
  }

  protected static void moveCorrupted() {
    if (!file.exists()) return;
    // Move file instead of overwriting it
    try {
      String extension = file.extension();
      String name = file.nameWithoutExtension() + "_corrupted-" + System.currentTimeMillis() +
                    (extension.isEmpty() ? "" : "." + extension);
      file.moveTo(file.parent().child(name));
    } catch (Exception ignored) {}
  }

  protected static void setModified() {
    modified = true;
  }

  protected static Jval getValues() {
    return values;
  }

  protected static <T> Jval encode(T o) {
    Class<?> type = o == null ? null : o.getClass();
    Jval v = o == null ? Jval.NULL :
             type == Boolean.class ? Jval.valueOf((boolean)o) :
             type == Integer.class ? Jval.valueOf((int)o) :
             type == Float.class ? Jval.valueOf((float)o) :
             type == Long.class ? Jval.valueOf((long)o) :
             type == Double.class ? Jval.valueOf((double)o) :
             type == String.class ? Jval.valueOf((String)o) :
             null;
    if (v == null) throw new IllegalArgumentException("object is not a null, string or primive value");
    return v;
  }

  protected static <T> T decode(Jval v) {
    return (T)switch (v.getType()) {
      case string -> v.asString();
      case number -> v.asNumber();
      case object -> v.asObject();
      case array -> v.asArray();
      case bool -> v.asBool();
      case nil -> null;
    };
  }

  public static boolean needSettingsMigration() {
    return Core.settings.has("simple-blacklist") || Core.settings.has("simple-blacklist-regexlist") ||
           Core.settings.has("simple-blacklist-message") || Core.settings.has("simple-blacklist-settings");
  }

  public static void migrateOldSettings() {
    check();
    // Move existing config to avoid overriding it
    if (file.exists()) {
      try {
        String extension = file.extension();
        String name = file.nameWithoutExtension() + (extension.isEmpty() ? "" : "." + extension) + ".old";
        file.moveTo(file.parent().child(name));
      } catch (Exception ignored) {}
    }

    if (Core.settings.has("simple-blacklist"))
      namesList.set(Core.settings.getJson("simple-blacklist", ArrayMap.class, ArrayMap::new));

    if (Core.settings.has("simple-blacklist-regexlist"))
      Core.settings.getJson("simple-blacklist-regexlist", ObjectMap.class, ObjectMap::new)
                   .each((k, v) -> regexList.put(Pattern.compile((String)k), (Integer)v));

    if (Core.settings.has("simple-blacklist-message"))
      message.set(Core.settings.getString("simple-blacklist-message"));

    if (Core.settings.has("simple-blacklist-settings")) {
      int value = Core.settings.getInt("simple-blacklist-settings");
      if (value != 0) {
        int size = (int) (Math.log(value)/Math.log(2)+1);
        boolean[] settings = new boolean[size];
        while (size-- > 0) {
          settings[size] = (value & 1) != 0;
          value >>= 1;
        }

        // In case of
        try {
          // First bit was used to prevent data loss. Idk why...
          mode.set(settings[1] ? WorkingMode.banuuid : WorkingMode.kick);
          //listenerPriority.set(settings[2]); //useless and now it's falling back automatically
          //regexPriority.set(settings[3]); // useless
          namesEnabled.set(settings[4]);
          regexEnabled.set(settings[5]);
          ignoreAdmins.set(settings[6]);
          if (settings[1] && settings[7]) mode.set(WorkingMode.banip);
        } catch (IndexOutOfBoundsException ignored) {}
      }
    }

    Core.settings.remove("simple-blacklist");
    Core.settings.remove("simple-blacklist-regexlist");
    Core.settings.remove("simple-blacklist-message");
    Core.settings.remove("simple-blacklist-settings");
    save();
  }


  public static class Field<T> {
    public final String name, desc;
    protected final Prov<T> defaultMaker;
    protected final Func<Jval, T> loader;
    protected final Func<T, Jval> saver;
    protected boolean loaded;
    protected T value;

    public Field(String name, String desc, T def) {
      this(name, desc, () -> def);
    }

    public Field(String name, String desc, T def, Func<Jval, T> loader, Func<T, Jval> saver) {
      this(name, desc, () -> def, loader, saver);
    }

    public Field(String name, String desc, Prov<T> def) {
      this(name, desc, def, Config::decode, Config::encode);
    }

    public Field(String name, String desc, Prov<T> def, Func<Jval, T> loader, Func<T, Jval> saver) {
      this.name = name;
      this.desc = desc;
      this.defaultMaker = def;
      this.loader = loader;
      this.saver = saver;

      // Little test to ensure loader and saver are working
      T d = getDefault();
      T v = loader.get(saver.get(d));
      if (v != null && d != null && v.getClass() != d.getClass())
        throw new IllegalArgumentException("Invalid loader/saver: decoded type is not the same as the default type; "
                                         + v.getClass().getName() + " != " + d.getClass().getName());

      all.add(this);
    }

    public T get() {
      if (!loaded) {
        load();
        loaded = true;
      }
      return value;
    }

    public T getDefault() {
      return defaultMaker.get();
    }

    public void setDefault() {
      value = getDefault();
    }

    public T set(T value) {
      T old = this.value;
      this.value = value;
      setModified();
      loaded = true;
      return old;
    }

    public void load() {
      Jval v = getValues().get(name);
      value = v == null ? getDefault() : loader.get(v);
    }

    public void save() {
      getValues().add(name, saver.get(loaded ? value : getDefault()));
    }

    @Override
    public String toString() {
      return String.valueOf(get());
    }
  }

  public static class FieldMap<K, V> extends Field<ArrayMap<K, V>> {
    public FieldMap(String name, String desc, Func<String, K> keyLoader, Func<K, String> keySaver) {
      this(name, desc, ArrayMap::new, keyLoader, keySaver);
    }

    public FieldMap(String name, String desc, Func<String, K> keyLoader, Func<K, String> keySaver,
                    Func<Jval, V> valueLoader, Func<V, Jval> valueSaver) {
      this(name, desc, ArrayMap::new, keyLoader, keySaver, valueLoader, valueSaver);
    }

    public FieldMap(String name, String desc, Prov<ArrayMap<K, V>> def, Func<String, K> keyLoader,
                    Func<K, String> keySaver) {
      this(name, desc, def, keyLoader, keySaver, Config::decode, Config::encode);
    }

    public FieldMap(String name, String desc, Prov<ArrayMap<K, V>> def, Func<String, K> keyLoader,
                    Func<K, String> keySaver, Func<Jval, V> valueLoader, Func<V, Jval> valueSaver) {
      super(name, desc, def, v -> {
        Jval.JsonMap map = v.asObject();
        ArrayMap<K, V> out = new ArrayMap<>(map.size);
        out.size = map.size;
        for (int i=0; i<map.size; i++){
          out.setKey(i, keyLoader.get(map.getKeyAt(i)));
          out.setValue(i, valueLoader.get(map.getValueAt(i)));
        }
        return out;
      }, v -> {
        Jval out = Jval.newObject();
        Jval.JsonMap map = out.asObject();
        map.ensureCapacity(v.size);
        map.size = v.size;
        for (int i=0; i<v.size; i++) {
          map.setKey(i, keySaver.get(v.getKeyAt(i)));
          map.setValue(i, valueSaver.get(v.getValueAt(i)));
        }
        return out;
      });
    }

    public V put(K key, V value) {
      ArrayMap<K, V> map = get();
      int index = map.indexOfKey(key);
      V old = null;
      if (index != -1) {
        old = map.getValueAt(index);
        map.setValue(index, value);
      } else map.insert(map.size, key, value);
      setModified();
      return old;
    }

    /**
     * Same as {@link #put()} but does not replace an existing value.
     * @return the current value with the key, not the specified one, or null if not present.
     */
    public V add(K key, V value) {
      ArrayMap<K, V> map = get();
      int index = map.indexOfKey(key);
      if (index != -1) return map.getValueAt(index);
      map.insert(map.size, key, value);
      setModified();
      return null;
    }

    public V remove(K key) {
      V old = get().removeKey(key);
      setModified();
      return old;
    }

    public boolean contains(K key) {
      return get().containsKey(key);
    }

    public V get(K key) {
      return get().get(key);
    }

    public void clear() {
      get().clear();
    }
  }

  /** Helper for regex list. */
  public static Pattern newPattern(String pattern) {
    return Pattern.compile(pattern, caseSensitive.get() ? 0 : Pattern.CASE_INSENSITIVE);
  }
  /** Helper for regex list. */
  public static void recompileRegexList() {
    for (int i=0, n=regexList.get().size; i<n; i++)
      regexList.get().setKey(n, newPattern(regexList.get().getKeyAt(n).pattern()));
  }


  // Settings
  public static final Field<Boolean>
    namesEnabled = new Field<>("names-enabled", "Whether nickname list is enabled", true),
    regexEnabled = new Field<>("regex-enabled", "Whether regex list is enabled", true);
  public static final FieldMap<String, Integer>
    namesList = new FieldMap<>("names", "Nickname list", k -> k, k -> k, Jval::asInt, Jval::valueOf);
  public static final FieldMap<Pattern, Integer>
    regexList = new FieldMap<>("regex", "Regex list", Config::newPattern, Pattern::pattern, Jval::asInt, Jval::valueOf);
  public static final Field<String>
    message = new Field<>("message", "Kick message (can be empty)", "A part of your nickname is prohibited.");
  public static final Field<WorkingMode>
    mode = new Field<>("mode", "Working mode", WorkingMode.kick, v -> WorkingMode.valueOf(v.asString()),
                       v -> Jval.valueOf(v.name()));
  public static final Field<Boolean>
    ignoreAdmins = new Field<>("ignore-admins", "Ignore admin players", false),
    caseSensitive = new Field<>("case-sensitive", "Case sensitive", false);
}
