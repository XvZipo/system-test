package stest.tron.wallet.dailybuild.tvmnewcommand.newGrammar;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.junit.Assert;
import org.testng.annotations.AfterClass;
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

@Slf4j
public class NewFeatureForSolc0828 {

  private static final long SOLC_TIMEOUT_SECONDS = 90L;

  private final String solc = Configuration.getByPath("testng.conf")
      .getString("defaultParameter.solidityCompile");
  private final String testNetAccountKey = Configuration.getByPath("testng.conf")
      .getString("foundationAccount.key2");
  private final byte[] testNetAccountAddress = PublicMethed.getFinalAddress(testNetAccountKey);
  private final Long maxFeeLimit = Configuration.getByPath("testng.conf")
      .getLong("defaultParameter.maxFeeLimit");
  private final String fullnode = Configuration.getByPath("testng.conf")
      .getStringList("fullnode.ip.list").get(0);
  private final ECKey ecKey = new ECKey(Utils.getRandom());
  private final byte[] contractExcAddress = ecKey.getAddress();
  private final String contractExcKey = ByteArray.toHexString(ecKey.getPrivKeyBytes());

  private ManagedChannel channelFull = null;
  private WalletGrpc.WalletBlockingStub blockingStubFull = null;
  private byte[] chainProbeContract = null;

  private static final String CHAIN_PROBE_CONTRACT = "Solc0828DailyChainProbe";
  private static final String CHAIN_PROBE_FILE =
      "src/test/resources/soliditycode/Solc0828DailyChainProbe.sol";
  private static final long CHAIN_PROBE_DEPLOY_VALUE = 2_000_000L;

  @BeforeClass(enabled = true)
  public void beforeClass() {
    PublicMethed.printAddress(contractExcKey);
    channelFull = ManagedChannelBuilder.forTarget(fullnode)
        .usePlaintext()
        .build();
    blockingStubFull = WalletGrpc.newBlockingStub(channelFull);
    Assert.assertTrue(PublicMethed
        .sendcoin(contractExcAddress, 300_100_000_000L,
            testNetAccountAddress, testNetAccountKey, blockingStubFull));
    PublicMethed.waitProduceNextBlock(blockingStubFull);
  }

  @AfterClass(enabled = true)
  public void afterClass() throws InterruptedException {
    if (channelFull != null) {
      channelFull.shutdown().awaitTermination(5, TimeUnit.SECONDS);
    }
  }

  @Test(enabled = true, description = "TC-123 daily: getchainparameter compile and chain runtime")
  public void test01GetChainParameterNoIce() throws Exception {
    String source = ""
        + "// SPDX-License-Identifier: GPL-3.0\n"
        + "pragma solidity >=0.8.28;\n"
        + "contract GetChainParameterDaily {\n"
        + "  function f() public view returns (uint256) {\n"
        + "    return getchainparameter(0);\n"
        + "  }\n"
        + "  function boundary(uint64 i) public view returns (uint256, uint256, uint256) {\n"
        + "    return (getchainparameter(0), getchainparameter(1), getchainparameter(i));\n"
        + "  }\n"
        + "}\n";

    SolcResult legacy = compileSource("tc123_getchainparameter.sol", source,
        "--no-cbor-metadata", "--bin");
    assertSuccessNoIce(legacy);
    Assert.assertTrue(legacy.output.contains("Binary:"));
    Assert.assertTrue(legacy.output.contains("630100000b"));

    SolcResult viaIr = compileSource("tc123_getchainparameter.sol", source,
        "--experimental-via-ir", "--no-cbor-metadata", "--bin");
    assertSuccessNoIce(viaIr);
    Assert.assertTrue(viaIr.output.contains("Binary:"));
    Assert.assertTrue(viaIr.output.contains("630100000b"));

    ensureChainProbeContract();
    Assert.assertEquals("getchainparameter(0) must return zero on chain",
        BigInteger.ZERO, constantUint("f()", "#"));
    Assert.assertEquals("param(0) must return zero on chain",
        BigInteger.ZERO, constantUint("param(uint64)", "0"));
    Assert.assertTrue("param(1) must return a positive chain parameter",
        constantUint("param(uint64)", "1").compareTo(BigInteger.ZERO) > 0);
    Assert.assertTrue("param(5) must be decoded as a non-negative uint256",
        constantUint("param(uint64)", "5").compareTo(BigInteger.ZERO) >= 0);
    Assert.assertEquals("unknown parameter code must return zero",
        BigInteger.ZERO, constantUint("param(uint64)", "999999"));
  }

