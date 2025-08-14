package stest.tron.wallet.dailybuild.tvmnewcommand.newGrammar;

import com.google.protobuf.ByteString;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.binary.Hex;
import org.junit.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.tron.api.GrpcAPI;
import org.tron.api.WalletGrpc;
import org.tron.protos.Protocol;
import org.tron.protos.contract.SmartContractOuterClass;
import stest.tron.wallet.common.client.Configuration;
import stest.tron.wallet.common.client.utils.ByteArray;
import stest.tron.wallet.common.client.utils.ECKey;
import stest.tron.wallet.common.client.utils.PublicMethed;
import stest.tron.wallet.common.client.utils.Utils;

import java.util.HashMap;
import java.util.concurrent.TimeUnit;

@Slf4j
public class NewFeatureForSolc0825 {

  private final String testNetAccountKey = Configuration.getByPath("testng.conf")
      .getString("foundationAccount.key2");
  private final byte[] testNetAccountAddress = PublicMethed.getFinalAddress(testNetAccountKey);
  byte[] contractC = null;
  ECKey ecKey1 = new ECKey(Utils.getRandom());
  byte[] contractExcAddress = ecKey1.getAddress();
  String contractExcKey = ByteArray.toHexString(ecKey1.getPrivKeyBytes());
  private Long maxFeeLimit = Configuration.getByPath("testng.conf")
      .getLong("defaultParameter.maxFeeLimit");
  private ManagedChannel channelFull = null;
  private WalletGrpc.WalletBlockingStub blockingStubFull = null;
  private String fullnode = Configuration.getByPath("testng.conf")
      .getStringList("fullnode.ip.list").get(0);


  /**
   * constructor.
   */

  @BeforeClass(enabled = true)
  public void beforeClass() {
    PublicMethed.printAddress(contractExcKey);
    channelFull = ManagedChannelBuilder.forTarget(fullnode)
        .usePlaintext()
        .build();
    blockingStubFull = WalletGrpc.newBlockingStub(channelFull);
    Assert.assertTrue(PublicMethed
            .sendcoin(contractExcAddress, 11010_000_000L,
                    testNetAccountAddress, testNetAccountKey, blockingStubFull));
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    String filePath = "src/test/resources/soliditycode/NewFeature0825.sol";
    String contractName = "C";
    HashMap retMap = PublicMethed.getBycodeAbi(filePath, contractName);
    String code = retMap.get("byteCode").toString();
    String abi = retMap.get("abI").toString();
    Assert.assertTrue(code.contains("5e"));
    contractC = PublicMethed.deployContract(contractName, abi, code, "", maxFeeLimit,
        0L, 100, null, contractExcKey,
        contractExcAddress, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    SmartContractOuterClass.SmartContract smartContract = PublicMethed.getContract(contractC,
        blockingStubFull);
    Assert.assertNotNull(smartContract.getAbi());
  }


  @Test(enabled = true, description = "Code Generator: Use ``MCOPY`` instead of ``MLOAD``/``MSTORE`` loop " +
          "when copying byte arrays.")
  public void test001McopyCopyBytes() {
    String txid = PublicMethed
            .triggerContract(contractC,"copyBytes()", "#", false,
                    0, maxFeeLimit, "0", 0, contractExcAddress, contractExcKey, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);

    Protocol.TransactionInfo info = PublicMethed.getTransactionInfoById(txid, blockingStubFull).get();
    Assert.assertEquals(Protocol.TransactionInfo.code.SUCESS, info.getResult());
    String out = ByteArray.toHexString(info.getContractResult(0).toByteArray()).substring(128);
    Assert.assertEquals("6161616161000000000000000000000000000000000000000000000000000000", out);
  }

  @Test(enabled = true, description = "Code Generator: Use ``MCOPY`` instead of ``MLOAD``/``MSTORE`` loop " +
          "when copying byte arrays.")
  public void test002McopyCopyString() {
    String txid = PublicMethed
            .triggerContract(contractC, "copyString()", "#", false,
                    0, maxFeeLimit, "0", 0, contractExcAddress, contractExcKey, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    Protocol.TransactionInfo info = PublicMethed.getTransactionInfoById(txid, blockingStubFull).get();
    Assert.assertEquals(Protocol.TransactionInfo.code.SUCESS, info.getResult());
    String out = ByteArray.toHexString(info.getContractResult(0).toByteArray()).substring(128);
    Assert.assertEquals("72657475726e20737472696e6700000000000000000000000000000000000000", out);

  }

  /**
   * constructor.
   */
  @AfterClass
  public void shutdown() throws InterruptedException {
    PublicMethed.freedResource(contractExcAddress, contractExcKey,
        testNetAccountAddress, blockingStubFull);
    if (channelFull != null) {
      channelFull.shutdown().awaitTermination(5, TimeUnit.SECONDS);
    }
  }


}

