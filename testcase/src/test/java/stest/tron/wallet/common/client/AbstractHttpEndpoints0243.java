package stest.tron.wallet.common.client;

/**
 * HTTP endpoint URLs from {@code httpnode.ip.list} indices 0, 2, 4, and the fullnode solidity port
 * at index 3.
 */
public abstract class AbstractHttpEndpoints0243 {

  protected String httpnode = HttpNodeList.get(0);
  protected String httpSoliditynode = HttpNodeList.get(2);
  protected String httpPbftNode = HttpNodeList.get(4);
  protected String httpnodeSolidityPort = HttpNodeList.get(3);
}

