package com.carter.repo;


import com.carter.entity.TalentProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface TalentProfileRepository extends JpaRepository<TalentProfile, Long> {
    Optional<TalentProfile> findByEmployeeName(String employeeName);
}