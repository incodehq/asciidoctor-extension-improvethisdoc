package org.incode.asciidoctor.extensions.inclcontrib;

import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.asciidoctor.ast.Document;
import org.asciidoctor.extension.Postprocessor;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class IncludeWithContributePostprocessor extends Postprocessor {

    public IncludeWithContributePostprocessor(Map<String, Object> config) {
        super(config);
    }


    @Override
    public String process(Document document, String output) {


        final Map<String, Object> attributes = document.getAttributes();

        // "docfile" => "C:/APACHE/isis-git-rw/adocs/documentation/src/main/asciidoc/migration-notes.adoc"
        final String docfile = (String) attributes.get("docfile");
        // System.out.println("docfile: " + docfile);

        // we use the rootDir and srcDir to help parse the docFile.
        // the defaults correspond to the structure of github.com/apache/isis

        final String rootDir = readAttribute(document, "improvethisdoc.rootDir", "/adocs/documentation");
        final String srcDir = readAttribute(document, "improvethisdoc.srcDir", "/src/main/asciidoc");


        Parsed parsed = new Parsed(document, docfile, rootDir, srcDir);
        if (parsed.isDoesNotMatch()) {
            return output;
        }

        if(document.basebackend("html")) {
            org.jsoup.nodes.Document doc = Jsoup.parse(output, "UTF-8");

            Element docContentDiv = doc.select("div#doc-content").first();
            String mainUrl = urlFor(parsed, parsed.getFile());

            docContentDiv.prepend(buildHtml(mainUrl, "", parsed));

            Elements sect1Elements = doc.select("div.sect1");
            for (Element sect1Element : sect1Elements) {

                Elements h2Elements = sect1Element.select("h2");
                for (Element h2Element : h2Elements) {

                    final String id = h2Element.id();
                    if(id == null || id.trim().isEmpty()) {
                        continue;
                    }

                    if(id.startsWith("__")) {
                        continue;
                    }

                    String url = urlFor(parsed, id + ".adoc");
                    h2Element.after(buildHtml(url, "margin-top: -55px;", parsed));

                }

                Elements sect2Elements = sect1Element.select("div.sect2");
                for (Element sect2Element : sect2Elements) {

                    Elements h3Elements = sect1Element.select("h3");
                    for (Element h3Element : h3Elements) {

                        final String id = h3Element.id();
                        if(id == null || id.trim().isEmpty()) {
                            continue;
                        }

                        if(id.startsWith("__")) {
                            continue;
                        }

                        String url = urlFor(parsed, id + ".adoc");
                        h3Element.after(buildHtml(url, "margin-top: -55px;", parsed));
                    }

                    Elements sect3Elements = sect1Element.select("div.sect3");
                    for (Element sect3Element : sect2Elements) {

                        Elements h4Elements = sect1Element.select("h4");
                        for (Element h4Element : h4Elements) {

                            final String id = h4Element.id();
                            if(id == null || id.trim().isEmpty()) {
                                continue;
                            }

                            if(id.startsWith("__")) {
                                continue;
                            }

                            String url = urlFor(parsed, id + ".adoc");
                            h4Element.after(buildHtml(url, "margin-top: -55px;", parsed));
                        }
                    }
                }

            }

//            List<Section> sections = Lists.newArrayList(
//                    Section.of("h2", "div.sect2"),
//                    Section.of("h3", "div.sect3"),
//                    Section.of("h4", "div.sect4"),
//                    Section.of("h5", "div.sect5"));
//
//            process(doc, parsed, divElement, sections);

            output = doc.html();
        }
        return output;
    }

    private void process(
            final org.jsoup.nodes.Document doc,
            final Parsed parsed,
            final Element parentElement,
            final List<Section> sections) {

        if(sections == null || sections.isEmpty()) {
            return;
        }

        Section section = sections.remove(0);

        Elements divElements = parentElement.select(section.getSect());
        for (Element divElement : divElements) {
            Elements subHeadingEls = divElement.select(section.getTag());
            for (Element subHeadingEl : subHeadingEls) {

                final String id = subHeadingEl.id();
                if(id == null || id.trim().isEmpty()) {
                    continue;
                }

                if(id.startsWith("__")) {
                    continue;
                }

                String url = urlFor(parsed, id + ".adoc");
                subHeadingEl.after(buildHtml(url, "margin-top: -55px;", parsed));

                process(doc, parsed, subHeadingEl, sections);
            }
        }
    }

    static class Section {

        public static Section of(String tag, String sect) {
            return new Section(tag, sect);
        }
        private final String tag;
        private final String sect;

        Section(final String tag, final String sect) {
            this.sect = sect;
            this.tag = tag;
        }

        public String getTag() {
            return tag;
        }

        public String getSect() {
            return sect;
        }

    }

    private String readAttribute(final Document document, final String attri, final String fallback) {
        final Map<String, Object> attributes2 = document.getAttributes();
        String rootDir = (String) attributes2.get(attri);
        if(rootDir == null || rootDir.trim().isEmpty()) {
            rootDir = fallback;
        }
        return rootDir;
    }

    private String buildHtml(String url, final String extraStyle, final Parsed parsed) {
        String label = parsed.getLabel();

        return "<button " +
                    "type=\"button\" " +
                    "class=\"button secondary\" " +
                    "onclick=\"window.location.href=&quot;" + url + "&quot;\"" +
                    "style=\"float: right; font-size: small; padding: 6px; " + extraStyle + " \"" +
                ">" +
                    "<i class=\"fa fa-pencil-square-o\"></i>&nbsp;" + label + "</button>";
    }

    private static String urlFor(final Parsed parsed, final String file) {

        String organisation = parsed.getOrganisation();
        String repo = parsed.getRepo();
        String branch = parsed.getBranch();
        String path = parsed.getPath();

        return "https://github.com/"
                + organisation
                + "/"
                + repo
                + "/edit/"
                + branch
                + path
                + file;

// https://github.com/apache/isis/edit/master/adocs/documentation/src/main/asciidoc/migration-notes.adoc
        //https://github.com/grails/grails-doc/edit/3.2.x/src/en/guide/GORM/quickStartGuide/basicCRUD.adoc
    }

    private class Parsed {
        private boolean doesNotMatch;
        private String file;
        private String path;
        private String organisation;
        private String repo;
        private String branch;
        private String label;

        public Parsed(
                final Document document,
                final String docfile,
                final String rootDir,
                final String srcDir) {

            final Pattern docFilePattern = Pattern.compile(".*/([^/]+)/([^/]+)" + rootDir + srcDir + "(.*)/([^/]+)");

            final Matcher matcher = docFilePattern.matcher(docfile);

            this.doesNotMatch = !matcher.matches();
            if(doesNotMatch) {
                return;
            }

            final String relativeDir = matcher.group(3);
            this.file = matcher.group(4);
            this.path = rootDir + srcDir + relativeDir + "/";

            this.organisation = readAttribute(document, "improvethisdoc.organisation", matcher.group(1));
            this.repo = readAttribute(document, "improvethisdoc.repo", matcher.group(2));
            this.branch = readAttribute(document, "improvethisdoc.branch", "master");
            this.label = readAttribute(document, "improvethisdoc.label", "Improve this doc");
        }

        boolean isDoesNotMatch() {
            return doesNotMatch;
        }

        public String getFile() {
            return file;
        }

        public String getPath() {
            return path;
        }

        public String getOrganisation() {
            return organisation;
        }

        public String getRepo() {
            return repo;
        }

        public String getBranch() {
            return branch;
        }

        public String getLabel() {
            return label;
        }


    }
}