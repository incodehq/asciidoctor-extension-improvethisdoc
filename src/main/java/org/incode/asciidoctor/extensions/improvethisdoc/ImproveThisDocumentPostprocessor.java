package org.incode.asciidoctor.extensions.improvethisdoc;

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

public class ImproveThisDocumentPostprocessor extends Postprocessor {

    public ImproveThisDocumentPostprocessor(Map<String, Object> config) {
        super(config);
    }


    @Override
    public String process(Document document, String output) {

        if (!document.basebackend("html")) {
            return output;
        }

        final Map<String, Object> attributes = document.getAttributes();

        // "docfile" => "C:/APACHE/isis-git-rw/adocs/documentation/src/main/asciidoc/migration-notes.adoc"
        final String docfile = (String) attributes.get("docfile");

        // we use the rootDir and srcDir to help parse the docFile.
        // the defaults correspond to the structure of github.com/apache/isis
        final String rootDir = readAttribute(document, "improvethisdoc.rootDir", "/adocs/documentation");
        final String srcDir = readAttribute(document, "improvethisdoc.srcDir", "/src/main/asciidoc");

        final Parsed parsed = new Parsed(document, docfile, rootDir, srcDir);
        if (parsed.isDoesNotMatch()) {
            return output;
        }

        org.jsoup.nodes.Document doc = Jsoup.parse(output, "UTF-8");

        Element docContentDiv = doc.select("div#doc-content").first();
        final String file = parsed.getFile();
        final String html = buildHtml(parsed, file, "");

        docContentDiv.prepend(html);

        List<Section> sections = Lists.newArrayList(
                Section.of("div.sect1", "h2"),
                Section.of("div.sect2", "h3"),
                Section.of("div.sect3", "h4"),
                Section.of("div.sect4", "h5"),
                Section.of("div.sect5", "h6"),
                Section.of("div.sect6", "h7"),
                Section.of("div.sect7", "h8")
        );

        // editable sections will need to have an id starting with this, eg "_rgmvn"
        final String fileNoSuffix = parsed.file.substring(0, parsed.file.lastIndexOf("."));
        sections.get(0).id = "_" + fileNoSuffix;

        handle(doc, parsed, sections);

        return doc.html();
    }


    private void handle(
            final Element parentElement,
            final Parsed parsed,
            final List<Section> sections) {

        if(sections.isEmpty()) {
            return;
        }
        final Section parentSection = sections.remove(0);

        Elements sectionElements = parentElement.select(parentSection.sect);
        for (Element sectionElement : sectionElements) {

            Elements headingElements = sectionElement.select(parentSection.tag);

            for (Element headingElement : headingElements) {
                String id = headingElement.id();

                if (isChild(parentSection, id)) {

                    final String file = id + ".adoc";
                    final String html = buildHtml(parsed, file, "margin-top: -55px;");
                    headingElement.after(html);

                    // push the id for next section
                    if (!sections.isEmpty()) {
                        sections.get(0).id = id;
                    }

                    handle(sectionElement, parsed, deepCopy(sections));
                }
            }
        }
    }

    private static boolean isChild(final Section parentSection, final String id) {
        if (id == null) {
            return false;
        }
        final String trimmedId = id.trim();
        if (trimmedId.isEmpty()) {
            return false;
        }
        return trimmedId.startsWith(parentSection.id);
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

        private String id;

        @Override
        public String toString() {
            return sect + ":" + tag;
        }
    }

    private static List<Section> deepCopy(final List<Section> sections) {
        List<Section> list = Lists.newArrayList();
        for (Section section : sections) {
            Section copy = Section.of(section.sect, section.tag);
            copy.id = section.id;
            list.add(copy);
        }
        return list;
    }

    private static String readAttribute(final Document document, final String attribute, final String fallback) {
        final Map<String, Object> attributes2 = document.getAttributes();
        String rootDir = (String) attributes2.get(attribute);
        if(rootDir == null || rootDir.trim().isEmpty()) {
            rootDir = fallback;
        }
        return rootDir;
    }

