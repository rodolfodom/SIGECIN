package com.sigecin.business.service;

import com.sigecin.business.dto.FavoriteEntry;
import com.sigecin.business.entity.Business;
import com.sigecin.business.repository.BusinessRepository;
import com.sigecin.common.exception.NotFoundException;
import com.sigecin.user.entity.User;
import com.sigecin.user.enums.Role;
import com.sigecin.user.repository.UserRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

/**
 * Negocios favoritos de un cliente. Solo se puede marcar un negocio visible; un favorito
 * cuyo negocio deja de serlo se conserva y se muestra como "no disponible".
 */
@Service
public class FavoriteService {

    private final UserRepository users;
    private final BusinessRepository businesses;

    public FavoriteService(UserRepository users, BusinessRepository businesses) {
        this.users = users;
        this.businesses = businesses;
    }

    @Transactional(readOnly = true)
    public boolean isFavorite(Long clientId, Long businessId) {
        return users.isFavorite(clientId, businessId);
    }

    @Transactional(readOnly = true)
    public List<FavoriteEntry> list(Long clientId) {
        List<Business> favorites = users.findFavorites(clientId);
        if (favorites.isEmpty()) {
            return List.of();
        }
        Set<Long> visible = businesses.findVisibleIds(favorites.stream().map(Business::getId).toList());
        return favorites.stream()
                .map(business -> new FavoriteEntry(business, visible.contains(business.getId())))
                .toList();
    }

    /** Marca el negocio como favorito; si ya lo era no hace nada. */
    @Transactional
    public void add(Long clientId, Long businessId) {
        Business business = businesses.findVisibleById(businessId)
                .orElseThrow(() -> new NotFoundException("Negocio " + businessId + " no disponible"));
        User client = client(clientId);
        if (client.getFavoriteBusinesses().add(business)) {
            try {
                users.flush();
            } catch (DataIntegrityViolationException e) {
                // Otra petición simultánea ya lo marcó (llave primaria compuesta): el resultado es el mismo
            }
        }
    }

    /** Quita el negocio de favoritos aunque ya no sea visible; si no lo era no hace nada. */
    @Transactional
    public void remove(Long clientId, Long businessId) {
        client(clientId).getFavoriteBusinesses().removeIf(business -> business.getId().equals(businessId));
    }

    // Que el usuario tenga rol CLIENT se valida aquí: la BD no puede verificarlo con un CHECK
    private User client(Long clientId) {
        return users.findById(clientId)
                .filter(user -> user.getRole() == Role.CLIENT)
                .orElseThrow(() -> new IllegalStateException("Solo un usuario CLIENT tiene favoritos"));
    }
}
