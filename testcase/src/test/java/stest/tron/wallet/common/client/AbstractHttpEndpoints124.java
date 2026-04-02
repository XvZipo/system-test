package stest.tron.wallet.common.client;

/**
 * HTTP endpoint URLs from {@code httpnode.ip.list}: full node index 1, solidity index 2, PBFT index
 * 4. Used when the primary HTTP full node is the second entry in the list.
 */
public abstract class AbstractHttpEndpoints124 {

  protected String httpnode = HttpNodeList.get(1);
  protected String httpSoliditynode = HttpNodeList.get(2);
  protected String httpPbftNode = HttpNodeList.get(4);
}
