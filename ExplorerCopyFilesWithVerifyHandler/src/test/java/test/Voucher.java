package test;

import lombok.Data;

import java.util.concurrent.ThreadLocalRandom;

@Data
public class Voucher {
  private String serial = "2343243243";
  private String pin = "2343243243";
  private String hash = "234324324342341231231231241242354543524523sd";
  private String group = "6SD1";
  private String state = "Enabled";
  private long id;
  private long series;
  private long seriesType;
  private long id2;
  private int value;
  private String region = "Bas";
  
  public Voucher() {
    ThreadLocalRandom random = ThreadLocalRandom.current();
    id = random.nextLong();
    series = random.nextLong();
    seriesType = random.nextLong();
    id2 = random.nextLong();
    long l = random.nextLong(1000, 10000);
  
    serial = serial + l;
    pin = pin + l;
    hash = hash + l;
    region = region + l;
  }
}
