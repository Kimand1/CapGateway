package com.iot.capGateway.repo;
import com.iot.capGateway.domain.DisasterMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface DisasterMessageRepo extends JpaRepository<DisasterMessage, Long> {
    Optional<DisasterMessage> findByIdentifier(String identifier);
}