    private String buildHtml(
            final Parsed parsed,
            final String file,
            final String extraStyle) {

/*
http://getbootstrap.com/components/#btn-dropdowns-single

<div class="btn-group">
   <button type="button" class="btn btn-xs btn-default" onclick="...">Edit</button>
   <button type="button" class="btn btn-xs btn-default dropdown-toggle" data-toggle="dropdown" aria-haspopup="true" aria-expanded="false">
      <span class="caret"></span>
      <span class="sr-only">Toggle Dropdown</span>
  </button>

    <button type="button" class="btn btn-xs btn-default dropdown-toggle" data-toggle="dropdown" aria-haspopup="true" aria-expanded="false">Source<span class="caret"></span>

  <ul class="dropdown-menu">
    <li><a href="#">History</a></li>
    <li><a href="#">Raw</a></li>
    <li><a href="#">Blame</a></li>
  </ul>
</div>
 */

/*
 -        return "<button " +
 -                    "type=\"button\" " +
 -                    "class=\"button secondary\" " +
 -
 -                    "style=\"float: right; font-size: small; padding: 6px; " + extraStyle + " \"" +
                  ">" +
  "<i class=\"fa fa-pencil-square-o\"></i>&nbsp;" + label + "</button>";
 */
        return "<div class=\"btn-group\" " + "style=\"float: right; font-size: small; padding: 6px; " + extraStyle + " \"" +
                ">" +
                    "<button type=\"button\" " +
                            "class=\"btn btn-xs btn-default\" " +
                            "onclick=\"window.location.href=&quot;" + buildUrl(parsed, file, "edit") + "&quot;\" >" +
                        "<i class=\"fa fa-pencil-square-o\"></i>&nbsp;" + parsed.getEditLabel() +
                    "</button>" +
                    "<button type=\"button\" class=\"btn btn-xs btn-default dropdown-toggle\" data-toggle=\"dropdown\" aria-haspopup=\"true\" aria-expanded=\"false\">" +
                        "<span class=\"caret\"></span>" +
                        "<span class=\"sr-only\">Toggle Dropdown</span>" +
                    "</button>" +
                    "<ul class=\"dropdown-menu\">" +
                        buildLink(buildUrl(parsed, file, "edit"), "fa-pencil-square-o", parsed.getEditLabel()) +
                        buildLink(buildUrl(parsed, file, "commits"), "fa-clock-o", parsed.getHistoryLabel()) +
                        buildLink(buildUrl(parsed, file, "raw"), "fa-file-text-o", parsed.getRawLabel()) +
                        buildLink(buildUrl(parsed, file, "blame"), "fa-hand-o-right", parsed.getBlameLabel()) +
                    "</ul>" +
                "</div>";
    }

    private String buildLink(final String editUrl, final String editIcon, final String editLabel) {
        return "<li><a href=\"" + editUrl + "\" target=\"_blank\"><i class=\"fa "
                + editIcon
                + " fa-fw\" aria-hidden=\"true\"></i>&nbsp; " + editLabel + "</a></li>";
    }

    private String buildUrl(
            final Parsed parsed,
            final String file,
            final String verb) {

        String organisation = parsed.getOrganisation();
        String repo = parsed.getRepo();
        String branch = parsed.getBranch();
        String path = parsed.getPath();

        // eg https://github.com/apache/isis/edit/master/adocs/documentation/src/main/asciidoc/migration-notes.adoc
        return "https://github.com/"
                + organisation
                + "/"
                + repo
                + "/"
                + verb
                + "/"
                + branch
                + path
                + file;
    }

    private class Parsed {

        private final boolean doesNotMatch;
        private String file;
        private String path;
        private String organisation;
        private String repo;
        private String branch;
        private String label;
        private String historyLabel;
        private String sourceLabel;
        private String rawLabel;
        private String blameLabel;

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
            this.label = readAttribute(document, "improvethisdoc.label", "Edit");
            this.sourceLabel = readAttribute(document, "improvethisdoc.sourceLabel", "Source");
            this.historyLabel = readAttribute(document, "improvethisdoc.historyLabel", "History");
            this.rawLabel = readAttribute(document, "improvethisdoc.rawLabel", "Raw");
            this.blameLabel = readAttribute(document, "improvethisdoc.blameLabel", "Blame");
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

        String getEditLabel() {
            return label;
        }

        public String getSourceLabel() {
            return sourceLabel;
        }

        public String getHistoryLabel() {
            return historyLabel;
        }

        public String getRawLabel() {
            return rawLabel;
        }

        public String getBlameLabel() {
            return blameLabel;
        }

    }
}