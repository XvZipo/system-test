package stest.tron.wallet.dailybuild.manual;

import com.google.protobuf.ByteString;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.StatusRuntimeException;

import java.util.HashMap;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.tron.api.GrpcAPI;
import org.tron.api.WalletGrpc;
import org.tron.api.WalletSolidityGrpc;
import org.tron.protos.Protocol;
import stest.tron.wallet.common.client.Configuration;
import stest.tron.wallet.common.client.utils.*;

@Slf4j
public class WalletTestAccount015 {

  private static final long now = System.currentTimeMillis();
  private static long amount = 100000000L;
  private static String accountId = "accountid_" + Long.toString(now);
  private final String testKey002 = Configuration.getByPath("testng.conf")
      .getString("foundationAccount.key2");
  private final byte[] fromAddress = PublicMethed.getFinalAddress(testKey002);
  ECKey ecKey1 = new ECKey(Utils.getRandom());
  byte[] account015Address = ecKey1.getAddress();
  String account015Key = ByteArray.toHexString(ecKey1.getPrivKeyBytes());
  private ManagedChannel channelFull = null;
  private ManagedChannel channelSolidity = null;
  private ManagedChannel channelSoliInFull = null;
  private ManagedChannel channelPbft = null;
  private WalletGrpc.WalletBlockingStub blockingStubFull = null;
  private WalletSolidityGrpc.WalletSolidityBlockingStub blockingStubSolidity = null;
  private WalletSolidityGrpc.WalletSolidityBlockingStub blockingStubSoliInFull = null;
  private WalletSolidityGrpc.WalletSolidityBlockingStub blockingStubPbft = null;
  private Long maxFeeLimit = Configuration.getByPath("testng.conf")
      .getLong("defaultParameter.maxFeeLimit");
  private String fullnode = Configuration.getByPath("testng.conf").getStringList("fullnode.ip.list")
      .get(0);
  private String soliditynode = Configuration.getByPath("testng.conf")
      .getStringList("solidityNode.ip.list").get(0);
  private String soliInFullnode = Configuration.getByPath("testng.conf")
      .getStringList("solidityNode.ip.list").get(1);
  private String soliInPbft = Configuration.getByPath("testng.conf")
      .getStringList("solidityNode.ip.list").get(2);

  

  /**
   * constructor.
   */
  @BeforeClass(enabled = true)
  public void beforeClass() {
    PublicMethed.printAddress(testKey002);
    channelFull = ManagedChannelBuilder.forTarget(fullnode)
        .usePlaintext()
        .build();
    blockingStubFull = WalletGrpc.newBlockingStub(channelFull);

    channelSolidity = ManagedChannelBuilder.forTarget(soliditynode)
        .usePlaintext()
        .build();
    blockingStubSolidity = WalletSolidityGrpc.newBlockingStub(channelSolidity);

    channelSoliInFull = ManagedChannelBuilder.forTarget(soliInFullnode)
        .usePlaintext()
        .build();
    blockingStubSoliInFull = WalletSolidityGrpc.newBlockingStub(channelSoliInFull);

    channelPbft = ManagedChannelBuilder.forTarget(soliInPbft)
        .usePlaintext()
        .build();
    blockingStubPbft = WalletSolidityGrpc.newBlockingStub(channelPbft);

    Random rand = new Random();
    amount = amount + rand.nextInt(10000);
  }

  @Test(enabled = true, description = "Set account id")
  public void test01SetAccountId() {
    //Create account014
    ecKey1 = new ECKey(Utils.getRandom());
    account015Address = ecKey1.getAddress();
    account015Key = ByteArray.toHexString(ecKey1.getPrivKeyBytes());

    PublicMethed.printAddress(account015Key);
    Assert.assertTrue(PublicMethed.sendcoin(account015Address, amount, fromAddress,
        testKey002, blockingStubFull));
    PublicMethed.waitProduceNextBlock(blockingStubFull);

    Assert.assertTrue(PublicMethed.setAccountId(accountId.getBytes(),
        account015Address, account015Key, blockingStubFull));
    PublicMethed.waitSolidityNodeSynFullNodeData(blockingStubFull, blockingStubSoliInFull);
    PublicMethed.waitSolidityNodeSynFullNodeData(blockingStubFull, blockingStubSoliInFull);
  }

  @Test(enabled = true, description = "Get account by id")
  public void test02GetAccountById() {
    Assert.assertEquals(amount, PublicMethed.getAccountById(
        accountId, blockingStubFull).getBalance());
  }


  @Test(enabled = true, description = "Get account by id from solidity")
  public void test03GetAccountByIdFromSolidity() {
    Assert.assertEquals(amount, PublicMethed.getAccountByIdFromSolidity(
        accountId, blockingStubSoliInFull).getBalance());
  }

  @Test(enabled = true, description = "Get account by id from PBFT")
  public void test04GetAccountByIdFromPbft() {
    Assert.assertEquals(amount, PublicMethed.getAccountByIdFromSolidity(
        accountId, blockingStubPbft).getBalance());
  }


  @Test(enabled = true, description = "Get account from PBFT")
  public void test05GetAccountFromPbft() {
    Assert.assertEquals(amount, PublicMethed.queryAccount(
        account015Address, blockingStubPbft).getBalance());
  }


