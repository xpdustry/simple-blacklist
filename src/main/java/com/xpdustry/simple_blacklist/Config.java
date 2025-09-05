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

import arc.ApplicationListener;
import arc.Core;
import arc.files.Fi;
import arc.struct.ObjectMap;
import arc.struct.Seq;
import arc.util.Timer;
import arc.util.serialization.Json;
import arc.util.serialization.JsonValue;

import com.xpdustry.simple_blacklist.util.JsonSettings;
import com.xpdustry.simple_blacklist.util.Strings;


@SuppressWarnings({ "unchecked", "rawtypes" })
public class Config {
  public static final Seq<Field<?>> all = new Seq<>();
  protected static JsonSettings settings;
  
  public static void init(Fi settingsFile) {
    settings = new JsonSettings(settingsFile);
    
    // Add regex serializer
    settings.getJson().setSerializer(Pattern.class, new Json.Serializer<Pattern>() {
      public void write(Json json, Pattern object, Class knownType) { json.writeValue(object.toString()); }
      public Pattern read(Json json, JsonValue jsonData, Class type) { return Pattern.compile(jsonData.asString()); }    
    });

    // Add an autosave task for every minutes
    Timer.schedule(() -> {
      if (all.contains(Field::modified) || settings.modified()) save();
    }, 60, 60);
    // Add a listener when exiting the server
    Core.app.addListener(new ApplicationListener() {
      public void dispose() { save(); }
    });  
  }
  
  public static synchronized void load() {
    if (settings == null) throw new IllegalStateException("#init() must be called before.");
    settings.load();
    all.each(Field::load);
  }
  
  public static synchronized void save() {
    if (settings == null) throw new IllegalStateException("#init() must be called before.");
    all.each(Field::save);
    settings.save();
  }
  
  public static boolean needSettingsMigration() {
    return Core.settings.has("simple-blacklist") || Core.settings.has("simple-blacklist-regexlist") ||
           Core.settings.has("simple-blacklist-message") || Core.settings.has("simple-blacklist-settings");
  }

  public static void migrateOldSettings() {
    if (Core.settings.has("simple-blacklist")) 
      namesList.set(Core.settings.getJson("simple-blacklist", ObjectMap.class, ObjectMap::new));
    if (Core.settings.has("simple-blacklist-regexlist")) 
      Core.settings.getJson("simple-blacklist-regexlist", ObjectMap.class, ObjectMap::new)
                   .each((k, v) -> regexList.put(Pattern.compile((String)k), (Integer)v));
    if (Core.settings.has("simple-blacklist-message")) 
      message.set(Core.settings.getString("simple-blacklist-message"));
    if (Core.settings.has("simple-blacklist-settings")) 
      // In case of
      try {
        boolean[] settings = Strings.int2bits(Core.settings.getInt("simple-blacklist-settings"));
        
        mode.set(settings[1] ? WorkingMode.banuuid : WorkingMode.kick);
        //listenerPriority.set(settings[2]); //useless and now it's falling back automatically
        //regexPriority.set(settings[3]); // useless
        namesEnabled.set(settings[4]);
        regexEnabled.set(settings[5]);
        ignoreAdmins.set(settings[6]);
        if (settings[1] && settings[7]) mode.set(WorkingMode.banip);
      } catch (IndexOutOfBoundsException ignored) {}

    Core.settings.remove("simple-blacklist");
    Core.settings.remove("simple-blacklist-regexlist");
    Core.settings.remove("simple-blacklist-message");
    Core.settings.remove("simple-blacklist-settings");
    save();
  }
  
  
  public static enum WorkingMode {
    kick("kick player"), banuuid("ban player UUID"), banip("ban player IP");
    
    public final String desc;
    WorkingMode(String desc) { this.desc = desc; }
  }
  
  
  public static class Field<T> {
    public final T defaultValue;
    public final String name, desc;
    protected boolean modified, loaded;
    protected T value;
    
