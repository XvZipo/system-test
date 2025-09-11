package stest.tron.wallet.dailybuild.tvmnewcommand.contractselfdestruct;

import com.google.protobuf.ByteString;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import lombok.extern.slf4j.Slf4j;
import org.junit.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.tron.api.GrpcAPI.AccountResourceMessage;
import org.tron.api.GrpcAPI.TransactionExtention;
import org.tron.api.WalletGrpc;
import org.tron.protos.Protocol;
import org.tron.protos.Protocol.Account;
import org.tron.protos.Protocol.Transaction.Result.contractResult;
import org.tron.protos.Protocol.TransactionInfo;
import org.tron.protos.Protocol.TransactionInfo.code;
import org.tron.protos.contract.SmartContractOuterClass;
import stest.tron.wallet.common.client.Configuration;
import stest.tron.wallet.common.client.utils.*;

import java.util.HashMap;
import java.util.List;
import java.util.concurrent.TimeUnit;


@Slf4j
public class KillContract01 {

  private String testFoundationKey = Configuration.getByPath("testng.conf")
      .getString("foundationAccount.key2");
  private byte[] testFoundationAddress = PublicMethed.getFinalAddress(testFoundationKey);
  private Long maxFeeLimit = Configuration.getByPath("testng.conf")
      .getLong("defaultParameter.maxFeeLimit");
  private String fullnode = Configuration.getByPath("testng.conf").getStringList("fullnode.ip.list")
      .get(0);

  private ManagedChannel channelFull = null;
  private WalletGrpc.WalletBlockingStub blockingStubFull = null;

  ECKey ecKeyExc = new ECKey(Utils.getRandom());
  byte[] excAdd = ecKeyExc.getAddress();
  String excKey = ByteArray.toHexString(ecKeyExc.getPrivKeyBytes());

  ECKey targetEckey = new ECKey(Utils.getRandom());
  byte[] targetAdd = targetEckey.getAddress();
  String target58 = Base58.encode58Check(targetAdd);
  String targetKey = ByteArray.toHexString(targetEckey.getPrivKeyBytes());

  ECKey delegateReceiverEckey = new ECKey(Utils.getRandom());
  byte[] delegateReceiverAdd = delegateReceiverEckey.getAddress();
  String delegateReceiver58 = Base58.encode58Check(delegateReceiverAdd);
  String delegateReceiverEckeyKey = ByteArray.toHexString(delegateReceiverEckey.getPrivKeyBytes());

  private byte[] contractAddressD;
  private static final long now = System.currentTimeMillis();
  private static final long TotalSupply = 1000L;
  private static String tokenName = "testAssetIssue_" + Long.toString(now);
  private static ByteString assetAccountId = null;
  private String description = Configuration.getByPath("testng.conf")
          .getString("defaultParameter.assetDescription");
  private String url = Configuration.getByPath("testng.conf")
          .getString("defaultParameter.assetUrl");



  /**
   * constructor.
   */

