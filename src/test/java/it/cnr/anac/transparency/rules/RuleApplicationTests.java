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

package it.cnr.anac.transparency.rules;

import it.cnr.anac.transparency.rules.configuration.RuleConfiguration;
import it.cnr.anac.transparency.rules.domain.RuleResponse;
import it.cnr.anac.transparency.rules.domain.Rule;
import it.cnr.anac.transparency.rules.exception.RuleNotFoundException;
import it.cnr.anac.transparency.rules.service.RuleService;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@SpringBootTest
class RuleApplicationTests {
	private static final String TIPOLOGIE_PROCEDIMENTO = "tipologie-procedimento";
	public static final String AMMINISTRAZIONE_URL = "https://www.cnr.it";
	public static final int TIMEOUT_MILLIS = 10000;
	@Autowired
	RuleConfiguration ruleConfiguration;
	@Autowired
	RuleService ruleService;

	boolean isValidURL(String url) throws MalformedURLException, URISyntaxException {
		try {
			new URL(url).toURI();
			return true;
		} catch (MalformedURLException e) {
			return false;
		} catch (URISyntaxException e) {
			return false;
		}
	}

	URL getURL(String url, String base) throws MalformedURLException, URISyntaxException {
		if (isValidURL(url))
			return new URL(url);
		return new URL(base.concat(url));
	}

	@Test
	void contextLoads() {
		final Map<String, Rule> rules = ruleConfiguration.getRules();
		Assertions.assertEquals(Boolean.FALSE, rules.isEmpty());
		final Map<String, Rule> flattenRules = ruleConfiguration.getFlattenRules();
		Assertions.assertEquals(Boolean.FALSE, flattenRules.isEmpty());
		Assertions.assertEquals(Boolean.TRUE, Optional.ofNullable(flattenRules.get(TIPOLOGIE_PROCEDIMENTO)).isPresent());
		Assertions.assertEquals(
				23,
				Optional.ofNullable(flattenRules.get(ruleConfiguration.getRoot_rule()))
						.flatMap(rule -> Optional.ofNullable(rule.getChilds()))
						.map(Map::size)
						.orElse(0)

		);
	}

	@Test
	void blank() throws IOException, URISyntaxException {
		Assertions.assertThrows(RuleNotFoundException.class, () -> {
			Document doc = Jsoup.parse( new File(this.getClass().getResource("/blank.html").toURI()));
			ruleService.executeRule(doc.html(), Optional.empty());
		});
	}

	@Test
	void local() throws IOException, URISyntaxException {
		Document doc = Jsoup.parse( new File(this.getClass().getResource("/amministrazione.html").toURI()));
		final RuleResponse ruleResponse = ruleService.executeRule(doc.html(), Optional.empty());
		Assertions.assertNotNull(ruleResponse.getUrl());
	}

	@Test
	void amministrazione() throws IOException, URISyntaxException {
		Document doc = Jsoup.parse(new URL(AMMINISTRAZIONE_URL), TIMEOUT_MILLIS);
		final RuleResponse ruleResponse = ruleService.executeRule(doc.html(), Optional.empty());

		Document doc2 = Jsoup.parse(getURL(ruleResponse.getUrl(),AMMINISTRAZIONE_URL), TIMEOUT_MILLIS);
		final List<RuleResponse> ruleResponse2 = ruleService.executeChildRule(doc2.html(), Optional.of(ruleResponse.getRuleName()));
		Assertions.assertEquals(23, ruleResponse2.size());
		Assertions.assertEquals(
				0,
				ruleResponse2.stream().filter(r -> !Optional.ofNullable(r.getUrl()).isPresent()).collect(Collectors.toList()).size(),
				"The rules not satisfied are: " + String.join(",", ruleResponse2.stream().filter(r -> !Optional.ofNullable(r.getUrl()).isPresent())
						.map(RuleResponse::getRuleName)
						.collect(Collectors.toList()))

		);
		ruleResponse2.stream().filter(ruleResponse1 -> !ruleResponse1.getIsLeaf()).forEach(r -> {
			try {
				final List<RuleResponse> ruleResponse3 = ruleService.executeChildRule(doc2.html(), Optional.of(r.getRuleName()));
				Assertions.assertEquals(Boolean.FALSE, ruleResponse3.isEmpty());
			} catch (IOException e) {
				throw new RuntimeException(e);
			}

		});
	}
}