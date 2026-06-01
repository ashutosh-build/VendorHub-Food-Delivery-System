package com.vendorhub.repository;

import com.vendorhub.document.Notification;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.List;

public interface NotificationRepository extends MongoRepository<Notification, String> {
    List<Notification> findByUserIdOrderByCreatedAtDesc(String userId);
    long               countByUserIdAndIsReadFalse(String userId);
    void               deleteByUserId(String userId);
}