  @BeforeClass(enabled = true)
  public void beforeClass() {
    PublicMethed.printAddress(excKey);
    PublicMethed.printAddress(targetKey);
    PublicMethed.printAddress(delegateReceiverEckeyKey);
    channelFull = ManagedChannelBuilder.forTarget(fullnode).usePlaintext().build();
    blockingStubFull = WalletGrpc.newBlockingStub(channelFull);

    Assert.assertTrue(PublicMethed.sendcoin(excAdd, 100000_000000L,
        testFoundationAddress, testFoundationKey, blockingStubFull));

    Assert.assertTrue(PublicMethed.sendcoin(delegateReceiverAdd, 1_000000L,
        testFoundationAddress, testFoundationKey, blockingStubFull));
    PublicMethed.waitProduceNextBlock(blockingStubFull);

    String filePath = "src/test/resources/soliditycode/killcontract01.sol";
    String contractName = "D";
    HashMap retMap = PublicMethed.getBycodeAbi(filePath, contractName);
    String code = retMap.get("byteCode").toString();
    String abi = retMap.get("abI").toString();
    contractAddressD = PublicMethed
        .deployContractFallback(contractName, abi, code, "", maxFeeLimit, 0L,
            100, null, excKey,
                excAdd, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    SmartContractOuterClass.SmartContract smartContract = PublicMethed.getContract(contractAddressD, blockingStubFull);
    Assert.assertFalse(smartContract.getAbi().toString().isEmpty());
    Assert.assertFalse(smartContract.getBytecode().toString().isEmpty());
    long start = System.currentTimeMillis() + 2000;
    long end = System.currentTimeMillis() + 1000000000;
    Assert.assertTrue(PublicMethed
            .createAssetIssue(excAdd, tokenName, TotalSupply, 1, 1000, start, end, 1,
                    description, url, 100000L, 100000L, 3L, 30L, excKey, blockingStubFull));
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    assetAccountId = PublicMethed.queryAccount(excAdd, blockingStubFull).getAssetIssuedID();
    logger.info("The token name: " + tokenName);
    logger.info("The token ID: " + assetAccountId.toStringUtf8());
  }


  @Test(enabled = true, description = "getPredictedAddress,sendcoin,create2,freezev2,delegateResource," +
          "delegateResource,unfreezeBalanceV2,cancelAllUnfreezeV2,vote,kill")
  void kill01() {
    String methedStr = "getPredictedAddress(bytes32)";
    String argsStr = "1122";
    TransactionExtention transactionExtention =
            PublicMethed.triggerConstantContractForExtention(contractAddressD, methedStr, argsStr,
                    false, 0, maxFeeLimit, "0", 0, excAdd, excKey, blockingStubFull);

    logger.info("getPredictedAddress transactionExtention: " + transactionExtention.toString());
    String create2Add41 = "41" + ByteArray.toHexString(transactionExtention.getConstantResult(0)
            .toByteArray()).substring(24);
    byte[] create2AddBytes = ByteArray.fromHexString(create2Add41);
    String create2Add58 = Base58.encode58Check(create2AddBytes);
    Assert.assertTrue(PublicMethed.sendcoin(create2AddBytes, 2001000000L,
            testFoundationAddress, testFoundationKey, blockingStubFull));
    Assert.assertTrue(PublicMethed.transferAsset(create2AddBytes,
            assetAccountId.toByteArray(), 100L, excAdd, excKey, blockingStubFull));
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    methedStr = "complexCreateKill(bytes32,address,address)";
    String args = argsStr +",\"" + target58 + "\",\"" + delegateReceiver58 + "\"";
    String txid  = PublicMethed.triggerContract(contractAddressD, methedStr, args,
            false, 0, maxFeeLimit, testFoundationAddress, testFoundationKey, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    TransactionInfo info = PublicMethed.getTransactionInfoById(txid,blockingStubFull).get();
    logger.info("kill01: " +info);
    Assert.assertEquals(code.SUCESS, info.getResult());
    SmartContractOuterClass.SmartContract smartContract = PublicMethed.getContract(create2AddBytes, blockingStubFull);
    Account create2Account = PublicMethed.queryAccount(create2AddBytes, blockingStubFull);
    logger.info("kill01 create2Account: " + create2Account.toString());
    logger.info("kill01 smartContract: " + smartContract.toString());
    Assert.assertEquals("", smartContract.toString());
    Assert.assertEquals("", create2Account.toString());
    Account targetAccount = PublicMethed.queryAccount(targetAdd,blockingStubFull);
    Account delegateReceiverAccount = PublicMethed.queryAccount(delegateReceiverAdd,blockingStubFull);
    Assert.assertEquals(1801000000L, targetAccount.getBalance());
    Assert.assertEquals(100000000L, targetAccount.getFrozenV2(0).getAmount());
    Assert.assertEquals(100000000L, targetAccount.getFrozenV2(1).getAmount());
    long targetAssetCount = PublicMethed.getAssetIssueValue(targetAdd,
            assetAccountId, blockingStubFull);
    Assert.assertEquals(100, targetAssetCount);
    Assert.assertEquals(1_000000L, delegateReceiverAccount.getBalance());
    long delegateReceiverAssetCount = PublicMethed.getAssetIssueValue(delegateReceiverAdd,
            assetAccountId, blockingStubFull);
    Assert.assertEquals(0, delegateReceiverAssetCount);
    long execAssetCount = PublicMethed.getAssetIssueValue(excAdd,
            assetAccountId, blockingStubFull);
    Assert.assertEquals(897, execAssetCount);
  }




  /**
   * constructor.
   */
  @AfterClass
  public void shutdown() throws InterruptedException {
    if (channelFull != null) {
      channelFull.shutdown().awaitTermination(5, TimeUnit.SECONDS);
    }
  }


}
