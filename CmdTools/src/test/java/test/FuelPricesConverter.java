package test;


import javax.annotation.Nonnull;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;

public class FuelPricesConverter {

    String p1, p2;

    public void name() throws IOException {
        String p = "F:\\__Docs\\STUDIA\\II_Magisterskie\\Semestr 9\\Wielowymiarowa analiza danych\\Projekt 2\\benzyna.csv";
        List<String> l = Files.readAllLines(Paths.get(p));
        LocalDate prev = null;
        LocalDate date;
        StringBuilder b = new StringBuilder();
        for (String s : l) {
            String[] c = s.split(";");

            String v = getString(c[0]);
            date = LocalDate.parse(v);
            if (date.getDayOfWeek() == DayOfWeek.MONDAY) {
                b.append(date);
                prev = date;
                rest(b, c);
                b.append("\n");
            } else if (prev != null && date.compareTo(prev.minusDays(7)) < 0) {
                b.append(prev.minusDays(7));
                rest(b, c);
                prev = prev.minusDays(7);
                b.append("\n");
            }
            if (!c[3].contains("-")) {
                p1 = c[3];
            }
            if (!c[4].contains("-")) {
                p2 = c[4];
            }
        }
        Files.write(Paths.get("F:\\__Docs\\STUDIA\\II_Magisterskie\\Semestr 9\\Wielowymiarowa analiza danych\\Projekt 2\\parsed2.csv"), b.toString().getBytes());
    }

    private void rest(StringBuilder builder, String[] c) {
        String v;
        if (c[3].contains("-") && p1 != null) {
            c[3] = p1;
        }
        if (c[4].contains("-") && p2 != null) {
            c[4] = p2;
        }
        for (int i = 1; i < c.length; i++) {
            v = getString(c[i]);
            if (v.contains("/")) {
                v = v.substring(0, v.indexOf("/") - 1);
            }
            builder.append(" ").append(v);
        }
    }

    @Nonnull
    private String getString(String part) {
        String v = part.substring(1, part.length() - 1);
        v = v.trim();
        return v;
    }


}
