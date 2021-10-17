package test;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

public class Testing {
  
  @Test
  void name() throws InterruptedException {
    Thread.sleep(10000);
    System.out.println("Creating...");
    List<Voucher> v = new ArrayList<Voucher>(1_000_000);
    for (int i = 0; i < 1_000_000; i++) {
      v.add(new Voucher());
    }
    Thread.sleep(5000);
  
    v = null;
    System.gc();
    System.out.println("Cleaning...");
    
    Thread.sleep(10000);
    System.out.println("Creating...");
    v = new ArrayList<Voucher>(1_000_000);
    for (int i = 0; i < 1_000_000; i++) {
      v.add(new Voucher());
    }
    Thread.sleep(20000);
  }
}
