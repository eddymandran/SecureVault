package com.securevault.backend.identity.service;

import com.securevault.backend.identity.domain.model.Email;
import com.securevault.backend.identity.domain.model.RefreshToken;
import com.securevault.backend.identity.domain.model.User;
import com.securevault.backend.identity.port.in.AuthenticateUserUseCase;
import com.securevault.backend.identity.port.in.AuthenticationResult;
import com.securevault.backend.identity.port.in.LogoutUseCase;
import com.securevault.backend.identity.port.in.RefreshTokenUseCase;
import com.securevault.backend.identity.port.out.*;

import java.time.Clock;

public class AuthenticationService implements
        AuthenticateUserUseCase, RefreshTokenUseCase, LogoutUseCase {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordHasher passwordHasher;
    private final JwtSigner jwtSigner;
    private final RefreshTokenGenerator refreshTokenGenerator;
    private final Clock clock; // injecté, pas Instant.now() en dur → testabilité

    @Override
    public AuthenticationResult authenticate(String email, String rawPassword) {
        User user = userRepository.findByEmail(new Email(email))
                .orElseThrow(InvalidCredentialsException::new);

        if (!user.verifyPassword(rawPassword, passwordHasher)) {
            throw new InvalidCredentialsException();
        }

        return issueTokens(user.getId());
    }

    @Override
    public AuthenticationResult refresh(String rawRefreshToken) {
        String hash = refreshTokenGenerator.hash(rawRefreshToken);
        RefreshToken existing = refreshTokenRepository.findByTokenHash(hash)
                .orElseThrow(InvalidRefreshTokenException::new);

        if (!existing.isValid(clock)) {
            throw new InvalidRefreshTokenException();
        }

        // Rotation : l'ancien token est révoqué immédiatement
        refreshTokenRepository.revoke(existing.getId());

        return issueTokens(existing.getUserId());
    }

    @Override
    public void logout(String rawRefreshToken) {
        String hash = refreshTokenGenerator.hash(rawRefreshToken);
        refreshTokenRepository.findByTokenHash(hash)
                .ifPresent(t -> refreshTokenRepository.revoke(t.getId()));
    }

    private AuthenticationResult issueTokens(UserId userId) {
        String accessToken = jwtSigner.generateAccessToken(userId);
        String rawRefresh = refreshTokenGenerator.generate();

        RefreshToken token = new RefreshToken(
                RefreshTokenId.generate(),
                userId,
                refreshTokenGenerator.hash(rawRefresh),
                clock.instant().plus(30, ChronoUnit.DAYS),
                null
        );
        refreshTokenRepository.save(token);

        return new AuthenticationResult(accessToken, rawRefresh, /* expiry access */);
    }
}