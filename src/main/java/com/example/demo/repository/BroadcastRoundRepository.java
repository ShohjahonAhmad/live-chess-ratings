package com.example.demo.repository;

import com.example.demo.entity.BroadcastRound;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BroadcastRoundRepository extends JpaRepository<BroadcastRound, String> {
}
