package org.molgenis.core.util;

import com.google.common.hash.HashCode;
import com.google.common.hash.Hashing;
import java.io.IOException;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.molgenis.util.ResourceUtils;

public class ResourceFingerprintRegistry {
  private final Map<String, String> resourceFingerprints;

  public ResourceFingerprintRegistry() {
    resourceFingerprints = new ConcurrentHashMap<>();
  }

  // Used in freemarker macros (see org.molgenis.core.ui.MolgenisInterceptor)
  @SuppressWarnings("unused")
  public String getFingerprint(String resourceName) throws IOException {
    return getFingerprint(this.getClass(), resourceName);
  }

  public String getFingerprint(Class<?> contextClass, String resourceName) throws IOException {
    String resourceFingerprint = resourceFingerprints.get(resourceName);
    if (resourceFingerprint == null) {
      resourceFingerprint = createFingerprint(contextClass, resourceName);
      resourceFingerprints.put(resourceName, resourceFingerprint);
    }
    return resourceFingerprint;
  }

  private String createFingerprint(Class<?> contextClass, String resourceName) throws IOException {
    byte[] bytes =
        contextClass != null
            ? ResourceUtils.getBytes(contextClass, resourceName)
            : ResourceUtils.getBytes(resourceName);
    HashCode crc32 = Hashing.crc32().hashBytes(bytes);
    return Base64.getUrlEncoder().withoutPadding().encodeToString(crc32.asBytes());
  }
}
