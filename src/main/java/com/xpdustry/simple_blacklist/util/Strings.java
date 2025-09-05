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

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;

import arc.func.Intf;
import arc.struct.Seq;
import arc.util.serialization.JsonValue;
import arc.util.serialization.JsonWriter.OutputType;


public class Strings extends arc.util.Strings {
  public static String rJust(String str, int length) { return rJust(str, length, " "); }
  /** Justify string to the right. E.g. "&emsp; right" */
  public static String rJust(String str, int length, String filler) {
    int sSize = str.length(), fSize = filler.length();
    
    if (fSize == 0 || sSize >= length) return str; 
    if (fSize == 1) return repeat(filler, length-sSize)+str;   
    int add = length-sSize;
    return repeat(filler, add/fSize)+filler.substring(0, add%fSize)+str;
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
    if (fSize == 1) return str+repeat(filler, length-sSize);
    int add = length-sSize;
    return str+repeat(filler, add/fSize)+filler.substring(0, add%fSize);
  }
  public static Seq<String> lJust(Seq<String> list, int length) { return lJust(list, length, " "); }
  public static Seq<String> lJust(Seq<String> list, int length, String filler) {
    list.replace(str -> lJust(str, length, filler));
    return list;
  }
  
  public static String cJust(String str, int length) { return cJust(str, length, " "); }
  /** Justify string to the center. E.g. "&emsp; center &emsp;". */
  public static String cJust(String str, int length, String filler) {
    int sSize = str.length(), fSize = filler.length();
    
    if (fSize == 0 || sSize >= length) return str;
    int add = length-sSize, left = add/2, right = add-add/2;
    if (fSize == 1) return repeat(filler, left)+str+repeat(filler, right);
    return repeat(filler, left/fSize)+filler.substring(0, left%fSize)+str+
           repeat(filler, right/fSize)+filler.substring(0, right%fSize);
  }
  public static Seq<String> cJust(Seq<String> list, int length) { return cJust(list, length, " "); }
  public static Seq<String> cJust(Seq<String> list, int length, String filler) {
    list.replace(str -> cJust(str, length, filler));
    return list;
  }

