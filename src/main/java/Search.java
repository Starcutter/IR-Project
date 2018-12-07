import org.apache.commons.cli.*;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.cn.smart.SmartChineseAnalyzer;
import org.apache.lucene.analysis.en.EnglishAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexableField;
import org.apache.lucene.queryparser.classic.MultiFieldQueryParser;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.highlight.*;
import org.apache.lucene.search.similarities.BM25Similarity;
import org.apache.lucene.search.similarities.Similarity;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.nio.file.Paths;
import java.util.Scanner;

public class Search {

    private DirectoryReader dirReader = null;
    private IndexSearcher iSearcher = null;

    private Analyzer queryAnalyzer = null;

    private Analyzer enAnalyzer = null;
    private Analyzer cnAnalyzer = null;
    private Analyzer textAnalyzer = null;

    private Similarity similarity = null;

    private Query query = null;
    private TopDocs topDocs = null;

    public Search(Directory indexDir) throws IOException {
        this.dirReader = DirectoryReader.open(indexDir);
        this.iSearcher = new IndexSearcher(this.dirReader);
    }

    public void setEnAnalyzer(Analyzer analyzer) {
        this.enAnalyzer = analyzer;
    }

    public void setCnAnalyzer(Analyzer analyzer) {
        this.cnAnalyzer = analyzer;
    }

    public void setQueryAnalyzer(Analyzer queryAnalyzer) {
        this.queryAnalyzer = queryAnalyzer;
    }

    public void setSimilarity(Similarity similarity) {
        this.similarity = similarity;
    }

    public void search(String queryStr, int n)
            throws IOException, ParseException {
        if (this.queryAnalyzer == null) {
            this.queryAnalyzer = new SmartChineseAnalyzer();
        }
        MultiFieldQueryParser parser = new MultiFieldQueryParser(
                Config.fieldNames, this.queryAnalyzer
        );

        if (this.similarity == null) {
            this.similarity = new BM25Similarity();
        }
        this.iSearcher.setSimilarity(this.similarity);
        System.out.println("Using Similarity: " + this.iSearcher.getSimilarity(true).getClass().getName());

        this.query = parser.parse(queryStr);
        this.topDocs = this.iSearcher.search(query, n);
    }

    private void setTextAnalyzer() {
        if (this.enAnalyzer == null) {
            this.enAnalyzer = new EnglishAnalyzer();
        }
        if (this.cnAnalyzer == null) {
            this.cnAnalyzer = new SmartChineseAnalyzer();
        }
        if (this.textAnalyzer == null) {
            this.textAnalyzer = Config.getAnalyzerWrapper(this.enAnalyzer, this.cnAnalyzer);
        }
    }

    public void printHits() throws IOException, InvalidTokenOffsetsException {
        setTextAnalyzer();
        ScoreDoc[] scoreDocs = this.topDocs.scoreDocs;
        System.out.println("\n======== Results ========");
        for (ScoreDoc scoreDoc : scoreDocs) {
            System.out.println("Score: " + scoreDoc.score);
            Document hitDoc = this.iSearcher.doc(scoreDoc.doc);
            for (IndexableField field : hitDoc.getFields()) {
                String name = field.name();
                String value = field.stringValue();
                String lighted = lightedStr(this.textAnalyzer, this.query, value, name);
                if (lighted != null) {
                    System.out.println(name + ": " + lighted);
                }
                else if (Config.vitalFields.contains(name)) {
                    System.out.println(name + ": " + value);
                }
            }
            System.out.println("=========================");
        }
        System.out.println("");
    }

    private String lightedStr(
            Analyzer analyzer, Query query, String txt, String fieldName
    ) throws IOException, InvalidTokenOffsetsException {
        String str = null;
        QueryScorer queryScorer = new QueryScorer(query);
        Fragmenter fragmenter = new SimpleSpanFragmenter(queryScorer);
        Highlighter highlighter = new Highlighter(queryScorer);
        highlighter.setTextFragmenter(fragmenter);
        str = highlighter.getBestFragment(analyzer, fieldName, txt);
        return str;
    }

