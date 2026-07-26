package com.javacodingagent.analysis;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.*;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.nodeTypes.NodeWithAnnotations;
import com.javacodingagent.analysis.model.*;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

/** Extracts source-level symbols and method-call names; it deliberately does not infer hidden runtime behavior. */
public class JavaSourceAnalyzer {
    public JavaFileAnalysis analyze(Path path) {
        CompilationUnit unit;
        try { unit = StaticJavaParser.parse(path); } catch (Exception exception) { throw new IllegalArgumentException("Cannot parse Java source: " + path, exception); }
        String packageName = unit.getPackageDeclaration().map(p -> p.getNameAsString()).orElse("");
        List<String> imports = unit.getImports().stream().map(i -> i.getNameAsString()).toList();
        List<CodeSymbol> symbols = unit.findAll(TypeDeclaration.class).stream().flatMap(type -> symbolsForType(type, packageName, path).stream()).toList();
        return new JavaFileAnalysis(path.toString(), packageName, imports, symbols);
    }
    private List<CodeSymbol> symbolsForType(TypeDeclaration<?> type, String packageName, Path path) {
        var result = new java.util.ArrayList<CodeSymbol>();
        result.add(symbol(type, packageName, path, classify(type), type.getNameAsString(), type.getNameAsString(), List.of()));
        for (BodyDeclaration<?> body : type.getMembers()) {
            if (body instanceof MethodDeclaration method) result.add(symbol(method, packageName, path, SymbolType.METHOD, method.getNameAsString(), method.getDeclarationAsString(false, false, false), calls(method)));
            else if (body instanceof ConstructorDeclaration constructor) result.add(symbol(constructor, packageName, path, SymbolType.CONSTRUCTOR, constructor.getNameAsString(), constructor.getDeclarationAsString(false, false, false), calls(constructor)));
            else if (body instanceof FieldDeclaration field) for (VariableDeclarator variable : field.getVariables()) result.add(symbol(field, packageName, path, SymbolType.FIELD, variable.getNameAsString(), field.getElementType() + " " + variable.getNameAsString(), List.of()));
        }
        return result;
    }
    private CodeSymbol symbol(Node node, String pkg, Path path, SymbolType kind, String name, String signature, List<String> refs) {
        int start = node.getRange().map(r -> r.begin.line).orElse(0), end = node.getRange().map(r -> r.end.line).orElse(start);
        List<String> annotations = node instanceof NodeWithAnnotations<?> annotated ? annotated.getAnnotations().stream().map(a -> a.getNameAsString()).toList() : List.of();
        return new CodeSymbol(pkg, path.toString(), name, kind, start, end, signature, annotations, refs);
    }
    private List<String> calls(com.github.javaparser.ast.Node node) { return node.findAll(MethodCallExpr.class).stream().map(MethodCallExpr::getNameAsString).distinct().toList(); }
    private SymbolType classify(TypeDeclaration<?> type) {
        if (type.isClassOrInterfaceDeclaration() && type.asClassOrInterfaceDeclaration().isInterface()) return SymbolType.INTERFACE;
        if (type.isEnumDeclaration()) return SymbolType.ENUM;
        return SymbolType.CLASS;
    }
}
