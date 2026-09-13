package com.finledger.transaction.repository;

import com.finledger.transaction.entity.OutboxEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface OutboxEventRepository extends JpaRepository<OutboxEvent, UUID> {

    List<OutboxEvent> findTop100ByPublishedAtIsNullOrderByCreatedAtAsc();

    boolean existsByTopicAndEventKey(String topic, String eventKey);
}
