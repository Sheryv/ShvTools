package test;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class WadDataConverter {

    public static final String FILE = "F:\\__Docs\\STUDIA\\II_Magisterskie\\Semestr 9\\Wielowymiarowa analiza danych\\Projekt 3\\marzec.csv";

    public static void main(String[] args) throws IOException {
        var list = new ArrayList<List<String>>();
        for (int i = 0; i < 24; i++) {
            list.add(new ArrayList<>());
        }

        Locale.setDefault(Locale.US);
        List<String> f = Files.readAllLines(Paths.get(FILE));
        try (FileWriter writer = new FileWriter(new File(FILE + "_done.csv"))) {

            for (String line : f) {
                line = line.replace(',', '.')
                        .replace("20-", "20 ")
                        .replace("21-", "21 ")
                        .trim();
                var values = line.split(";");
                for (int i = 0; i < values.length; i++) {
                    var v = values[i];
                    if (v.trim().isEmpty()) {
                        List<String> column = list.get(i);
                        v = String.format("%s", column.get(column.size() - 1));
                    }
                    writer.append(v);
                    if (i < values.length - 1) {
                        writer.append(";");
                    }
                    list.get(i).add(v);
                }
                writer.write("\n");
            }

        }
        System.out.println();
    }
}
