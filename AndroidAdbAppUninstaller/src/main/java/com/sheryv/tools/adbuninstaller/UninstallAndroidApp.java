package com.sheryv.tools.adbuninstaller;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class UninstallAndroidApp {
    public static void main(String[] args) {
        try {
            if (args.length == 1) {
                if ("h".equals(args[0])) {
                    System.out.println(DOCS);
                    return;
                }
                Path listFile = Paths.get("list.txt");
                if (!listFile.toFile().exists()) {
                    System.out.println("list.txt file not found");
                    System.out.println(DOCS);
                }

                File file = new File("backups");
                file = new File(file.getAbsolutePath());
                file.mkdirs();
                List<String> list = Files.readAllLines(listFile);
                List<App> toBackupPaths = new ArrayList<>();
                for (String s : list) {
                    String read = s.trim();
                    if (read.startsWith("#") || "".equals(read))
                        continue;
                    Process exec = Runtime.getRuntime().exec("adb shell pm path " + read, null, file);
                    BufferedReader reader = new BufferedReader(new InputStreamReader(exec.getInputStream()));
                    String line = reader.readLine();
                    if (line == null || "".equals(line) || line.indexOf(':') < 0) {
                        continue;
                    }
                    String res = line.split(":")[1];
                    String fileName = fileNameWithoutExt(Paths.get(res).getFileName().toString());
                    toBackupPaths.add(new App(res, fileName, read));
                    System.out.printf("Path for %-15s => %15s%n", read, res);
                }

                if (args[0].contains("b"))
                    for (App app : toBackupPaths) {
                        Process exec = Runtime.getRuntime().exec("adb pull " + app.path + " " + app.fileName + "_" + app.packageName + ".apk", null, file);
                        System.out.println("Pulled " + app.fileName + "_" + app.packageName + ".apk");
                    }

                if (args[0].contains("u"))
                    for (App app : toBackupPaths) {
                        Process exec = Runtime.getRuntime().exec("adb shell pm uninstall -k --user 0 " + app.packageName, null, file);
                        System.out.println("Removed " + app.fileName + " -> " + app.packageName);
                    }
            } else {
                System.out.println(DOCS);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static String fileNameWithoutExt(String fileName) {
        int pos = fileName.lastIndexOf(".");
        if (pos == -1) return fileName;
        return fileName.substring(0, pos);
    }

    private static class App {
        final String path;
        final String fileName;
        final String packageName;

        private App(String path, String fileName, String packageName) {
            this.path = path;
            this.fileName = fileName;
            this.packageName = packageName;
        }
    }

    private static final String DOCS = "This app uninstalls applications on Android device \nconnected to local machine." +
            " Apps to uninstall have to be written in separate lines in list.txt file \nin the same directory. " +
            "\nEach line should contain application id [package] i.e: com.google.chrome. " +
            "\nCharacter # can be used to skip lines. \n\n\n" +

            "usage:\n" +
            " java -jar android-adb-app-uninstaller-all-*.jar [params]\n" +
            "  params:\n\n" +
            "  h - show this help\n" +
            "  l - list paths for given packages\n" +
            "  b - backup uninstalled apps\n" +
            "  u - uninstall apps\n\n" +
            "Example: java -jar android-adb-app-uninstaller-all-*.jar lb\n" +
            " java -jar android-adb-app-uninstaller-all-*.jar lbu";
}
