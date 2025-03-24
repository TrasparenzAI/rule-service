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

package it.cnr.anac.transparency.rules;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.servers.Server;
import org.springframework.context.annotation.Configuration;

/**
 * Configurazione dei parametri generali della documentazione
 * tramite OpenAPI.
 */
@Configuration
@OpenAPIDefinition(
    info = @Info(title = "Rule Service", 
    version = "0.0.1-SNAPSHOT", 
    description = "Rule Service per l'applicazione delle regole ai siti istituzionali"),
    servers = {
        @Server(url = "/rule-service", description = "Rule Service URL"),
        @Server(url = "/", description = "Rule Service URL")}
    )
public class OpenApiConfiguration {

  //Empty class
}