    public Field(String name, String desc, T def) {
      this.name = name;
      this.desc = desc;
      this.defaultValue = def;
      all.add(this);
    }
    
    public boolean modified() {
      return modified;
    }
    
    public T get() {
      if (!loaded) {
        load();
        loaded = true;
      }
      return value;
    }
    
    public void set(T value) {
      this.value = value;
      modified = true;
      loaded = true;
    }

    public void load() {
      value = (T)settings.getOrPut(name, (Class<T>)defaultValue.getClass(), defaultValue);
      modified = false;
    }
    
    public void save() {
      if (modified) forcesave();
    }
    
    public void forcesave() {
      settings.put(name, loaded ? value : defaultValue);
      modified = false;
    }
    
    @Override
    public String toString() {
      return String.valueOf(get());
    }
  }
  
  public static class FieldList<T> extends Field<Seq<T>> {
    public final Class<?> elementType;
    
    public FieldList(String name, String desc, Class<T> elementType) {
      this(name, desc, elementType, new Seq<>());
    }
    
    public FieldList(String name, String desc, Class<T> elementType, Seq<T> def) { 
      super(name, desc, def); 
      this.elementType = elementType;
    }
    
    public void add(T element) {
      value.add(element);
      modified = true;
    }
    
    public boolean remove(T element) {
      boolean removed = value.remove(element);
      modified = true;
      return removed;
    }
    
    public boolean contains(T element) {
      return value.contains(element);
    }
    
    public T get(int index) {
      return value.get(index);
    }
    
    public void load() {
      value = (Seq<T>)settings.getOrPut(name, (Class<Seq<T>>)defaultValue.getClass(), elementType, defaultValue);
      modified = false;
    }
    
    public void forcesave() {
      settings.put(name, elementType, loaded ? value : defaultValue);
      modified = false;
    }
  }
  
  public static class FieldMap<K, V> extends Field<ObjectMap<K, V>> {
    public final Class<?> keyType, valueType;
    
    public FieldMap(String name, String desc, Class<K> keyType, Class<V> valueType) {
      this(name, desc, keyType, valueType, new ObjectMap<>());
    }
    
    public FieldMap(String name, String desc, Class<K> keyType, Class<V> valueType, ObjectMap<K, V> def) {
      super(name, desc, def);
      this.keyType = keyType;
      this.valueType = valueType;
    }
    
    public V put(K key, V value) {
      V old = this.value.put(key, value);
      modified = true;
      return old;
    }
    
    public V remove(K key) {
      V old = value.remove(key);
      modified = true;
      return old;
    }
    
    public boolean contains(K key) {
      return value.containsKey(key);
    }
    
    public V get(K key) {
      return value.get(key);
    }
    
    public void load() {
      value = (ObjectMap<K, V>)settings.getOrPut(name, (Class<ObjectMap<K, V>>)defaultValue.getClass(), 
                                                 valueType, keyType, defaultValue);
      modified = false;
    }
    
    public void forcesave() {
      settings.put(name, valueType, keyType, loaded ? value : defaultValue);
      modified = false;
    }
  }
  
  
  // Settings
  public static final Field<Boolean> 
    namesEnabled = new Field<>("names-enabled", "Whether nickname list is enabled", true),
    regexEnabled = new Field<>("regex-enabled", "Whether regex list is enabled", true);
  
  public static final FieldMap<String, Integer>
    namesList = new FieldMap("names", "Nickname list", String.class, Integer.class);
  public static final FieldMap<Pattern, Integer> 
    regexList = new FieldMap("regex", "Regex list", Pattern.class, Integer.class);

  public static final Field<String>
    message = new Field<>("message", "Kick message (can be empty)", "A part of your nickname is prohibited.");
  public static final Field<WorkingMode>
    mode = new Field<>("mode", "Working mode", WorkingMode.kick);
  public static final Field<Boolean>
    ignoreAdmins = new Field<>("ignore-admins", "Ignore admin players", false),
    nameCaseSensitive = new Field<>("case-sensitive", "Nickname list case sensitive", false);
}
