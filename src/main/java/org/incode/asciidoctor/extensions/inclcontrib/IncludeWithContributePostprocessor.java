package org.incode.asciidoctor.extensions.inclcontrib;

import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.beust.jcommander.internal.Lists;

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

            List<Section> sections = Lists.newArrayList(
                    Section.of("div.sect1", "h2"),
                    Section.of("div.sect2", "h3"),
                    Section.of("div.sect3", "h4"),
                    Section.of("div.sect4", "h5"),
                    Section.of("div.sect5", "h6")
            );

            handle1(parsed, doc, sections);

            output = doc.html();
        }
        return output;
    }

    private void handle1(
            final Parsed parsed,
            final org.jsoup.nodes.Document doc,
            final List<Section> sections
    ) {

        if(sections.isEmpty()) {
            return;
        }
        final Section section = sections.remove(0);

        final String sectionQuery = section.sect;
        final String headingTag = section.tag;

        Elements sectionElements = doc.select(sectionQuery);
        for (Element sectionElement : sectionElements) {


            Elements headingElements = sectionElement.select(headingTag);
            for (Element headingElement : headingElements) {

                final String id = headingElement.id();
                if(id == null || id.trim().isEmpty()) {
                    continue;
                }

                if(id.startsWith("__")) {
                    continue;
                }

                String url = urlFor(parsed, id + ".adoc");
                headingElement.after(buildHtml(url, "margin-top: -55px;", parsed));
            }

            handle2(parsed, sectionElement, Lists.newArrayList(sections));
        }
    }

    private void handle2(
            final Parsed parsed,
            final Element parentElement,
            final List<Section> sections) {

        if(sections.isEmpty()) {
            return;
        }
        final Section section = sections.remove(0);

        final String sectionQuery = section.sect;
        final String headingTag = section.tag;

        Elements sectionElements = parentElement.select(sectionQuery);
        for (Element sectionElement : sectionElements) {

            Elements headingElements = sectionElement.select(headingTag);
            for (Element headingElement : headingElements) {

                final String id = headingElement.id();
                if(id == null || id.trim().isEmpty()) {
                    continue;
                }

                if(id.startsWith("__")) {
                    continue;
                }

                String url = urlFor(parsed, id + ".adoc");
                headingElement.after(buildHtml(url, "margin-top: -55px;", parsed));
            }

            handle3(parsed, sectionElement, Lists.newArrayList(sections));
        }
    }

    private void handle3(
            final Parsed parsed,
            final Element parentElement,
            final List<Section> sections) {

        if(sections.isEmpty()) {
            return;
        }
        final Section section = sections.remove(0);

        final String sectionQuery = section.sect;
        final String headingTag = section.tag;

        Elements sectionElements = parentElement.select(sectionQuery);
        for (Element sectionElement : sectionElements) {

            Elements headingElements = sectionElement.select(headingTag);
            for (Element headingElement : headingElements) {

                final String id = headingElement.id();
                if(id == null || id.trim().isEmpty()) {
                    continue;
                }

                if(id.startsWith("__")) {
                    continue;
                }

                String url = urlFor(parsed, id + ".adoc");
                headingElement.after(buildHtml(url, "margin-top: -55px;", parsed));
            }

            handle4(parsed, sectionElement, Lists.newArrayList(sections));
        }
    }

    private void handle4(
            final Parsed parsed,
            final Element parentEl,
            final List<Section> sections) {

        if(sections.isEmpty()) {
            return;
        }
        final Section section = sections.remove(0);

        final String sectionQuery = section.sect;
        final String headingTag = section.tag;

        Elements sectElements = parentEl.select(sectionQuery);
        for (Element sectEl : sectElements) {

            Elements headingElements = sectEl.select(headingTag);
            for (Element headingEl : headingElements) {

                final String id = headingEl.id();
                if(id == null || id.trim().isEmpty()) {
                    continue;
                }

                if(id.startsWith("__")) {
                    continue;
                }

                String url = urlFor(parsed, id + ".adoc");
                headingEl.after(buildHtml(url, "margin-top: -55px;", parsed));
            }
        }
    }


    static class Section {

        static Section of(String sect, String tag) {
            return new Section(sect, tag);
        }
        private final String tag;
        private final String sect;

        Section(final String sect, final String tag) {
            this.sect = sect;
            this.tag = tag;
        }

        @Override
        public String toString() {
            return sect + ":" + tag;
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

        Parsed(
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

        String getFile() {
            return file;
        }

        String getPath() {
            return path;
        }

        String getOrganisation() {
            return organisation;
        }

        String getRepo() {
            return repo;
        }

        String getBranch() {
            return branch;
        }

        String getLabel() {
            return label;
        }

    }
}