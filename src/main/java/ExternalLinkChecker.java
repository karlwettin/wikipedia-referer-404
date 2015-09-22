import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author kalle
 * @since 21/09/15 20:17
 */
public class ExternalLinkChecker {

  private Writer out;

  private long leniencyMilliseconds = 0;

  private String userAgent = "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_10_5) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/45.0.2454.93 Safari/537.36";

  public void checkLinks(final Map<String, Set<String>> wikipediaPagesByExternalURL, File outputFile, int numberOfThreads) throws Exception {
//    final AtomicInteger foo = new AtomicInteger(0);
    out = new OutputStreamWriter(new FileOutputStream(outputFile));
    out.write("[");
    try {

      final ConcurrentLinkedQueue<String> externalURLs = new ConcurrentLinkedQueue<>(wikipediaPagesByExternalURL.keySet());
      Thread[] threads = new Thread[numberOfThreads];
      for (int i = 0; i < threads.length; i++) {
        threads[i] = new Thread(new Runnable() {
          @Override
          public void run() {

            CloseableHttpClient client = HttpClientBuilder.create()
                .setUserAgent(userAgent)
                .build();
            try {
              String externalURL;
              while ((externalURL = externalURLs.poll()) != null) {

                System.out.println(externalURL);

                try {

                  if (leniencyMilliseconds > 0) {
                    Thread.sleep(leniencyMilliseconds);
                  }

                  Set<String> wikipediaPageURLs = wikipediaPagesByExternalURL.get(externalURL);

                  URI uri;
                  try {
                    uri = new URI(externalURL);
                  } catch (URISyntaxException e) {

                    JSONObject json = new JSONObject(new LinkedHashMap(3));
                    json.put("URL", externalURL);
                    json.put("error", "Malformed external URL");
                    JSONArray wikipediaPageURLsJSON = new JSONArray(new ArrayList(wikipediaPageURLs.size()));
                    json.put("wikipediaPageURLs", wikipediaPageURLsJSON);
                    for (String wikipediaPageURL : wikipediaPageURLs) {
                      wikipediaPageURLsJSON.put(wikipediaPageURL);
                    }
                    writeJSON(json);

                    continue;
                  }


                  CloseableHttpResponse response;
                  try {
                    response = client.execute(new HttpGet(uri));
                  } catch (UnknownHostException e) {

                    JSONObject json = new JSONObject(new LinkedHashMap(3));
                    json.put("URL", externalURL);
                    json.put("error", "Unknown host");
                    JSONArray wikipediaPageURLsJSON = new JSONArray(new ArrayList(wikipediaPageURLs.size()));
                    json.put("wikipediaPageURLs", wikipediaPageURLsJSON);
                    for (String wikipediaPageURL : wikipediaPageURLs) {
                      wikipediaPageURLsJSON.put(wikipediaPageURL);
                    }
                    writeJSON(json);

                    continue;
                  } catch (java.net.SocketException e) {

                    JSONObject json = new JSONObject(new LinkedHashMap(3));
                    json.put("URL", externalURL);
                    json.put("error", e.getMessage());
                    JSONArray wikipediaPageURLsJSON = new JSONArray(new ArrayList(wikipediaPageURLs.size()));
                    json.put("wikipediaPageURLs", wikipediaPageURLsJSON);
                    for (String wikipediaPageURL : wikipediaPageURLs) {
                      wikipediaPageURLsJSON.put(wikipediaPageURL);
                    }
                    writeJSON(json);

                    continue;

                  } catch (Exception e) {

                    StringWriter strackTrace = new StringWriter(1024);
                    PrintWriter strackTracePrintWriter = new PrintWriter(strackTrace);
                    e.printStackTrace(strackTracePrintWriter);
                    strackTracePrintWriter.close();

                    JSONObject json = new JSONObject(new LinkedHashMap(3));
                    json.put("URL", externalURL);
                    json.put("error", "Unhandled exception");
                    json.put("exception", strackTrace.toString());
                    JSONArray wikipediaPageURLsJSON = new JSONArray(new ArrayList(wikipediaPageURLs.size()));
                    json.put("wikipediaPageURLs", wikipediaPageURLsJSON);
                    for (String wikipediaPageURL : wikipediaPageURLs) {
                      wikipediaPageURLsJSON.put(wikipediaPageURL);
                    }
                    writeJSON(json);


                    continue;
                  }

                  try {
                    if (response.getStatusLine().getStatusCode() != 200) {

//                      if (response.getStatusLine().getStatusCode() == 500) {
//                        String html = IOUtils.toString(response.getEntity().getContent());
//                        FileUtils.write(new File("src/main/webapp/foo-"+foo.incrementAndGet()+".html"), html, "UTF8");
//                        System.currentTimeMillis();
//                      }

                      JSONObject json = new JSONObject(new LinkedHashMap(3));
                      json.put("URL", externalURL);
                      json.put("error", "HTTP " + response.getStatusLine().getStatusCode());
                      JSONArray wikipediaPageURLsJSON = new JSONArray(new ArrayList(wikipediaPageURLs.size()));
                      json.put("wikipediaPageURLs", wikipediaPageURLsJSON);
                      for (String wikipediaPageURL : wikipediaPageURLs) {
                        wikipediaPageURLsJSON.put(wikipediaPageURL);
                      }
                      writeJSON(json);

                    }

                  } finally {
                    response.close();
                  }

                } catch (Exception e) {
                  e.printStackTrace();
                }
              }
            } finally {
              try {
                client.close();
              } catch (Exception e) {
                e.printStackTrace();
              }
            }
          }
        });
        threads[i].setDaemon(true);
        threads[i].setName("External link worker #" + i);
        threads[i].start();
      }
      for (Thread thread : threads) {
        thread.join();
      }


    } finally {
      out.write("\n]");
      out.close();
    }


  }

  boolean needsComma = false;
  private synchronized void writeJSON(JSONObject json) throws Exception {
    if (needsComma) {
      out.write(",");
    }
    out.write("\n");
    json.write(out);
    out.flush();
    needsComma = true;
  }


  public String getUserAgent() {
    return userAgent;
  }

  public void setUserAgent(String userAgent) {
    this.userAgent = userAgent;
  }

  public long getLeniencyMilliseconds() {
    return leniencyMilliseconds;
  }

  public void setLeniencyMilliseconds(long leniencyMilliseconds) {
    this.leniencyMilliseconds = leniencyMilliseconds;
  }
}
