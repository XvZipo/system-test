package stest.tron.wallet.common.client;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import org.testng.annotations.BeforeClass;
import org.tron.api.WalletGrpc;
import org.tron.api.WalletSolidityGrpc;

/**
 * gRPC setup for tests that need two full-node channels plus one solidity channel (indices 0 and 1
 * on fullnode.ip.list, index 0 on solidityNode.ip.list).
 */
public abstract class AbstractGrpcDualFullAndSolidityTest {

  protected ManagedChannel channelFull = null;
  protected ManagedChannel channelFull1 = null;
  protected ManagedChannel channelSolidity = null;
  protected WalletGrpc.WalletBlockingStub blockingStubFull = null;
  protected WalletGrpc.WalletBlockingStub blockingStubFull1 = null;
  protected WalletSolidityGrpc.WalletSolidityBlockingStub blockingStubSolidity = null;
  protected String fullnode = GrpcNodeList.full(0);
  protected String fullnode1 = GrpcNodeList.full(1);
  protected String soliditynode = GrpcNodeList.solidity(0);

  @BeforeClass
  public void initGrpcDualFullAndSolidityChannels() {
    channelFull = ManagedChannelBuilder.forTarget(fullnode).usePlaintext().build();
    blockingStubFull = WalletGrpc.newBlockingStub(channelFull);
    channelFull1 = ManagedChannelBuilder.forTarget(fullnode1).usePlaintext().build();
    blockingStubFull1 = WalletGrpc.newBlockingStub(channelFull1);
    channelSolidity = ManagedChannelBuilder.forTarget(soliditynode).usePlaintext().build();
    blockingStubSolidity = WalletSolidityGrpc.newBlockingStub(channelSolidity);
  }
}
