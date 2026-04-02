package stest.tron.wallet.common.client;

/**
 * Same indices as {@link AbstractHttpEndpoints024} (0, 2, 4 on {@code httpnode.ip.list}) but uses
 * {@code httpSolidityNode} to match shield/Zen HTTP tests that reference that field name.
 */
public abstract class AbstractHttpEndpointsZen024 {

  protected String httpnode = HttpNodeList.get(0);
  protected String httpSolidityNode = HttpNodeList.get(2);
  protected String httpPbftNode = HttpNodeList.get(4);
}
