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

package it.cnr.anac.transparency.rules.configuration;

import it.cnr.anac.transparency.rules.domain.Rule;
import it.cnr.anac.transparency.rules.exception.RuleNotFoundException;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Configuration
@ConfigurationProperties
@Getter
@Setter
public class RuleConfiguration {
    @Value("${rules_root}")
    protected String root_rule;
    @Value("${anchor_expression}")
    protected String anchorExpression;
    protected Map<String, Rule> rules;
    protected Map<String, Rule> flattenRules;

    @PostConstruct
    public void postConstruct() {
        flattenRules = new HashMap<String, Rule>();
        rules
                .entrySet()
                .stream()
                .forEach(entry -> {
                    addToFlattenRules(entry.getKey(), entry.getValue());
                });
    }

    private void addToFlattenRules(String key, Rule rule) {
        flattenRules.put(key, rule);
        Optional.ofNullable(rule.getChilds())
            .orElse(Collections.emptyMap())
            .entrySet()
            .stream()
            .forEach(entry -> addToFlattenRules(entry.getKey(), entry.getValue()));
    }

    public Rule getRule(String ruleName) {
        return Optional.ofNullable(flattenRules.get(ruleName))
                .orElseThrow(() -> new RuleNotFoundException());
    }

    public Rule getRootRule() {
        return getRule(root_rule);
    }
}
