package com.iot.capGateway.config;

import lombok.Data;

@Data
public class Config {
    private String LAGDbId;
    private String LAGDbPw;
    private String NAGAuthId;
    private String NAGAuthPw;
    private String LocationCode;
    private String LocationLevel;
}