  @Test(enabled = false, description = "TC-121 daily: TRON SMT builtins warn but do not ICE")
  public void test02SmtTronBuiltinsWarnNoIce() throws Exception {
    String source = ""
        + "// SPDX-License-Identifier: GPL-3.0\n"
        + "pragma solidity >=0.8.0;\n"
        + "contract SMTTronBuiltinsDaily {\n"
        + "  function multi(address account, bytes32 content, bytes[] memory signatures)\n"
        + "      public pure returns (bool) {\n"
        + "    return validatemultisign(account, 0, content, signatures);\n"
        + "  }\n"
        + "  function zk(bytes32[9] memory output, bytes32[2] memory bindingSignature,\n"
        + "      uint64 value, bytes32 signHash, bytes32[33] memory frontier,\n"
        + "      uint256 leafCount) public pure returns (bytes32[] memory) {\n"
        + "    return verifyMintProof(output, bindingSignature, value, signHash, frontier, leafCount);\n"
        + "  }\n"
        + "  function pedersen(uint32 i, bytes32 left, bytes32 right) public pure returns (bytes32) {\n"
        + "    return pedersenHash(i, left, right);\n"
        + "  }\n"
        + "}\n";

    SolcResult result = compileSource("tc121_smt_tron_builtins.sol", source,
        "--model-checker-engine", "all", "--model-checker-show-unsupported");
    assertSuccessNoIce(result);
    Assert.assertTrue(result.output.contains("Assertion checker does not yet implement"));
    Assert.assertTrue(result.output.contains("validatemultisign"));
    Assert.assertTrue(result.output.contains("verifyMintProof"));
    Assert.assertTrue(result.output.contains("pedersenHash"));
  }

  @Test(enabled = true, description = "TC-124 daily: AST export/import round trip")
  public void test03AstExportImportRoundTrip() throws Exception {
    String source = ""
        + "// SPDX-License-Identifier: GPL-3.0\n"
        + "pragma solidity >=0.8.0;\n"
        + "contract DenominationDaily {\n"
        + "  uint256 constant A = 1 sun;\n"
        + "  uint256 constant B = 2E10 sun;\n"
        + "  uint256 constant C = uint256(4 trx) / 3 hours;\n"
        + "}\n";

    Path dir = Files.createTempDirectory("solc0828DailyAst");
    Path sourceFile = dir.resolve("tc124_denomination.sol");
    Path astFile = dir.resolve("tc124_denomination_ast.json");
    Files.write(sourceFile, source.getBytes(StandardCharsets.UTF_8));

    SolcResult exportAst = runSolc(null, "--combined-json", "ast", sourceFile.toString());
    assertSuccessNoIce(exportAst);
    Assert.assertTrue(exportAst.output.contains("\"subdenomination\":\"sun\""));
    Files.write(astFile, exportAst.output.getBytes(StandardCharsets.UTF_8));

    SolcResult importAst = runSolc(null, "--import-ast", "--bin", astFile.toString());
    assertSuccessNoIce(importAst);
    Assert.assertTrue(importAst.output.contains("Binary:"));
  }

  @Test(enabled = true, description = "TC-125 daily: modifier token diagnostics mention token fields")
  public void test04ModifierTokenDiagnosticMentionsTokenFields() throws Exception {
    String source = ""
        + "// SPDX-License-Identifier: GPL-3.0\n"
        + "pragma solidity >=0.8.0;\n"
        + "contract MsgTokenModifierDaily {\n"
        + "  modifier checkToken() {\n"
        + "    uint256 a = msg.tokenid;\n"
        + "    uint256 b = msg.tokenvalue;\n"
        + "    require(a + b >= 0);\n"
        + "    _;\n"
        + "  }\n"
        + "  function f() public checkToken returns (uint256) {\n"
        + "    return 1;\n"
        + "  }\n"
        + "}\n";

    SolcResult result = compileSource("tc125_modifier_tokenid.sol", source, "--error-codes");
    Assert.assertTrue("negative diagnostic case must fail", result.exitCode != 0);
    assertNoIce(result);
    Assert.assertTrue(result.output.contains("This modifier uses"));
    Assert.assertTrue(result.output.contains("msg.tokenid"));
    Assert.assertTrue(result.output.contains("msg.tokenvalue"));
  }

