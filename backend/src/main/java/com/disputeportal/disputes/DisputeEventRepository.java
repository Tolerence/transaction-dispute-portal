package com.disputeportal.disputes;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DisputeEventRepository extends JpaRepository<DisputeEvent, Long> {

    List<DisputeEvent> findByDisputeIdOrderByCreatedAtAscIdAsc(Long disputeId);
}
