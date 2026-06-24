package stest.tron.wallet.common.client.utils;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.google.protobuf.ByteString;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.SecureRandom;
import org.bouncycastle.crypto.params.MLDSAParameters;
import org.bouncycastle.crypto.params.MLDSAPrivateKeyParameters;
import org.bouncycastle.crypto.params.ParametersWithRandom;
import org.bouncycastle.crypto.signers.MLDSASigner;
import org.bouncycastle.pqc.crypto.falcon.FalconParameters;
import org.bouncycastle.pqc.crypto.falcon.FalconPrivateKeyParameters;
import org.bouncycastle.pqc.crypto.falcon.FalconSigner;
import org.tron.api.GrpcAPI;
import org.tron.api.WalletGrpc;
import org.tron.protos.Protocol;
import org.tron.protos.Protocol.PQAuthSig;
import org.tron.protos.Protocol.PQScheme;
import org.tron.protos.Protocol.Transaction;
import org.tron.protos.contract.BalanceContract.TransferContract;
import org.tron.protos.contract.WitnessContract.WitnessCreateContract;

/**
 * 抗量子（PQ）签名工具 —— 全新工具类，不改动任何老函数。
 *
 * <p>与老的 {@code TransactionUtils.sign}（ECDSA）平行：这里用 BouncyCastle 1.84 的
 * ML-DSA-44 / Falcon-512 对交易的 {@code SHA-256(raw_data)} 摘要签名，并把结果放入
 * {@code Transaction.pq_auth_sig}（而非 ECDSA {@code signature}）。镜像自 java-tron 产品的
 * crypto/pqc/MLDSA44.java 与 FNDSA512.java。
 *
 * <p>同包可直接用 {@link ByteArray}/{@link Base58}/{@link Hash}/{@link Sha256Hash}/
 * {@link CommonParameter}，无需额外 import。
 */
public final class PqSigner {

  // ---- 方案的 proto 枚举名（与 Protocol.PQScheme 一致）----
  public static final String SCHEME_FN_DSA_512 = "FN_DSA_512";
  public static final String SCHEME_ML_DSA_44 = "ML_DSA_44";

  // ---- 长度常量（镜像 MLDSA44 / FNDSA512）----
  private static final int MLDSA_PRIV = 2560;
  private static final int FALCON_FG = 384;
  private static final int FALCON_BIGF = 512;
  private static final int FALCON_PRIV_MIN = FALCON_FG + FALCON_FG + FALCON_BIGF; // 1280
  private static final int FALCON_SIG_MAX = 667;
  private static final int FALCON_RETRY = 16;

  private static final SecureRandom RNG = new SecureRandom();

  private PqSigner() {
  }

  /** Toolkit 生成的密钥文件内容（字段：scheme / privateKey / publicKey / address）。 */
  public static final class PqKey {
    public String scheme;
    public byte[] privateKey;
    public byte[] publicKey;
    public String address;
  }

  /** 从 Toolkit 的 JSON 密钥文件加载（privateKey/publicKey 为 hex）。 */
  public static PqKey loadKey(String jsonPath) {
    try {
      String text = new String(Files.readAllBytes(Paths.get(jsonPath)), StandardCharsets.UTF_8);
      JSONObject o = JSON.parseObject(text);
      PqKey k = new PqKey();
      k.scheme = o.getString("scheme");
      k.privateKey = ByteArray.fromHexString(o.getString("privateKey"));
      k.publicKey = ByteArray.fromHexString(o.getString("publicKey"));
      k.address = o.getString("address");
      return k;
    } catch (Exception e) {
      throw new IllegalStateException("load pq key file failed: " + jsonPath, e);
    }
  }

  // ===================== 低层签名（新函数）=====================

  /** ML-DSA-44 签名：priv 必须 2560 字节，返回固定 2420 字节签名。 */
  public static byte[] signMlDsa44(byte[] privateKey, byte[] message) {
    if (privateKey == null || privateKey.length != MLDSA_PRIV) {
      throw new IllegalArgumentException("ML-DSA private key length must be " + MLDSA_PRIV);
    }
    MLDSAPrivateKeyParameters sk =
        new MLDSAPrivateKeyParameters(MLDSAParameters.ml_dsa_44, privateKey);
    MLDSASigner signer = new MLDSASigner();
    signer.init(true, new ParametersWithRandom(sk, RNG));
    signer.update(message, 0, message.length);
    try {
      return signer.generateSignature();
    } catch (Exception e) {
      throw new IllegalStateException("ML-DSA signing failed", e);
    }
  }

  /**
   * Falcon-512 签名：取私钥前 1280 字节作为裸密钥 f‖g‖F（兼容 1280B 裸形式与 2176B 带公钥形式），
   * 重试至多 16 次以拒绝超出规范上界（&gt;667B）的签名。
   */
  public static byte[] signFalcon512(byte[] privateKey, byte[] message) {
    if (privateKey == null || privateKey.length < FALCON_PRIV_MIN) {
      throw new IllegalArgumentException("FN-DSA private key length must be >= " + FALCON_PRIV_MIN);
    }
    byte[] f = new byte[FALCON_FG];
    byte[] g = new byte[FALCON_FG];
    byte[] bigF = new byte[FALCON_BIGF];
    System.arraycopy(privateKey, 0, f, 0, FALCON_FG);
    System.arraycopy(privateKey, FALCON_FG, g, 0, FALCON_FG);
    System.arraycopy(privateKey, FALCON_FG + FALCON_FG, bigF, 0, FALCON_BIGF);
    FalconPrivateKeyParameters sk =
        new FalconPrivateKeyParameters(FalconParameters.falcon_512, f, g, bigF, new byte[0]);
    FalconSigner signer = new FalconSigner();
    signer.init(true, new ParametersWithRandom(sk, RNG));
    Exception lastFailure = null;
    for (int attempt = 0; attempt < FALCON_RETRY; attempt++) {
      try {
        byte[] sig = signer.generateSignature(message);
        if (sig.length <= FALCON_SIG_MAX) {
          return sig;
        }
      } catch (IllegalStateException e) {
        // BC 的 comp_encode 内部缓冲溢出 —— 等价于规范超长，换随机数重试
        lastFailure = e;
      } catch (Exception e) {
        throw new IllegalStateException("FN-DSA signing failed", e);
      }
    }
    throw new IllegalStateException(
        "FN-DSA signing failed: no signature <= " + FALCON_SIG_MAX + " bytes after "
            + FALCON_RETRY + " attempts", lastFailure);
  }

