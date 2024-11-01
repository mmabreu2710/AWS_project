#java -cp <app classpath> -javaagent:</path/to/JavassistAgent.jar>=<tool name>:<package to instrument>:<path to write instrumented bytecodes> <main app class>
#java -cp apps -javaagent:target/JavassistWrapper-1.0-jar-with-dependencies.jar=ICount:pt.ulisboa.tecnico.cnv.javassist.apps:output pt.ulisboa.tecnico.cnv.javassist.apps.Fibonacci

java -cp webserver/target/webserver-1.0.0-SNAPSHOT-jar-with-dependencies.jar -javaagent:javassist/target/JavassistWrapper-1.0-jar-with-dependencies.jar=HandleAnalyzer:pt.ulisboa.tecnico.cnv:output pt.ulisboa.tecnico.cnv.webserver.WebServer 8000