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
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * @author kalle
 * @since 21/09/15 19:50
 */
public class Search {

  private XPathFactory xPathfactory;
  private XPath xpath;
  private XPathExpression linksExpression;
  private XPathExpression externalLinkExpression;
  private XPathExpression wikipediaLinkExpression;

  public Search() throws Exception {

    xPathfactory = XPathFactory.newInstance();
    xpath = xPathfactory.newXPath();
    linksExpression = xpath.compile("//DIV[@class='mw-spcontent']/OL/LI");
    externalLinkExpression = xpath.compile("A[1]/@href");
    wikipediaLinkExpression = xpath.compile("A[2]/@href");


  }

  private Map<String, Set<String>> wikipediaPagesByExternalURL = new HashMap<>();


  public Map<String, Set<String>> execute(String query) throws Exception {

    String wikipediaLinkPrefix = "https://sv.wikipedia.org";

    int limit = 100;

    int offset = 0;

    DOMParser parser = new DOMParser();

    CloseableHttpClient client = HttpClientBuilder.create().build();
    try {



      while (true) {

        String URL = new StringBuilder(1024)
            .append("https://sv.wikipedia.org/w/index.php?title=Special:L%C3%A4nks%C3%B6kning")
            .append("&offset=").append(String.valueOf(offset))
            .append("&limit=").append(String.valueOf(limit))
            .append("&target=").append(URLEncoder.encode(query, "UTF8"))
            .toString();

        System.out.println(URL);

        CloseableHttpResponse response = client.execute(new HttpGet(URL));
        try {


          parser.parse(new InputSource(response.getEntity().getContent()));
          Document document = parser.getDocument();


          NodeList links = (NodeList) linksExpression.evaluate(document, XPathConstants.NODESET);
          for (int i = 0; i < links.getLength(); i++) {
            Node linkNode = links.item(i);
            String externalLink = externalLinkExpression.evaluate(linkNode);
            String wikipediaLink = wikipediaLinkPrefix + wikipediaLinkExpression.evaluate(linkNode);

            Set<String> pages = wikipediaPagesByExternalURL.get(externalLink);
            if (pages == null) {
              pages = new HashSet<>();
              wikipediaPagesByExternalURL.put(externalLink, pages);
            }

            pages.add(wikipediaLink);
          }


          if (links.getLength() < limit) {
            break;
          }

          offset += limit;

        } finally {
          response.close();
        }

      }


      return wikipediaPagesByExternalURL;

    } finally {
      client.close();
    }

  }

  public Map<String, Set<String>> getWikipediaPagesByExternalURL() {
    return wikipediaPagesByExternalURL;
  }
}
