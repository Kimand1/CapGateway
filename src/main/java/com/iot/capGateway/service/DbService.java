package com.iot.capGateway.service;

import com.iot.capGateway.domain.DisasterMessage;
import com.iot.capGateway.model.CapData;
import com.iot.capGateway.repo.DisasterMessageRepo;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service @RequiredArgsConstructor
public class DbService {
    private final DisasterMessageRepo repo;

    @Transactional
    public void saveCapDataRaw(String identifier, String rawXml) {
        if (identifier != null && repo.findByIdentifier(identifier).isPresent()) {
            return; // 중복 저장 방지
        }
        DisasterMessage dm = new DisasterMessage();
        dm.setIdentifier(identifier != null ? identifier : "UNKNOWN");
        dm.setCapXml(rawXml);
        dm.setResultCode("200");
        repo.save(dm);
    }
}
