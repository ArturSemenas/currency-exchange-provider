package com.currencyexchange.provider.repository;

import com.currencyexchange.provider.model.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RoleRepository extends JpaRepository<Role, Long> {
    
    /**
     * Find role by name.
     *
     * @param name the role name
     * @return the role if found
     */
    Optional<Role> findByName(String name);
    
    /**
     * Check if role exists by name.
     *
     * @param name the role name
     * @return true if role exists
     */
    boolean existsByName(String name);
}
