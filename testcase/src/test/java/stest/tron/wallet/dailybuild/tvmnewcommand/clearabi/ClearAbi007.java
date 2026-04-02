package stest.tron.wallet.dailybuild.tvmnewcommand.clearabi;


import stest.tron.wallet.common.client.AbstractGrpcDualFullAndSolidityTest;
import static org.hamcrest.core.StringContains.containsString;

import io.grpc.ManagedChannel;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.junit.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.tron.api.GrpcAPI.TransactionExtention;
import org.tron.api.WalletGrpc;
import org.tron.api.WalletSolidityGrpc;
import stest.tron.wallet.common.client.Configuration;
import stest.tron.wallet.common.client.utils.ByteArray;
import stest.tron.wallet.common.client.utils.PublicMethed;
import stest.tron.wallet.common.client.utils.Utils;
import stest.tron.wallet.common.client.utils.ECKey;
@Slf4j
public class ClearAbi007 extends AbstractGrpcDualFullAndSolidityTest {

  private final String testNetAccountKey = Configuration.getByPath("testng.conf")
      .getString("foundationAccount.key2");
  private final byte[] testNetAccountAddress = PublicMethed.getFinalAddress(testNetAccountKey);
  byte[] contractAddress = null;
  ECKey ecKey1 = new ECKey(Utils.getRandom());
  byte[] contractExcAddress = ecKey1.getAddress();
  String contractExcKey = ByteArray.toHexString(ecKey1.getPrivKeyBytes());
  ECKey ecKey2 = new ECKey(Utils.getRandom());
  byte[] contractExcAddress1 = ecKey2.getAddress();
  String contractExcKey1 = ByteArray.toHexString(ecKey2.getPrivKeyBytes());
  private Long maxFeeLimit = Configuration.getByPath("testng.conf")
      .getLong("defaultParameter.maxFeeLimit");



  /**
   * constructor.
   */

  @BeforeClass(enabled = true)
  public void beforeClass() {
    PublicMethed.printAddress(contractExcKey);
  }

  @Test(enabled = true, description = "Clear a not meet the rules address")
  public void testClearAbi() {
    Assert.assertTrue(PublicMethed
        .sendcoin(contractExcAddress, 10000000000L, testNetAccountAddress, testNetAccountKey,
            blockingStubFull));

    PublicMethed.waitProduceNextBlock(blockingStubFull);
    byte[] fakeAddress = "412B5D".getBytes();
    TransactionExtention transactionExtention = PublicMethed
        .clearContractAbiForExtention(fakeAddress, contractExcAddress, contractExcKey,
            blockingStubFull);
    Assert
        .assertThat(transactionExtention.getResult().getCode().toString(),
            containsString("CONTRACT_VALIDATE_ERROR"));
    Assert
        .assertThat(transactionExtention.getResult().getMessage().toStringUtf8(),
            containsString("Contract validate error : Contract not exists"));
    byte[] fakeAddress1 = "412B5D3405B2D26767C9C09886D53DEAFF6EB718AC111".getBytes();

    TransactionExtention transactionExtention1 = PublicMethed
        .clearContractAbiForExtention(fakeAddress1, contractExcAddress, contractExcKey,
            blockingStubFull);
    Assert
        .assertThat(transactionExtention1.getResult().getCode().toString(),
            containsString("CONTRACT_VALIDATE_ERROR"));
    Assert
        .assertThat(transactionExtention1.getResult().getMessage().toStringUtf8(),
            containsString("Contract validate error : Contract not exists"));


  }


  /**
   * constructor.
   */
  @AfterClass
  public void shutdown() throws InterruptedException {
    PublicMethed
        .freedResource(contractExcAddress, contractExcKey, testNetAccountAddress, blockingStubFull);
    if (channelFull != null) {
      channelFull.shutdown().awaitTermination(5, TimeUnit.SECONDS);
    }
    if (channelFull1 != null) {
      channelFull1.shutdown().awaitTermination(5, TimeUnit.SECONDS);
    }
    if (channelSolidity != null) {
      channelSolidity.shutdown().awaitTermination(5, TimeUnit.SECONDS);
    }
  }


}
