import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * @author kalle
 * @since 21/09/15 20:17
 */
public class ExternalLinkChecker {

  public void checkLinks(Map<String, Set<String>> wikipediaPagesByExternalURL, File outputFile) throws Exception {

    Writer out = new OutputStreamWriter(new FileOutputStream(outputFile));
    try {

      CloseableHttpClient client = HttpClientBuilder.create().build();
      try {


        for (String externalLink : wikipediaPagesByExternalURL.keySet()) {

          System.out.println(externalLink);

          CloseableHttpResponse response;
          try {
            response = client.execute(new HttpGet(externalLink));
          } catch (Exception e) {
            out.write("Bad external URL? " + externalLink + "\n");
            for (String wikipediaLink : wikipediaPagesByExternalURL.get(externalLink)) {
              out.write(wikipediaLink + "\n");
            }
            out.write("\n");
            out.flush();
            continue;
          }

          try {
            if (response.getStatusLine().getStatusCode() != 200) {
              out.write("HTTP " + response.getStatusLine().getStatusCode() + " for " + externalLink + "\n");
              for (String wikipediaLink : wikipediaPagesByExternalURL.get(externalLink)) {
                out.write(wikipediaLink + "\n");
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
