package stest.tron.wallet.common.client;

/** HTTP endpoint URLs for smart-contract HTTP tests (indices 0, 2, 3). */
public abstract class AbstractHttpSmartContractNodes023 {

  protected String httpnode = HttpNodeList.get(0);
  protected String httpSolidityNode = HttpNodeList.get(2);
  protected String httpRealSolidityNode = HttpNodeList.get(3);
}

