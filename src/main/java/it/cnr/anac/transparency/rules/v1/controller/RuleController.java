/*
 *  Copyright (C) 2023 Consiglio Nazionale delle Ricerche
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 *  You should have received a copy of the GNU Affero General Public License
 *  along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package it.cnr.anac.transparency.rules.v1.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import it.cnr.anac.transparency.rules.configuration.RuleConfiguration;
import it.cnr.anac.transparency.rules.domain.RuleResponse;
import it.cnr.anac.transparency.rules.exception.RuleException;
import it.cnr.anac.transparency.rules.exception.RuleNotFoundException;
import it.cnr.anac.transparency.rules.service.RuleService;
import it.cnr.anac.transparency.rules.v1.dto.RuleDto;
import it.cnr.anac.transparency.rules.v1.dto.RuleMapper;
import it.cnr.anac.transparency.rules.v1.dto.RuleResponseDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

@Tag(name = "Rule Controller", description = "Metodi di consultazione e applicazione delle regole sulla trasparenza")
@RequiredArgsConstructor
@Slf4j
@RestController
@RequestMapping("/v1/rules")
public class RuleController {
    private final RuleConfiguration ruleConfiguration;
    private final RuleMapper ruleMapper;
    private final RuleService ruleService;
    @Operation(
            summary = "Visualizzazione dell'albero delle regole.",
            description = "Il servizio recupera dalla configurazione l'albero delle regole e lo presenta come json")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Restitutito l'albero delle regole.")
    })
    @GetMapping
    public ResponseEntity<Map<String, RuleDto>> get() {
        return ResponseEntity.ok().body(
                ruleConfiguration.getRules()
                        .entrySet()
                        .stream()
                        .collect(Collectors.toMap(
                                e -> e.getKey(),
                                e -> ruleMapper.convert(e.getValue())
                        ))
        );
    }
    @Operation(
            summary = "Applicazione di una singola regola allo stream in base64 passato in input.",
            description = "Il servizio accetta in input una stringa in base64 contenente la pagina html e il nome logico" +
                    " di una regola da applicare, in alternativa viene aplicata la regola root.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Il termine della regola è stato trovato e " +
                    "viene restituito un oggetto json con le informazioni sullo score"),
            @ApiResponse(responseCode = "400", description = "Il termine della regola non è stato trovato o la regola non esiste.")
    })
    @PostMapping
    public ResponseEntity post(@RequestBody String content, @RequestParam(name = "ruleName") Optional<String> ruleName) {
        try {
            final RuleResponse ruleResponse = ruleService.executeRule(
                    ruleService.base64Decode(content),
                    ruleName
            );
            return ResponseEntity.ok().body(ruleMapper.convert(ruleResponse));
        } catch (RuleException e) {
            return ResponseEntity.status(e.getHttpStatus()).body(e.getMessage());
        } catch (RuleNotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (IOException e) {
            log.error("Cannot execute rule {}", ruleName, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @Operation(
            summary = "Vengono applicate tutte le regole figlie di una determinata regola, allo stream in base64 passato in input.",
            description = "Il servizio accetta in input una stringa in base64 contenente la pagina html e il nome logico" +
                    " di una regola da cui recuperare i figli, in alternativa vengono cercati i figli della regola root.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "La regola padre esiste " +
                    "viene restituito una lista di oggetti json con le informazioni sullo score"),
            @ApiResponse(responseCode = "400", description = "La regola padre non esiste.")
    })
    @PostMapping("/child")
    public ResponseEntity<List<RuleResponseDto>> postChild(@RequestBody String content, @RequestParam(name = "ruleName") Optional<String> ruleName) {
        try {
            final String contentDecoded = ruleService.base64Decode(content);
            List<RuleResponse> ruleResponses = ruleService.executeChildRule(contentDecoded, ruleName);
            if (ruleResponses.stream().filter(ruleResponse -> ruleResponse.getStatus().equals(HttpStatus.NOT_FOUND)).collect(Collectors.toList()).size() >
                ruleService.childRules(ruleName).size() / 2) {
                ruleResponses = ruleService.executeChildRuleAlternative(contentDecoded, ruleName);
            }
            if (!ruleResponses.stream().filter(ruleResponse -> !ruleResponse.getStatus().equals(HttpStatus.NOT_FOUND)).findAny().isPresent()) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.ok().body(
                    ruleResponses
                            .stream()
                            .map(ruleResponse -> ruleMapper.convert(ruleResponse))
                            .collect(Collectors.toList())

            );
        } catch (RuleNotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (IOException e) {
            log.error("Cannot execute rule {}", ruleName, e);
            return ResponseEntity.internalServerError().build();
        }
    }

}
