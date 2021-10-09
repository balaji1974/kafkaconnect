package com.bala.kafka.kafka_connect;

import org.junit.Test;

public class MySourceConnectorConfigTest {
  @Test
  public void doc() {
    System.out.println(MySourceConnectorConfig.conf().toRst());
  }
}