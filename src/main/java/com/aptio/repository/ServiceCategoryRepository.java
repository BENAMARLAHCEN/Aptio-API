package com.aptio.repository;

import com.aptio.model.ServiceCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ServiceCategoryRepository extends JpaRepository<ServiceCategory, String> {
    Optional<ServiceCategory> findByName(String name);

    boolean existsByName(String name);
}