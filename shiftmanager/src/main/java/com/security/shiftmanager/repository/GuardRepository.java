package com.security.shiftmanager.repository;

import com.security.shiftmanager.model.Guard;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface GuardRepository extends JpaRepository<Guard, Long> {

    Optional<Guard> findByDni(String dni);

    /**
     * Searches by first name, last name, full name or DNI. Both parameters are optional.
     * The search term must already be lowercased and wrapped with '%'.
     */
    @Query("""
            select g from Guard g
            where (:active is null or g.active = :active)
              and (:search is null
                   or lower(g.firstName) like :search
                   or lower(g.lastName) like :search
                   or lower(concat(g.firstName, ' ', g.lastName)) like :search
                   or g.dni like :search)
            order by g.lastName, g.firstName
            """)
    List<Guard> search(@Param("search") String search, @Param("active") Boolean active);
}
