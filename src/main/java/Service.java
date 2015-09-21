import java.io.File;

/**
 * @author kalle
 * @since 21/09/15 18:17
 */
public class Service {

  public static void main(String[] args) throws Exception {
    Service service = new Service();
    service.testDomain(args[0]);
  }


  public void testDomain(String domain) throws Exception {
    Search search = new Search();
    search.execute("http://*." + domain);
    search.execute("https://*." + domain);

    ExternalLinkChecker externalLinkChecker = new ExternalLinkChecker();
    externalLinkChecker.checkLinks(search.getWikipediaPagesByExternalURL(), new File(domain + ".json"), 10);
  }

}