  @Test(enabled = true, description = "TC-117 daily: Standard JSON partial outputSelection")
  public void test05StandardJsonPartialOutputSelection() throws Exception {
    String source = ""
        + "// SPDX-License-Identifier: GPL-3.0\n"
        + "pragma solidity >=0.8.28;\n"
        + "type UserInt is uint256;\n"
        + "contract Requested {\n"
        + "  uint256 public stored;\n"
        + "  uint128 transient counter;\n"
        + "  bool transient flag;\n"
        + "  address transient owner;\n"
        + "  UserInt transient wrapped;\n"
        + "}\n"
        + "contract LayoutOnly {\n"
        + "  function f() public pure returns (uint256) { return 7; }\n"
        + "}\n";
    String standardJson = standardJson(source,
        "\"evmVersion\":\"cancun\","
            + "\"outputSelection\":{\"*\":{\"Requested\":[\"evm.bytecode.object\","
            + "\"transientStorageLayout\"],\"LayoutOnly\":[\"abi\"]}}");

    SolcResult result = runSolc(standardJson, "--standard-json");
    assertSuccessNoIce(result);
    Assert.assertTrue(result.output.contains("\"Requested\""));
    Assert.assertTrue(result.output.contains("\"transientStorageLayout\""));
    Assert.assertTrue(result.output.contains("\"counter\""));
    String layoutOnlyJson = jsonObjectForKey(result.output, "LayoutOnly");
    Assert.assertTrue(layoutOnlyJson.contains("\"abi\""));
    Assert.assertTrue(layoutOnlyJson.contains("\"name\":\"f\""));
    Assert.assertFalse(layoutOnlyJson.contains("\"evm\""));
    Assert.assertFalse(layoutOnlyJson.contains("\"transientStorageLayout\""));
    Assert.assertFalse(layoutOnlyJson.contains("\"storageLayout\""));
  }

  @Test(enabled = true, description = "TC-117 daily: Yul AST generated only on demand")
  public void test06YulAstGeneratedOnlyOnDemand() throws Exception {
    String source = ""
        + "// SPDX-License-Identifier: GPL-3.0\n"
        + "pragma solidity >=0.8.28;\n"
        + "contract YulAstDaily {\n"
        + "  function f(uint256 a) public pure returns (uint256) {\n"
        + "    uint256 marker = 42;\n"
        + "    return a + marker;\n"
        + "  }\n"
        + "}\n";

    SolcResult irOnly = runSolc(standardJson(source,
        "\"outputSelection\":{\"*\":{\"*\":[\"ir\"]}}"), "--standard-json");
    assertSuccessNoIce(irOnly);
    Assert.assertTrue(irOnly.output.contains("\"ir\""));
    Assert.assertFalse(irOnly.output.contains("\"irAst\""));
    Assert.assertFalse(irOnly.output.contains("\"irOptimizedAst\""));

    SolcResult irAst = runSolc(standardJson(source,
        "\"outputSelection\":{\"*\":{\"*\":[\"irAst\"]}}"), "--standard-json");
    assertSuccessNoIce(irAst);
    Assert.assertTrue(irAst.output.contains("\"irAst\""));
  }

  @Test(enabled = true, description = "TC-117 daily: SMTChecker address array assignment")
  public void test07SmtAddressArrayAssignmentNoLogicError() throws Exception {
    String source = ""
        + "// SPDX-License-Identifier: GPL-3.0\n"
        + "pragma solidity >=0.8.28;\n"
        + "contract SMTAddressArrayDaily {\n"
        + "  function f(address a, address b) public pure {\n"
        + "    address[] memory xs = new address[](2);\n"
        + "    xs[0] = a;\n"
        + "    xs[1] = b;\n"
        + "    address[] memory ys = xs;\n"
        + "    assert(ys[0] == a);\n"
        + "    assert(ys[1] == b);\n"
        + "  }\n"
        + "}\n";

    SolcResult result = compileSource("tc117_smt_address_array.sol", source,
        "--model-checker-engine", "all");
    assertSuccessNoIce(result);
    assertNoModelCheckerCounterexample(result);
  }

