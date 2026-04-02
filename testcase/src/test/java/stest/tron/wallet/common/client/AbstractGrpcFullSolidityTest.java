package stest.tron.wallet.common.client;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import org.testng.annotations.BeforeClass;
import org.tron.api.WalletGrpc;
import org.tron.api.WalletSolidityGrpc;

/**
 * Shared gRPC setup for tests that need a default full node and solidity node (first entries in
 * testng.conf lists). Subclasses use {@code blockingStubFull} and {@code blockingStubSolidity}.
 */
public abstract class AbstractGrpcFullSolidityTest {

  protected ManagedChannel channelFull = null;
  protected ManagedChannel channelSolidity = null;
  protected WalletGrpc.WalletBlockingStub blockingStubFull = null;
  protected WalletSolidityGrpc.WalletSolidityBlockingStub blockingStubSolidity = null;
  protected String fullnode = GrpcNodeList.full(0);
  protected String soliditynode = GrpcNodeList.solidity(0);

  @BeforeClass
  public void initGrpcFullAndSolidityChannels() {
    channelFull = ManagedChannelBuilder.forTarget(fullnode).usePlaintext().build();
    blockingStubFull = WalletGrpc.newBlockingStub(channelFull);
    channelSolidity = ManagedChannelBuilder.forTarget(soliditynode).usePlaintext().build();
    blockingStubSolidity = WalletSolidityGrpc.newBlockingStub(channelSolidity);
  }
}
