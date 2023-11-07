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

package it.cnr.anac.transparency.rules.util;

import it.cnr.anac.transparency.rules.domain.Anchor;
import lombok.extern.slf4j.Slf4j;
import org.apache.lucene.analysis.it.ItalianAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StoredField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.ByteBuffersDirectory;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
public class LuceneSearch {

    public static final String URL = "url";
    public static final String CONTENT = "content";

    private final IndexSearcher dirSearcher;

    public LuceneSearch(List<Anchor> values) throws IOException {
        ByteBuffersDirectory directory = new ByteBuffersDirectory();
        try (IndexWriter directoryWriter = new IndexWriter(directory, new IndexWriterConfig(new ItalianAnalyzer()))) {
            for (Anchor anchor : values) {
                Document doc = new Document();
                doc.add(new StoredField(URL, anchor.getHref()));
                doc.add(new TextField(CONTENT, anchor.getContent(), Field.Store.YES));
                directoryWriter.addDocument(doc);
            }
        }
        DirectoryReader indexReader = DirectoryReader.open(directory);
        dirSearcher = new IndexSearcher(indexReader);
    }

    public Optional<LuceneResult> search(String keyword) throws ParseException, IOException {
        QueryParser parser = new QueryParser(CONTENT, new ItalianAnalyzer());
        Query query = parser.parse(keyword);
        TopDocs topDocs = dirSearcher.search(query, 10);
        return Arrays.stream(topDocs.scoreDocs).map(scoreDoc -> {
            try {
                final Document doc = dirSearcher.doc(scoreDoc.doc);
                log.info("Search document for \"{}\" and find \"{}\" width score: {}", keyword, doc.get(LuceneSearch.CONTENT), scoreDoc.score);
                return new LuceneResult(doc.get(LuceneSearch.URL), doc.get(LuceneSearch.CONTENT), scoreDoc.score);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }).findFirst();
    }

}