  @Test(enabled = true, description = "TC-122 daily: lightweight deterministic snapshot")
  public void test09SnapshotDeterministicForTronBuiltinFixture() throws Exception {
    String source = ""
        + "// SPDX-License-Identifier: GPL-3.0\n"
        + "pragma solidity >=0.8.28;\n"
        + "contract SnapshotDaily {\n"
        + "  function f(uint64 i) public view returns (uint256, uint256) {\n"
        + "    return (getchainparameter(0), getchainparameter(i));\n"
        + "  }\n"
        + "}\n";

    Path dir = Files.createTempDirectory("solc0828DailySnapshot");
    Path file = dir.resolve("tc122_snapshot.sol");
    Files.write(file, source.getBytes(StandardCharsets.UTF_8));

    SolcResult first = runSolc(null, "--no-cbor-metadata", "--bin", "--asm",
        file.toString());
    SolcResult second = runSolc(null, "--no-cbor-metadata", "--bin", "--asm",
        file.toString());
    assertSuccessNoIce(first);
    assertSuccessNoIce(second);
    Assert.assertEquals(first.output, second.output);
    Assert.assertTrue(first.output.contains("630100000b"));
  }

  private SolcResult compileSource(String fileName, String source, String... args)
      throws Exception {
    Path dir = Files.createTempDirectory("solc0828Daily");
    Path file = dir.resolve(fileName);
    Files.write(file, source.getBytes(StandardCharsets.UTF_8));
    List<String> command = new ArrayList<String>(Arrays.asList(args));
    command.add(file.toString());
    return runSolc(null, command.toArray(new String[0]));
  }

  private void ensureChainProbeContract() throws Exception {
    if (chainProbeContract != null) {
      return;
    }

    chainProbeContract = deployChainProbeContract();
  }

