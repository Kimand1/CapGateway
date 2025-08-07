package com.iot.capGateway.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.stereotype.Component;

@Component
@Getter
@Setter
public class GlobalValues {
    private String locationCode;
    private int locationLevel;
}
