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
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
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
    @Autowired
    SeleniumAnchorService seleniumAnchorService;

    public RuleResponse executeRule(String content, Optional<String> ruleName, List<Anchor> anchors) throws RuleNotFoundException, IOException {
        final Rule rule = ruleName
                .flatMap(s -> Optional.ofNullable(ruleConfiguration.getRule(s)))
                .orElseGet(() -> ruleConfiguration.getRootRule());
        log.debug("Founded {} anchor in content for rule {}", anchors.size(), ruleName.orElse("empty"));
        LuceneSearch luceneSearch = new LuceneSearch(anchors);
        return findTermInValues(luceneSearch, ruleName, rule);
    }

    public RuleResponse executeRule(String content, Optional<String> ruleName, Optional<String> url) throws RuleNotFoundException, IOException {
        try {
            return executeRule(content, ruleName, anchorService.find(content));
        } catch (RuleNotFoundException _ex) {
            return executeRuleAlternative(content, ruleName, url);
        }
    }
    public RuleResponse executeRuleAlternative(String content, Optional<String> ruleName, Optional<String> url) throws RuleNotFoundException, IOException {
        try {
            return executeRule(content, ruleName, anchorsWidthJsoup(content));
        } catch (RuleNotFoundException _ex) {
            if (url.isPresent())
                return executeRule(content, ruleName, seleniumAnchorService.findAnchor(url.get()));
            throw _ex;
        }
    }

    public Map<String, Rule> childRules(Optional<String> ruleName) {
        return ruleName
                .flatMap(s -> Optional.ofNullable(ruleConfiguration.getRule(s)))
                .orElseGet(() -> ruleConfiguration.getRootRule())
                .getChilds();
    }
    public List<RuleResponse> executeChildRule(String content, Optional<String> ruleName, List<Anchor> anchors) throws RuleNotFoundException, IOException {
        final Map<String, Rule> childs = childRules(ruleName);
        log.debug("Founded {} anchor in content for rule {}", anchors.size(), ruleName.orElse("empty"));
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
                                ruleConfiguration.getFlattenRules().get(entry.getKey()).getTerm(),
                                null,
                                Optional.ofNullable(entry.getValue().getChilds()).map(c -> c.isEmpty()).orElse(Boolean.TRUE),
                                HttpStatus.NOT_FOUND,
                                null
                        );
                    }
                })
                .collect(Collectors.toList());
    }
    public List<RuleResponse> executeChildRule(String content, Optional<String> ruleName) throws RuleNotFoundException, IOException {
        return executeChildRule(content, ruleName, anchorService.find(content));
    }

    public List<RuleResponse> executeChildRuleAlternative(String content, Optional<String> ruleName) throws RuleNotFoundException, IOException {
        return executeChildRule(content, ruleName, anchorsWidthJsoup(content));
    }

    private RuleResponse findTermInValues(LuceneSearch luceneSearch, Optional<String> ruleName, Rule rule) throws RuleNotFoundException {
        try {
            final Optional<LuceneResult> luceneResult = luceneSearch.search(rule.getTerm());
            if (luceneResult.isPresent()) {
                log.debug("Term {} - find {} URL: {}", rule.getTerm(),
                        luceneResult.get().getContent(), luceneResult.get().getUrl());
                final String r = ruleName.orElse(ruleConfiguration.getRoot_rule());
                return new RuleResponse(
                        luceneResult.get().getUrl(),
                        r,
                        ruleConfiguration.getFlattenRules().get(r).getTerm(),
                        luceneResult.get().getContent(),
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

    private List<Anchor> anchorsWidthJsoup(String content) {
        Document doc = Jsoup.parse(content);
        return doc.getElementsByTag(AnchorService.ANCHOR)
                .stream()
                .map(element -> {
                    return new Anchor(element.attr(AnchorService.HREF), element.parent().text());
                }).collect(Collectors.toList());
    }
}
