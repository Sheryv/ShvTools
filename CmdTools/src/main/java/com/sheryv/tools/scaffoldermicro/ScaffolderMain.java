package com.sheryv.tools.scaffoldermicro;

import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.Mustache;
import com.github.mustachejava.MustacheFactory;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;

public class ScaffolderMain {
  
  public static void main(String[] args) {
    System.out.println(ScaffolderMain.class.getName());
    System.out.println(System.getProperty("user.home"));
    
    
    Scanner scanner = new Scanner(System.in);
    while (scanner.hasNext()) {
      String s = scanner.nextLine();
      if (s.equals("exit")) {
        System.out.println("Exiting");
        return;
      }
      System.out.println("Read: " + s);
    }

//        MustacheFactory mf = new DefaultMustacheFactory();
////        FileReader reader = new FileReader();
//        Mustache mustache = mf.compile("template.mustache");
//        try {
//            mustache.execute(new PrintWriter(System.out), new ScaffolderMain()).flush();
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
  
  }
  
  
  List<Item> items() {
    return Arrays.asList(
        new Item("Item 1", "$19.99", Arrays.asList(new Feature("New!"), new Feature("Awesome!"))),
        new Item("Item 2", "$29.99", Arrays.asList(new Feature("Old."), new Feature("Ugly.")))
    );
  }
  
  static class Item {
    Item(String name, String price, List<Feature> features) {
      this.name = name;
      this.price = price;
      this.features = features;
    }
    
    String name, price;
    List<Feature> features;
  }
  
  static class Feature {
    Feature(String description) {
      this.description = description;
    }
    
    String description;
  }
}
