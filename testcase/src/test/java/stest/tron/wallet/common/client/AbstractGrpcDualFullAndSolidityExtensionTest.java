package stest.tron.wallet.common.client;

import org.testng.annotations.BeforeClass;
import org.tron.api.WalletExtensionGrpc;

/** Adds WalletExtension stub on top of {@link AbstractGrpcDualFullAndSolidityTest}. */
public abstract class AbstractGrpcDualFullAndSolidityExtensionTest
    extends AbstractGrpcDualFullAndSolidityTest {

  protected WalletExtensionGrpc.WalletExtensionBlockingStub blockingStubExtension = null;

  @BeforeClass
  public void initGrpcExtensionStub() {
    blockingStubExtension = WalletExtensionGrpc.newBlockingStub(channelSolidity);
  }
}

