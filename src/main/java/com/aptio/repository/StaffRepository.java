package com.aptio.repository;

import com.aptio.model.Staff;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StaffRepository extends JpaRepository<Staff, String> {

    List<Staff> findByIsActive(boolean isActive);

    @Query("SELECT s FROM Staff s JOIN s.specialties spec WHERE spec = :specialty")
    List<Staff> findBySpecialty(String specialty);

    Optional<Staff> findByUserEmail(String mail);
}