package it.cnr.anac.transparency.rules.configuration;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties("security.oauth2")
public class Oauth2Properties {
    /**
     * Name of the roles
     */
    private String[] roles;

}
