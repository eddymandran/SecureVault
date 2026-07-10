package com.securevault.backend.identity.port.out;

public interface RefreshTokenGenerator {
    String generate();       // token brut, haute entropie, à renvoyer au client
    String hash(String rawToken);   // SHA-256, pour stockage
}
