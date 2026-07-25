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

package com.xpdustry.simple_blacklist;

import arc.func.Intf;
import arc.struct.IntSeq;
import arc.struct.Seq;
import arc.util.Log;


public class Strings extends arc.util.Strings {
  public static String rJust(String str, int length) { return rJust(str, length, " "); }
  /** Justify string to the right. E.g. "&emsp; right" */
  public static String rJust(String str, int length, String filler) {
    int sSize = str.length(), fSize = filler.length();

    if (fSize == 0 || sSize >= length) return str;
    if (fSize == 1) return filler.repeat(length - sSize) + str;
    int add = length - sSize;
    return filler.repeat(add / fSize) + filler.substring(0, add % fSize) + str;
  }
  public static Seq<String> rJust(Seq<String> list, int length) { return rJust(list, length, " "); }
  public static Seq<String> rJust(Seq<String> list, int length, String filler) {
    list.replace(str -> rJust(str, length, filler));
    return list;
  }

  public static String lJust(String str, int length) { return lJust(str, length, " "); }
  /** Justify string to the left. E.g. "left &emsp;" */
  public static String lJust(String str, int length, String filler) {
    int sSize = str.length(), fSize = filler.length();

    if (fSize == 0 || sSize >= length) return str;
    if (fSize == 1) return str + filler.repeat(length - sSize);
    int add = length - sSize;
    return str + filler.repeat(add / fSize) + filler.substring(0, add % fSize);
  }
  public static Seq<String> lJust(Seq<String> list, int length) { return lJust(list, length, " "); }
  public static Seq<String> lJust(Seq<String> list, int length, String filler) {
    list.replace(str -> lJust(str, length, filler));
    return list;
  }

  public static String sJust(String left, String right, int length) { return sJust(left, right, length, " "); }
  /** Justify string to the sides. E.g. "left &emsp; right" */
  public static String sJust(String left, String right, int length, String filler) {
    int fSize = filler.length(), lSize = left.length(), rSize = right.length();

    if (fSize == 0 || lSize + rSize >= length) return left + right;
    int add = length - lSize - rSize;
    if (fSize == 1) return left + filler.repeat(add) + right;
    return left + filler.repeat(add / fSize) + filler.substring(0, add % fSize) + right;
  }
  public static Seq<String> sJust(Seq<String> left, Seq<String> right, int length) { return sJust(left, right, length, " "); }
  public static Seq<String> sJust(Seq<String> left, Seq<String> right, int length, String filler) {
    Seq<String> arr = /*new Seq<>(Integer.max(left.size, right.size))*/left; // for optimization, the left side will be used
    int i = 0, min = Integer.min(left.size, right.size);

    for (; i<min; i++) arr.set(i, /*.add(*/sJust(left.get(i), right.get(i), length, filler));
    // Fill the rest
    for (; i<left.size; i++) arr.set(i, /*.add(*/lJust(left.get(i), length, filler));
    for (; i<right.size; i++) arr.add(rJust(right.get(i), length, filler));

    return arr;
  }

  public static Seq<String> columnify(Seq<String> left, Seq<String> right) {
    String lf = " ".repeat(IntSeq_max(left.mapInt(l -> Log.removeColors(l).length()))),
           rf = " ".repeat(IntSeq_max(right.mapInt(l -> Log.removeColors(l).length())));
    Seq<String> arr = left;
    int i = 0;

    for (; i<Integer.min(left.size, right.size); i++) arr.set(i, left.get(i)+right.get(i));
    // Fill the rest
    for (; i<left.size; i++) arr.set(i, left.get(i)+rf);
    for (; i<right.size; i++) arr.add(lf+right.get(i));

    return arr;
  }

  private static int IntSeq_max(IntSeq array) {
    boolean first = true;
    int best = 0;
    for (int i=0; i<array.size; i++) {
      int value = array.items[i];
      if (first) best = value;
      else if (value > best) best = value;
      first = false;
    }
    return best;
  }

  public static String normalise(String str) {
    return stripGlyphs(stripColors(str)).strip();
  }

  /** @return whether the specified string mean true */
  public static boolean isTrue(String str) {
    return switch (str.toLowerCase()) {
      case "1", "true", "on", "enable", "activate", "yes" -> true;
      default -> false;
    };
  }

  /** @return whether the specified string mean false */
  public static boolean isFalse(String str) {
    return switch (str.toLowerCase()) {
      case "0", "false", "off", "disable", "desactivate", "no" -> true;
      default -> false;
    };
  }

  public static <T> int max(Iterable<T> list, Intf<T> intifier) {
    boolean first = true;
    int best = 0;
    for (T i : list) {
      int s = intifier.get(i);
      if (first) best = s;
      else if (s > best) best = s;
      first = false;
    }
    return best;
  }
}
