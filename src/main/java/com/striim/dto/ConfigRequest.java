package com.striim.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ConfigRequest {
    private String striimUrl;
    private String striimUser;
    private String striimPassword;
    private String splunkHecUrl;
    private String splunkToken;
    private String splunkIndex;
    private Integer collectionIntervalSeconds;
}
