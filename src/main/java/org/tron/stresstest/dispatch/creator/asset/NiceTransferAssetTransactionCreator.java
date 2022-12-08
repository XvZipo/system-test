package org.tron.stresstest.dispatch.creator.asset;

import com.google.protobuf.ByteString;
import lombok.Setter;
import org.tron.common.crypto.ECKey;
import org.tron.common.utils.Configuration;
import org.tron.core.Wallet;
import org.tron.program.FullNode;
import org.tron.stresstest.dispatch.AbstractTransactionCreator;
import org.tron.stresstest.dispatch.GoodCaseTransactonCreator;
import org.tron.stresstest.dispatch.TransactionFactory;
import org.tron.stresstest.dispatch.creator.CreatorCounter;
import org.tron.common.utils.ByteArray;
import org.tron.protos.Contract;
import org.tron.protos.Protocol;
import org.tron.protos.Protocol.Transaction.Contract.ContractType;

@Setter
public class NiceTransferAssetTransactionCreator extends AbstractTransactionCreator implements GoodCaseTransactonCreator {

//  private String assetName = "1002136";
  private String assetName = commontokenid;
  private String ownerAddress = Configuration.getByPath("stress.conf").getString("address.assetIssueOwnerAddress");
  private String toAddress = commonToAddress;
  private long amount = 1L;
  private String privateKey = Configuration.getByPath("stress.conf").getString("privateKey.assetIssueOwnerKey");

  @Override
  protected Protocol.Transaction create() {

    String curAccount = FullNode.accountQueue.poll();

    TransactionFactory.context.getBean(CreatorCounter.class).put(this.getClass().getName());
    Contract.TransferAssetContract contract = Contract.TransferAssetContract.newBuilder()
            .setAssetName(ByteString.copyFrom(assetName.getBytes()))
            .setOwnerAddress(ByteString.copyFrom(Wallet.decodeFromBase58Check(ownerAddress)))
            .setToAddress(ByteString.copyFrom(Wallet.decodeFromBase58Check(curAccount)))
            .setAmount(amount)
            .build();
    FullNode.accountQueue.offer(curAccount);
    Protocol.Transaction transaction = createTransaction(contract, ContractType.TransferAssetContract);
    transaction = sign(transaction, ECKey.fromPrivate(ByteArray.fromHexString(privateKey)));
    return transaction;
  }
}
