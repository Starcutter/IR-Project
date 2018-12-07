import org.apache.commons.cli.*;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.cn.smart.SmartChineseAnalyzer;
import org.apache.lucene.analysis.en.EnglishAnalyzer;
import org.apache.lucene.analysis.miscellaneous.PerFieldAnalyzerWrapper;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.search.similarities.BM25Similarity;
import org.apache.lucene.search.similarities.Similarity;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.nio.file.Paths;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Index {

    private String dataPath;
    private Pattern pattern;

    private Analyzer enAnalyzer = null;
    private Analyzer cnAnalyzer = null;
    private PerFieldAnalyzerWrapper aWrapper = null;

    private Similarity similarity = null;

    public Index(String dataPath) {
        this.dataPath = dataPath;
        this.pattern = Pattern.compile("^<(.*)>=(.*?);*$");
    }

    public void setEnAnalyzer(Analyzer analyzer) {
        this.enAnalyzer = analyzer;
    }

    public void setCnAnalyzer(Analyzer analyzer) {
        this.cnAnalyzer = analyzer;
    }

    public void setSimilarity(Similarity similarity) {
        this.similarity = similarity;
    }

    private void setAnalyzerWrapper() {
        if (this.enAnalyzer == null) {
            this.enAnalyzer = new EnglishAnalyzer();
        }
        if (this.cnAnalyzer == null) {
            this.cnAnalyzer = new SmartChineseAnalyzer();
        }
        if (this.aWrapper == null) {
            this.aWrapper = Config.getAnalyzerWrapper(this.enAnalyzer, this.cnAnalyzer);
        }
    }

    public void indexInto(Directory indexDir) throws IOException {
        setAnalyzerWrapper();
        IndexWriterConfig iWriterConfig = new IndexWriterConfig(this.aWrapper);
        if (this.similarity == null) {
            this.similarity = new BM25Similarity();
        }
        iWriterConfig.setSimilarity(this.similarity);
        System.out.println("Using Similarity: " + iWriterConfig.getSimilarity().getClass().getName());
        IndexWriter iWriter = new IndexWriter(
                indexDir, iWriterConfig
        );
        Document doc = null;


        BufferedReader br = new BufferedReader(new FileReader(dataPath));
        String line = null;
        while ((line = br.readLine()) != null) {
            Matcher m = pattern.matcher(line.trim());
            if (m.find()) {
                String chTag = m.group(1);
                String content = m.group(2);
                if (Config.fieldCh2En.containsKey(chTag)) {
                    String enTag = Config.fieldCh2En.get(chTag);
                    doc.add(new Field(
                            enTag,
                            content,
                            TextField.TYPE_STORED
                    ));
                }
            } else {
                // new entry
                if (doc != null) {
                    iWriter.addDocument(doc);
                }
                doc = new Document();
            }
        }
        iWriter.close();
        br.close();
    }

    public static void main(String[] args) {
        Options options = new Options();

        Option dataPathOption = new Option("i", "input", true, "input data path");
        dataPathOption.setRequired(true);
        options.addOption(dataPathOption);

        Option indexPathOption = new Option("o", "output", true, "output index path");
        indexPathOption.setRequired(true);
        options.addOption(indexPathOption);

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
        } catch (ParseException e) {
            System.out.println(e.getMessage());
            formatter.printHelp("Index", options);
            System.exit(1);
        }

        assert cmd != null;
        String dataPath = cmd.getOptionValue("input");
        String indexPath = cmd.getOptionValue("output");

        try {
            Directory indexDir = FSDirectory.open(Paths.get(indexPath));
            Index indexer = new Index(dataPath);

            if (cmd.hasOption("en-analyzer")) {
                String enAnalyzerName = cmd.getOptionValue("en-analyzer");
                Class clazz = Class.forName(enAnalyzerName);
                indexer.setEnAnalyzer((Analyzer) clazz.newInstance());
            }
            if (cmd.hasOption("cn-analyzer")) {
                String cnAnalyzerName = cmd.getOptionValue("cn-analyzer");
                Class clazz = Class.forName(cnAnalyzerName);
                indexer.setCnAnalyzer((Analyzer) clazz.newInstance());
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
                    indexer.setSimilarity((Similarity) con.newInstance(params));
                } else {
                    indexer.setSimilarity((Similarity) clazz.newInstance());
                }
            }

            System.out.println("Indexing data from "
                    + dataPath + " into "
                    + indexPath);
            System.out.println("Please wait...");
            indexer.indexInto(indexDir);
            indexDir.close();
        } catch (ClassNotFoundException e) {
            System.out.println("Wrong class name. Please check.");
            e.printStackTrace();
            System.exit(1);
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(-1);
        }
    }
}