    public void close() throws IOException {
        this.dirReader.close();
    }

    public static void main(String[] args) throws IOException {
        Options options = new Options();

        Option indexPathOption = new Option("i", "index", true, "index path");
        indexPathOption.setRequired(true);
        options.addOption(indexPathOption);

        Option topNOption = new Option("n", "top-n", true, "show top n results");
        topNOption.setRequired(false);
        options.addOption(topNOption);

        Option analyzerOption = new Option("a", "q-analyzer", true, "query analyzer");
        analyzerOption.setRequired(false);
        options.addOption(analyzerOption);

        Option enAnalyzerOption = new Option("e", "en-analyzer", true, "English analyzer");
        enAnalyzerOption.setRequired(false);
        options.addOption(enAnalyzerOption);

        Option cnAnalyzerOption = new Option("c", "cn-analyzer", true, "Chinese analyzer");
        cnAnalyzerOption.setRequired(false);
        options.addOption(cnAnalyzerOption);

        Option similarityOption = new Option("s", "similarity", true, "Similarity class to use");
        similarityOption.setRequired(false);
        options.addOption(similarityOption);

        Option mixLMLambdaOption = new Option("l", "lambda", true, "lambda value for MixLMSimilarity");
        mixLMLambdaOption.setRequired(false);
        options.addOption(mixLMLambdaOption);

        CommandLineParser parser = new DefaultParser();
        HelpFormatter formatter = new HelpFormatter();
        CommandLine cmd = null;

        try {
            cmd = parser.parse(options, args);
        } catch (org.apache.commons.cli.ParseException e) {
            System.out.println(e.getMessage());
            formatter.printHelp("Search", options);
            System.exit(1);
        }

        assert cmd != null;
        String indexPath = cmd.getOptionValue("index");
        int topN = 10;
        if (cmd.hasOption("top-n")) {
            topN = Integer.parseInt(cmd.getOptionValue("top-n"));
        }

        Directory indexDir = null;
        Search searcher = null;
        try {
            indexDir = FSDirectory.open(Paths.get(indexPath));
            searcher = new Search(indexDir);
            if (cmd.hasOption("q-analyzer")) {
                String analyzerName = cmd.getOptionValue("q-analyzer");
                Class clazz = Class.forName(analyzerName);
                searcher.setQueryAnalyzer((Analyzer) clazz.newInstance());
            }
            if (cmd.hasOption("en-analyzer")) {
                String enAnalyzerName = cmd.getOptionValue("en-analyzer");
                Class clazz = Class.forName(enAnalyzerName);
                searcher.setEnAnalyzer((Analyzer) clazz.newInstance());
            }
            if (cmd.hasOption("cn-analyzer")) {
                String cnAnalyzerName = cmd.getOptionValue("cn-analyzer");
                Class clazz = Class.forName(cnAnalyzerName);
                searcher.setCnAnalyzer((Analyzer) clazz.newInstance());
            }
            if (cmd.hasOption("similarity")) {
                String similarityName = cmd.getOptionValue("similarity");
                Class clazz = Class.forName(similarityName);
                if (MixLMSimilarity.class.isAssignableFrom(clazz)) {
                    float lambda = 1.0f;
                    if (cmd.hasOption("lambda")) {
                        lambda = Float.parseFloat(cmd.getOptionValue("lambda"));
                    }
                    Class[] paramTypes = {float.class};
                    Object[] params = {lambda};
                    Constructor con = clazz.getConstructor(paramTypes);
                    searcher.setSimilarity((Similarity) con.newInstance(params));
                } else {
                    searcher.setSimilarity((Similarity) clazz.newInstance());
                }
            }

            while (true) {
                System.out.println("Input your query: ");
                Scanner scanner = new Scanner(System.in);
                String queryStr = scanner.nextLine().trim();
                searcher.search(queryStr, topN);
                searcher.printHits();
            }
        } catch (Exception e) {
            e.printStackTrace();
            if (searcher != null) {
                searcher.close();
            }
            if (indexDir != null ){
                indexDir.close();
            }
            System.exit(-1);
        }
    }
}
