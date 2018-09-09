package com.sheryv.utils;

import com.sheryv.common.property.PropertyUtils;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;

public class FileUtils {
    private FileUtils() {
    }

    private static Charset charset;

    public static String readFileInMemory(Path path) throws IOException {
        return new String(Files.readAllBytes(path), getCharset());
    }

    public static String readFileInMemory(String path) throws IOException {
        return new String(Files.readAllBytes(Paths.get(path)), getCharset());
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

    public static boolean saveFile(String text, Path file) {
        try {
            Files.write(file, text.getBytes(getCharset()));
            return true;
        } catch (IOException e) {
//            Log.e(e);
        }
        return false;
    }


    public static Charset getCharset() {
        if (charset == null)
            charset = Charset.forName(PropertyUtils.getDefaultProperty(true).getCharset());
        return charset;
    }


}
