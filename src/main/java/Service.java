import org.apache.http.HttpResponse;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.cyberneko.html.parsers.DOMParser;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathFactory;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.URLEncoder;
import java.util.*;

/**
 * @author kalle
 * @since 21/09/15 18:17
 */
public class Service {

  public static void main(String[] args) throws Exception {

    Writer out = new OutputStreamWriter(new FileOutputStream(new File("out.txt")));
    try {

      XPathFactory xPathfactory = XPathFactory.newInstance();
      XPath xpath = xPathfactory.newXPath();
      XPathExpression linksExpression = xpath.compile("//DIV[@class='mw-spcontent']/OL/LI");
      XPathExpression externalLinkExpression = xpath.compile("A[1]/@href");
      XPathExpression wikipediaLinkExpression = xpath.compile("A[2]/@href");


      Map<String, Set<String>> pagesByURL = new HashMap<>();

      String head = "https://sv.wikipedia.org/w/index.php?title=Special:L%C3%A4nks%C3%B6kning";

      int limit = 10000;

      String query;
      if (args.length == 0) {
        query = "http://www.regeringen.se/";
      } else {
        query = args[0];
      }

      CloseableHttpClient client = HttpClientBuilder.create().build();
      try {

        CloseableHttpResponse response = client.execute(new HttpGet(head + "&limit=" + limit + "&target=" + URLEncoder.encode(query, "UTF8")));
        try {

          DOMParser parser = new DOMParser();
          parser.parse(new InputSource(response.getEntity().getContent()));
          Document document = parser.getDocument();

          NodeList links = (NodeList) linksExpression.evaluate(document, XPathConstants.NODESET);
          for (int i = 0; i < links.getLength(); i++) {
            Node linkNode = links.item(i);
            String externalLink = externalLinkExpression.evaluate(linkNode);
            String wikipediaLink = wikipediaLinkExpression.evaluate(linkNode);

            Set<String> pages = pagesByURL.get(externalLink);
            if (pages == null) {
              pages = new HashSet<>();
              pagesByURL.put(externalLink, pages);
            }

            pages.add(wikipediaLink);

            System.currentTimeMillis();
          }

        } finally {
          response.close();
        }

        for (String externalLink : pagesByURL.keySet()) {

          System.out.println(externalLink);

          try {
            response = client.execute(new HttpGet(externalLink));
          } catch (Exception e) {
            out.write("Bad external URL? " + externalLink + "\n");
            out.write("HTTP " + response.getStatusLine().getStatusCode() + " for " + externalLink + "\n");
            for (String wikipediaLink : pagesByURL.get(externalLink)) {
              out.write("https://sv.wikipedia.org" + wikipediaLink + "\n");
            }
            out.write("\n");
            out.flush();
            continue;
          }

          try {
            if (response.getStatusLine().getStatusCode() != 200) {
              out.write("HTTP " + response.getStatusLine().getStatusCode() + " for " + externalLink + "\n");
              for (String wikipediaLink : pagesByURL.get(externalLink)) {
                out.write("https://sv.wikipedia.org" + wikipediaLink + "\n");
              }
              out.write("\n");
              out.flush();
            }
          } finally {
            response.close();
          }

        }


      } finally {
        client.close();
      }

    } finally {
      out.close();
    }

  }

}
