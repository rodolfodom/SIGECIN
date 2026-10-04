package com.sigecin.auth;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    /**
     * Busca el token bloqueando su fila: dos renovaciones simultáneas con el mismo
     * token se serializan y la segunda ya lo ve rotado.
     * Sin join al usuario: en MariaDB el FOR UPDATE bloquearía también su fila y dos
     * renovaciones podrían tomar los bloqueos en distinto orden (deadlock).
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select t from RefreshToken t where t.tokenHash = :hash")
    Optional<RefreshToken> findByHashForUpdate(@Param("hash") String hash);

    Optional<RefreshToken> findByTokenHash(String tokenHash);

    /** Revoca todos los tokens aún vigentes de una familia (logout o reutilización). */
    @Modifying
    @Query("update RefreshToken t set t.revokedAt = :now where t.familyId = :familyId and t.revokedAt is null")
    int revokeFamily(@Param("familyId") String familyId, @Param("now") LocalDateTime now);

    @Modifying
    @Query("delete from RefreshToken t where t.expiresAt < :limit")
    int deleteExpiredBefore(@Param("limit") LocalDateTime limit);
}
