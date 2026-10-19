package com.security.shiftmanager.repository;

import com.security.shiftmanager.model.Service;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ServiceRepository extends JpaRepository<Service, Long> {

    /**
     * Searches by name or location. Both parameters are optional.
     * The search term must already be lowercased and wrapped with '%'.
     */
    @Query("""
            select s from Service s
            where (:active is null or s.active = :active)
              and (:search is null
                   or lower(s.name) like :search
                   or lower(s.location) like :search)
            order by s.name
            """)
    List<Service> search(@Param("search") String search, @Param("active") Boolean active);
}
