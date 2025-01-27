package it.cnr.anac.transparency.rules.configuration;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.stream.Collectors;

@Configuration
@EnableWebSecurity
@ConditionalOnProperty(name = "security.oauth2.enabled", havingValue = "true")
@EnableConfigurationProperties(Oauth2Properties.class)
public class Oauth2Configuration {
    private final Oauth2Properties properties;

    public Oauth2Configuration(Oauth2Properties properties) {
        this.properties = properties;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(AbstractHttpConfigurer::disable)
                .authorizeRequests(expressionInterceptUrlRegistry -> {
                    expressionInterceptUrlRegistry
                            .anyRequest()
                            .hasAnyRole(properties.getRoles());
                })
                .httpBasic(AbstractHttpConfigurer::disable)
                .oauth2ResourceServer(httpSecurityOAuth2ResourceServerConfigurer -> {
                    httpSecurityOAuth2ResourceServerConfigurer.jwt(jwtConfigurer -> {
                        jwtConfigurer.jwtAuthenticationConverter(
                                new RolesClaimConverter(
                                        new JwtGrantedAuthoritiesConverter()
                                )
                        );
                    });
                });
        return http.build();
    }

    static class RolesClaimConverter implements Converter<Jwt, AbstractAuthenticationToken> {

        private static final String CLAIM_REALM_ACCESS = "realm_access";
        private static final String CLAIM_RESOURCE_ACCESS = "resource_access";
        private static final String CLAIM_ROLES = "roles";
        private final JwtGrantedAuthoritiesConverter wrappedConverter;

        public RolesClaimConverter(JwtGrantedAuthoritiesConverter conv) {
            wrappedConverter = conv;
        }

        @Override
        public AbstractAuthenticationToken convert(@NonNull Jwt jwt) {
            var grantedAuthorities = new ArrayList<>(wrappedConverter.convert(jwt));
            Map<String, Collection<String>> realmAccess = jwt.getClaim(CLAIM_REALM_ACCESS);

            if (realmAccess != null && !realmAccess.isEmpty()) {
                Collection<String> roles = realmAccess.get(CLAIM_ROLES);
                if (roles != null && !roles.isEmpty()) {
                    Collection<GrantedAuthority> realmRoles = roles.stream()
                            .map(SimpleGrantedAuthority::new)
                            .collect(Collectors.toList());
                    grantedAuthorities.addAll(realmRoles);
                }
            }
            Map<String, Map<String, Collection<String>>> resourceAccess = jwt.getClaim(CLAIM_RESOURCE_ACCESS);

            if (resourceAccess != null && !resourceAccess.isEmpty()) {
                resourceAccess.forEach((resource, resourceClaims) -> {
                    resourceClaims.get(CLAIM_ROLES).forEach(
                            role -> grantedAuthorities.add(new SimpleGrantedAuthority(resource + "_" + role))
                    );
                });
            }
            return new JwtAuthenticationToken(jwt, grantedAuthorities);
        }
    }
}
