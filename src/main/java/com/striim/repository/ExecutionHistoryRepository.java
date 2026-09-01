package com.striim.repository;

import com.striim.entity.ExecutionHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ExecutionHistoryRepository extends JpaRepository<ExecutionHistory, String> {
    @Query(value = "SELECT * FROM execution_history ORDER BY start_time DESC LIMIT 100", nativeQuery = true)
    List<ExecutionHistory> findRecentExecutions();
}
