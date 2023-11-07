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
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
@Slf4j
@Service
@Profile("regular-expression")
public class RegularExpressionAnchorService implements AnchorService{
    @Autowired
    RuleConfiguration ruleConfiguration;
    @Override
    public List<Anchor> find(String content) {
        Pattern pattern = Pattern.compile(ruleConfiguration.getAnchorExpression(), Pattern.UNICODE_CHARACTER_CLASS);
        Matcher matcher = pattern.matcher(content);
        List<Anchor> result = new ArrayList<>();
        while (matcher.find()) {
            final String href = matcher.group(HREF);
            final String text = matcher.group(TEXT);
            result.add(Anchor.newInstance(href, text));
            log.debug("Find anchor width href: {} and text: {}", href, text);
        }
        return result;
    }
}