  public static String sJust(String left, String right, int length) { return sJust(left, right, length, " "); }
  /** Justify string to the sides. E.g. "left &emsp; right" */
  public static String sJust(String left, String right, int length, String filler) {
    int fSize = filler.length(), lSize = left.length(), rSize = right.length();
    
    if (fSize == 0 || lSize+rSize >= length) return left+right; 
    int add = length-lSize-rSize;
    if (fSize == 1) return left+repeat(filler, add)+right;
    return left+repeat(filler, add/fSize)+filler.substring(0, add%fSize)+right;
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
  
  /** Taken from the {@link String#repeat(int)} method of JDK 11 */
  public static String repeat(String str, int count) {
    if (count < 0) throw new IllegalArgumentException("count is negative: " + count);
    if (count == 1) return str;

    final byte[] value = str.getBytes();
    final int len = value.length;
    if (len == 0 || count == 0)  return "";
    if (Integer.MAX_VALUE / count < len) throw new OutOfMemoryError("Required length exceeds implementation limit");
    if (len == 1) {
      final byte[] single = new byte[count];
      java.util.Arrays.fill(single, value[0]);
      return new String(single);
    }
    
    final int limit = len * count;
    final byte[] multiple = new byte[limit];
    System.arraycopy(value, 0, multiple, 0, len);
    int copied = len;
    for (; copied < limit - copied; copied <<= 1) 
      System.arraycopy(multiple, 0, multiple, copied, copied);
    System.arraycopy(multiple, 0, multiple, copied, limit - copied);
    return new String(multiple);
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

  public static int maxLength(Iterable<String> list) {
    return max(list, str -> str.length());
  }

  public static String normalise(String str) {
    return stripGlyphs(stripColors(str)).trim();
  }

  public static long bits2int(boolean... list) {
    int out = 0;

    for (int i=0; i<list.length; i++) {
      out |= list[i] ? 1 : 0;
      out <<= 1;
    }
    
    return out >> 1;
  }

  public static boolean[] int2bits(long number) { return int2bits(number, 0); }
  public static boolean[] int2bits(long number, int bits) {
    // Check value because 0 have a negative size  
    if (number == 0) return new boolean[bits == 0 ? 1 : bits];
      
    int size = bits < 1 ? (int) (Math.log(number)/Math.log(2)+1) : bits;
    boolean[] out = new boolean[size];
    
    while (size-- > 0) {
      out[size] = (number & 1) != 0;
      number >>= 1;
    }

    return out;
  }
  
  /** @return whether the specified string mean true */
  public static boolean isTrue(String str) {
    switch (str.toLowerCase()) {
      case "1": case "true": case "on": 
      case "enable": case "activate": case "yes":
               return true;
      default: return false;
    }
  }
  
  /** @return whether the specified string mean false */
  public static boolean isFalse(String str) {
    switch (str.toLowerCase()) {
      case "0": case "false": case "off": 
      case "disable": case "desactivate": case "no":
               return true;
      default: return false;
    }
  }

  public static String jsonPrettyPrint(JsonValue object, OutputType outputType) {
    StringWriter out = new StringWriter();
    try { jsonPrettyPrint(object, out, outputType, 0); } 
    catch (IOException ignored) { return ""; }
    return out.toString();
  }
  
  public static void jsonPrettyPrint(JsonValue object, Writer writer, OutputType outputType) throws IOException {
    jsonPrettyPrint(object, writer, outputType, 0);
  }
  
  /** 
   * Re-implementation of {@link JsonValue#prettyPrint(OutputType, Writer)}, 
   * because the ident isn't correct and the max object size before new line is too big.
   */
  public static void jsonPrettyPrint(JsonValue object, Writer writer, OutputType outputType, int indent) throws IOException {
    if (object.isObject()) {
      if (object.child == null) writer.append("{}");
      else {
        indent++;
        boolean newLines = needNewLine(object, 1);
        writer.append(newLines ? "{\n" : "{ ");
        for (JsonValue child = object.child; child != null; child = child.next) {
          if(newLines) writer.append(repeat("  ", indent));
          writer.append(outputType.quoteName(child.name));
          writer.append(": ");
          jsonPrettyPrint(child, writer, outputType, indent);
          if((!newLines || outputType != OutputType.minimal) && child.next != null) writer.append(',');
          writer.append(newLines ? '\n' : ' ');
        }
        if(newLines) writer.append(repeat("  ", indent - 1));
        writer.append('}');
      }
    } else if (object.isArray()) {
      if (object.child == null) writer.append("[]");
      else {
        indent++;
        boolean newLines = needNewLine(object, 1);
        writer.append(newLines ? "[\n" : "[ ");
        for (JsonValue child = object.child; child != null; child = child.next) {
          if (newLines) writer.append(repeat("  ", indent));
          jsonPrettyPrint(child, writer, outputType, indent);
          if ((!newLines || outputType != OutputType.minimal) && child.next != null) writer.append(',');
          writer.append(newLines ? '\n' : ' ');
        }
        if (newLines) writer.append(repeat("  ", indent - 1));
        writer.append(']');
      }
    } else if(object.isString()) writer.append(outputType.quoteValue(object.asString()));
    else if(object.isDouble()) writer.append(Double.toString(object.asDouble()));
    else if(object.isLong()) writer.append(Long.toString(object.asLong()));
    else if(object.isBoolean()) writer.append(Boolean.toString(object.asBoolean()));
    else if(object.isNull()) writer.append("null");
    else throw new arc.util.serialization.SerializationException("Unknown object type: " + object);
  }
  
  private static boolean needNewLine(JsonValue object, int maxChildren) {
    for (JsonValue child = object.child; child != null; child = child.next) 
      if (child.isObject() || child.isArray() || --maxChildren < 0) return true;
    return false;
  }
}
