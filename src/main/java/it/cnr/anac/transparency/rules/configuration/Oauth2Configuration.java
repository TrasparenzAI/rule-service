/*
 * Copyright (c) 2025 Consiglio Nazionale delle Ricerche
 *
 * 	This program is free software: you can redistribute it and/or modify
 * 	it under the terms of the GNU Affero General Public License as
 * 	published by the Free Software Foundation, either version 3 of the
 * 	License, or (at your option) any later version.
 *
 * 	This program is distributed in the hope that it will be useful,
 * 	but WITHOUT ANY WARRANTY; without even the implied warranty of
 * 	MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * 	GNU Affero General Public License for more details.
 *
 * 	You should have received a copy of the GNU Affero General Public License
 * 	along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package it.cnr.anac.transparency.rules.configuration;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.http.HttpMethod;
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
@EnableConfigurationProperties(Oauth2Properties.class)
public class Oauth2Configuration {
    private final Oauth2Properties oauth2Properties;

    public Oauth2Configuration(Oauth2Properties properties) {
        this.oauth2Properties = properties;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        if (oauth2Properties.isEnabled()) {
            http.authorizeHttpRequests(expressionInterceptUrlRegistry -> {
                expressionInterceptUrlRegistry
                        .requestMatchers(HttpMethod.OPTIONS).permitAll()
                        .requestMatchers(HttpMethod.GET, "/actuator/*").permitAll()
                        .requestMatchers(HttpMethod.GET,"/api-docs/**","/swagger-ui/**").permitAll();
                oauth2Properties
                        .getRoles()
                        .forEach((key, value) ->
                                expressionInterceptUrlRegistry
                                        .requestMatchers(HttpMethod.valueOf(key)).hasAnyRole(value)
                        );
            });
        }
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(AbstractHttpConfigurer::disable)
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
