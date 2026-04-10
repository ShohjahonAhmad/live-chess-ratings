package com.example.demo.repository;

import com.example.demo.entity.BroadcastRound;
import com.example.demo.utils.Status;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BroadcastRoundRepository extends JpaRepository<BroadcastRound, String> {
    List<BroadcastRound> findByStatus(Status status);
}