  /** 按 scheme 分派签名。message 即 32 字节 SHA-256(raw_data) 摘要。 */
  public static byte[] sign(PqKey key, byte[] message) {
    if (SCHEME_ML_DSA_44.equals(key.scheme)) {
      return signMlDsa44(key.privateKey, message);
    }
    if (SCHEME_FN_DSA_512.equals(key.scheme)) {
      return signFalcon512(key.privateKey, message);
    }
    throw new IllegalArgumentException("unknown PQ scheme: " + key.scheme);
  }

  // ===================== 高层（新函数，PQ 版 sign/sendcoin）=====================

  /** 交易摘要 = SHA-256(raw_data)（与节点验签、txid 口径一致）。 */
  public static byte[] digestOf(Transaction tx) {
    return Sha256Hash.hash(CommonParameter.getInstance().isECKeyCryptoEngine(),
        tx.getRawData().toByteArray());
  }

  /**
   * 给一笔未签名交易加 PQ 签名：算摘要 → 签名 → 写入 pq_auth_sig。
   * 不动 ECDSA signature 列表（保持为空）。与老的 TransactionUtils.sign 平行、互不影响。
   */
  public static Transaction signTransaction(Transaction unsigned, PqKey key) {
    byte[] digest = digestOf(unsigned);
    byte[] sig = sign(key, digest);
    PQAuthSig pq = PQAuthSig.newBuilder()
        .setScheme(schemeEnum(key.scheme))
        .setPublicKey(ByteString.copyFrom(key.publicKey))
        .setSignature(ByteString.copyFrom(sig))
        .build();
    return unsigned.toBuilder().addPqAuthSig(pq).build();
  }

  /** 构造并 PQ 签名一笔 TRX 转账（不广播），返回已签名交易。 */
  public static Transaction buildPqTransfer(byte[] owner, byte[] to, long amount, PqKey key,
      WalletGrpc.WalletBlockingStub stub) {
    TransferContract contract = TransferContract.newBuilder()
        .setOwnerAddress(ByteString.copyFrom(owner))
        .setToAddress(ByteString.copyFrom(to))
        .setAmount(amount)
        .build();
    Transaction unsigned = stub.createTransaction(contract);
    if (unsigned == null || unsigned.getRawData().getContractCount() == 0) {
      throw new IllegalStateException("createTransaction returned empty tx");
    }
    return signTransaction(unsigned, key);
  }

  /**
   * 构造并 PQ 签名一笔「创建见证人」交易（WitnessCreateContract，不广播），返回已签名交易。
   *
   * @param owner 见证人账户地址(21字节)。PQ 账户即其 PQ 公钥派生地址；该账户需先有足够余额
   *              (创建见证人需消耗较多 TRX，见链上 witnessCreateFee)。
   * @param url   见证人主页地址(任意字符串)。
   */
  public static Transaction buildPqCreateWitness(byte[] owner, String url, PqKey key,
      WalletGrpc.WalletBlockingStub stub) {
    WitnessCreateContract contract = WitnessCreateContract.newBuilder()
        .setOwnerAddress(ByteString.copyFrom(owner))
        .setUrl(ByteString.copyFrom(url.getBytes(StandardCharsets.UTF_8)))
        .build();
    Transaction unsigned = stub.createWitness(contract);
    if (unsigned == null || unsigned.getRawData().getContractCount() == 0) {
      throw new IllegalStateException("createWitness returned empty tx");
    }
    return signTransaction(unsigned, key);
  }

  /** 构造 + PQ 签名 + 广播一笔 TRX 转账，返回广播结果。 */
  public static GrpcAPI.Return sendPqTransfer(byte[] owner, byte[] to, long amount, PqKey key,
      WalletGrpc.WalletBlockingStub stub) {
    Transaction signed = buildPqTransfer(owner, to, amount, key, stub);
    return stub.broadcastTransaction(signed);
  }

  // ===================== 地址派生 =====================

  /** PQ 地址（21 字节，0x41 ‖ Keccak256(pub)[12..32]），用于 owner/to 字段。 */
  public static byte[] deriveAddressBytes(byte[] publicKey) {
    return Hash.sha3omit12(publicKey);
  }

  /** PQ 地址的 base58check（T 地址），用于与密钥文件 address 字段交叉校验。 */
  public static String deriveAddressBase58(byte[] publicKey) {
    return Base58.encode58Check(deriveAddressBytes(publicKey));
  }

  /** scheme 字符串 → Protocol.PQScheme。 */
  public static PQScheme schemeEnum(String scheme) {
    if (SCHEME_ML_DSA_44.equals(scheme)) {
      return Protocol.PQScheme.ML_DSA_44;
    }
    if (SCHEME_FN_DSA_512.equals(scheme)) {
      return Protocol.PQScheme.FN_DSA_512;
    }
    throw new IllegalArgumentException("unknown PQ scheme: " + scheme);
  }
}
