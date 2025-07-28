import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Modifier;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.symbolsolver.JavaSymbolSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.CombinedTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.JavaParserTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.ReflectionTypeSolver;
import com.github.javaparser.utils.SourceRoot;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class RepoAnalyzer {

	private static final Map<String, Set<String>> deps = new java.util.HashMap<>();

	public static void main(String[] args) throws IOException {
		Path repoPath = Paths.get("src/main/java");
		CombinedTypeSolver combinedTypeSolver = new CombinedTypeSolver();
		combinedTypeSolver.add(new ReflectionTypeSolver());
		combinedTypeSolver.add(new JavaParserTypeSolver(repoPath.toFile()));

		JavaSymbolSolver javaSymbolSolver = new JavaSymbolSolver(combinedTypeSolver);
		ParserConfiguration parserConfiguration = new ParserConfiguration();
		parserConfiguration.setSymbolResolver(javaSymbolSolver);
		StaticJavaParser.setConfiguration(parserConfiguration);

		SourceRoot sourceRoot = new SourceRoot(repoPath);

		List<CompilationUnit> compilationUnits = sourceRoot.tryToParse()
			.stream()
			.filter(result -> result.isSuccessful() && result.getResult().isPresent())
			.map(result -> result.getResult().get())
			.collect(Collectors.toList());

		System.out.println("Parsed " + compilationUnits.size() + " compilation units.");

		for (CompilationUnit cu : compilationUnits) {
			extractInfo(cu);
		}

		System.out.println("\n--- DOT (copy to graph.dot) ---");
		System.out.println("digraph G {");
		System.out.println("  rankdir=LR;");
		deps.forEach((from, tos) -> tos.forEach(to -> System.out.printf("  \"%s\" -> \"%s\";%n", from, to)));
		System.out.println("}");

	}

	private static void addEdge(String from, String to) {
		if (to == null || from.equals(to) || to.startsWith("java.lang"))
			return;
		deps.computeIfAbsent(from, k -> new java.util.HashSet<>()).add(to);
	}

	private static void extractInfo(CompilationUnit cu) {
		cu.findAll(ClassOrInterfaceDeclaration.class).forEach(classDecl -> {
			String fqn = classDecl.getFullyQualifiedName().orElse(classDecl.getNameAsString());
			System.out.println("---------------------------------------");
			System.out.println("Class Name: " + classDecl.getNameAsString());
			System.out.println("  Package: " + cu.getPackageDeclaration().map(p -> p.getNameAsString()).orElse(""));
			System.out.println("  Is Interface: " + classDecl.isInterface());
			System.out.println("  Is Abstract: " + classDecl.isAbstract());
			System.out.println("  Is Public: " + classDecl.isPublic());
			System.out.println("  Line Number: " + classDecl.getBegin().map(pos -> pos.line).orElse(-1));

			classDecl.getExtendedTypes()
				.forEach(extendedType -> System.out.println("  Extends: " + extendedType.getNameAsString()));
			classDecl.getImplementedTypes()
				.forEach(implementedType -> System.out.println("  Implements: " + implementedType.getNameAsString()));

			System.out.println("  Fields:");
			classDecl.findAll(FieldDeclaration.class).forEach(fieldDecl -> {
				String modifiers = fieldDecl.getModifiers()
					.stream()
					.map(Modifier::getKeyword)
					.map(Modifier.Keyword::asString)
					.collect(Collectors.joining(" "));

				fieldDecl.getVariables().forEach(variable -> {
					System.out.println("    - " + (modifiers.isEmpty() ? "" : modifiers + " ")
							+ variable.getType().asString() + " " + variable.getNameAsString());
				});
			});

			System.out.println("  Methods:");
			classDecl.findAll(MethodDeclaration.class).forEach(methodDecl -> {
				String modifiers = methodDecl.getModifiers()
					.stream()
					.map(Modifier::getKeyword)
					.map(Modifier.Keyword::asString)
					.collect(Collectors.joining(" "));

				String returnType = methodDecl.getType().asString();
				String methodName = methodDecl.getNameAsString();
				String parameters = methodDecl.getParameters()
					.stream()
					.map(param -> param.getType().asString() + " " + param.getNameAsString())
					.collect(Collectors.joining(", ", "(", ")"));

				System.out.println("    - " + (modifiers.isEmpty() ? "" : modifiers + " ") + returnType + " "
						+ methodName + "(" + parameters + ")");
				System.out.println("      Line Number: " + methodDecl.getBegin().map(pos -> pos.line).orElse(-1));
				System.out.println("      Body Line Count: " + methodDecl.getBody()
					.map(body -> body.getEnd().get().line - body.getBegin().get().line - 1)
					.orElse(0));
			});
		});
	}

}
