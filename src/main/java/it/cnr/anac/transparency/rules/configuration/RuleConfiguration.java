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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.cnr.anac.transparency.rules.domain.Rule;
import it.cnr.anac.transparency.rules.exception.RuleNotFoundException;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.*;

@Configuration
@ConfigurationProperties
@Getter
@Setter
public class RuleConfiguration {

    protected String defaultRule;
    protected String anchorRegex;
    protected String hrefRegex;
    protected Integer maxLengthContent;
    protected Integer maxLengthPageByte;
    protected Integer maxLengthContentRegularExpression;
    protected List<String> tagAttributes;
    private List<Character> searchTokens;

    protected String jsonrules;

    protected Map<String, Rule> rules;
    protected Map<String, Map<String, Rule>> flattenRules;

    @PostConstruct
    public void postConstruct() {
        Optional.ofNullable(jsonrules)
                .ifPresent(s -> {
                    try {
                        rules = new ObjectMapper().readValue(jsonrules, new TypeReference<Map<String,Rule>>(){});
                    } catch (JsonProcessingException e) {
                        throw new RuntimeException(e);
                    }
                });
        flattenRules = new HashMap<String, Map<String, Rule>>();
        rules
                .entrySet()
                .stream()
                .forEach(entry -> {
                    addToFlattenRules(entry.getKey(), entry.getValue());
                });
    }

    private void addToFlattenRules(String key, Rule rule) {
        if (flattenRules.containsKey(key))
            throw new RuntimeException("There is rule alredy registered: " + key);
        Map<String, Rule>flattenRulesChild = new HashMap<String, Rule>();
        flattenRulesChild.put(key, rule);
        flattenRules.put(key, flattenRulesChild);

        Optional.ofNullable(rule.getChilds())
                .orElse(Collections.emptyMap())
                .entrySet()
                .stream()
                .forEach(entry -> addToFlattenRulesChild(flattenRulesChild, entry.getKey(), entry.getValue()));
    }

    private void addToFlattenRulesChild(Map<String, Rule>flattenRulesChild, String key, Rule rule) {
        flattenRulesChild.put(key, rule);
        Optional.ofNullable(rule.getChilds())
                .orElse(Collections.emptyMap())
                .entrySet()
                .stream()
                .forEach(entry -> addToFlattenRulesChild(flattenRulesChild, entry.getKey(), entry.getValue()));
    }

    public Rule getRule(Optional<String> rootRule, Optional<String> ruleName) {
        if(!ruleName.isPresent()) {
            return Optional.ofNullable(flattenRules.get(rootRule.orElse(defaultRule)))
                    .orElseGet(() -> flattenRules.get(defaultRule))
                    .get(rootRule.orElse(defaultRule));
        } else {
            return Optional.ofNullable(flattenRules.get(rootRule.orElse(defaultRule)))
                    .orElseGet(() -> flattenRules.get(defaultRule))
                    .get(ruleName.orElseThrow(RuleNotFoundException::new));
        }
    }

    public Rule getRootRule() {
        return getRule(Optional.of(defaultRule), Optional.empty());
    }

}
