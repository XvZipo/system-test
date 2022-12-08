package org.tron.stresstest.dispatch.creator.contract;

import com.google.protobuf.ByteString;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;
import lombok.Setter;
import org.spongycastle.util.encoders.Hex;
import org.tron.common.crypto.ECKey;
import org.tron.common.utils.ByteArray;
import org.tron.core.Wallet;
import org.tron.core.capsule.TransactionResultCapsule;
import org.tron.program.FullNode;
import org.tron.protos.Contract.TriggerSmartContract;
import org.tron.protos.Protocol;
import org.tron.protos.Protocol.Transaction.Contract.ContractType;
import org.tron.protos.Protocol.Transaction.Result.code;
import org.tron.stresstest.AbiUtil;
import org.tron.stresstest.dispatch.AbstractTransactionCreator;
import org.tron.stresstest.dispatch.GoodCaseTransactonCreator;
import org.tron.stresstest.dispatch.TransactionFactory;
import org.tron.stresstest.dispatch.creator.CreatorCounter;
import org.tron.stresstest.exception.EncodingException;

@Setter
public class TransferTrc20Creator extends AbstractTransactionCreator implements GoodCaseTransactonCreator {

  private String ownerAddress = triggerRandomTrc20Address;
  private String contractAddress = commonContractAddress4;
  private long callValue = 0L;
  private String methodSign = "transfer(address,uint256)";
  private boolean hex = false;
  private String param = "\"" + commonToAddress + "\",1";
  private long feeLimit = 1000000000L;
  private String privateKey = triggerRandomTrc20Key;
  private static AtomicInteger contractIndex = new AtomicInteger(0);
  private static String line;



  @Override
  protected Protocol.Transaction create() {
    byte[] ownerAddressBytes = Wallet.decodeFromBase58Check(ownerAddress);

    TransactionFactory.context.getBean(CreatorCounter.class).put(this.getClass().getName());

    TriggerSmartContract contract = null;

    contractAddress= FullNode.contractAddressList.get(contractIndex.getAndAdd(1) % 200);
    String curAccount = FullNode.accountQueue.poll();
    param = "\"" + curAccount + "\",1";
    FullNode.accountQueue.offer(curAccount);
    try {

      contract = triggerCallContract(
              ownerAddressBytes, Wallet.decodeFromBase58Check(contractAddress), callValue, Hex.decode(AbiUtil.parseMethod(methodSign, param, hex)));
    } catch (EncodingException e) {
      e.printStackTrace();
    }

    Protocol.Transaction transaction = createTransaction(contract, ContractType.TriggerSmartContract);

    transaction = transaction.toBuilder().setRawData(transaction.getRawData().toBuilder().setFeeLimit(feeLimit).setData(
        ByteString.copyFromUtf8(getRandomLengthStr(contractIndex.get() % 4))).build()).build();
    TransactionResultCapsule ret = new TransactionResultCapsule();

    ret.setStatus(0, code.SUCESS);
    transaction = transaction.toBuilder().addRet(ret.getInstance())
        .build();
    transaction = sign(transaction, ECKey.fromPrivate(ByteArray.fromHexString(privateKey)));
    return transaction;
  }

  public static String getRandomLengthStr(long n) {
    String result = "";
    for(int i = 1; i <= n;i++) {
      result = result + 'a';
    }

    return result;
  }
}
