package com.notification.repository;

import com.notification.entity.NotificationEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface NotificationEventRepository extends JpaRepository<NotificationEvent, Long> {

    List<NotificationEvent> findByStatusIgnoreCase(String status);
    List<NotificationEvent> findByPriorityIgnoreCase(String priority);
    List<NotificationEvent> findByChannelIgnoreCase(String channel);
    List<NotificationEvent> findByRecipient(String recipient);
    List<NotificationEvent> findByRetryCount(Integer retryCount);
    List<NotificationEvent> findByStatusAndScheduledAtBefore(String status, LocalDateTime time);


    // FRONTEND HISTORY FILTER (COMPLETE)
    @Query("SELECT e FROM NotificationEvent e WHERE " +
            "(:status IS NULL OR UPPER(e.status) = UPPER(:status)) AND " +
            "(:priority IS NULL OR UPPER(e.priority) = UPPER(:priority)) AND " +
            "(:channel IS NULL OR UPPER(e.channel) = UPPER(:channel)) AND " +
            "(:startDate IS NULL OR e.createdAt >= :startDate) " +
            "ORDER BY e.createdAt DESC")
    List<NotificationEvent> findByFilters(
            @Param("status") String status,
            @Param("priority") String priority,
            @Param("channel") String channel,
            @Param("startDate") LocalDateTime startDate
    );
}
