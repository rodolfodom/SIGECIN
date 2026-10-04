package com.sigecin.user.repository;

import com.sigecin.user.entity.User;
import com.sigecin.business.entity.Business;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    @Query("""
            select count(b) > 0 from User u join u.favoriteBusinesses b
             where u.id = :userId and b.id = :businessId
            """)
    boolean isFavorite(@Param("userId") Long userId, @Param("businessId") Long businessId);

    /** Negocios favoritos del cliente con su categoría (visibles o no). */
    @Query("""
            select b from User u join u.favoriteBusinesses b join fetch b.category
             where u.id = :userId
             order by b.name
            """)
    List<Business> findFavorites(@Param("userId") Long userId);
}
