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

package it.cnr.anac.transparency.rules.service;

import it.cnr.anac.transparency.rules.configuration.RuleConfiguration;
import it.cnr.anac.transparency.rules.domain.Anchor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.text.StringEscapeUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
public class RegularExpressionAnchorService implements AnchorService{
    @Autowired
    RuleConfiguration ruleConfiguration;
    @Override
    public List<Anchor> find(String content, boolean allTags) {
        content = content.replaceAll("\\s+", " ");
        final Pattern patternAnchor = Pattern.compile(ruleConfiguration.getAnchorRegex(), Pattern.CASE_INSENSITIVE);
        final Pattern patternHref = Pattern.compile(ruleConfiguration.getHrefRegex(), Pattern.CASE_INSENSITIVE);
        Matcher matcher = patternAnchor.matcher(content);
        List<Anchor> result = new ArrayList<>();
        while (matcher.find()) {
            final String attributes = matcher.group(1);
            final Matcher matcherHref = patternHref.matcher(attributes);
            matcherHref.find();
            try {
                final String href = matcherHref.group(HREF);
                final String text = matcher.group(TEXT);
                result.add(Anchor.newInstance(href, StringEscapeUtils.unescapeHtml4(text), "text"));
                result.add(Anchor.newInstance(
                        href,
                        Optional.ofNullable(StringEscapeUtils.unescapeHtml4(
                                text.replaceAll("\\<[^>]*>","")
                        )).map(String::trim).orElse(""),
                        "text"
                ));
                log.debug("Find anchor width href: {} and text: {}", href, text);
            } catch (IllegalStateException _ex) {
                log.debug("No match found in attributes: {}", attributes);
            }
        }
        return result;
    }
}
