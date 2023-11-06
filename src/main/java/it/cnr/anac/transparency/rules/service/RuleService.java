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

package it.cnr.anac.transparency.rules.service;

import it.cnr.anac.transparency.rules.configuration.RuleConfiguration;
import it.cnr.anac.transparency.rules.domain.Rule;
import it.cnr.anac.transparency.rules.domain.RuleResponse;
import it.cnr.anac.transparency.rules.exception.RuleNotFoundException;
import it.cnr.anac.transparency.rules.util.LuceneSearch;
import lombok.extern.slf4j.Slf4j;
import org.apache.lucene.queryparser.classic.ParseException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@Slf4j
public class RuleService {
    public static final String HREF = "href";
    public static final String TEXT = "text";
    @Autowired
    RuleConfiguration ruleConfiguration;

    private Map<String, String> anchor(String content) {
        Pattern pattern = Pattern.compile(ruleConfiguration.getAnchorExpression());
        Matcher matcher = pattern.matcher(content);
        Map<String, String> result = new HashMap<>();
        while (matcher.find()) {
            final String href = matcher.group(HREF);
            final String text = matcher.group(TEXT);
            result.put(href, text);
            log.debug("Find anchor width heref: {} and text: {}", href, text);
        }
        return result;
    }

    public RuleResponse executeRule(String content, Optional<String> ruleName) throws RuleNotFoundException, IOException {
        final Rule rule = ruleName
                .flatMap(s -> Optional.ofNullable(ruleConfiguration.getRule(s)))
                .orElseGet(() -> ruleConfiguration.getRootRule());
        final Map<String, String> maps = anchor(content);
        LuceneSearch luceneSearch = new LuceneSearch(maps);
        return findTermInValues(luceneSearch, maps, ruleName, rule);
    }

    public List<RuleResponse> executeChildRule(String content, Optional<String> ruleName) throws RuleNotFoundException, IOException {
        final Map<String, Rule> childs = ruleName
                .flatMap(s -> Optional.ofNullable(ruleConfiguration.getRule(s)))
                .orElseGet(() -> ruleConfiguration.getRootRule())
                .getChilds();
        final Map<String, String> maps = anchor(content);
        LuceneSearch luceneSearch = new LuceneSearch(maps);
        return childs.entrySet()
                .stream()
                .map(entry -> {
                    try {
                        return findTermInValues(luceneSearch, maps, Optional.of(entry.getKey()), entry.getValue());
                    } catch (RuleNotFoundException _ex) {
                        return new RuleResponse(
                                null,
                                entry.getKey(),
                                Optional.ofNullable(entry.getValue().getChilds()).map(c -> c.isEmpty()).orElse(Boolean.TRUE),
                                HttpStatus.NOT_FOUND
                        );
                    }
                })
                .collect(Collectors.toList());
    }

    private RuleResponse findTermInValues(LuceneSearch luceneSearch, Map<String, String> maps, Optional<String> ruleName, Rule rule) throws RuleNotFoundException {
        try {
            final Optional<org.apache.lucene.document.Document> document = luceneSearch.search(rule.getTerm());
            if (document.isPresent()) {
                log.debug("Term {} - find {} URL: {}", rule.getTerm(),
                        document.get().getField(LuceneSearch.CONTENT), document.get().get(LuceneSearch.URL));
                return new RuleResponse(
                        document.get().get(LuceneSearch.URL),
                        ruleName.orElse(ruleConfiguration.getRoot_rule()),
                        Optional.ofNullable(rule.getChilds()).map(c -> c.isEmpty()).orElse(Boolean.TRUE),
                        HttpStatus.OK
                );
            }
            throw new RuleNotFoundException();
        } catch (IOException | ParseException e) {
            throw new RuleNotFoundException();
        }
    }
}
