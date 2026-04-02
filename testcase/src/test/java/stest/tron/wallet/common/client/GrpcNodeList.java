package stest.tron.wallet.common.client;

import com.typesafe.config.Config;
import java.util.List;

/** Shared access to gRPC endpoint lists from {@code testng.conf}. */
public final class GrpcNodeList {

  private static final String CONF_PATH = "testng.conf";
  private static final String FULL_KEY = "fullnode.ip.list";
  private static final String SOLIDITY_KEY = "solidityNode.ip.list";

  private static volatile List<String> cachedFull;
  private static volatile List<String> cachedSolidity;

  private GrpcNodeList() {}

  public static String full(int index) {
    return fullList().get(index);
  }

  public static String solidity(int index) {
    return solidityList().get(index);
  }

  public static List<String> fullList() {
    List<String> current = cachedFull;
    if (current != null) {
      return current;
    }
    Config config = Configuration.getByPath(CONF_PATH);
    List<String> loaded = config.getStringList(FULL_KEY);
    cachedFull = loaded;
    return loaded;
  }

  public static List<String> solidityList() {
    List<String> current = cachedSolidity;
    if (current != null) {
      return current;
    }
    Config config = Configuration.getByPath(CONF_PATH);
    List<String> loaded = config.getStringList(SOLIDITY_KEY);
    cachedSolidity = loaded;
    return loaded;
  }
}

