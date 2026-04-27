package com.liveclass.assignment.domain.classmate.repository;

import com.liveclass.assignment.domain.classmate.entity.Classmate;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ClassmateRepository extends JpaRepository<Classmate, Long> {
}
