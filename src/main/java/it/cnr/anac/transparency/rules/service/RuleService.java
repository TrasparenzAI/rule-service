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
import it.cnr.anac.transparency.rules.domain.Term;
import it.cnr.anac.transparency.rules.exception.RuleException;
import it.cnr.anac.transparency.rules.exception.RuleNotFoundException;
import it.cnr.anac.transparency.rules.search.CustomTokenizer;
import it.cnr.anac.transparency.rules.search.CustomTokenizerAnalyzer;
import it.cnr.anac.transparency.rules.search.LuceneResult;
import it.cnr.anac.transparency.rules.search.LuceneSearch;
import lombok.extern.slf4j.Slf4j;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.queryparser.classic.ParseException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Scope;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

import org.apache.commons.codec.binary.Base64;

@Service
@Slf4j
public class RuleService {
    @Autowired
    RuleConfiguration ruleConfiguration;
    @Autowired
    RegularExpressionAnchorService regularExpressionAnchorService;
    @Autowired
    JsoupAnchorService jsoupAnchorService;

    public String base64Decode(String content) {
        if (Base64.isBase64(content)) {
            return new String(Base64.decodeBase64(content.getBytes(StandardCharsets.UTF_8)), StandardCharsets.UTF_8);
        } else if (content.contains("b'")) {
            return new String(Base64.decodeBase64(content.replace("b'", "").getBytes(StandardCharsets.UTF_8)), StandardCharsets.UTF_8);
        }
        return content;
    }

    private Analyzer createCustomAnalyzer() {
        return new CustomTokenizerAnalyzer(new CustomTokenizer(ruleConfiguration.getSearchTokens()));
    }

    public LuceneSearch createLuceneSearch(List<Anchor> anchors) throws IOException {
        return new LuceneSearch(anchors, createCustomAnalyzer(), ruleConfiguration.getMaxLengthContent());
    }

    public RuleResponse executeRule(Optional<String> ruleName, List<Anchor> anchors) throws RuleNotFoundException, IOException {
        final Rule rule = ruleName
                .flatMap(s -> Optional.ofNullable(ruleConfiguration.getRule(s)))
                .orElseGet(() -> ruleConfiguration.getRootRule());
        log.debug("Founded {} anchor in content for rule {}", anchors.size(), ruleName.orElse("empty"));
        LuceneSearch luceneSearch = createLuceneSearch(anchors);
        return findTermInValues(luceneSearch, ruleName, rule);
    }

    public RuleResponse executeRule(String content, Optional<String> ruleName) throws RuleNotFoundException, IOException, RuleException {
        try {
            return executeRule(ruleName, regularExpressionAnchorService.find(content));
        } catch (RuleNotFoundException _ex) {
            return executeRuleAlternative(content, ruleName);
        }
    }
    public RuleResponse executeRuleAlternative(String content, Optional<String> ruleName) throws RuleNotFoundException, IOException, RuleException {
        return executeRule(ruleName, anchorsWidthJsoup(content));
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
        LuceneSearch luceneSearch = createLuceneSearch(anchors);
        return childs.entrySet()
                .stream()
                .map(entry -> {
                    try {
                        return findTermInValues(luceneSearch, Optional.of(entry.getKey()), entry.getValue());
                    } catch (RuleNotFoundException _ex) {
                        return new RuleResponse(
                                null,
                                entry.getKey(),
                                String.join(",", ruleConfiguration
                                        .getFlattenRules()
                                        .get(entry.getKey())
                                        .getTerm()
                                        .stream()
                                        .map(Term::getKey)
                                        .collect(Collectors.toList())),
                                null,
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
        return executeChildRule(content, ruleName, regularExpressionAnchorService.find(content));
    }

    public List<RuleResponse> executeChildRuleAlternative(String content, Optional<String> ruleName) throws RuleNotFoundException, IOException {
        return executeChildRule(content, ruleName, anchorsWidthJsoup(content));
    }
    private RuleResponse findTermInValues(LuceneSearch luceneSearch, Optional<String> ruleName, Rule rule, Term term) throws RuleNotFoundException {
        try {
            final Optional<LuceneResult> luceneResult = luceneSearch.search(term.getKey());
            if (luceneResult.isPresent()) {
                log.debug("Term {} - find {} URL: {}", rule.getTerm(),
                        luceneResult.get().getContent(), luceneResult.get().getUrl());
                final String r = ruleName.orElse(ruleConfiguration.getRulesRoot());
                return new RuleResponse(
                        luceneResult.get().getUrl(),
                        r,
                        term.getKey(),
                        luceneResult.get().getContent(),
                        luceneResult.get().getWhere(),
                        Optional.ofNullable(rule.getChilds()).map(c -> c.isEmpty()).orElse(Boolean.TRUE),
                        HttpStatus.valueOf(term.getCode()),
                        luceneResult.get().getScore()
                );
            }
            throw new RuleNotFoundException();
        } catch (IOException | ParseException e) {
            throw new RuleNotFoundException();
        }
    }

    private RuleResponse findTermInValues(LuceneSearch luceneSearch, Optional<String> ruleName, Rule rule) throws RuleNotFoundException {
        for (Term term: rule.getTerm()) {
            try {
                return findTermInValues(luceneSearch, ruleName, rule, term);
            } catch (RuleNotFoundException e) {
                log.trace("Term {} not found on rule {}", term, rule);
            }
        }
        throw new RuleNotFoundException();
    }

    private List<Anchor> anchorsWidthJsoup(String content) {
        return jsoupAnchorService.find(content);
    }
}
