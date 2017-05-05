[ ![Download](https://api.bintray.com/packages/obsidiandynamics/indigo/indigo-core/images/download.svg) ](https://bintray.com/obsidiandynamics/indigo/indigo-core/_latestVersion)

Indigo
===
Indigo is a next generation dynamic [actor model](https://en.wikipedia.org/wiki/Actor_model) framework that makes writing asynchronous and concurrent applications easy.

## The actor model, in one paragraph
Rather than allowing multiple threads to contend for shared resources using locks, an actor is a unit of concurrency that provides serial execution closed over the actor's private state, using message passing for inter-actor communication. There can be millions of actors lying dormant in a system, consuming minimal resources (memory, but not CPU), and are activated by external stimuli. Compared to traditional lock-based concurrency, the actor model eliminates deadlocks and allows for the partitioning of application state so that it can safely be operated on in parallel.

# Why Indigo
With a handful of actor frameworks already available on the JVM and the already excellent Akka, why build another? 

The current generation of actor frameworks are statically organised, meaning the actors have to be created up-front and wired in some pre-defined manner (typically a hierarchy) prior to being used. Certain actors are created purely for monitoring and supervision, and have no purpose other than creating and destroying actors. This model puts the onus of actor life-cycle management on the application developer; much like C/C++ makes one explicitly manage memory (de)allocation, static actor frameworks require the developer to nurture actors.

To the current gen of actor frameworks, Indigo is what Java is to C. Akin to the approach taken by [Orleans](https://dotnet.github.io/orleans/), Indigo actors are elastic, on-demand entities that are specified up-front but aren't activated until a demand for them exists. Actors can be made to scale with demand and may be passivated when no longer needed - persisting an actor's state to stable storage and removing it from memory. The dynamic actor model hides much of the underlying complexity and is significantly easier to work with, while offering the power and flexibility comparable to traditional 'static' models. In much the same way the transition to Java's managed heap and bounds checking simplifies the life of a traditional C/C++ developer - shifting the focus from memory management to writing business logic, next generation actor frameworks boost productivity - shifting focus from actor management to implementing concurrent business logic.

Indigo actors are driven off a highly optimised thread scheduler, tunable for latency and throughput sensitive workloads - and generally an order of magnitude more efficient than the scheduler used by Akka and Scala. So although Indigo is a higher level framework, its scheduling efficiency provides for an overall faster and less latent performance. To put things into perspective, Indigo has been benchmarked in excess of 185M sustained messages/sec and sub-microsecond latencies on a consumer-grade Intel i7 processor; the same hardware yields 7M messages/sec when running an equivalent benchmark on Akka 2.3.

Indigo is optimised for Java 8 and functional programming, while leaving the imperative option open. Where just about all actor frameworks define actors through sub-classing with state as private variables, Indigo allows the actor system to be described declaratively with stateless functions that operate on dedicated state objects. It also supports the modified agent pattern, similar to [Clojure](https://clojure.org/reference/agents) and [GPars](http://www.gpars.org/guide/guide/agents.html).

# Getting Started
## Get the binaries
Indigo builds are hosted on JCenter (MavenCentral is coming soon). Just add the following snippet to your build file (replacing the version number in the snippet with the version shown on the Download badge at the top of this README).

For Maven:

```xml
<dependency>
  <groupId>com.obsidiandynamics.indigo</groupId>
  <artifactId>indigo-core</artifactId>
  <version>0.9.0</version>
  <type>pom</type>
</dependency>
```

For Gradle:

```groovy
compile 'com.obsidiandynamics.indigo:indigo-core:0.9.0'
```

## Hello world

```java
ActorSystem.create()
.on("sysout").cue((a, m) -> System.out.println(a.self() + " received " + m.body()))
.ingress(a -> a.to(ActorRef.of("sysout")).tell("hello world"))
.shutdown();
```

What's happening here? The actor system is defined in a just a few lines of code using lambda expressions, starting by invoking `ActorSystem.create()`. The `on()` block indicates the new actor's role - in this case `sysout`, while the chained `cue()` block stipulates how an actor with the role `sysout` should behave when called upon - in this case a function that takes an `Activation` parameter named `a`  and a message `m` and simply prints the message received. To send the message we go 'inside' the actor system with the `ingress()` method and invoke the `to().tell()` fluid API.

Now for a minimal bit of theory. An Indigo actor is identified by an opaque address, called an `ActorRef`. This comprises a mandatory `role` and an optional `key`, which collectively must be unique. Singleton actors (as in the above example) will only have a role, and so we address them with `ActorRef.of(String role)`. Multiple actors can have the same role, in which case they would need a role-unique key, and would be addressed with `ActorRef.of(String role, String key)`. The function provided to `cue()` defines the behaviour of all actors with the role in question.

Every active (i.e. memory resident) actor has an associated `Activation` object, which is essentially a micro-container specifically for that actor instance. The activation is responsible for managing the actor's state and acts as an interface between the actor and its peers within the actor system. When an actor is passivated, it will be removed from memory and its activation will also be discarded. The activation's `self()` method reports the actor's address.

The `ingress()` method is an implementation of the agent pattern, and enables you to run arbitrary code from within the actor system. In other words, by operating within an ingress, your code assumes the role of an actor and will get dispatched using Indigo's internal scheduler. Being an actor it will have an address, and will not only be able to send messages, but also to receive responses.

The `shutdown()` method is a safe and convenient way of winding down an actor system. It is shut down in an orderly manner, waiting for all pending messages to be drained from their respective mailboxes. Effectively, by the time `shutdown()` returns, the actor system will have finished all its work. Invoking methods on an actor system after `shutdown()` will result in an error, as the underlying scheduler will have been terminated.

# In Summary
Indigo is -

* Much easier to use than Akka, and is nearly as flexible;
* Addictive yet safe - you won't go back to thread-safe collections, `volatile` instance variables and `synchronized` blocks, guaranteed;
* Significantly faster than any JVM-based actor framework - in the order of 200 million messages/sec and sub-microsecond latencies.

Give it a whirl.