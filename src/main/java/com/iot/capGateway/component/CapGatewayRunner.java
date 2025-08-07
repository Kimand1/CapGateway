package com.iot.capGateway.component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.iot.capGateway.config.Config;
import com.iot.capGateway.config.DbInformation;
import com.iot.capGateway.config.GlobalValues;
import com.iot.capGateway.config.LogProvider;
import com.iot.capGateway.service.GatewayManager;
import io.micrometer.common.util.StringUtils;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.CountDownLatch;

@Component
public class CapGatewayRunner implements CommandLineRunner {
    private final DbInformation dbInformation;
    private GlobalValues globalValues;

    public CapGatewayRunner(DbInformation dbInformation) {
        this.dbInformation = dbInformation;
        this.globalValues = globalValues;
    }

    @Override
    public void run(String... args) {
        if (args.length != 4) {
            System.out.println("NAG ip, NAG port, GwDB ip, GwDB port");
            return;
        }

        String nagIp = args[0];
        int nagPort = Integer.parseInt(args[1]);
        dbInformation.setServer(args[2]);
        dbInformation.setPort(Integer.parseInt(args[3]));

        // AppConfig.json 읽기
        Path configPath = Paths.get("AppConfig.json");
        if (!Files.exists(configPath)) {
            System.out.println("AppConfig.json 파일이 없습니다.");
            LogProvider.getInstance().fatal("AppConfig.json 파일이 없습니다.");
            return;
        }

        try {
            String json = Files.readString(configPath, StandardCharsets.UTF_8);
            ObjectMapper objectMapper = new ObjectMapper();
            Config config = objectMapper.readValue(json, Config.class);

            if (StringUtils.isBlank(config.getLAGDbId()) ||
                    StringUtils.isBlank(config.getLAGDbPw()) ||
                    StringUtils.isBlank(config.getNAGAuthId()) ||
                    StringUtils.isBlank(config.getNAGAuthPw())) {

                System.out.println("AppConfig.json 구성이 잘못되었습니다.");
                LogProvider.getInstance().fatal("AppConfig.json 구성 오류");
                return;
            }

            globalValues.setLocationCode(config.getLocationCode());
            globalValues.setLocationLevel(Integer.parseInt(config.getLocationLevel()));
            dbInformation.setDbId(config.getLAGDbId());
            dbInformation.setDbPw(config.getLAGDbPw());

            // 게이트웨이 실행
            GatewayManager gatewayManager = new GatewayManager(nagIp, nagPort, config.getNAGAuthId(), config.getNAGAuthPw());
            gatewayManager.run();

            // 무한 대기
            new CountDownLatch(1).await();

        } catch (Exception e) {
            LogProvider.getInstance().fatal("예상되지 않은 오류", e);
        }
    }

}
