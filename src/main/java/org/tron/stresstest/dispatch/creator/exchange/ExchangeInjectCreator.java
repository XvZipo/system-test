package org.tron.stresstest.dispatch.creator.exchange;

import lombok.Setter;
import org.tron.common.crypto.ECKey;
import org.tron.common.utils.ByteArray;
import org.tron.common.utils.Configuration;
import org.tron.core.Wallet;
import org.tron.protos.Contract;
import org.tron.protos.Protocol;
import org.tron.protos.Protocol.Transaction.Contract.ContractType;
import org.tron.stresstest.dispatch.AbstractTransactionCreator;
import org.tron.stresstest.dispatch.GoodCaseTransactonCreator;
import org.tron.stresstest.dispatch.TransactionFactory;
import org.tron.stresstest.dispatch.creator.CreatorCounter;

@Setter
public class ExchangeInjectCreator extends AbstractTransactionCreator implements GoodCaseTransactonCreator {
  private String firstTokenID = "_";
  private String ownerAddress = Configuration.getByPath("stress.conf").getString("address.exchangeOwnerAddress");
  private long exchangeID = commonexchangeid;
  private long quant = 1000000L;
  private String privateKey = Configuration.getByPath("stress.conf").getString("privateKey.exchangeOwnerKey");

  @Override
  protected Protocol.Transaction create() {
    byte[] tokenId = firstTokenID.getBytes();
    byte[] ownerAddressBytes = Wallet.decodeFromBase58Check(ownerAddress);

    TransactionFactory.context.getBean(CreatorCounter.class).put(this.getClass().getName());

    Contract.ExchangeInjectContract contract = createExchangeInjectContract(ownerAddressBytes,
            exchangeID, tokenId, quant);

    Protocol.Transaction transaction = createTransaction(contract, ContractType.ExchangeInjectContract);
    transaction = sign(transaction, ECKey.fromPrivate(ByteArray.fromHexString(privateKey)));
    return transaction;
  }
}
