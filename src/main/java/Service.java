import org.apache.commons.cli.*;

import java.io.File;

/**
 * @author kalle
 * @since 21/09/15 18:17
 */
public class Service {

  public static void main(String[] args) throws Exception {

    int searchLimit = 1000;
    int numberOfThreads = 1;
    long leniencyMilliseconds = 1000;
    String domain;
    File outputFile;

    Options options = new Options();
    options.addOption("h", "help", false, "Display this help");
    options.addOption("d", "domain", true, "Domain name of links to be queried for. Will match exact domain and any subdomain. (Required)");
    options.addOption("p", "pagination", true, "Number of results per pagination of Wikipedia external link query. (Default 1000)");
    options.addOption("t", "threads", true, "Number of threads when testing external URLs. (Default 1)");
    options.addOption("l", "leniency", true, "Milliseconds pause between testing external URLs, per thread. (Default 1000 for 1 thread, 0 for multiple threads.)");
    options.addOption("o", "output", true, "Output JSON file name. (Default domain-argument.json)");

    CommandLineParser parser = new DefaultParser();
    CommandLine cmd = parser.parse( options, args);


    if (!cmd.hasOption('d') || cmd.hasOption('h')) {
      HelpFormatter formatter = new HelpFormatter();
      formatter.printHelp( "java -jar wikipedia-referer.jar", options );
      return;
    }

    domain = cmd.getOptionValue('d');

    if (cmd.hasOption('p')) {
      searchLimit = Integer.valueOf(cmd.getOptionValue('p'));
    }
    if (cmd.hasOption('t')) {
      numberOfThreads = Integer.valueOf('t');
      leniencyMilliseconds = 0;
    }
    if (cmd.hasOption('l')) {
      leniencyMilliseconds = Long.valueOf(cmd.getOptionValue('l'));
    }
    if (cmd.hasOption('o')) {
      outputFile = new File(cmd.getOptionValue('o'));
      if (!outputFile.getParentFile().exists()) {
        System.out.println("Output file path does not exist! " + outputFile.getParentFile().getAbsolutePath());
        return;
      }
    } else {
      outputFile = new File(domain + ".json");
    }

    System.out.println("Executing using the following settings:");
    System.out.println("Domain: " + domain);
    System.out.println("Pagination: " + searchLimit);
    System.out.println("Threads: " + numberOfThreads);
    System.out.println("Leniency: " + leniencyMilliseconds);
    System.out.println("Output: " + outputFile.getAbsolutePath());
    System.out.println("-------------------------------------------------------------------------------");


    Search search = new Search();
    search.setLimit(searchLimit);
    search.setMaximumExternalURLs(5000);
    search.execute("http://*." + domain);
    search.execute("https://*." + domain);

    ExternalLinkChecker externalLinkChecker = new ExternalLinkChecker();
    externalLinkChecker.setLeniencyMilliseconds(leniencyMilliseconds);
    externalLinkChecker.checkLinks(search.getWikipediaPagesByExternalURL(), outputFile, numberOfThreads);

  }


}
