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
import it.cnr.anac.transparency.rules.domain.Anchor;
import it.cnr.anac.transparency.rules.domain.Rule;
import it.cnr.anac.transparency.rules.domain.RuleResponse;
import it.cnr.anac.transparency.rules.exception.RuleNotFoundException;
import it.cnr.anac.transparency.rules.util.LuceneResult;
import it.cnr.anac.transparency.rules.util.LuceneSearch;
import lombok.extern.slf4j.Slf4j;
import org.apache.lucene.queryparser.classic.ParseException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
public class RuleService {
    @Autowired
    RuleConfiguration ruleConfiguration;
    @Autowired
    AnchorService anchorService;

    public RuleResponse executeRule(String content, Optional<String> ruleName) throws RuleNotFoundException, IOException {
        final Rule rule = ruleName
                .flatMap(s -> Optional.ofNullable(ruleConfiguration.getRule(s)))
                .orElseGet(() -> ruleConfiguration.getRootRule());
        final List<Anchor> anchors = anchorService.find(content);
        log.debug("Find {} anchor in content", anchors.size());
        LuceneSearch luceneSearch = new LuceneSearch(anchors);
        return findTermInValues(luceneSearch, ruleName, rule);
    }

    public List<RuleResponse> executeChildRule(String content, Optional<String> ruleName) throws RuleNotFoundException, IOException {
        final Map<String, Rule> childs = ruleName
                .flatMap(s -> Optional.ofNullable(ruleConfiguration.getRule(s)))
                .orElseGet(() -> ruleConfiguration.getRootRule())
                .getChilds();
        final List<Anchor> anchors = anchorService.find(content);
        log.debug("Find {} anchor in content", anchors.size());
        LuceneSearch luceneSearch = new LuceneSearch(anchors);
        return childs.entrySet()
                .stream()
                .map(entry -> {
                    try {
                        return findTermInValues(luceneSearch, Optional.of(entry.getKey()), entry.getValue());
                    } catch (RuleNotFoundException _ex) {
                        return new RuleResponse(
                                null,
                                entry.getKey(),
                                Optional.ofNullable(entry.getValue().getChilds()).map(c -> c.isEmpty()).orElse(Boolean.TRUE),
                                HttpStatus.NOT_FOUND,
                                null
                        );
                    }
                })
                .collect(Collectors.toList());
    }

    private RuleResponse findTermInValues(LuceneSearch luceneSearch, Optional<String> ruleName, Rule rule) throws RuleNotFoundException {
        try {
            final Optional<LuceneResult> luceneResult = luceneSearch.search(rule.getTerm());
            if (luceneResult.isPresent()) {
                log.debug("Term {} - find {} URL: {}", rule.getTerm(),
                        luceneResult.get().getContent(), luceneResult.get().getUrl());
                return new RuleResponse(
                        luceneResult.get().getUrl(),
                        ruleName.orElse(ruleConfiguration.getRoot_rule()),
                        Optional.ofNullable(rule.getChilds()).map(c -> c.isEmpty()).orElse(Boolean.TRUE),
                        HttpStatus.OK,
                        luceneResult.get().getScore()
                );
            }
            throw new RuleNotFoundException();
        } catch (IOException | ParseException e) {
            throw new RuleNotFoundException();
        }
    }
}