  @Test(enabled = true, description = "List witnesses")
  public void test06ListWitness() {
    Assert.assertTrue(PublicMethed.listWitnesses(blockingStubFull)
        .get().getWitnessesCount() >= 2);
  }

  @Test(enabled = true, description = "List witnesses from solidity node")
  public void test07ListWitnessFromSolidity() {
    Assert.assertTrue(PublicMethed.listWitnessesFromSolidity(blockingStubSolidity)
        .get().getWitnessesCount() >= 2);
    Assert.assertTrue(PublicMethed.listWitnessesFromSolidity(blockingStubSoliInFull)
        .get().getWitnessesCount() >= 2);
  }

  @Test(enabled = true, description = "List witnesses from PBFT node")
  public void test08ListWitnessFromPbft() {
    Assert.assertTrue(PublicMethed.listWitnessesFromSolidity(blockingStubPbft)
        .get().getWitnessesCount() >= 2);
  }


  @Test(enabled = true, retryAnalyzer = Retry.class, description = "List witness realTime vote data")
  public void test09CheckVoteChangesRealtimeAfterVote(){
    GrpcAPI.WitnessList witnessList = getPaginatedNowWitnessListAfterMaintenance();
    GrpcAPI.WitnessList witnessListSolidity = PublicMethed.getPaginatedNowWitnessListSolidity(0L,100L, blockingStubSolidity);
    Assert.assertTrue(witnessListSolidity.getWitnessesList().size() > 1);

    ECKey voter = new ECKey(Utils.getRandom());
    byte[] voterAddress = voter.getAddress();
    Long freezeBalance = 30500_000000L;
    PublicMethed.sendcoin(voterAddress, freezeBalance, fromAddress , testKey002, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    PublicMethed.freezeBalanceV2(voterAddress, freezeBalance, 0, ByteArray.toHexString(voter.getPrivateKey()), blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    HashMap<byte[], Long> voteMap = new HashMap<>();
    Long voteCount = 10000L;
    String votedWitness = "TT1smsmhxype64boboU8xTuNZVCKP1w6qT";
    for(int i = 0; i< witnessList.getWitnessesCount(); i++){
      Protocol.Witness witness = witnessList.getWitnesses(i);
      String witnessTAddress = Base58.encode58Check(witness.getAddress().toByteArray());
      if(witnessTAddress.equals(votedWitness)){
        voteMap.put(witness.getAddress().toByteArray(), voteCount);
      }
    }
    PublicMethed.voteWitness(voterAddress, ByteArray.toHexString(voter.getPrivateKey()),voteMap,blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    GrpcAPI.WitnessList witnessListAfterVote = getPaginatedNowWitnessListAfterMaintenance();
    boolean checkedVote = false;
    for(int i = 0; i< witnessList.getWitnessesCount(); i++){
      Protocol.Witness witness = witnessList.getWitnesses(i);
      String witnessTAddress = Base58.encode58Check(witness.getAddress().toByteArray());
      if(witnessTAddress.equals(votedWitness)){
        for(int j=0; j<witnessListAfterVote.getWitnessesCount();j++){
          Protocol.Witness witnessAfterVote = witnessListAfterVote.getWitnesses(j);
          String witnessAfterAddress = Base58.encode58Check(
              witnessAfterVote.getAddress().toByteArray());
          if(witnessAfterAddress.equals(votedWitness)){
            long voteDiff = witnessAfterVote.getVoteCount() - witness.getVoteCount();
            logger.info("voteDiff: " + voteDiff);
            Assert.assertTrue(voteDiff > 9500L);
            Assert.assertTrue(voteDiff < 10500L);
            checkedVote = true;
          }
        }
      }
    }
    Assert.assertTrue(checkedVote);
  }

  /**
   * Fullnode getPaginatedNowWitnessList rejects the chain head during maintenance.
   * This network's maintenance interval is 20 seconds, so wait across one interval.
   */
  private GrpcAPI.WitnessList getPaginatedNowWitnessListAfterMaintenance() {
    StatusRuntimeException last = null;
    for (int attempt = 1; attempt <= 12; attempt++) {
      try {
        return PublicMethed.getPaginatedNowWitnessList(0L, 100L, blockingStubFull);
      } catch (StatusRuntimeException e) {
        String message = e.getStatus().getDescription();
        if (message == null) {
          message = e.getMessage();
        }
        if (message == null || !message.contains(
            "Service temporarily unavailable during maintenance period")) {
          throw e;
        }
        last = e;
        logger.info("getPaginatedNowWitnessList during maintenance, retry " + attempt);
        try {
          Thread.sleep(3000L);
        } catch (InterruptedException interrupted) {
          Thread.currentThread().interrupt();
          throw e;
        }
      }
    }
    throw last;
  }


  /**
   * constructor.
   */
  @AfterClass
  public void shutdown() throws InterruptedException {
    PublicMethed.freedResource(account015Address, account015Key, fromAddress, blockingStubFull);
    if (channelFull != null) {
      channelFull.shutdown().awaitTermination(5, TimeUnit.SECONDS);
    }
    if (channelSolidity != null) {
      channelSolidity.shutdown().awaitTermination(5, TimeUnit.SECONDS);
    }
    if (channelPbft != null) {
      channelPbft.shutdown().awaitTermination(5, TimeUnit.SECONDS);
    }
    if (channelSoliInFull != null) {
      channelSoliInFull.shutdown().awaitTermination(5, TimeUnit.SECONDS);
    }
  }
}