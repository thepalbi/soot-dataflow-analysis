## Soot data-flow analysis

This repo consists of some [Soot](https://sable.github.io/soot/) data-flow analysis.

In order to run any of the analysis, one has to compile the desired one with *Maven*, from the root folder
of this repository:

```bash
mvn clean package
```

This will run all tests, and build all analysis, producing a jar named 
`zero-analysis-1.0-SNAPSHOT-jar-with-dependencies.jar`, for example.

To run any of the analysis, use the following command, turning on the corresponding phase (for this 
ones, check the analysis title):
````bash
java -jar sensible-data-leak-detector/target/sensible-data-leak-detector-1.0-SNAPSHOT-jar-with-dependencies.jar 
    -keep-line-number 
     -f J 
     -v -pp 
     -cp sensible-data-leak-detector/target/test-classes:sensible-data-leak-detector/target/classes 
     -print-tags 
     -p jtp.SensibleData on 
     soot.analyzables.TestMain
````

### Division by zero analysis
Phase name: **jtp.DivisionByZeroAnalysis**

Checks if a division by zero is caused in some operation. If positive, logs a warning during the 
**JTP** (Jimple transformation pack) phase.


### Sensibility and Leaks analysis
Phase name: **jtp.SensibleData**

Evaluates whether a sensible value, marked by the `SensibilityMarker.markAsSensible` method, is leaked by
some offending method.

The offending methods are read from a config file named `offending-methods.yaml`.


### Other modules
There are some other modules inside this repository, for example:
- **Utils**: Contains some cross-analysis classes, like `ValueVisitor<T>`.
- **Analysis Parent POM**: Maven's parent pom for any analysis. Contains plugins in the build and
package phase useful for packing all dependencies, and setting the **main** class.
