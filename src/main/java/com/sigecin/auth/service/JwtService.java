package com.sigecin.auth.service;

import com.sigecin.auth.security.AuthenticatedUser;
import com.sigecin.config.AuthProperties;
import com.sigecin.user.entity.User;
import com.sigecin.user.enums.Role;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

/** Emite los JWT y los convierte de vuelta en la autenticación de la petición. */
@Service
@RequiredArgsConstructor
public class JwtService {

    static final String ISSUER = "sigecin";
    private static final String CLAIM_ROLE = "role";
    private static final String CLAIM_NAME = "name";
    private static final String CLAIM_EMAIL = "email";

    private final JwtEncoder encoder;
    private final AuthProperties properties;

    public String issueToken(User user) {
        Instant now = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(ISSUER)
                .subject(user.getId().toString())
                .issuedAt(now)
                .expiresAt(now.plus(properties.accessTokenTtl()))
                .claim(CLAIM_ROLE, user.getRole().name())
                .claim(CLAIM_NAME, user.getFullName())
                .claim(CLAIM_EMAIL, user.getEmail())
                .build();
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        return encoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
    }

    /** Convierte un JWT ya validado en la autenticación con rol ROLE_CLIENT / ROLE_BUSINESS. */
    public UsernamePasswordAuthenticationToken toAuthentication(Jwt jwt) {
        Role role = Role.valueOf(jwt.getClaimAsString(CLAIM_ROLE));
        AuthenticatedUser user = new AuthenticatedUser(
                Long.valueOf(jwt.getSubject()),
                jwt.getClaimAsString(CLAIM_NAME),
                jwt.getClaimAsString(CLAIM_EMAIL),
                role);
        return UsernamePasswordAuthenticationToken.authenticated(
                user, jwt, List.of(new SimpleGrantedAuthority("ROLE_" + role.name())));
    }
}
