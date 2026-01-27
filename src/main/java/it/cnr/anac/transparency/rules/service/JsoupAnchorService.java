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
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.parser.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@Slf4j
public class JsoupAnchorService implements AnchorService{
    @Autowired
    RuleConfiguration ruleConfiguration;

    @Override
    public List<Anchor> find(String content, boolean allTags) {
        if (Optional.ofNullable(content)
                .map(String::toUpperCase)
                .filter(s -> s.contains("HTML"))
                .isEmpty()) {
            log.warn("Content '{}' .... is not HTML Page!", content.substring(0, Math.min(50, content.length())));
            return Collections.emptyList();
        }
        Document doc = Jsoup.parse(content);
        if (allTags) {
            return doc.getAllElements()
                    .stream()
                    .map(this::convert)
                    .flatMap(List::stream)
                    .collect(Collectors.toList());
        }
        return doc.getElementsByTag(AnchorService.ANCHOR)
                .stream()
                .map(this::convert)
                .flatMap(List::stream)
                .collect(Collectors.toList());
    }

    private List<Anchor> convert(Element element) {
        final String href = Optional.of(element.attr(AnchorService.HREF)).filter(s -> !s.trim().isEmpty()).orElse("#");
        if (!element.tag().getName().equalsIgnoreCase(AnchorService.ANCHOR)) {
            final List<Anchor> firstList = List.of(
                    new Anchor(href, element.text(), "text")
            );
            return firstList
                    .stream()
                    .filter(anchor -> Optional.ofNullable(anchor.getContent()).isPresent())
                    .collect(Collectors.toList());
        } else {
            final List<Anchor> firstList = Arrays.asList(
                    new Anchor(href, element.text(), isProbablyVisible(element) ? "text" : "text:none"),
                    new Anchor(href, Optional.ofNullable(element.parent())
                            .map(Element::text)
                            .orElse(null), "text::parent")
            );
            final List<Anchor> secondList = ruleConfiguration.getTagAttributes().stream().map(s -> {
                return new Anchor(href, Optional.of(element)
                        .flatMap(element1 -> Optional.of(element1.attributes().get(s)))
                        .orElse(null), "attribute::" + s);
            }).toList();
            final List<Anchor> thirdList = ruleConfiguration.getTagAttributes().stream().map(s -> {
                return new Anchor(href, Optional.ofNullable(element.parent())
                        .flatMap(element1 -> Optional.of(element1.attributes().get(s)))
                        .orElse(null), "attribute::parent::" + s);
            }).toList();
            return Stream.concat(
                    Stream.concat(
                            firstList.stream().filter(anchor -> Optional.ofNullable(anchor.getContent()).isPresent()),
                            secondList.stream().filter(anchor -> Optional.ofNullable(anchor.getContent()).isPresent())
                    ),
                    thirdList.stream().filter(anchor -> Optional.ofNullable(anchor.getContent()).isPresent())
            ).collect(Collectors.toList());
        }
    }

    private boolean isProbablyVisible(Element el) {
        if (el == null) return false;

        // 1. Attributo hidden
        if (el.hasAttr("hidden")) {
            return false;
        }

        // 2. Analizza lo stile inline
        String style = el.attr("style").toLowerCase();
        if (style.contains("display:none")
                || style.contains("visibility:hidden")
                || style.contains("visibility:collapse")
                || style.contains("opacity:0")
                || style.contains("height:0")
                || style.contains("width:0")) {
            return false;
        }

        // 3. Classi tipiche usate per nascondere elementi
        String cls = el.className().toLowerCase();
        String[] hiddenClasses = {
                "hidden", "invisible", "sr-only", "d-none", "visually-hidden"
        };

        for (String c : hiddenClasses) {
            if (cls.contains(c)) {
                return false;
            }
        }

        // Se nessun indicatore di invisibilità è stato trovato
        return true;
    }
}
