/*
 * Copyright (c) 2026 Consiglio Nazionale delle Ricerche
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

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Verifica delle regole di autorizzazione definite in {@link Oauth2Configuration}.
 */
@SpringBootTest(properties = {
		"spring.cloud.config.enabled=false",
		"security.oauth2.enabled=true"
})
@AutoConfigureMockMvc
class Oauth2ConfigurationTests {

	private static final String RULES_PATH = "/v1/rules";

	@Autowired
	MockMvc mockMvc;

	@Test
	void getRulesAnonymousIsAllowed() throws Exception {
		mockMvc.perform(get(RULES_PATH)).andExpect(status().isOk());
	}

	@Test
	void postRulesAnonymousIsUnauthorized() throws Exception {
		mockMvc.perform(post(RULES_PATH)).andExpect(status().isUnauthorized());
	}

	@Test
	void patchAnonymousIsUnauthorized() throws Exception {
		mockMvc.perform(patch(RULES_PATH)).andExpect(status().isUnauthorized());
	}

	@Test
	void patchWithoutAdminRoleIsForbidden() throws Exception {
		mockMvc.perform(patch(RULES_PATH)
						.with(jwt().authorities(new SimpleGrantedAuthority("ROLE_USER"))))
				.andExpect(status().isForbidden());
	}

	@Test
	void patchWithAdminRoleReachesTheController() throws Exception {
		// Non esistono endpoint PATCH: superata l'autorizzazione la risposta è 405
		mockMvc.perform(patch(RULES_PATH)
						.with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
				.andExpect(status().isMethodNotAllowed());
	}

	@Test
	void actuatorHealthAndInfoAreAnonymous() throws Exception {
		mockMvc.perform(get("/actuator/health")).andExpect(status().isOk());
		mockMvc.perform(get("/actuator/info")).andExpect(status().isOk());
	}

	@Test
	void otherActuatorEndpointsRequireAuthentication() throws Exception {
		mockMvc.perform(get("/actuator/env")).andExpect(status().isUnauthorized());
		mockMvc.perform(get("/actuator")).andExpect(status().isUnauthorized());
	}

	@Test
	void actuatorRefreshRequiresAdminRole() throws Exception {
		mockMvc.perform(post("/actuator/refresh")).andExpect(status().isUnauthorized());
		mockMvc.perform(post("/actuator/refresh")
						.with(jwt().authorities(new SimpleGrantedAuthority("ROLE_USER"))))
				.andExpect(status().isForbidden());
	}
}
