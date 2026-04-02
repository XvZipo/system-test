package stest.tron.wallet.common.client.utils;

import stest.tron.wallet.common.client.HttpNodeList;

/** HTTP endpoint URLs for Zen TRC20 shield tests (indices 0, 1, 2). */
public abstract class AbstractZenTrc20HttpNodes012 extends ZenTrc20Base {

  protected String httpnode = HttpNodeList.get(0);
  protected String anotherHttpnode = HttpNodeList.get(1);
  protected String httpSolidityNode = HttpNodeList.get(2);
}

