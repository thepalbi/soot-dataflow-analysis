### Sensibility and Leaks analysis
Phase name: **jtp.SensibleData**

Evaluates whether a sensible value, marked the by the following method call:
```java
Object newSensibleObject;
analysis.SensibilityMarker.markAsSensible(newSensibleObject);
```
is leaked by some offending method. There's also a way of cleaning the sensitiveness of an object, by calling it upon
 this method:
```java
Object newSensibleObject;
analysis.SensibilityMarker.sanitize(newSensibleObject);
```
Both method are detected just by being declared as this [example](src/main/java/analysis/example/SensibilityMarker.java),
as long as they respect the package, method and class names.

The sensibility values belong to the lattice described below:
```
BOTTOM -> NOT_SENSIBLE ---> MAYBE_SENSIBLE (TOP)
       \                /
        `-> HIGH ------´
```

This is implemented in the class [SensibilityLattice](src/main/java/analysis/abstraction/SensibilityLattice.java
). The main operation is the `supremeBetween` which is the equivalent to the mathematical lattice operator.

When a local variable marked with a sensibility level `HIGH` reaches an offending method (just checking for
 `java.io.PrintStream#println`, but easily extensible), the analysis marks that statement as a leak-producer. This
  means that this statement may expose a sensible value to a user-facing interface (log, screen, request response
  , etc.).

The analysis has the following features analyzing it in the data-flow framework:

- **Inter-procedural**: See [this section](#inter-procedural-implementation-details) for more details.
- Handles just local assignment operations (does not support fields or array references).
- **Handles polymorphic method invocations**: See [this section](#Inter-procedural-implementation-details) for more
 details.

### Inter-procedural implementation details
#### Overview
The inter-procedural implementation of this analysis is both naive and technical. First, let's characterize the features
 implemented when handling method invocations:
- When the called method is defined in the same package being analyzed (in the user-code per-se, and not a third
 party or JVM library), the method called is fully analyzed running this same procedure in the invoked method
. This is done passing a context, which contains the sensibility level of the called method's arguments.
- Recursion not explicitly handled. A method that modifies some value every time called recursively might make this
 analysis produce an `StackOverflowException`. This is due to a huge number of nested calls, hence running this
  analysis once for every one of them.
- Polymorphic calls [handled](#polymorphic-calls-handling).
- Non-user methods (those described in the first point) are handled with a [simplified invocation model](#simplification-model)

#### Simplification model
When a non-user method is called, one of two things can be done:
- Treat them as method calls, which implies analyzing the called method (which belongs to third-party libraries, JVM
 libraries of native code). This might look like an easy idea at first, but there are some challenges:
  - The call-chains can be really long. For example calling the good-old `System.out.prinln` leads to a long chain
   of `PrintStream` indirections which use low-level JVM features.
  - Because the called method might use those low-level JVM features (`goto` statements for example, or some bit-based
  operator), their handlers have to be implemented in the analysis. This makes the model more and more complex as new
   methods need to be handled.
- Simplify invocation of non-user methods by a naive model which captures the effect that those calls might have in the
 analysis we are performing.

While developing this analysis, I tried implementing the former option, but quickly run
into problems like `StackOverflowError` due to long chained-calls, or some other feature of the language not being
handled correctly or omitted by my analysis (array references for example). 

This led towards choosing the latter, and simplifying the invocation semantics of non-user methods. These can be
described with the following rules:
- If one of the arguments is labeled as sensible, and the method returns a non-void type, the returned value is
 sensible. This captures calls to methods like `new StringBuilder().append(aSensibleValue)`.
- If the invocation is an instance invocation, and the receiver of the method call is a sensible value, the returned
 value is sensible. This captures calls like `someSensibleString.concat`.
 
Also, non-user called methods are that are not considered offending methods are assumed not never produce a leak as
side effect. This is a very naive approach, but since offending methods are checked way before this simplification
is reached, this *can be assumed*.

This rules are implemented [here](https://github.com/thepalbi/soot-dataflow-analysis/blob/9a43888469f712b79a99987e8c2c1238b94c44d2/sensible-data-leak-detector/src/main/java/analysis/InvocationVisitor.java#L94).

#### Polymorphic calls handling
When an invoked method corresponds to an interface method, some extra information is needed to decide which
implementation is being called. Without additional settings (Soot's whole world mode), Soot is not able to provide the
necessary to handle this situations. 

To cope with this, before running the main analysis a points-to analysis is performed to have
information of
which Object might pointed from each local variable in the analyzed program. For details, check [this repo](https://github.com/thepalbi/souffle-points-to-analysis).

There might be some cases when the base object of the invocation points to several object (due to the lack of context
sensitivity of the points-to used, for example). In those cases, every called is analyzed in a similar way, and the
results are merged making this decision in a MAY-fashion. This might generate false-positive, but helps in keeping
the overall analysis sound.

This is implemented in [here](https://github.com/thepalbi/soot-dataflow-analysis/blob/9a43888469f712b79a99987e8c2c1238b94c44d2/sensible-data-leak-detector/src/main/java/analysis/InvocationVisitor.java#L32.)

### Future work
All over the analysis code there are TODO's statements suggesting future improvements for this project. Overall, they can be summarized with the following:
- Add more language features support (field-sensitivity, arrays, etc.).
- Support non-user method invocations.
- Review context-sensitivity.
