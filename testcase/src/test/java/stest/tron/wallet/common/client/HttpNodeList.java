package stest.tron.wallet.common.client;

import com.typesafe.config.Config;
import java.util.List;

/**
 * Shared access to {@code httpnode.ip.list} from {@code testng.conf}.
 *
 * <p>This keeps base classes small and avoids repeating long config lookup expressions, while
 * preserving the existing values and indices used by tests.
 */
public final class HttpNodeList {

  private static final String CONF_PATH = "testng.conf";
  private static final String KEY = "httpnode.ip.list";

  private static volatile List<String> cached;

  private HttpNodeList() {}

  public static String get(int index) {
    return list().get(index);
  }

  public static List<String> list() {
    List<String> current = cached;
    if (current != null) {
      return current;
    }
    Config config = Configuration.getByPath(CONF_PATH);
    List<String> loaded = config.getStringList(KEY);
    cached = loaded;
    return loaded;
  }
}

