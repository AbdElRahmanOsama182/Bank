package org.example.logging.repository;

import org.example.logging.model.logDump;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface logRepository extends JpaRepository<logDump, UUID> {
}