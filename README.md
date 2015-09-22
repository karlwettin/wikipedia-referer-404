Söker Svenska Wikipedia efter externa länkar som är 404 eller på annat sätt trasiga för en given domain och dess subdomänder.

```
usage: java -jar wikipedia-referer.jar
 
-d,--domain <arg>       Domain name of links to be queried for. Will
                        match exact domain and any subdomain. (Required)
-h,--help               Display this help
-l,--leniency <arg>     Milliseconds pause between testing external URLs,
                        per thread. (Default 1000 for 1 thread, 0 for
                        multiple threads.)
-o,--output <arg>       Output JSON file name. (Default
                        domain-argument.json)
-p,--pagination <arg>   Number of results per pagination of Wikipedia
                        external link query. (Default 1000)
-t,--threads <arg>      Number of threads when testing external URLs.
                        (Default 1)
```
