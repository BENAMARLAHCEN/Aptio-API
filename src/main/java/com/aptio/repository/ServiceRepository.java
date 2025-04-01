package com.aptio.repository;

import com.aptio.model.Service;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ServiceRepository extends JpaRepository<Service, String> {

    @Query("SELECT s FROM Service s WHERE s.category.name = :categoryName")
    List<Service> findByCategoryName(String categoryName);

    @Query("SELECT s FROM Service s WHERE " +
            "LOWER(s.name) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
            "LOWER(s.description) LIKE LOWER(CONCAT('%', :query, '%'))")
    List<Service> searchServices(String query);

    Optional<Service> findByName(String name);
}