package com.gpintus.sqscustomendpoint;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import javax.validation.constraints.NotBlank;

@ConfigurationProperties(prefix = "app")
@Getter
@Setter
@Validated
public class AppProperties {
    @NotBlank
    private String queueName;
}
