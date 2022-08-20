package com.sheryv.util;

import org.apache.commons.lang3.StringUtils;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.regex.Pattern;

public class FileUtils {
  
  private static final Pattern FILE_NAME_FORBIDDEN_CHARS_PATTER = Pattern.compile("[\\\\/:*?\"<>|]");
  
  private FileUtils() {
  }
  
  public static final String CHARSET = "UTF-8";
  
  
  public static String readFileInMemory(Path path) throws IOException {
    return new String(Files.readAllBytes(path), CHARSET);
  }
  
  public static String readFileInMemory(String path) throws IOException {
    return new String(Files.readAllBytes(Paths.get(path)), CHARSET);
  }
  
  
  public static Optional<String> readFileInMemorySilently(String path) {
    try {
      Path p = Paths.get(path);
      return Optional.of(readFileInMemory(p));
    } catch (IOException e) {
//            Log.e(e);
      return Optional.empty();
    }
  }
  
  public static Optional<String> readFileInMemorySilently(Path path) {
    try {
      return Optional.of(readFileInMemory(path));
    } catch (IOException e) {
//            Log.e(e);
      return Optional.empty();
    }
  }
  
  public static BufferedReader readFileStream(String path) throws FileNotFoundException, InvalidPathException {
    return readFileStream(Paths.get(path));
  }
  
  public static BufferedReader readFileStream(Path path) throws FileNotFoundException {
    return new BufferedReader(new FileReader(path.toFile()));
  }
  
  public static BufferedWriter writeFileStream(Path path) throws IOException {
    return new BufferedWriter(new FileWriter(path.toFile()));
  }
  
  public static boolean saveFile(String text, Path file) {
    try {
      Files.write(file, text.getBytes(CHARSET));
      return true;
    } catch (IOException e) {
//            Log.e(e);
    }
    return false;
  }
  
  
  public static String sizeString(long bytes) {
    double mb = bytes / 1024D / 1024D;
    String label = "MB";
    int precision = 0;
    if (mb > 1024) {
      label = "GB";
      mb = mb / 1024D;
      precision = 2;
    }
    return String.format("%." + precision + "f %s", mb, label);
  }
  
  public static String fixFileName(String fileName) {
    return FILE_NAME_FORBIDDEN_CHARS_PATTER.matcher(fileName).replaceAll("");
  }
  
  public static String fixFileNameWithCollonSupport(String fileName) {
    String s = StringUtils.replace(fileName, ": ", " - ");
    s = StringUtils.replace(s, ":", "-");
    return fixFileName(s);
  }
}
