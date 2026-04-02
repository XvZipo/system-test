package stest.tron.wallet.common.client;

/**
 * HTTP endpoint URLs from {@code httpnode.ip.list}: full node index 0, solidity index 2, PBFT index
 * 4. Matches the most common triple used across {@code dailybuild/http} tests.
 */
public abstract class AbstractHttpEndpoints024 {

  protected String httpnode = HttpNodeList.get(0);
  protected String httpSoliditynode = HttpNodeList.get(2);
  protected String httpPbftNode = HttpNodeList.get(4);
}
