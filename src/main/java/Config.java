import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.cn.smart.SmartChineseAnalyzer;
import org.apache.lucene.analysis.en.EnglishAnalyzer;
import org.apache.lucene.analysis.miscellaneous.PerFieldAnalyzerWrapper;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class Config {
    public static final String[] fieldNames = {
            "Title", "enTitle", "Authors", "enAuthors", "Publisher", "enPublisher",
            "Year", "Issue", "Keywords", "enKeywords", "Abstract", "enAbstract",
    };
    public static final Map<String, String> fieldCh2En = new HashMap<>();
    public static final Set<String> vitalFields = new HashSet<>();

    static {
        fieldCh2En.put("题名", "Title");
        fieldCh2En.put("英文篇名", "enTitle");
        fieldCh2En.put("作者", "Authors");
        fieldCh2En.put("英文作者", "enAuthors");
        fieldCh2En.put("出版单位", "Publisher");
        fieldCh2En.put("英文刊名", "enPublisher");
        fieldCh2En.put("关键词", "Keywords");
        fieldCh2En.put("英文关键词", "enKeywords");
        fieldCh2En.put("摘要", "Abstract");
        fieldCh2En.put("英文摘要", "enAbstract");
        fieldCh2En.put("年", "Year");
        fieldCh2En.put("期", "Issue");

        vitalFields.add("Title");
        vitalFields.add("Authors");
        vitalFields.add("Publisher");
        vitalFields.add("Year");
        vitalFields.add("Issue");
    }

    public static PerFieldAnalyzerWrapper getAnalyzerWrapper(
            Analyzer enAnalyzer, Analyzer cnAnalyzer
    ) {
        Map<String, Analyzer> analyzerPerField = new HashMap<>();
        analyzerPerField.put("Title", cnAnalyzer);
        analyzerPerField.put("Authors", cnAnalyzer);
        analyzerPerField.put("Keywords", cnAnalyzer);
        analyzerPerField.put("Abstract", cnAnalyzer);
        analyzerPerField.put("Publisher", cnAnalyzer);

        return new PerFieldAnalyzerWrapper(
                enAnalyzer,
                analyzerPerField
        );
    }
}
