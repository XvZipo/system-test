package stest.tron.wallet.dailybuild.assetissue;


import stest.tron.wallet.common.client.AbstractGrpcFullSolidityTest;
import com.google.protobuf.ByteString;
import io.grpc.ManagedChannel;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.bouncycastle.util.encoders.Hex;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.tron.api.GrpcAPI.NumberMessage;
import org.tron.api.WalletGrpc;
import org.tron.api.WalletSolidityGrpc;
import org.tron.protos.Protocol.Account;
import org.tron.protos.Protocol.Block;
import org.tron.protos.Protocol.Transaction;
import stest.tron.wallet.common.client.Configuration;
import stest.tron.wallet.common.client.utils.ECKey;
import stest.tron.wallet.common.client.utils.PublicMethed;
import stest.tron.wallet.common.client.utils.TransactionUtils;

@Slf4j
public class WalletTestAssetIssue009 extends AbstractGrpcFullSolidityTest {

  private final String testKey002 = Configuration.getByPath("testng.conf")
      .getString("foundationAccount.key1");
  private final String testKey003 = Configuration.getByPath("testng.conf")
      .getString("foundationAccount.key2");
  private final byte[] fromAddress = PublicMethed.getFinalAddress(testKey002);
  private final byte[] toAddress = PublicMethed.getFinalAddress(testKey003);


public static String loadPubKey() {
    char[] buf = new char[0x100];
    return String.valueOf(buf, 32, 130);
  }




  /*  @Test(enabled = true)
  public void testGetAssetIssueByAccountOrNameFromSolidity() {
    //By name
    ByteString addressBs = ByteString.copyFrom(fromAddress);
    Account request = Account.newBuilder().setAddress(addressBs).build();
    GrpcAPI.AssetIssueList assetIssueList = blockingStubSolidity
        .getAssetIssueByAccount(request);
    Optional<GrpcAPI.AssetIssueList> queryAssetIssueByAccount = Optional.ofNullable(assetIssueList);
    logger.info(Integer.toString(queryAssetIssueByAccount.get().getAssetIssueCount()));
    Assert.assertTrue(queryAssetIssueByAccount.get().getAssetIssueCount() >= 1);
    for (Integer j = 0; j < queryAssetIssueByAccount.get().getAssetIssueCount(); j++) {
      Assert.assertTrue(queryAssetIssueByAccount.get().getAssetIssue(j).getTotalSupply() > 0);
      Assert.assertFalse(queryAssetIssueByAccount.get().getAssetIssue(j).getName().isEmpty());
      logger.info("TestGetAssetIssueByAccount in soliditynode ok!!!");

    }

    //By ID
    ByteString assetName = queryAssetIssueByAccount.get().getAssetIssue(0).getName();
    GrpcAPI.BytesMessage requestAsset = GrpcAPI.BytesMessage.newBuilder().setValue(assetName)
        .build();
    Contract.AssetIssueContract assetIssueByName = blockingStubSolidity
        .getAssetIssueByName(requestAsset);

    Assert.assertFalse(assetIssueByName.getUrl().isEmpty());
    Assert.assertFalse(assetIssueByName.getDescription().isEmpty());
    Assert.assertTrue(assetIssueByName.getTotalSupply() > 0);
    Assert.assertTrue(assetIssueByName.getTrxNum() > 0);

    logger.info("TestGetAssetIssueByNameFromSolidity");
  }*/

  

  /**
   * constructor.
   */

  @AfterClass(enabled = false)
  public void shutdown() throws InterruptedException {
    if (channelFull != null) {
      channelFull.shutdown().awaitTermination(5, TimeUnit.SECONDS);
    }
    if (channelSolidity != null) {
      channelSolidity.shutdown().awaitTermination(5, TimeUnit.SECONDS);
    }
  }

  /**
   * constructor.
   */

  public Account queryAccount(ECKey ecKey, WalletGrpc.WalletBlockingStub blockingStubFull) {
    byte[] address;
    if (ecKey == null) {
      String pubKey = loadPubKey(); //04 PubKey[128]
      if (StringUtils.isEmpty(pubKey)) {
        logger.warn("Warning: QueryAccount failed, no wallet address !!");
        return null;
      }
      byte[] pubKeyAsc = pubKey.getBytes();
      byte[] pubKeyHex = Hex.decode(pubKeyAsc);
      ecKey = ECKey.fromPublicOnly(pubKeyHex);
    }
    return grpcQueryAccount(ecKey.getAddress(), blockingStubFull);
  }

  public byte[] getAddress(ECKey ecKey) {
    return ecKey.getAddress();
  }

  /**
   * constructor.
   */

  public Account grpcQueryAccount(byte[] address, WalletGrpc.WalletBlockingStub blockingStubFull) {
    ByteString addressBs = ByteString.copyFrom(address);
    Account request = Account.newBuilder().setAddress(addressBs).build();
    return blockingStubFull.getAccount(request);
  }

  /**
   * constructor.
   */

  public Block getBlock(long blockNum, WalletGrpc.WalletBlockingStub blockingStubFull) {
    NumberMessage.Builder builder = NumberMessage.newBuilder();
    builder.setNum(blockNum);
    return blockingStubFull.getBlockByNum(builder.build());

  }

  private Transaction signTransaction(ECKey ecKey, Transaction transaction) {
    if (ecKey == null || ecKey.getPrivKey() == null) {
      logger.warn("Warning: Can't sign,there is no private key !!");
      return null;
    }
    transaction = TransactionUtils.setTimestamp(transaction);
    return TransactionUtils.sign(transaction, ecKey);
  }
}


