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

package it.cnr.anac.transparency.rules.search;

import it.cnr.anac.transparency.rules.domain.Anchor;
import lombok.extern.slf4j.Slf4j;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StoredField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.*;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.ByteBuffersDirectory;
import org.apache.lucene.util.BytesRef;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Slf4j
public class LuceneSearch {

    public static final String URL = "url";
    public static final String CONTENT = "content";
    public static final String WHERE = "where";
    private final IndexSearcher dirSearcher;

    private final CustomTokenizerAnalyzer customTokenizerAnalyzer;

    Comparator<LuceneResult> compareLuceneResult = Comparator
            .comparing(LuceneResult::getScore)
            .reversed()
            .thenComparing((luceneResult, t1) -> Integer.valueOf(luceneResult.getUrl().length()).compareTo(t1.getUrl().length()) * -1);

    public LuceneSearch(List<Anchor> values, CustomTokenizerAnalyzer customTokenizerAnalyzer) throws IOException {
        this.customTokenizerAnalyzer = customTokenizerAnalyzer;
        ByteBuffersDirectory directory = new ByteBuffersDirectory();
        try (IndexWriter directoryWriter = new IndexWriter(directory, new IndexWriterConfig(this.customTokenizerAnalyzer))) {
            values
                    .stream()
                    .filter(anchor -> Optional.ofNullable(anchor.getHref()).filter(s -> !s.trim().isEmpty()).isPresent())
                    .filter(anchor -> Optional.ofNullable(anchor.getContent()).filter(s -> s.trim().length() > 0).isPresent())
                    .forEach(anchor -> {
                        Document doc = new Document();
                        doc.add(new StoredField(URL, anchor.getHref()));
                        doc.add(new TextField(CONTENT, anchor.getContent(), Field.Store.YES));
                        doc.add(new TextField(WHERE, anchor.getWhere(), Field.Store.YES));
                        try {
                            directoryWriter.addDocument(doc);
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    });
        }
        DirectoryReader indexReader = DirectoryReader.open(directory);
        dirSearcher = new IndexSearcher(indexReader);
        getTokensForField(indexReader, CONTENT);
    }

    private void getTokensForField(IndexReader reader, String fieldName) throws IOException {
        List<LeafReaderContext> list = reader.leaves();

        for (LeafReaderContext lrc : list) {
            Terms terms = lrc.reader().terms(fieldName);
            if (terms != null) {
                TermsEnum termsEnum = terms.iterator();

                BytesRef term;
                while ((term = termsEnum.next()) != null) {
                    System.out.println(term.utf8ToString());
                }
            }
        }
    }
    public Optional<LuceneResult> search(String keyword) throws ParseException, IOException {
        QueryParser parser = new QueryParser(CONTENT, this.customTokenizerAnalyzer);
        parser.setDefaultOperator(QueryParser.Operator.AND);
        Query query = parser.parse(keyword);
        TopDocs topDocs = dirSearcher.search(query, 10);
        return Arrays.stream(topDocs.scoreDocs).map(scoreDoc -> {
                    try {
                        final Document doc = dirSearcher.doc(scoreDoc.doc);
                        log.debug("Search document for \"{}\" and find \"{}\" width score: {}", keyword, doc.get(LuceneSearch.CONTENT), scoreDoc.score);
                        return new LuceneResult(doc.get(LuceneSearch.URL), doc.get(LuceneSearch.CONTENT), doc.get(LuceneSearch.WHERE), scoreDoc.score);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                })
                .sorted(compareLuceneResult)
                .findFirst();
    }
}
