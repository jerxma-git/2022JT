package info.kgeorgiy.ja.zheromskij.implementor;

import java.lang.reflect.Modifier;
import java.net.URISyntaxException;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Arrays;
import java.util.jar.Attributes;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;

import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;

import info.kgeorgiy.java.advanced.implementor.*;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.*;

/**
 * Class for implementing interfaces or classes with ability to implement them
 * as JAR files.
 * 
 * @author Maxim Zheromskij
 */
public class Implementor implements JarImpler {

    /**
     * End of line.
     */
    private static final String EOL = System.lineSeparator();
    /**
     * Tab character.
     */
    private static final String TAB = "\t";
    /**
     * Deleting file visitor.
     */
    private static final FileVisitor<Path> deleting_visitor = new SimpleFileVisitor<>() {
        @Override
        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
            Files.delete(file);
            return FileVisitResult.CONTINUE;
        }

        @Override
        public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
            Files.delete(dir);
            return FileVisitResult.CONTINUE;
        }
    };

    /**
     * Implements class or interface.
     * <br>
     * Can be run either as {@code Implementor FullClassName}
     * or {@code Implementor -jar FullClassName OutputJarFile}.
     * <br>
     * The first variant creates a file containing an implementation of
     * {@code FullClassName}
     * in the current directory.
     * <br>
     * The second variant creates an implementation of {@code FullClassName} in a
     * temporary directory, compiles it,
     * collects it into a JAR file with the file name {@code OutputJarFile}.
     *
     * @param args command line arguments.
     */
    public static void main(String[] args) {
        if (args == null || (args.length != 1 && args.length != 3) || args[0] == null) {
            printUsage();
            return;
        }

        boolean jar = args.length == 3;
        ClassLoader cl = ClassLoader.getSystemClassLoader();
        try {
            Class<?> token = cl.loadClass(args[jar ? 1 : 0]);
            if (!jar) {
                Path root = Path.of(".");
                new Implementor().implement(token, root);
            } else {
                if (!args[0].equals("-jar")) {
                    printUsage();
                    return;
                }
                new Implementor().implementJar(token, Path.of(args[2]));
            }
        } catch (ImplerException e) {
            System.out.println("Can't implement due to: " + e.getMessage());
        } catch (ClassNotFoundException ignored) {
            System.out.println("Can't find class " + args[jar ? 1 : 0]);
        }
    }

    /**
     * Prints usage of the program.
     */
    private static void printUsage() {
        System.out.println("Usage: Implementor FullClassName");
        System.out.println("   or: Implementor -jar FullClassName OutputJarFile");
    }

    /**
     * Creates an implementation of {@code token} in a temporary directory, compiles
     * it and writes to a JAR file located at {@code outputFile}.
     *
     * @param token      type of token to create implementation for.
     * @param outputFile path to the output JAR file.
     * @throws ImplerException if cannot create an implementation, write it to a
     *                         temporary directory,
     *                         compile it, or save it to a JAR file.
     */
    @Override
    public void implementJar(Class<?> token, Path outputFile) throws ImplerException {
        if (token == null || outputFile == null) {
            throw new ImplerException("Null argument");
        }
        Path tmpDir;
        try {
            tmpDir = Files.createTempDirectory("jarimpl");
        } catch (IOException | SecurityException e) {
            throw new ImplerException("Can't create temporary directory", e);
        }
        implement(token, tmpDir);
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        if (compiler == null) {
            throw new ImplerException("Can't find java compiler");
        }
        String classpath;
        try {
            classpath = Path.of(token.getProtectionDomain().getCodeSource().getLocation().toURI()) + File.pathSeparator
                    + tmpDir;
        } catch (URISyntaxException | NullPointerException ignored) {
            classpath = tmpDir.toString();
        }
        String[] args = { "-cp", classpath, resolveFilePath(token, tmpDir, "java").toString() };
        if (compiler.run(null, null, null, args) != 0) {
            throw new ImplerException("Can't compile");
        }
        Manifest manifest = new Manifest();
        Attributes attrs = manifest.getMainAttributes();
        attrs.put(Attributes.Name.MANIFEST_VERSION, "1.0");
        try (JarOutputStream out = new JarOutputStream(Files.newOutputStream(outputFile), manifest)) {
            try {
                out.putNextEntry(
                        new ZipEntry(token.getPackageName().replace(".", "/") + "/" + getImplName(token) + ".class"));
                Files.copy(resolveFilePath(token, tmpDir, "class"), out);
            } catch (IOException e) {
                throw new ImplerException("Can't write to JAR file", e);
            }
        } catch (IOException e) {
            throw new ImplerException("Can't create jar file", e);
        } finally {
            try {
                Files.walkFileTree(tmpDir, deleting_visitor);
            } catch (IOException e) {
                System.out.println("Can't delete temporary directory");
            }
        }
    }

    /**
     * Creates an implementation of {@code token} in {@code root} directory.
     *
     * @param token type token to create implementation for.
     * @param root  root directory.
     * @throws ImplerException if cannot create an implementation or write it to a
     *                         file.
     */
    @Override
    public void implement(Class<?> token, Path root) throws ImplerException {
        checkIsImplementable(token);

        final Path outputFilePath = resolveFilePath(token, root, "java");
        try {
            Path parent = outputFilePath.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
        } catch (final IOException ignored) {
            System.err.println("Couldn't create directories");
            return;
        }

        String code = "package " + token.getPackageName() + ";" + EOL +
                String.join(" ",
                        "class",
                        getImplName(token),
                        Modifier.isInterface(token.getModifiers()) ? "implements" : "extends",
                        token.getCanonicalName(),
                        getClassBody(token))
                + EOL;

        try (final BufferedWriter writer = Files.newBufferedWriter(outputFilePath)) {
            try {
                writer.write(code);
            } catch (final IOException e) {
                System.err.println("I/O error in writing to out file");
                throw new ImplerException("I/O error in writing to out file", e);
            }
        } catch (final IOException e) {
            System.err.println("I/O error in creating output file");
            throw new ImplerException("I/O error in creating output file", e);
        }
    }

    /**
     * Checks whether {@code token} is implementable.
     * 
     * @param token type token to check.
     * @throws ImplerException if {@code token} is not implementable
     */
    private void checkIsImplementable(Class<?> token) throws ImplerException {
        if (token.isPrimitive()) {
            throw new ImplerException("Can't implement primitive types");
        }
        if (token.isEnum()) {
            throw new ImplerException("Can't implement Enums");
        }
        if (token == Enum.class) {
            throw new ImplerException("Can't implement java.lang.Enum");
        }
        if (Modifier.isFinal(token.getModifiers())) {
            throw new ImplerException("Can't implement final classes");
        }
        if (Modifier.isPrivate(token.getModifiers())) {
            throw new ImplerException("Can't implement private classes");
        }
        Constructor<?>[] constructors = token.getDeclaredConstructors();
        if (constructors.length > 0
                && Arrays.stream(constructors).allMatch(constr -> Modifier.isPrivate(constr.getModifiers()))) {
            throw new ImplerException("Can't implement class with only private constructors");
        }
    }

    /**
     * Resolves file path for implementation of {@code token} in {@code root}
     * directory.
     *
     * @param token         type token to resolve for.
     * @param root          directory to resolve in.
     * @param fileExtension file extension to use.
     * @return resolved file path.
     */
    private static Path resolveFilePath(Class<?> token, Path root, String fileExtension) {
        return root
                .resolve(token.getPackage().getName().replace(".", File.separator))
                .resolve(getImplName(token) + "." + fileExtension);
    }

    /**
     * Escapes all non-ASCII characters in {@code str}.
     * 
     * @param str string to escape.
     * @return escaped string.
     */
    private static String escapeUnicode(String str) {
        return str.chars()
                .mapToObj(c -> c <= 127 ? String.valueOf((char) c) : String.format("\\u%04x", c))
                .collect(Collectors.joining());
    }

    /**
     * Returns class body for {@code token} implementation.
     * 
     * @param token type token to get body for.
     * @return class body.
     */
    private static String getClassBody(Class<?> token) {
        return Stream.of(
                token.getMethods(),
                token.getDeclaredMethods(),
                token.getConstructors(),
                token.getDeclaredConstructors())
                .flatMap(Arrays::stream)
                .distinct()
                .filter((Executable e) -> Modifier.isAbstract(e.getModifiers())
                        || (!Modifier.isPrivate(e.getModifiers()) && e instanceof Constructor))
                .map(Implementor::getExecutableCode)
                .map(Implementor::escapeUnicode)
                .collect(Collectors.joining("", "{" + EOL, "}" + EOL));
    }

    /**
     * Returns code for {@code executable} modifiers.
     * 
     * @param e {@link Executable} to get modifiers for.
     * @return modifiers code.
     */
    private static String modifiersRepr(Executable e) {
        return Modifier.toString(e.getModifiers() & ~Modifier.TRANSIENT & ~Modifier.ABSTRACT);
    }

    /**
     * Returns code for {@code executable} return type.
     * 
     * @param e {@link Executable} to get return type for.
     * @return return type code.
     */
    private static String returnTypeRepr(Executable e) {
        return e instanceof Method ? ((Method) e).getReturnType().getCanonicalName() : "";

    }

    /**
     * Returns code for {@code executable} parameters.
     * 
     * @param e {@link Executable} to get parameters for.
     * @return parameters code.
     */
    private static String parametersRepr(Executable e) {
        return Arrays.stream(e.getParameters())
                .map((Parameter p) -> p.getType().getCanonicalName() + " " + p.getName())
                .collect(Collectors.joining(", ", "(", ")"));
    }

    /**
     * Returns code for {@code executable} body.
     * 
     * @param e {@link Executable} to get body for.
     * @return body code.
     */
    private static String bodyRepr(Executable e) {
        return "{" + EOL + TAB + TAB + (e instanceof Method
                ? "return " + getDefaultVal(((Method) e).getReturnType())
                : "super" + Arrays.stream(e.getParameters()).map(Parameter::getName)
                        .collect(Collectors.joining(", ", "(", ")")))
                + ";" + EOL + TAB + "}" + EOL;
    }

    /**
     * Returns code for {@code executable} signature.
     * 
     * @param e {@link Executable} to get signature for.
     * @return signature code.
     */
    private static String signatureRepr(Executable e) {
        return (e instanceof Method
                ? e.getName()
                : getImplName(e.getDeclaringClass()))
                + parametersRepr(e);
    }

    /**
     * Returns code for {@code executable} declaration.
     * 
     * @param e {@link Executable} to get declaration for.
     * @return declaration code.
     */
    private static String throwsRepr(Executable e) {
        if (e instanceof Constructor) {
            Class<?>[] exceptionTypes = e.getExceptionTypes();
            if (exceptionTypes.length != 0) {
                return Arrays.stream(exceptionTypes)
                        .map(Class::getCanonicalName)
                        .collect(Collectors.joining(", ", "throws ", ""));
            }
        }
        return "";
    }

    /**
     * Returns code for {@code executable}.
     * 
     * @param e {@link Executable} to get code for.
     * @return executable code.
     */
    private static String getExecutableCode(Executable e) {
        return TAB + String.join(" ",
                modifiersRepr(e),
                returnTypeRepr(e),
                signatureRepr(e),
                throwsRepr(e),
                bodyRepr(e));
    }

    /**
     * Returns default value for {@code type}.
     * 
     * @param c type to get default value for.
     * @return default value.
     */
    private static String getDefaultVal(Class<?> c) {
        if (c == boolean.class) {
            return "false";
        }
        if (c == void.class) {
            return "";
        }
        if (Object.class.isAssignableFrom(c)) {
            return "null";
        }
        return "0";
    }

    /**
     * Returns name of implementation class for {@code token} class.
     * 
     * @param token type token to get implementation name for.
     * @return implementation name.
     */
    private static String getImplName(Class<?> token) {
        return token.getSimpleName() + "Impl";
    }
}