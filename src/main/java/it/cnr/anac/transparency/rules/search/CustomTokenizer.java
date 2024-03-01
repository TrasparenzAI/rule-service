/*
 *  Copyright (C) 2024 Consiglio Nazionale delle Ricerche
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

package it.cnr.anac.transparency.rules.search;

import it.cnr.anac.transparency.rules.configuration.RuleConfiguration;
import org.apache.lucene.analysis.util.CharTokenizer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

public class CustomTokenizer extends CharTokenizer {
    private final List<Character> searchTokens;

    public CustomTokenizer(List<Character> searchTokens) {
        this.searchTokens = searchTokens;
    }

    protected boolean isTokenChar(int c) {
        return !(
                searchTokens
                    .stream()
                    .filter(character -> character == c)
                    .findAny()
                    .isPresent() ||
                    Character.isDigit(c)
        );
    }
}
