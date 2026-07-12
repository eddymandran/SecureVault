package com.securevault.backend.identity.service;

import com.securevault.backend.identity.domain.exception.AccountDisabledException;
import com.securevault.backend.identity.domain.exception.InvalidCredentialsException;
import com.securevault.backend.identity.domain.exception.InvalidRefreshTokenException;
import com.securevault.backend.identity.domain.model.*;
import com.securevault.backend.identity.port.in.AuthenticationResult;
import com.securevault.backend.identity.port.out.*;
import com.securevault.backend.shared.UserId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthenticationService — tests unitaires")
class AuthenticationServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private RefreshTokenRepository refreshTokenRepository;
    @Mock private PasswordHasher passwordHasher;
    @Mock private JwtSigner jwtSigner;
    @Mock private RefreshTokenGenerator refreshTokenGenerator;

    private AuthenticationService authenticationService;

    // Horloge figée — indispensable pour tester expiration/rotation de façon déterministe
    private final Instant now = Instant.parse("2026-07-12T10:00:00Z");
    private final Clock clock = Clock.fixed(now, ZoneOffset.UTC);

    private static final String RAW_PASSWORD = "SuperSecret123!";
    private static final String STORED_HASH = "$2a$12$storedHashValue";

    @BeforeEach
    void setUp() {
        authenticationService = new AuthenticationService(
                userRepository, refreshTokenRepository, passwordHasher,
                jwtSigner, refreshTokenGenerator, clock
        );
    }

    private User buildUser(boolean enabled) {
        User user = new User(
                UserId.generate(),
                new Email("alice@securevault.com"),
                new HashedPassword(STORED_HASH),
                now.minus(30, ChronoUnit.DAYS)
        );
        if (!enabled) user.disable();
        return user;
    }

    // ---------- authenticate() ----------

    @Test
    @DisplayName("authenticate — doit émettre access token et refresh token quand les identifiants sont valides")
    void should_issue_tokens_on_valid_credentials() {
        // ARRANGE
        User user = buildUser(true);
        when(userRepository.findByEmail(new Email("alice@securevault.com")))
                .thenReturn(Optional.of(user));
        when(passwordHasher.matches(RAW_PASSWORD, STORED_HASH)).thenReturn(true);
        when(jwtSigner.generateAccessToken(user.getId())).thenReturn("access-token-jwt");
        when(refreshTokenGenerator.generate()).thenReturn("raw-refresh-token");
        when(refreshTokenGenerator.hash("raw-refresh-token")).thenReturn("hashed-refresh-token");

        // ACT
        AuthenticationResult result = authenticationService.authenticate("alice@securevault.com", RAW_PASSWORD);

        // ASSERT
        assertThat(result.accessToken()).isEqualTo("access-token-jwt");
        assertThat(result.refreshToken()).isEqualTo("raw-refresh-token");
        assertThat(result.accessTokenExpiresAt()).isEqualTo(now.plus(10, ChronoUnit.MINUTES));

        ArgumentCaptor<RefreshToken> captor = ArgumentCaptor.forClass(RefreshToken.class);
        verify(refreshTokenRepository).save(captor.capture());
        RefreshToken saved = captor.getValue();
        assertThat(saved.getUserId()).isEqualTo(user.getId());
        assertThat(saved.getTokenHash()).isEqualTo("hashed-refresh-token");
        assertThat(saved.isRevoked()).isFalse();
    }

    @Test
    @DisplayName("authenticate — doit rejeter un email inexistant sans révéler l'absence du compte (protection timing)")
    void should_reject_unknown_email_with_generic_error() {
        // ARRANGE
        when(userRepository.findByEmail(any())).thenReturn(Optional.empty());

        // ACT & ASSERT
        assertThatThrownBy(() -> authenticationService.authenticate("ghost@securevault.com", RAW_PASSWORD))
                .isInstanceOf(InvalidCredentialsException.class);

        // Le hash factice DOIT être appelé pour égaliser le coût CPU avec le cas "utilisateur existant."
        verify(passwordHasher).matches(eq(RAW_PASSWORD), anyDummyHash());
        // Aucun accès à un vrai hash utilisateur, puisqu'aucun utilisateur n'a été trouvé
        verify(userRepository, never()).findById(any());
    }

    @Test
    @DisplayName("authenticate — doit rejeter un mauvais mot de passe")
    void should_reject_wrong_password() {
        // ARRANGE
        User user = buildUser(true);
        when(userRepository.findByEmail(any())).thenReturn(Optional.of(user));
        when(passwordHasher.matches(RAW_PASSWORD, STORED_HASH)).thenReturn(false);

        // ACT & ASSERT
        assertThatThrownBy(() -> authenticationService.authenticate("alice@securevault.com", RAW_PASSWORD))
                .isInstanceOf(InvalidCredentialsException.class);

        verify(refreshTokenRepository, never()).save(any());
    }

    @Test
    @DisplayName("authenticate — un compte désactivé doit échouer comme un mauvais mot de passe (pas de disclosure)")
    void should_reject_disabled_account_with_generic_error_not_account_disabled() {
        // ARRANGE
        // Comportement INTENTIONNEL : au login, on ne révèle jamais qu'un compte
        // existe-mais-est-désactivé à quelqu'un de non authentifié. Contraste volontaire
        // avec refresh(), où l'appelant possède déjà une preuve d'authentification.
        User disabledUser = buildUser(false);
        when(userRepository.findByEmail(any())).thenReturn(Optional.of(disabledUser));

        // ACT & ASSERT
        assertThatThrownBy(() -> authenticationService.authenticate("alice@securevault.com", RAW_PASSWORD))
                .isInstanceOf(InvalidCredentialsException.class)
                .isNotInstanceOf(AccountDisabledException.class);
    }

    // ---------- refresh() ----------

    @Test
    @DisplayName("refresh — doit faire tourner le token : révoquer l'ancien, en émettre un nouveau")
    void should_rotate_refresh_token_on_valid_refresh() {
        // ARRANGE
        UserId userId = UserId.generate();
        RefreshTokenId oldTokenId = RefreshTokenId.generate();
        RefreshToken existing = new RefreshToken(oldTokenId, userId, "hashed-old-token", now.plus(1, ChronoUnit.DAYS));
        User user = new User(userId, new Email("alice@securevault.com"), new HashedPassword(STORED_HASH), now.minus(30, ChronoUnit.DAYS));

        when(refreshTokenGenerator.hash("raw-old-token")).thenReturn("hashed-old-token");
        when(refreshTokenRepository.findByTokenHash("hashed-old-token")).thenReturn(Optional.of(existing));
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(jwtSigner.generateAccessToken(userId)).thenReturn("new-access-token");
        when(refreshTokenGenerator.generate()).thenReturn("raw-new-token");
        when(refreshTokenGenerator.hash("raw-new-token")).thenReturn("hashed-new-token");

        // ACT
        AuthenticationResult result = authenticationService.refresh("raw-old-token");

        // ASSERT
        InOrder order = inOrder(refreshTokenRepository);
        order.verify(refreshTokenRepository).revoke(oldTokenId);
        order.verify(refreshTokenRepository).save(any(RefreshToken.class));

        assertThat(result.accessToken()).isEqualTo("new-access-token");
        assertThat(result.refreshToken()).isEqualTo("raw-new-token");
    }

    @Test
    @DisplayName("refresh — doit rejeter un token inconnu")
    void should_reject_unknown_refresh_token() {
        // ARRANGE
        when(refreshTokenGenerator.hash(any())).thenReturn("hashed-token");
        when(refreshTokenRepository.findByTokenHash("hashed-token")).thenReturn(Optional.empty());

        // ACT & ASSERT
        assertThatThrownBy(() -> authenticationService.refresh("unknown-token"))
                .isInstanceOf(InvalidRefreshTokenException.class);

        verify(refreshTokenRepository, never()).revoke(any());
    }

    @Test
    @DisplayName("refresh — doit rejeter un token expiré")
    void should_reject_expired_refresh_token() {
        // ARRANGE
        RefreshToken expired = new RefreshToken(
                RefreshTokenId.generate(), UserId.generate(), "hashed-token", now.minus(1, ChronoUnit.SECONDS)
        );
        when(refreshTokenGenerator.hash(any())).thenReturn("hashed-token");
        when(refreshTokenRepository.findByTokenHash("hashed-token")).thenReturn(Optional.of(expired));

        // ACT & ASSERT
        assertThatThrownBy(() -> authenticationService.refresh("expired-token"))
                .isInstanceOf(InvalidRefreshTokenException.class);

        verify(refreshTokenRepository, never()).revoke(any());
    }

    @Test
    @DisplayName("refresh — doit rejeter un token déjà révoqué")
    void should_reject_already_revoked_refresh_token() {
        // ARRANGE
        RefreshToken revoked = new RefreshToken(
                RefreshTokenId.generate(), UserId.generate(), "hashed-token", now.plus(1, ChronoUnit.DAYS)
        );
        revoked.revoke(); // NB : utilise Instant.now() en interne, cf. remarque sur la Clock non injectée
        when(refreshTokenGenerator.hash(any())).thenReturn("hashed-token");
        when(refreshTokenRepository.findByTokenHash("hashed-token")).thenReturn(Optional.of(revoked));

        // ACT & ASSERT
        assertThatThrownBy(() -> authenticationService.refresh("revoked-token"))
                .isInstanceOf(InvalidRefreshTokenException.class);
        // TODO Phase 2 : ceci devrait déclencher une détection de vol de token (RFC 9700),
        // pas juste un rejet silencieux identique à un token expiré.
    }

    @Test
    @DisplayName("refresh — doit rejeter si l'utilisateur associé n'existe plus")
    void should_reject_when_user_no_longer_exists() {
        // ARRANGE
        UserId userId = UserId.generate();
        RefreshToken existing = new RefreshToken(RefreshTokenId.generate(), userId, "hashed-token", now.plus(1, ChronoUnit.DAYS));
        when(refreshTokenGenerator.hash(any())).thenReturn("hashed-token");
        when(refreshTokenRepository.findByTokenHash("hashed-token")).thenReturn(Optional.of(existing));
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        // ACT & ASSERT
        assertThatThrownBy(() -> authenticationService.refresh("some-token"))
                .isInstanceOf(InvalidRefreshTokenException.class);
    }

    @Test
    @DisplayName("refresh — doit rejeter explicitement un compte désactivé (contraste volontaire avec authenticate())")
    void should_reject_disabled_account_with_explicit_exception_on_refresh() {
        // ARRANGE
        UserId userId = UserId.generate();
        RefreshToken existing = new RefreshToken(RefreshTokenId.generate(), userId, "hashed-token", now.plus(1, ChronoUnit.DAYS));
        User disabledUser = buildUser(false);

        when(refreshTokenGenerator.hash(any())).thenReturn("hashed-token");
        when(refreshTokenRepository.findByTokenHash("hashed-token")).thenReturn(Optional.of(existing));
        when(userRepository.findById(userId)).thenReturn(Optional.of(disabledUser));

        // ACT & ASSERT
        // Contrairement à authenticate(), on RÉVÈLE ici que le compte est désactivé :
        // l'appelant possède déjà un refresh token valide, donc pas de risque d'énumération anonyme.
        assertThatThrownBy(() -> authenticationService.refresh("some-token"))
                .isInstanceOf(AccountDisabledException.class);
    }

    // ---------- logout() ----------

    @Test
    @DisplayName("logout — doit révoquer le token quand il existe")
    void should_revoke_token_on_logout() {
        // ARRANGE
        RefreshTokenId tokenId = RefreshTokenId.generate();
        RefreshToken existing = new RefreshToken(tokenId, UserId.generate(), "hashed-token", now.plus(1, ChronoUnit.DAYS));
        when(refreshTokenGenerator.hash("raw-token")).thenReturn("hashed-token");
        when(refreshTokenRepository.findByTokenHash("hashed-token")).thenReturn(Optional.of(existing));

        // ACT
        authenticationService.logout("raw-token");

        // ASSERT
        verify(refreshTokenRepository).revoke(tokenId);
    }

    @Test
    @DisplayName("logout — doit être silencieux (idempotent) si le token n'existe pas")
    void should_be_idempotent_when_logging_out_unknown_token() {
        // ARRANGE
        when(refreshTokenGenerator.hash(any())).thenReturn("unknown-hash");
        when(refreshTokenRepository.findByTokenHash("unknown-hash")).thenReturn(Optional.empty());

        // ACT & ASSERT — ne doit pas lever d'exception
        authenticationService.logout("already-gone-token");

        verify(refreshTokenRepository, never()).revoke(any());
    }

    // ---------- helpers ----------

    private static String anyDummyHash() {
        return any(); // simple alias sémantique pour la lisibilité du test de timing
    }
}