package stest.tron.wallet.common.client;

/**
 * Indices 1, 2, 4 on {@code httpnode.ip.list} with {@code httpSolidityNode} naming (market-style
 * HTTP tests).
 */
public abstract class AbstractHttpEndpointsZen124 {

  protected String httpnode = HttpNodeList.get(1);
  protected String httpSolidityNode = HttpNodeList.get(2);
  protected String httpPbftNode = HttpNodeList.get(4);
}
