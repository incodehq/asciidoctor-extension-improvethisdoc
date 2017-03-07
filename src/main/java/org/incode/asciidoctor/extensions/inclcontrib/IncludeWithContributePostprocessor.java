package org.incode.asciidoctor.extensions.inclcontrib;

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

        final Pattern docFilePattern = Pattern.compile(".*/([^/]+)/([^/]+)" + rootDir + srcDir + "(.*)/([^/]+)");

        Matcher matcher = docFilePattern.matcher(docfile);
        if(!matcher.matches()) {
            return output;
        }

        final String relativeDir = matcher.group(3);
        final String file = matcher.group(4);
        final String path = rootDir + srcDir + relativeDir + "/";

        final String organisation = readAttribute(document, "improvethisdoc.organisation", matcher.group(1));
        final String repo = readAttribute(document, "improvethisdoc.repo", matcher.group(2));
        final String branch = readAttribute(document, "improvethisdoc.branch", "master");
        final String label = readAttribute(document, "improvethisdoc.label", "Improve this doc");


        if(document.basebackend("html")) {
            org.jsoup.nodes.Document doc = Jsoup.parse(output, "UTF-8");

            Element docContentDiv = doc.select("div#doc-content").first();
            String mainUrl = urlFor(organisation, repo, branch, path, file);

            docContentDiv.prepend(buildHtml(mainUrl, "", label));

            Elements divElements = doc.select("div.sect1");
            for (Element divElement : divElements) {

                Elements h2Elements = divElement.select("h2");
                for (Element h2Element : h2Elements) {

                    final String id = h2Element.id();
                    if(id == null || id.trim().isEmpty()) {
                        continue;
                    }

                    if(id.startsWith("__")) {
                        continue;
                    }

                    String url = urlFor(organisation, repo, branch, path, id + ".adoc");
                    h2Element.after(buildHtml(url, "margin-top: -55px;", label));
                }
            }

            output = doc.html();
        }
        return output;
    }

    private String readAttribute(final Document document, final String attri, final String fallback) {
        final Map<String, Object> attributes2 = document.getAttributes();
        String rootDir = (String) attributes2.get(attri);
        if(rootDir == null || rootDir.trim().isEmpty()) {
            rootDir = fallback;
        }
        return rootDir;
    }

    private String buildHtml(String url, final String extraStyle, final String label) {
        return "<button " +
                    "type=\"button\" " +
                    "class=\"button secondary\" " +
                    "onclick=\"window.location.href=&quot;" + url + "&quot;\"" +
                    "style=\"float: right; font-size: small; padding: 6px; " + extraStyle + " \"" +
                ">" +
                    "<i class=\"fa fa-pencil-square-o\"></i>&nbsp;" + label + "</button>";
    }

    private String urlFor(
            final String organisation,
            final String repo,
            final String branch,
            final String path,
            final String fileName) {
        return "https://github.com/"
                + organisation
                + "/"
                + repo
                + "/edit/"
                + branch
                + path
                + fileName;

// https://github.com/apache/isis/edit/master/adocs/documentation/src/main/asciidoc/migration-notes.adoc
        //https://github.com/grails/grails-doc/edit/3.2.x/src/en/guide/GORM/quickStartGuide/basicCRUD.adoc
    }

}