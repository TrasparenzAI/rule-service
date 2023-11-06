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

import it.cnr.anac.transparency.rules.configuration.RuleConfiguration;
import it.cnr.anac.transparency.rules.domain.RuleResponse;
import it.cnr.anac.transparency.rules.exception.RuleNotFoundException;
import it.cnr.anac.transparency.rules.service.RuleService;
import it.cnr.anac.transparency.rules.v1.dto.RuleDto;
import it.cnr.anac.transparency.rules.v1.dto.RuleMapper;
import it.cnr.anac.transparency.rules.v1.dto.RuleResponseDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Slf4j
@RestController
@RequestMapping("/v1/rules")
public class RuleController {
    private final RuleConfiguration ruleConfiguration;
    private final RuleMapper ruleMapper;
    private final RuleService ruleService;

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

    @PostMapping
    public ResponseEntity<RuleResponseDto> post(@RequestBody String content, @RequestParam(name = "ruleName") Optional<String> ruleName) {
        try {
            final RuleResponse ruleResponse = ruleService.executeRule(
                    new String(Base64.getDecoder().decode(content), StandardCharsets.UTF_8),
                    ruleName
            );
            return ResponseEntity.ok().body(ruleMapper.convert(ruleResponse));
        } catch (RuleNotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (IOException e) {
            log.error("Cannot execute rule {}", ruleName, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @PostMapping("/child")
    public ResponseEntity<List<RuleResponseDto>> postChild(@RequestBody String content, @RequestParam(name = "ruleName") Optional<String> ruleName) {
        try {
            final List<RuleResponse> ruleResponses = ruleService.executeChildRule(
                    new String(Base64.getDecoder().decode(content), StandardCharsets.UTF_8),
                    ruleName
            );
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
