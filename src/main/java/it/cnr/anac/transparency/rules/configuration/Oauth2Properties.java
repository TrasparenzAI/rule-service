package it.cnr.anac.transparency.rules.configuration;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Map;

@Getter
@Setter
@ConfigurationProperties("security.oauth2")
public class Oauth2Properties {
    /**
     * Name of the roles
     */
    private Map<String, String[]> roles;
    private boolean enabled;

}
