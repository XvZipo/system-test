package stest.tron.wallet.dailybuild.tvmnewcommand.istanbul;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import java.util.HashMap;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.tron.api.GrpcAPI.BlockExtention;
import org.tron.api.GrpcAPI.TransactionExtention;
import org.tron.api.WalletGrpc;
import stest.tron.wallet.common.client.Configuration;
import stest.tron.wallet.common.client.utils.Base58;
import stest.tron.wallet.common.client.utils.ByteArray;
import stest.tron.wallet.common.client.utils.ECKey;
import stest.tron.wallet.common.client.utils.PublicMethed;
import stest.tron.wallet.common.client.utils.Utils;
public class ChainidAndSelfBalance001 {
  private String testFoundationKey = Configuration.getByPath("testng.conf")
      .getString("foundationAccount.key2");
  private byte[] testFoundationAddress = PublicMethed.getFinalAddress(testFoundationKey);
  private Long maxFeeLimit = Configuration.getByPath("testng.conf")
      .getLong("defaultParameter.maxFeeLimit");
  private String fullnode = Configuration.getByPath("testng.conf").getStringList("fullnode.ip.list")
      .get(0);
  private ManagedChannel channelFull = null;
  private WalletGrpc.WalletBlockingStub blockingStubFull = null;

  ECKey ecKey1 = new ECKey(Utils.getRandom());
  byte[] testAddress001 = ecKey1.getAddress();
  String testKey001 = ByteArray.toHexString(ecKey1.getPrivKeyBytes());

  ECKey ecKey2 = new ECKey(Utils.getRandom());
  byte[] testAddress002 = ecKey2.getAddress();
  String testKey002 = ByteArray.toHexString(ecKey2.getPrivKeyBytes());
  private byte[] contractAddress;


  /**
   * constructor.
   */

  @BeforeClass(enabled = true)
  public void beforeClass() {
    PublicMethed.printAddress(testKey001);
    channelFull = ManagedChannelBuilder.forTarget(fullnode).usePlaintext(true).build();
    blockingStubFull = WalletGrpc.newBlockingStub(channelFull);

    PublicMethed
        .sendcoin(testAddress001, 1000_000_000L, testFoundationAddress, testFoundationKey,
        blockingStubFull);
    PublicMethed
        .sendcoin(testAddress002, 1_000_000L, testFoundationAddress, testFoundationKey,
            blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    String filePath = "src/test/resources/soliditycode/chainid001.sol";
    String contractName = "IstanbulTest";
    HashMap retMap = PublicMethed.getBycodeAbi(filePath, contractName);
    String code = retMap.get("byteCode").toString();
    String abi = retMap.get("abI").toString();
    contractAddress = PublicMethed
      .deployContract(contractName, abi, code, "", maxFeeLimit, 123456789L, 100, null, testKey001,
        testAddress001, blockingStubFull);
  }

  @Test(enabled = true, description = "chainId should be block zero`s Hash")
  public void chainidTest001() {
    String methodStr = "getId()";
    TransactionExtention returns = PublicMethed
        .triggerConstantContractForExtention(contractAddress, methodStr, "#",
        false, 0, maxFeeLimit, "0", 0,  testAddress001, testKey001, blockingStubFull);

    String chainIdHex = ByteArray.toHexString(returns.getConstantResult(0).toByteArray());

    BlockExtention blockZero = PublicMethed.getBlock2(0, blockingStubFull);
    String tem = ByteArray.toHexString(blockZero.getBlockid().toByteArray()).substring(56);
    String blockZeroId = "00000000000000000000000000000000000000000000000000000000" + tem;

    Assert.assertEquals(chainIdHex, blockZeroId);
  }

  /*
   * New command selfBalance for solidity compiler,
   * optimize address.balance when contract`s balance
   */

  @Test(enabled = true, description = "selfBalance of addres(this).balance")
  public void getBalanceTest001() {

    String methodStr = "getBalance()";
    String argsStr = "";
    TransactionExtention returns = PublicMethed
        .triggerConstantContractForExtention(contractAddress, methodStr, argsStr,
        false, 0, maxFeeLimit, "", 0, testAddress001, testKey001, blockingStubFull);
    Long getBalance = ByteArray.toLong(returns.getConstantResult(0).toByteArray());

    Long contractBalance = PublicMethed
        .queryAccount(contractAddress, blockingStubFull).getBalance();

    Assert.assertEquals(contractBalance, getBalance);

  }


  @Test(enabled = true, description = "selfBalance of contractAddress")
  public void getBalanceTest002() {

    String methodStr = "getBalance(address)";
    String argsStr = "\"" + Base58.encode58Check(contractAddress) + "\"";
    TransactionExtention returns = PublicMethed
        .triggerConstantContractForExtention(contractAddress, methodStr, argsStr,
        false, 0, maxFeeLimit, "", 0, testAddress001, testKey001, blockingStubFull);
    Long getBalance = ByteArray.toLong(returns.getConstantResult(0).toByteArray());

    Long contractBalance = PublicMethed
        .queryAccount(contractAddress, blockingStubFull).getBalance();

    Assert.assertEquals(contractBalance, getBalance);

  }

  @Test(enabled = true, description = "selfBalance of normal Address")
  public void getBalanceTest003() {
    String methodStr = "getBalance(address)";
    String argsStr = "\"" + Base58.encode58Check(testAddress002) + "\"";
    TransactionExtention returns = PublicMethed
        .triggerConstantContractForExtention(contractAddress, methodStr, argsStr,
        false, 0, maxFeeLimit, "", 0, testAddress001, testKey001, blockingStubFull);
    Long getBalance = ByteArray.toLong(returns.getConstantResult(0).toByteArray());

    Long accountBalance = PublicMethed
        .queryAccount(testAddress002, blockingStubFull).getBalance();

    Assert.assertEquals(accountBalance, getBalance);

  }

  @Test(enabled = true, description = "selfBalance of unActive Address")
  public void getBalanceTest004() {
    String methodStr = "getBalance(address)";

    byte[] unActiveAddress = new ECKey(Utils.getRandom()).getAddress();

    String argsStr = "\"" + Base58.encode58Check(unActiveAddress) + "\"";
    TransactionExtention returns = PublicMethed
        .triggerConstantContractForExtention(contractAddress, methodStr, argsStr,
        false, 0, maxFeeLimit, "", 0, testAddress001, testKey001, blockingStubFull);
    Long getBalance = ByteArray.toLong(returns.getConstantResult(0).toByteArray());

    Assert.assertEquals(0, getBalance.longValue());

  }



}
