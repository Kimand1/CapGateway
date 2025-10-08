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
    public void saveCapData(CapData cap) {
        String id = (cap.getIdentifier()==null || cap.getIdentifier().isBlank())
                ? UUID.randomUUID().toString() : cap.getIdentifier();
        var dm = repo.findByIdentifier(id).orElseGet(DisasterMessage::new);
        dm.setIdentifier(id);
        dm.setResultCode(cap.getResultCode());
        dm.setCapXml(cap.toXml());  // CapData에 toXml 있으면 사용
        repo.save(dm);
    }
}
