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
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.parser.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@Slf4j
public class JsoupAnchorService implements AnchorService{
    @Autowired
    RuleConfiguration ruleConfiguration;

    @Override
    public List<Anchor> find(String content, boolean allTags) {
        if (!Optional.ofNullable(content)
                .map(String::toUpperCase)
                .filter(s -> s.contains("HTML"))
                .isPresent()) {
            log.warn("Content is not HTML Page!");
            return Collections.emptyList();
        }
        Document doc = Jsoup.parse(content);
        if (allTags) {
            return doc.getAllElements()
                    .stream()
                    .filter(element -> !element.tag().equals(Tag.valueOf(AnchorService.ANCHOR)))
                    .map(element -> {
                        final String href = "#";
                        final List<Anchor> firstList = Arrays.asList(
                                new Anchor(href, element.text(),"text")
                        );
                        return firstList
                                .stream()
                                .filter(anchor -> Optional.ofNullable(anchor.getContent()).isPresent())
                                .collect(Collectors.toList());
                    }).flatMap(List::stream).collect(Collectors.toList());
        }
        return doc.getElementsByTag(AnchorService.ANCHOR)
                .stream()
                .map(element -> {
                    final String href = Optional.ofNullable(element.attr(AnchorService.HREF)).filter(s -> s.trim().length() > 0).orElse("#");
                    final List<Anchor> firstList = Arrays.asList(
                            new Anchor(href, "text", element.text()),
                            new Anchor(href, Optional.ofNullable(element.parent())
                                    .map(Element::text)
                                    .orElse(null), "text::parent")
                    );
                    final List<Anchor> secondList = ruleConfiguration.getTagAttributes().stream().map(s -> {
                        return new Anchor(href, Optional.ofNullable(element)
                                .flatMap(element1 -> Optional.ofNullable(element1.attributes().get(s)))
                                .orElse(null), "attribute::" + s);
                    }).collect(Collectors.toList());
                    final List<Anchor> thirdList = ruleConfiguration.getTagAttributes().stream().map(s -> {
                        return new Anchor(href, Optional.ofNullable(element.parent())
                                .flatMap(element1 -> Optional.ofNullable(element1.attributes().get(s)))
                                .orElse(null), "attribute::parent::" + s);
                    }).collect(Collectors.toList());
                    return Stream.concat(
                            Stream.concat(
                                    firstList.stream().filter(anchor -> Optional.ofNullable(anchor.getContent()).isPresent()),
                                    secondList.stream().filter(anchor -> Optional.ofNullable(anchor.getContent()).isPresent())
                            ),
                            thirdList.stream().filter(anchor -> Optional.ofNullable(anchor.getContent()).isPresent())
                    ).collect(Collectors.toList());
                }).flatMap(List::stream).collect(Collectors.toList());
    }
}
