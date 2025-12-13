package com.notification.repository;

import com.notification.entity.NotificationRule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface NotificationRuleRepository extends JpaRepository<NotificationRule, Integer> {
    NotificationRule findByNotificationType(String notificationType);
}
