import java.nio.file.*;
import java.util.*;
import javax.tools.*;
import com.sun.source.util.JavacTask;
public class JavaSyntaxCheck {
 public static void main(String[] args) throws Exception {
  var compiler=ToolProvider.getSystemJavaCompiler();
  var diagnostics=new DiagnosticCollector<JavaFileObject>();
  try(var files=compiler.getStandardFileManager(diagnostics,null,null); var paths=Files.walk(Path.of(args[0]))) {
   var sources=paths.filter(p->p.toString().endsWith(".java")).map(Path::toFile).toList();
   var task=(JavacTask)compiler.getTask(null,files,diagnostics,List.of("-proc:none","--release","17"),null,files.getJavaFileObjectsFromFiles(sources));
   task.parse();
   int errors=0;
   for(var diagnostic:diagnostics.getDiagnostics()) {System.out.println(diagnostic);if(diagnostic.getKind()==Diagnostic.Kind.ERROR) errors++;}
   System.out.println("Parsed "+sources.size()+" Java files; syntax errors="+errors);
   if(errors>0)System.exit(1);
  }
 }
}
