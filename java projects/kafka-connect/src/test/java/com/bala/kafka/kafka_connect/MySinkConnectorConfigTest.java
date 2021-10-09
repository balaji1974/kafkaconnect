package com.bala.kafka.kafka_connect;

import org.junit.Test;

public class MySinkConnectorConfigTest {
  @Test
  public void doc() {
    System.out.println(MySinkConnectorConfig.conf().toRst());
  }
}
