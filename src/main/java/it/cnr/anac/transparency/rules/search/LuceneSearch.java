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

package it.cnr.anac.transparency.rules.search;

import it.cnr.anac.transparency.rules.domain.Anchor;
import lombok.extern.slf4j.Slf4j;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StoredField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.*;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.*;
import org.apache.lucene.search.similarities.ClassicSimilarity;
import org.apache.lucene.store.ByteBuffersDirectory;
import org.apache.lucene.util.BytesRef;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
public class LuceneSearch {

    public static final String URL = "url";
    public static final String CONTENT = "content";
    public static final String WHERE = "where";
    private final DirectoryReader indexReader;
    private final Analyzer customAnalyzer;

    Comparator<LuceneResultCount> compareLuceneResult = Comparator
            .comparing(LuceneResultCount::getScore)
            .reversed()
            .thenComparing((t1, t2) -> t1.getCount().compareTo(t2.getCount())* -1)
            .thenComparing((t1, t2) -> Integer.valueOf(t1.getLuceneResult().getUrl().length()).compareTo(t2.getLuceneResult().getUrl().length()) * -1);

    public LuceneSearch(List<Anchor> values, Analyzer customAnalyzer, Integer maxLengthContent) throws IOException {
        log.warn("Number of anchor to index is {}", values.size());
        this.customAnalyzer = customAnalyzer;
        ByteBuffersDirectory directory = new ByteBuffersDirectory();
        try (IndexWriter directoryWriter = new IndexWriter(directory, new IndexWriterConfig(this.customAnalyzer))) {
            values
                    .stream()
                    .filter(anchor -> Optional.ofNullable(anchor.getHref()).filter(s -> !s.trim().isEmpty()).isPresent())
                    .filter(anchor -> Optional.ofNullable(anchor.getContent())
                            .filter(s -> !s.trim().isEmpty() && s.trim().length() < maxLengthContent).isPresent())
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
        indexReader = DirectoryReader.open(directory);
        if (log.isTraceEnabled()) {
            getTokensForField(indexReader, CONTENT);
        }
    }

    private void getTokensForField(IndexReader reader, String fieldName) throws IOException {
        List<LeafReaderContext> list = reader.leaves();
        log.trace("============= START TOKEN =============");
        for (LeafReaderContext lrc : list) {
            Terms terms = lrc.reader().terms(fieldName);
            if (terms != null) {
                TermsEnum termsEnum = terms.iterator();
                BytesRef term;
                while ((term = termsEnum.next()) != null) {
                    log.trace(term.utf8ToString());
                }
            }
        }
        log.trace("============= END TOKEN =============");
    }

    public class BinarySimilarity extends ClassicSimilarity {
        @Override
        public float tf(float freq) {
            // Ignora la frequenza: conta solo presenza (1) o assenza (0)
            return freq > 0 ? 1.0f : 0.0f;
        }
    }

    public List<LuceneResult> search(String keyword) throws ParseException, IOException {
        BooleanQuery.Builder builder = new BooleanQuery.Builder();
        IndexSearcher dirSearcher = new IndexSearcher(indexReader);
        dirSearcher.setSimilarity(new BinarySimilarity());

        QueryParser parser = new QueryParser(CONTENT, this.customAnalyzer);
        parser.setDefaultOperator(QueryParser.Operator.AND);
        Query query = parser.parse(keyword);

        // query di match esatto (frase intera con boost)
        PhraseQuery exactPhrase = new PhraseQuery(CONTENT, keyword);
        Query boostedExact = new BoostQuery(exactPhrase, 5.0f);

        // do un peso maggiore a i contenuti presenti nel testo rispetto agli altri
        PhraseQuery exactText = new PhraseQuery(WHERE, "text");
        Query boostedText = new BoostQuery(exactText, 1.0f);

        builder.add(query, BooleanClause.Occur.MUST);
        builder.add(boostedExact, BooleanClause.Occur.SHOULD);
        builder.add(boostedText, BooleanClause.Occur.SHOULD);

        Query finalQuery = builder.build();

        TopDocs topDocs = dirSearcher.search(finalQuery, 10);
        final List<LuceneResult> luceneResults = Arrays.stream(topDocs.scoreDocs).map(scoreDoc -> {
                    try {
                        final Document doc = dirSearcher.getIndexReader().storedFields().document(scoreDoc.doc);
                        log.debug("Search document for \"{}\" and find \"{}\" width score: {} and URL: {}", keyword, doc.get(LuceneSearch.CONTENT), scoreDoc.score, doc.get(LuceneSearch.URL));
                        return new LuceneResult(doc.get(LuceneSearch.URL), doc.get(LuceneSearch.CONTENT), doc.get(LuceneSearch.WHERE), scoreDoc.score);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                })
                .toList().stream().distinct().toList();

        return luceneResults
                .stream()
                .collect(Collectors.collectingAndThen(
                        Collectors.maxBy(Comparator.comparing(LuceneResult::getScore)),
                        max -> luceneResults.stream()
                                .filter(p -> p.getScore().equals(max.get().getScore()))
                                .collect(Collectors.toList())
                ));
    }
}
