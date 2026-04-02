package stest.tron.wallet.common.client.utils;

import stest.tron.wallet.common.client.Configuration;

/** HTTP endpoint URLs for Zen TRC20 shield tests (indices 0, 1, 2). */
public abstract class AbstractZenTrc20HttpNodes012 extends ZenTrc20Base {

  protected String httpnode =
      Configuration.getByPath("testng.conf").getStringList("httpnode.ip.list").get(0);
  protected String anotherHttpnode =
      Configuration.getByPath("testng.conf").getStringList("httpnode.ip.list").get(1);
  protected String httpSolidityNode =
      Configuration.getByPath("testng.conf").getStringList("httpnode.ip.list").get(2);
}

