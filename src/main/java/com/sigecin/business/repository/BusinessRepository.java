package com.sigecin.business.repository;

import com.sigecin.business.entity.Business;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.Optional;
import java.util.Set;

public interface BusinessRepository extends JpaRepository<Business, Long> {

    // Condición de visibilidad: negocio activo con al menos un servicio activo
    // y al menos un día de horario activo. Si no la cumple, para el cliente no existe.
    String VISIBLE = """
            b.active = true
            and exists (select 1 from ServiceOffering s
                         where s.business = b and s.status = com.sigecin.serviceoffering.enums.ServiceStatus.ACTIVE)
            and exists (select 1 from BusinessSchedule h
                         where h.business = b and h.active = true)
            """;

    // :name llega con %, _ y ! escapados con "!" (ver BusinessDirectoryService). No se usa
    // la diagonal invertida porque en MariaDB '\' dentro de una cadena SQL escapa la comilla.
    String SEARCH_FILTERS = """
            and (:name is null or lower(b.name) like lower(concat('%', :name, '%')) escape '!')
            and (:categoryId is null or b.category.id = :categoryId)
            """;

    // Carga categoría y horario: con open-in-view desactivado, la vista no puede cargarlos después
    @EntityGraph(attributePaths = {"category", "schedules"})
    Optional<Business> findByOwnerId(Long ownerId);

    boolean existsByOwnerId(Long ownerId);

    /**
     * Bloquea la fila del negocio (SELECT ... FOR UPDATE) para serializar
     * reservas simultáneas. Debe llamarse dentro de una transacción.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select b from Business b where b.id = :id")
    Optional<Business> findByIdForUpdate(@Param("id") Long id);

    @EntityGraph(attributePaths = {"category", "schedules"})
    @Query("select b from Business b where b.id = :id and " + VISIBLE)
    Optional<Business> findVisibleById(@Param("id") Long id);

    /** De los negocios indicados, cuáles son visibles (para marcar favoritos "no disponibles"). */
    @Query("select b.id from Business b where b.id in :ids and " + VISIBLE)
    Set<Long> findVisibleIds(@Param("ids") Collection<Long> ids);

    /** Búsqueda pública por nombre (parcial) y/o categoría; ambos filtros son opcionales. */
    @Query(value = "select b from Business b join fetch b.category where " + VISIBLE + SEARCH_FILTERS + " order by b.name",
            countQuery = "select count(b) from Business b where " + VISIBLE + SEARCH_FILTERS)
    Page<Business> searchVisible(@Param("name") String name,
                                 @Param("categoryId") Integer categoryId,
                                 Pageable pageable);
}
