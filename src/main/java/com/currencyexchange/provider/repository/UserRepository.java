package com.currencyexchange.provider.repository;

import com.currencyexchange.provider.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    
    /**
     * Find user by username with roles eagerly loaded.
     *
     * @param username the username
     * @return the user if found
     */
    @Query("SELECT u FROM User u LEFT JOIN FETCH u.roles WHERE u.username = :username")
    Optional<User> findByUsernameWithRoles(@Param("username") String username);
    
    /**
     * Check if username exists.
     *
     * @param username the username
     * @return true if username exists
     */
    boolean existsByUsername(String username);
}
