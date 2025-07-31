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

package it.cnr.anac.transparency.rules.domain;

import lombok.*;
import org.springframework.http.HttpStatus;

import java.util.List;

@Getter
@Setter
public class RuleResponse {
    private String url;
    private String ruleName;
    private String term;
    private String content;
    private String where;
    private Boolean leaf;
    private HttpStatus status;
    private Float score;
    private List<RuleResponse> multiple;

    public RuleResponse(String url, String ruleName, String term, String content, String where, Boolean leaf, HttpStatus status, Float score) {
        this.url = url;
        this.ruleName = ruleName;
        this.term = term;
        this.content = content;
        this.where = where;
        this.leaf = leaf;
        this.status = status;
        this.score = score;
    }

    public RuleResponse(List<RuleResponse> multiple) {
        this.multiple = multiple;
    }
}
