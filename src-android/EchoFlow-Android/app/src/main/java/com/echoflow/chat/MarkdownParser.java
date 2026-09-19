package com.echoflow.chat;

import java.util.ArrayList;
import java.util.List;

/**
 * Markdown 轻量解析：代码块、标题、列表、引用、普通文本。
 */
public class MarkdownParser {

    public static class Segment {
        public final String type; // text | code | heading | bullet | quote
        public final String content;
        public final String lang;

        public Segment(String type, String content, String lang) {
            this.type = type;
            this.content = content;
            this.lang = lang;
        }
    }

    public static List<Segment> parse(String text) {
        List<Segment> segments = new ArrayList<>();
        if (text == null || text.isEmpty()) {
            return segments;
        }
        int last = 0;
        int idx = text.indexOf("```");
        while (idx >= 0) {
            int close = text.indexOf("```", idx + 3);
            if (close < 0) {
                break;
            }
            if (idx > last) {
                pushInline(segments, text.substring(last, idx));
            }
            String header = text.substring(idx + 3, close);
            String lang = "";
            String body = header;
            int nl = header.indexOf('\n');
            if (nl >= 0) {
                lang = header.substring(0, nl).trim();
                body = header.substring(nl + 1);
            } else {
                lang = header.trim();
                body = "";
            }
            if (body.endsWith("\n")) {
                body = body.substring(0, body.length() - 1);
            }
            segments.add(new Segment("code", body, lang));
            last = close + 3;
            idx = text.indexOf("```", last);
        }
        if (last < text.length()) {
            pushInline(segments, text.substring(last));
        }
        return segments;
    }

    private static void pushInline(List<Segment> segments, String raw) {
        String[] lines = raw.split("\n");
        for (String line : lines) {
            String t = line.trim();
            if (t.isEmpty()) {
                continue;
            }
            if (t.startsWith("#")) {
                segments.add(new Segment("heading", t.replaceAll("^#+\\s*", ""), ""));
            } else if (t.startsWith("- ") || t.startsWith("* ")) {
                segments.add(new Segment("bullet", t.substring(2), ""));
            } else if (t.startsWith("> ")) {
                segments.add(new Segment("quote", t.substring(2), ""));
            } else {
                segments.add(new Segment("text", t, ""));
            }
        }
    }
}
