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

import it.cnr.anac.transparency.rules.domain.Anchor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;
@Service
@Slf4j
@Profile("jsoup")
public class JsoupAnchorService implements AnchorService{
    @Override
    public List<Anchor> find(String content) {
        Document doc = Jsoup.parse(content);
        return doc.getElementsByTag(ANCHOR)
                .stream()
                .peek(element -> {
                    log.debug("Find anchor width href: {} and text: {}", element.attr(HREF), element.text());
                })
                .map(element -> {
                    return new Anchor(element.attr(HREF), element.text());
                }).collect(Collectors.toList());
    }
}
