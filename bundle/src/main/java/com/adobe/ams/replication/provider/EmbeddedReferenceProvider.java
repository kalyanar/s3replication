package com.adobe.ams.replication.provider;

import java.util.Set;

public interface EmbeddedReferenceProvider {
  Set<String> provideReferences(String path,byte[] response);
  String getProviderName();
}
