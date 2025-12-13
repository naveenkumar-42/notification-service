package com.notification.repository;

import com.notification.entity.NotificationEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface NotificationEventRepository extends JpaRepository<NotificationEvent, Long> {
    List<NotificationEvent> findByStatus(String status);
    List<NotificationEvent> findByRecipient(String recipient);
    List<NotificationEvent> findByRetryCount(Integer retryCount);
}
