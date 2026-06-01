package com.vendorhub.repository;

import com.vendorhub.document.Message;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import java.util.List;

public interface MessageRepository extends MongoRepository<Message, String> {

    @Query("{ $or: [ { 'senderId': ?0, 'receiverId': ?1 }, { 'senderId': ?1, 'receiverId': ?0 } ] }")
    List<Message> findConversation(String userId1, String userId2);

    long countByReceiverIdAndIsReadFalse(String receiverId);

    @Query(value = "{ $or: [ { 'senderId': ?0 }, { 'receiverId': ?0 } ] }",
           fields = "{ 'senderId': 1, 'receiverId': 1 }")
    List<Message> findAllByUserId(String userId);

    // Returns distinct partner IDs as strings
    @Query("{ $or: [ { 'senderId': ?0 }, { 'receiverId': ?0 } ] }")
    List<Message> findAllMessages(String userId);
}
