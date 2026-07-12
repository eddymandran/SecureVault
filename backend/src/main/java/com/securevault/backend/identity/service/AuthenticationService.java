package com.securevault.backend.identity.service;

import com.securevault.backend.identity.domain.exception.AccountDisabledException;
import com.securevault.backend.identity.domain.exception.InvalidCredentialsException;
import com.securevault.backend.identity.domain.exception.InvalidRefreshTokenException;
import com.securevault.backend.identity.domain.model.Email;
import com.securevault.backend.identity.domain.model.RefreshToken;
import com.securevault.backend.identity.domain.model.RefreshTokenId;
import com.securevault.backend.identity.domain.model.User;
import com.securevault.backend.identity.port.in.AuthenticateUserUseCase;
import com.securevault.backend.identity.port.in.AuthenticationResult;
import com.securevault.backend.identity.port.in.LogoutUseCase;
import com.securevault.backend.identity.port.in.RefreshTokenUseCase;
import com.securevault.backend.identity.port.out.*;
import com.securevault.backend.shared.UserId;
import lombok.RequiredArgsConstructor;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

@RequiredArgsConstructor
public class AuthenticationService implements
        AuthenticateUserUseCase, RefreshTokenUseCase, LogoutUseCase {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordHasher passwordHasher;
    private final JwtSigner jwtSigner;
    private final RefreshTokenGenerator refreshTokenGenerator;
    private final Clock clock; // injecté, pas Instant.now() en dur → testabilité

    private static final Duration ACCESS_TOKEN_TTL = Duration.ofMinutes(10);
    private static final Duration REFRESH_TOKEN_TTL = Duration.ofDays(30);

    private static final String DUMMY_PASSWORD_HASH =
            "$2a$12$C6UzMDM.H6dfI/f/IKcEeO2koYbEaCz0/xJ.ATlwBRB3B6yPeaXBu";
    // Hash BCrypt factice — sert uniquement à faire consommer le même temps CPU
    // à passwordHasher.matches() quand l'utilisateur n'existe pas.
    // Ce n'est PAS un secret, juste un leurre pour égaliser le timing.

    @Override
    public AuthenticationResult authenticate(String email, String rawPassword) {
        User user = userRepository.findByEmail(new Email(email)).orElse(null);

        boolean passwordMatches;
        if (user != null) {
            passwordMatches = user.verifyPassword(rawPassword, passwordHasher);
        } else {
            passwordHasher.matches(rawPassword, DUMMY_PASSWORD_HASH); // résultat ignoré, coût CPU consommé
            passwordMatches = false;
        }

        if (user == null || !passwordMatches) {
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

        // Verification si l'utilisateur existe toujours
        User userConnected = userRepository.findById(existing.getUserId())
                .orElseThrow(InvalidRefreshTokenException::new);

        // Verification si l'utilisateur est actif
        if (!userConnected.isEnabled()) {
            throw new AccountDisabledException();
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
        Instant accessTokenExpiresAt = clock.instant().plus(ACCESS_TOKEN_TTL);

        RefreshToken token = new RefreshToken(
                RefreshTokenId.generate(),
                userId,
                refreshTokenGenerator.hash(rawRefresh),
                clock.instant().plus(REFRESH_TOKEN_TTL)
        );
        refreshTokenRepository.save(token);

        return new AuthenticationResult(accessToken, rawRefresh, accessTokenExpiresAt);
    }

}