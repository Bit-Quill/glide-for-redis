package javabushka.client;

public interface SyncClient extends Client {
  void set(String key, String value);

  String get(String key);
}
