package uz.md.templatetopdf.parser.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import uz.md.templatetopdf.dto.FormDTO;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

@Component
@RequiredArgsConstructor
@Transactional
public class ParserUtils {

    private final ExpressionsUtils expressionsUtils;
    private final ObjectMapper objectMapper;

    public void editWordDocumentWithForm(XWPFDocument document, FormDTO form) {

        // Get the document body
        List<XWPFParagraph> paragraphs = document.getParagraphs();

        // Replace some text in the document
        for (XWPFParagraph paragraph : paragraphs) {
            List<XWPFRun> copyOnWriteList = new CopyOnWriteArrayList<>(paragraph.getRuns());
            for (XWPFRun run : copyOnWriteList) {
                String t = editWithForm(run.getText(0), form);
                XWPFRun newRun = paragraph.createRun();
                newRun.setText(t);
                fillThisRun(newRun, run);
                paragraph.removeRun(0);
            }
        }

    }

    private void fillThisRun(XWPFRun newRun, XWPFRun run) {
        newRun.setBold(run.isBold());
        newRun.setStyle(run.getStyle());
        newRun.setColor(run.getColor());
        newRun.setCapitalized(run.isCapitalized());
        newRun.setCharacterSpacing(run.getCharacterSpacing());
        newRun.setDoubleStrikethrough(run.isDoubleStrikeThrough());
        newRun.setEmbossed(run.isEmbossed());
        newRun.setEmphasisMark(run.getEmphasisMark().toString());
    }

    public String getFileName(File file) {
        return file.getName()
                .substring(0, file.getName().lastIndexOf('.'));
    }

    public String fillJsonTemplateWithForm(Map<String, String> jsonMap, FormDTO form) throws JsonProcessingException {

        // Modify the JSON tree

        Map<String, String> formBody = form.getFormBody();
        Map<String, String> resultMap = getModifiedMap(jsonMap, formBody);

        return objectMapper
                .writerWithDefaultPrettyPrinter()
                .writeValueAsString(resultMap);
    }

    private Map<String, String> getModifiedMap(Map<String, String> map, Map<String, String> formBody) {
        Map<String, String> resultMap = new HashMap<>();

        map.forEach((k, v) -> {
            if (v.startsWith("{{")) {
                String key = v.substring(2, v.length() - 2);
                resultMap.put(k, formBody.get(key) == null ? v : formBody.get(key));
            } else if (v.startsWith("[[")) {
                String key = v.substring(2, v.length() - 2);
                resultMap.put(k, expressionsUtils.getValueForExpression(key).orElse(v));
            }
        });

        return resultMap;
    }

    public String editHtmlBodyAndReturnEdited(String html, FormDTO form) {
        return editWithForm(html, form);
    }

    public String editWithForm(String text, FormDTO form) {
        Map<String, String> formBody = form.getFormBody();

        while (text.contains("[[") || text.contains("{{")) {

            int expStart = text.indexOf("[[");
            int expEnd = text.indexOf("]]");

            if (expStart != -1 && expEnd != -1) {
                String expression = text.substring(expStart + 2, expEnd);
                String value = expressionsUtils.getValueForExpression(expression).orElse(text.substring(expStart, expEnd));
                text = text.replace("[[" + expression + "]]", value);
            }

            int keyStart = text.indexOf("{{");
            int keyEnd = text.indexOf("}}");
            if (keyStart != -1 && keyEnd != -1) {
                String key = text.substring(keyStart + 2, keyEnd);
                String keyValue = formBody.get(key);
                text = text.replace("{{" + key + "}}", keyValue == null ? key : keyValue);
            }
        }

        return text;
    }

    public void fillYmlMapAndReturnModifiedMap(Map<String, Object> ymlMap, FormDTO form) {
        Map<String, String> formBody = form.getFormBody();
        for (Map.Entry<String, Object> entry : ymlMap.entrySet()) {
            Object value = entry.getValue();
            if (value instanceof String str) {
                String k;
                if (str.startsWith("{{")) {
                    k = str.substring(2, str.length() - 2);
                    entry.setValue(formBody.get(k) == null ? str : formBody.get(k));
                } else if (str.startsWith("[[")) {
                    k = str.substring(2, str.length() - 2);
                    entry.setValue(expressionsUtils.getValueForExpression(k).orElse(str));
                }
            } else if (value instanceof Map<?, ?>) {
                fillYmlMapAndReturnModifiedMap((Map<String, Object>) value, form);
            }
        }
    }

}
