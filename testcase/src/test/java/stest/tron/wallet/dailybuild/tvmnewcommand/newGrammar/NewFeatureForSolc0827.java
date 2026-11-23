package stest.tron.wallet.dailybuild.tvmnewcommand.newGrammar;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import lombok.extern.slf4j.Slf4j;
import org.junit.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.tron.api.GrpcAPI;
import org.tron.api.WalletGrpc;
import org.tron.protos.contract.SmartContractOuterClass;
import stest.tron.wallet.common.client.Configuration;
import stest.tron.wallet.common.client.utils.ByteArray;
import stest.tron.wallet.common.client.utils.ECKey;
import stest.tron.wallet.common.client.utils.PublicMethed;
import stest.tron.wallet.common.client.utils.Utils;

import java.util.HashMap;

@Slf4j
public class NewFeatureForSolc0827 {
    private final String testNetAccountKey = Configuration.getByPath("testng.conf")
        .getString("foundationAccount.key2");
    private final byte[] testNetAccountAddress = PublicMethed.getFinalAddress(testNetAccountKey);
    byte[] contract = null;
    ECKey ecKey = new ECKey(Utils.getRandom());
    byte[] contractExcAddress = ecKey.getAddress();
    String contractExcKey = ByteArray.toHexString(ecKey.getPrivKeyBytes());
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
            .sendcoin(contractExcAddress, 11011_000_000L,
                testNetAccountAddress, testNetAccountKey, blockingStubFull));
        PublicMethed.waitProduceNextBlock(blockingStubFull);
        String filePath = "src/test/resources/soliditycode/requireError.sol";
        String contractName = "C";
        HashMap retMap = PublicMethed.getBycodeAbi(filePath, contractName);
        String code = retMap.get("byteCode").toString();
        String abi = retMap.get("abI").toString();
        contract = PublicMethed.deployContract(contractName, abi, code, "", maxFeeLimit,
            0L, 100, null, contractExcKey,
            contractExcAddress, blockingStubFull);
        PublicMethed.waitProduceNextBlock(blockingStubFull);
        SmartContractOuterClass.SmartContract smartContract = PublicMethed.getContract(contract,
            blockingStubFull);
        Assert.assertNotNull(smartContract.getAbi());
    }

    @Test(enabled = true, description = "test require error like: require(false, AnError(1, \"two\", 3))")
    public void test01TestRequireError(){
        GrpcAPI.TransactionExtention transactionExtention = PublicMethed
            .triggerConstantContractForExtention(contract, "m()", "#", false,
                0, maxFeeLimit, "0", 0, contractExcAddress, contractExcKey, blockingStubFull);
        String resultHex = ByteArray.toHexString(transactionExtention.getConstantResult(0).toByteArray());
        Assert.assertEquals("f55fefe30000000000000000000000000000000000000000000000000000000000000001" +
                "0000000000000000000000000000000000000000000000000000000000000060" +
                "0000000000000000000000000000000000000000000000000000000000000003" +
                "0000000000000000000000000000000000000000000000000000000000000003" +
                "74776f0000000000000000000000000000000000000000000000000000000000", resultHex);

    }
    @Test(enabled = true, description = "test require error like: require(g(condition), CustomError(counter))")
    public void test02TestRequireError(){
        GrpcAPI.TransactionExtention transactionExtention = PublicMethed
                .triggerConstantContractForExtention(contract, "f(bool)", "false", false,
                        0, maxFeeLimit, "0", 0, contractExcAddress, contractExcKey, blockingStubFull);
        String resultHex = ByteArray.toHexString(transactionExtention.getConstantResult(0).toByteArray());
        Assert.assertEquals("110b36550000000000000000000000000000000000000000000000000000000000000001", resultHex);

    }

    @Test(enabled = true, description = "test require error like: require(condition, E1(a, b, c, this.e1))")
    public void test03TestRequireError(){
        //error E1(uint, uint, uint, function(uint256) external pure returns (uint256));
        GrpcAPI.TransactionExtention transactionExtention = PublicMethed
                .triggerConstantContractForExtention(contract,
                        "f1(bool,uint256,uint256,uint256)", "false,1,2,3", false,
                        0, maxFeeLimit, "0", 0, contractExcAddress, contractExcKey, blockingStubFull);
        String resultHex = ByteArray.toHexString(transactionExtention.getConstantResult(0).toByteArray());
        //e17a60d3: E1(uint256,uint256,uint256,function)
        //last 32 bytes: contract + kecca256: e1(uint256) + padding
        Assert.assertTrue(resultHex.contains("e17a60d30000000000000000000000000000000000000000000000000000000000000001" +
                "0000000000000000000000000000000000000000000000000000000000000002" +
                "0000000000000000000000000000000000000000000000000000000000000003"));
        Assert.assertTrue(resultHex.contains("3680e78b"));
    }

    @Test(enabled = true, description = "test require error like: require(false, CustomError1(reason))")
    public void test04TestRequireError(){
        GrpcAPI.TransactionExtention transactionExtention = PublicMethed
                .triggerConstantContractForExtention(contract, "f2()", "#", false,
                        0, maxFeeLimit, "0", 0, contractExcAddress, contractExcKey, blockingStubFull);
        String resultHex = ByteArray.toHexString(transactionExtention.getConstantResult(0).toByteArray());
        //eb36ccea: CustomError1(string)
        //0000000000000000000000000000000000000000000000000000000000000020:offset
        //000000000000000000000000000000000000000000000000000000000000000b:length
        //6572726f72526561736f6e000000000000000000000000000000000000000000:"errorReason"
        Assert.assertEquals("eb36ccea0000000000000000000000000000000000000000000000000000000000000020" +
                "000000000000000000000000000000000000000000000000000000000000000b" +
                "6572726f72526561736f6e000000000000000000000000000000000000000000", resultHex);
    }
}
