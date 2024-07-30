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
import it.cnr.anac.transparency.rules.exception.RuleException;
import it.cnr.anac.transparency.rules.exception.RuleNotFoundException;
import it.cnr.anac.transparency.rules.service.RuleService;
import it.cnr.anac.transparency.rules.v1.controller.RuleController;
import it.cnr.anac.transparency.rules.v1.dto.RuleResponseDto;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@SpringBootTest
class RuleApplicationTests {
	private static final String TIPOLOGIE_PROCEDIMENTO = "tipologie-procedimento";
	public static final String AMMINISTRAZIONE1_URL = "https://www.cnr.it";
	public static final int TIMEOUT_MILLIS = 10000;
	@Autowired
	RuleConfiguration ruleConfiguration;
	@Autowired
	RuleService ruleService;
	@Autowired
	RuleController ruleController;

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
				22,
				Optional.ofNullable(flattenRules.get(ruleConfiguration.getRulesRoot()))
						.flatMap(rule -> Optional.ofNullable(rule.getChilds()))
						.map(Map::size)
						.orElse(0)

		);
	}

	@Test
	void blank() throws IOException, URISyntaxException {
		Assertions.assertThrows(RuleNotFoundException.class, () -> {
			final InputStream resourceAsStream = this.getClass().getResourceAsStream("/blank.html");
			ruleService.executeRule(new BufferedReader(
					new InputStreamReader(resourceAsStream, StandardCharsets.UTF_8))
					.lines()
					.collect(Collectors.joining("\n")), Optional.empty());
		});
	}

	@Test
	void local() throws IOException, URISyntaxException, RuleException {
		Assertions.assertThrows(RuleNotFoundException.class, () -> {
			final InputStream resourceAsStream = this.getClass().getResourceAsStream("/amministrazione.html");
			final RuleResponse ruleResponse = ruleService.executeRule(new BufferedReader(
					new InputStreamReader(resourceAsStream, StandardCharsets.UTF_8))
					.lines()
					.collect(Collectors.joining("\n")), Optional.empty());
		});
	}
	@Test
	void localUnderscore() throws IOException, URISyntaxException, RuleException {
		final InputStream resourceAsStream = this.getClass().getResourceAsStream("/amministrazione_.html");
		final RuleResponse ruleResponse = ruleService.executeRule(new BufferedReader(
				new InputStreamReader(resourceAsStream, StandardCharsets.UTF_8))
				.lines()
				.collect(Collectors.joining("\n")), Optional.empty());
		Assertions.assertEquals("/", ruleResponse.getUrl());
	}
	@Test
	void dotontext() throws IOException, URISyntaxException, RuleException {
		final InputStream resourceAsStream = this.getClass().getResourceAsStream("/amministrazione_dotontext.html");
		final RuleResponse ruleResponse = ruleService.executeRule(new BufferedReader(
				new InputStreamReader(resourceAsStream, StandardCharsets.UTF_8))
				.lines()
				.collect(Collectors.joining("\n")), Optional.empty());
		Assertions.assertEquals(HttpStatus.OK, ruleResponse.getStatus());
		Assertions.assertEquals("/", ruleResponse.getUrl());
	}
	@Test
	void href() throws IOException, URISyntaxException, RuleException {
		Assertions.assertThrows(RuleNotFoundException.class, () -> {
			final InputStream resourceAsStream = this.getClass().getResourceAsStream("/amministrazione_href.html");
			ruleService.executeRule(new BufferedReader(
					new InputStreamReader(resourceAsStream, StandardCharsets.UTF_8))
					.lines()
					.collect(Collectors.joining("\n")), Optional.empty());
		});
	}

	@Test
	void local2() throws IOException, URISyntaxException, RuleException {
		final InputStream resourceAsStream = this.getClass().getResourceAsStream("/amministrazione2.html");
		final RuleResponse ruleResponse = ruleService.executeRule(new BufferedReader(
				new InputStreamReader(resourceAsStream, StandardCharsets.UTF_8))
				.lines()
				.collect(Collectors.joining("\n")), Optional.empty());
		Assertions.assertEquals(ruleResponse.getStatus(), HttpStatus.OK);
		Assertions.assertEquals("/prova-apici-singoli4", ruleResponse.getUrl());
	}

	@Test
	void local4() throws IOException, URISyntaxException, RuleException {
		final InputStream resourceAsStream = this.getClass().getResourceAsStream("/amministrazione4.html");
		final RuleResponse ruleResponse = ruleService.executeRule(new BufferedReader(
				new InputStreamReader(resourceAsStream, StandardCharsets.UTF_8))
				.lines()
				.collect(Collectors.joining("\n")), Optional.of("rendiconti-gruppi-consiliari-regionali-provinciali"));
		Assertions.assertEquals(ruleResponse.getStatus(), HttpStatus.OK);
		Assertions.assertEquals("https://cosenza.etrasparenza.it/pagina710_rendiconti-gruppi-consiliari-regionaliprovinciali.html", ruleResponse.getUrl());
	}

	@Test
	void local5() throws IOException, URISyntaxException, RuleException {
		final InputStream resourceAsStream = this.getClass().getResourceAsStream("/amministrazione5.html");
		final RuleResponse ruleResponse = ruleService.executeRule(new BufferedReader(
				new InputStreamReader(resourceAsStream, StandardCharsets.UTF_8))
				.lines()
				.collect(Collectors.joining("\n")), Optional.of("provvedimenti-organi-indirizzo-politico"));
		Assertions.assertEquals(ruleResponse.getStatus(), HttpStatus.OK);
		Assertions.assertEquals("https://cosenza.etrasparenza.it/pagina725_provvedimenti-organi-indirizzo-politico.html", ruleResponse.getUrl());
	}

	@Test
	void local3() throws IOException, URISyntaxException, RuleException {
		final InputStream resourceAsStream = this.getClass().getResourceAsStream("/amministrazione3.html");
		final RuleResponse ruleResponse = ruleService.executeRule(new BufferedReader(
				new InputStreamReader(resourceAsStream, StandardCharsets.UTF_8))
				.lines()
				.collect(Collectors.joining("\n")), Optional.empty());
		Assertions.assertEquals("/amministrazione-trasparente", ruleResponse.getUrl());
	}

	List<RuleResponseDto> internalChild(InputStream resourceAsStream, int expected) throws IOException, URISyntaxException {
		final ResponseEntity<List<RuleResponseDto>> ruleResponses = ruleController.postChild(Base64.getEncoder().encodeToString(new BufferedReader(
				new InputStreamReader(resourceAsStream, StandardCharsets.UTF_8))
				.lines()
				.collect(Collectors.joining("\n")).getBytes(StandardCharsets.UTF_8)), Optional.empty());

		Assertions.assertEquals(22, ruleResponses.getBody().size());
		Assertions.assertEquals(
				expected,
				ruleResponses.getBody().stream().filter(r -> !Optional.ofNullable(r.getUrl()).isPresent()).collect(Collectors.toList()).size(),
				"The rules not satisfied are: " + String.join(",", ruleResponses.getBody().stream().filter(r -> !Optional.ofNullable(r.getUrl()).isPresent())
						.map(RuleResponseDto::getRuleName)
						.collect(Collectors.toList()))

		);
		return ruleResponses.getBody();
	}
	@Test
	void localChild3() throws IOException, URISyntaxException {
		internalChild(this.getClass().getResourceAsStream("/amministrazione_child1.html"), 0);
	}
	@Test
	void localChild4() throws IOException, URISyntaxException {
		internalChild(this.getClass().getResourceAsStream("/amministrazione_child2.html"), 1);
	}
	@Test
	void localChild5() throws IOException, URISyntaxException {
		final List<RuleResponseDto> ruleResponseDtos = internalChild(this.getClass().getResourceAsStream("/amministrazione_child3.html"), 2);
		Assertions.assertEquals(20, ruleResponseDtos.stream().filter(ruleResponseDto -> ruleResponseDto.getStatus() == 200).count());
	}

	@Test
	void localChild6() throws IOException, URISyntaxException {
		final ResponseEntity<List<RuleResponseDto>> ruleResponses = ruleController.postChild(Base64.getEncoder().encodeToString(new BufferedReader(
				new InputStreamReader(this.getClass().getResourceAsStream("/amministrazione_child4.html"), StandardCharsets.UTF_8))
				.lines()
				.collect(Collectors.joining("\n")).getBytes(StandardCharsets.UTF_8)), Optional.of("disposizioni-generali"));
		Assertions.assertEquals(3, ruleResponses.getBody().size());
		Assertions.assertEquals(2, ruleResponses.getBody().stream().filter(ruleResponseDto -> ruleResponseDto.getStatus() == 200).count());
	}

	@Test
	void localChild7() throws IOException, URISyntaxException {
		final ResponseEntity<List<RuleResponseDto>> ruleResponses = ruleController.postChild(Base64.getEncoder().encodeToString(new BufferedReader(
				new InputStreamReader(this.getClass().getResourceAsStream("/amministrazione_child5.html"), StandardCharsets.UTF_8))
				.lines()
				.collect(Collectors.joining("\n")).getBytes(StandardCharsets.UTF_8)), Optional.of("personale"));
		Assertions.assertEquals(12, ruleResponses.getBody().size());
		Assertions.assertEquals(12, ruleResponses.getBody().stream().filter(ruleResponseDto -> ruleResponseDto.getStatus() == 200).count());
	}

	@Test
	void localChild8() throws IOException, URISyntaxException {
		final ResponseEntity<List<RuleResponseDto>> ruleResponses = ruleController.postChild(Base64.getEncoder().encodeToString(new BufferedReader(
				new InputStreamReader(this.getClass().getResourceAsStream("/amministrazione_child6.html"), StandardCharsets.UTF_8))
				.lines()
				.collect(Collectors.joining("\n")).getBytes(StandardCharsets.UTF_8)), Optional.of("servizi-erogati"));
		Assertions.assertEquals(5, ruleResponses.getBody().size());
		Assertions.assertEquals(2, ruleResponses.getBody().stream().filter(ruleResponseDto -> ruleResponseDto.getStatus() == 200).count());
	}

	@Test
	void localChild9() throws IOException, URISyntaxException {
		final ResponseEntity<List<RuleResponseDto>> ruleResponses = ruleController.postChild(Base64.getEncoder().encodeToString(new BufferedReader(
				new InputStreamReader(this.getClass().getResourceAsStream("/amministrazione_child7.html"), StandardCharsets.UTF_8))
				.lines()
				.collect(Collectors.joining("\n")).getBytes(StandardCharsets.UTF_8)), Optional.of("consulenti-collaboratori"));
		Assertions.assertEquals(1, ruleResponses.getBody().size());
		Assertions.assertEquals(1, ruleResponses.getBody().stream().filter(ruleResponseDto -> ruleResponseDto.getStatus() == 200).count());
	}

	@Test
	void amministrazione1() throws IOException, URISyntaxException, RuleException {
		Document doc = Jsoup.parse(new URL(AMMINISTRAZIONE1_URL), TIMEOUT_MILLIS);
		final RuleResponse ruleResponse = ruleService.executeRule(doc.html(), Optional.empty());

		Document doc2 = Jsoup.parse(getURL(ruleResponse.getUrl(), AMMINISTRAZIONE1_URL), TIMEOUT_MILLIS);
		final List<RuleResponse> ruleResponse2 = ruleService.executeChildRule(doc2.html(), Optional.of(ruleResponse.getRuleName()));
		Assertions.assertEquals(22, ruleResponse2.size());
		Assertions.assertEquals(
				0,
				ruleResponse2.stream().filter(r -> !Optional.ofNullable(r.getUrl()).isPresent()).collect(Collectors.toList()).size(),
				"The rules not satisfied are: " + String.join(",", ruleResponse2.stream().filter(r -> !Optional.ofNullable(r.getUrl()).isPresent())
						.map(RuleResponse::getRuleName)
						.collect(Collectors.toList()))

		);
		ruleResponse2.stream().filter(ruleResponse1 -> !ruleResponse1.getLeaf()).forEach(r -> {
			try {
				final List<RuleResponse> ruleResponse3 = ruleService.executeChildRule(doc2.html(), Optional.of(r.getRuleName()));
				Assertions.assertEquals(Boolean.FALSE, ruleResponse3.isEmpty());
			} catch (IOException e) {
				throw new RuntimeException(e);
			}

		});
	}
}