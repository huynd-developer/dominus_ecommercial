package org.example.datn_sd69.modules.vietqr.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "vietqr")
public class VietQrProperties {

    private String bankCode;
    private String bankBin;
    private String accountNo;
    private String accountName;
    private String template = "compact2";
}