  private byte[] deployChainProbeContract() throws Exception {
    HashMap<String, String> retMap = PublicMethed.getBycodeAbiWithParam(CHAIN_PROBE_FILE,
        CHAIN_PROBE_CONTRACT, "--experimental-via-ir");
    String code = retMap.get("byteCode");
    String abi = retMap.get("abI");
    Assert.assertNotNull("chain probe bytecode must not be null", code);
    Assert.assertNotNull("chain probe abi must not be null", abi);

    byte[] contractAddress = PublicMethed.deployContract(CHAIN_PROBE_CONTRACT, abi,
        code, "", maxFeeLimit, CHAIN_PROBE_DEPLOY_VALUE, 100, null, contractExcKey,
        contractExcAddress, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    Assert.assertNotNull("chain probe contract address must not be null", contractAddress);
    SmartContractOuterClass.SmartContract smartContract = PublicMethed.getContract(
        contractAddress, blockingStubFull);
    Assert.assertNotNull("chain probe abi must not be null", smartContract.getAbi());
    return contractAddress;
  }

  private BigInteger constantUint(String method, String args) {
    GrpcAPI.TransactionExtention transactionExtention = PublicMethed
        .triggerConstantContractForExtention(chainProbeContract, method, args, false,
            0, maxFeeLimit, "0", 0, contractExcAddress, contractExcKey, blockingStubFull);
    Assert.assertTrue("constant call must succeed: " + method,
        transactionExtention.getResult().getResult());
    Assert.assertTrue("constant call must return one uint256: " + method,
        transactionExtention.getConstantResultCount() > 0);
    return uint256(ByteArray.toHexString(transactionExtention.getConstantResult(0).toByteArray()));
  }

  private BigInteger uint256(String hex) {
    if (hex == null || hex.isEmpty()) {
      return BigInteger.ZERO;
    }
    return new BigInteger(hex, 16);
  }

  private void assertNoModelCheckerCounterexample(SolcResult result) {
    String lower = result.output.toLowerCase();
    Assert.assertFalse(result.output.contains("SMT logic error"));
    Assert.assertFalse(lower.contains("assertion violation"));
    Assert.assertFalse(lower.contains("counterexample:"));
    Assert.assertFalse(lower.contains("might happen"));
  }

  private String jsonObjectForKey(String json, String key) {
    String quotedKey = "\"" + key + "\"";
    int keyIndex = json.indexOf(quotedKey);
    Assert.assertTrue("json key must exist: " + quotedKey, keyIndex >= 0);
    int colon = json.indexOf(':', keyIndex + quotedKey.length());
    int objectStart = firstNonWhitespace(json, colon + 1);
    Assert.assertEquals("json value must be object for key: " + key,
        '{', json.charAt(objectStart));
    int objectEnd = findMatchingJson(json, objectStart, '{', '}');
    return json.substring(objectStart, objectEnd + 1);
  }

  private int firstNonWhitespace(String value, int start) {
    for (int i = start; i < value.length(); i++) {
      if (!Character.isWhitespace(value.charAt(i))) {
        return i;
      }
    }
    Assert.fail("no non-whitespace character found");
    return -1;
  }

  private int findMatchingJson(String value, int start, char open, char close) {
    Assert.assertTrue("matching scan start must point to " + open,
        start >= 0 && value.charAt(start) == open);
    int depth = 0;
    boolean inString = false;
    boolean escaped = false;
    for (int i = start; i < value.length(); i++) {
      char current = value.charAt(i);
      if (inString) {
        if (escaped) {
          escaped = false;
        } else if (current == '\\') {
          escaped = true;
        } else if (current == '"') {
          inString = false;
        }
        continue;
      }

      if (current == '"') {
        inString = true;
      } else if (current == open) {
        depth++;
      } else if (current == close) {
        depth--;
        if (depth == 0) {
          return i;
        }
      }
    }
    Assert.fail("no matching " + close + " found");
    return -1;
  }

  private SolcResult runSolc(String stdin, String... args) throws Exception {
    List<String> command = new ArrayList<String>();
    command.add(solc);
    command.addAll(Arrays.asList(args));
    logger.info("Run solc command: " + command);

    ProcessBuilder builder = new ProcessBuilder(command);
    builder.redirectErrorStream(true);
    Process process = builder.start();
    if (stdin != null) {
      OutputStream outputStream = process.getOutputStream();
      outputStream.write(stdin.getBytes(StandardCharsets.UTF_8));
      outputStream.close();
    } else {
      process.getOutputStream().close();
    }

    String output = readAll(process);
    boolean finished = process.waitFor(SOLC_TIMEOUT_SECONDS, TimeUnit.SECONDS);
    if (!finished) {
      process.destroyForcibly();
      Assert.fail("solc command timed out: " + command);
    }
    logger.info("solc exitCode=" + process.exitValue() + ", output=" + output);
    return new SolcResult(process.exitValue(), output);
  }

  private String readAll(Process process) throws IOException {
    ByteArrayOutputStream buffer = new ByteArrayOutputStream();
    byte[] data = new byte[4096];
    int read;
    while ((read = process.getInputStream().read(data)) != -1) {
      buffer.write(data, 0, read);
    }
    return new String(buffer.toByteArray(), StandardCharsets.UTF_8);
  }

  private String standardJson(String source, String settingsBody) {
    return "{"
        + "\"language\":\"Solidity\","
        + "\"sources\":{\"Daily.sol\":{\"content\":\"" + jsonEscape(source) + "\"}},"
        + "\"settings\":{" + settingsBody + "}"
        + "}";
  }

  private String jsonEscape(String value) {
    return value.replace("\\", "\\\\")
        .replace("\"", "\\\"")
        .replace("\n", "\\n")
        .replace("\r", "\\r");
  }

  private void assertSuccessNoIce(SolcResult result) {
    Assert.assertEquals(result.output, 0, result.exitCode);
    assertNoIce(result);
  }

  private void assertNoIce(SolcResult result) {
    Assert.assertFalse(result.output.contains("Internal compiler error"));
    Assert.assertFalse(result.output.contains("InternalCompilerError"));
  }

  private static class SolcResult {
    private final int exitCode;
    private final String output;

    private SolcResult(int exitCode, String output) {
      this.exitCode = exitCode;
      this.output = output;
    }
  }

}
