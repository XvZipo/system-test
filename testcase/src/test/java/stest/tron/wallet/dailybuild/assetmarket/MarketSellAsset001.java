package stest.tron.wallet.dailybuild.assetmarket;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import lombok.extern.slf4j.Slf4j;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.tron.api.WalletGrpc;
import stest.tron.wallet.common.client.Configuration;
import stest.tron.wallet.common.client.utils.*;

@Slf4j
public class MarketSellAsset001 {

  private static final long now = System.currentTimeMillis();
  private static final String name = "testAssetIssue003_" + Long.toString(now);
  private static final String shortname = "a";
  private final String foundationKey001 =
      Configuration.getByPath("testng.conf").getString("foundationAccount.key1");
  private final String foundationKey002 =
      Configuration.getByPath("testng.conf").getString("foundationAccount.key2");
  private final byte[] foundationAddress001 = PublicMethed.getFinalAddress(foundationKey001);
  private final byte[] foundationAddress002 = PublicMethed.getFinalAddress(foundationKey002);
  String description =
      Configuration.getByPath("testng.conf").getString("defaultParameter.assetDescription");
  String url = Configuration.getByPath("testng.conf").getString("defaultParameter.assetUrl");

  ECKey ecKey001 = new ECKey(Utils.getRandom());
  byte[] testAddress001 = ecKey001.getAddress();
  String testKey001 = ByteArray.toHexString(ecKey001.getPrivKeyBytes());
  byte[] assetAccountId001;

  ECKey ecKey002 = new ECKey(Utils.getRandom());
  byte[] testAddress002 = ecKey002.getAddress();
  String testKey002 = ByteArray.toHexString(ecKey002.getPrivKeyBytes());
  byte[] assetAccountId002;

  private ManagedChannel channelFull = null;
  private WalletGrpc.WalletBlockingStub blockingStubFull = null;

  /** constructor. */
  @BeforeClass
  public void beforeClass() {
    channelFull =
        ManagedChannelBuilder.forTarget("grpc.nile.trongrid.io:50051").usePlaintext().build();
    blockingStubFull = WalletGrpc.newBlockingStub(channelFull);

    PublicMethed.printAddress(testKey001);
    PublicMethed.printAddress(testKey002);

    Assert.assertTrue(
        PublicMethed.sendcoin(
            testAddress001,
            10000_000000L,
            foundationAddress001,
            foundationKey001,
            blockingStubFull));

    Long start = System.currentTimeMillis() + 20000;
    Long end = start + 100000;
    Assert.assertTrue(
        PublicMethed.createAssetIssue(
            testAddress001,
            name,
            500000000L,
            1,
            1,
            start,
            end,
            1,
            description,
            url,
            10000L,
            10000L,
            1L,
            1L,
            testKey001,
            blockingStubFull));
  }

  @Test(enabled = false, description = "create sellOrder")
  void marketSellAssetTest001() {
    logger.info("1");
  }
}
