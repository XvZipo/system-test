package stest.tron.wallet.dailybuild.tvmnewcommand.newGrammar;

import com.google.protobuf.ByteString;
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
public class NewFeatureForSolc0826 {
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
            .sendcoin(contractExcAddress, 11010_000_000L,
                testNetAccountAddress, testNetAccountKey, blockingStubFull));
        PublicMethed.waitProduceNextBlock(blockingStubFull);
        String filePath = "src/test/resources/soliditycode/newFeature0826.sol";
        String contractName = "RequireErrorTest";
        HashMap retMap = PublicMethed.getBycodeAbiWithParam(filePath, contractName,"--experimental-via-ir");
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

    @Test(enabled = true, description = "test new feature for 0.8.26")
    public void test01TestRequireError(){
        GrpcAPI.TransactionExtention transactionExtention = PublicMethed
            .triggerConstantContractForExtention(contract, "double(uint256)", "0", false,
                0, maxFeeLimit, "0", 0, contractExcAddress, contractExcKey, blockingStubFull);
        String resultHex = ByteArray.toHexString(transactionExtention.getConstantResult(0).toByteArray());
        Assert.assertEquals(resultHex, "b5e403ec" +
            "00000000000000000000000000000000000000000000000000000000000003e9" +
            "0000000000000000000000000000000000000000000000000000000000000060" +
            "0000000000000000000000000000000000000000000000000000000000000001" +
            "0000000000000000000000000000000000000000000000000000000000000012" +
            "78206d75737420626520706f7369746976650000000000000000000000000000");
    }
}
