package info.kgeorgiy.ja.zheromskij.implementor;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.stream.Collectors;
import java.util.stream.Stream;


import info.kgeorgiy.java.advanced.implementor.*;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.*;
public class Implementor implements Impler {
    
    private static final String EOL = System.lineSeparator();
    private static final String TAB = "\t";


    public static void main(String[] args) throws ImplerException {
        if (args == null || args.length < 1) {
            System.out.println("Usage: Implementor FullClassName");
        }
        
        ClassLoader cl = ClassLoader.getSystemClassLoader();
        try {
            Class<?> token = cl.loadClass(args[0]);
            Path root = Path.of(".");
            new Implementor().implement(token, root);
        } catch (ClassNotFoundException e) {
            throw new ImplerException("Couldn't fine class " + args[0]);
        }
        
    }

    @Override
    public void implement(Class<?> token, Path root) throws ImplerException {
        if (token.isPrimitive()) {
            throw new ImplerException("Can't implement primitive types");
        }

        // нужно ли?
        // if (token.isEnum()) {
        //     throw new ImplerException("Can't implement Enums");
        // }
        if (Modifier.isPrivate(token.getModifiers())) {
            throw new ImplerException("Can't implement private class/interface");
        }
        if (token == Enum.class) {
            throw new ImplerException("Can't implement java.lang.Enum");
        }
        if (Modifier.isFinal(token.getModifiers())) {
            throw new ImplerException("Can't implement final classes");
        }

        if (Arrays.stream(token.getDeclaredMethods()).allMatch(m -> Modifier.isStatic(m.getModifiers()))) {
            throw new ImplerException("Can't implement utility classes");
        }

        if (token.isInterface() && token.getDeclaredMethods().length == 0) {
            throw new ImplerException("Can't implement methodless interfaces");
        }
        

        final Path outputFilePath = root
            .resolve(token.getPackageName().replace(".", File.separator))
            .resolve(token.getSimpleName() + "Impl.java");
        try {
            Path parent = outputFilePath.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
        } catch (final IOException e) {
            System.err.println("Couldn't create directories");
            return;
        }


        String code = "package " + token.getPackageName() + ";" + EOL +
            String.join(" ",
                "class",
                getImplName(token),
                Modifier.isInterface(token.getModifiers()) ? "implements" : "extends", 
                token.getCanonicalName(),
                getClassBody(token)                
            ) + EOL;

        try (final BufferedWriter writer = Files.newBufferedWriter(outputFilePath)) {
            try {
                writer.write(code);
            } catch (final IOException e) {
                System.err.println("I/O error in writing to out file");
                throw new ImplerException("I/O error in writing to out file");
            }
        } catch (final IOException e) {
            System.err.println("I/O error in creating output file");
            throw new ImplerException("I/O error in creating output file");
        }
    }

    private static String getClassBody(Class<?> token) {
        return 
            Stream.of(
                token.getMethods(),
                token.getDeclaredMethods(),
                token.getConstructors(),
                token.getDeclaredConstructors())
            .flatMap(Arrays::stream)
            .distinct()
            .filter((Executable e) -> 
                Modifier.isAbstract(e.getModifiers()) 
                || (!Modifier.isPrivate(e.getModifiers()) && e instanceof Constructor))
            .map(Implementor::getExecutableCode).collect(Collectors.joining("", "{" + EOL, "}" + EOL));
    }


    private static String modifiersRepr(Executable e) {
        return Modifier.toString(e.getModifiers() & ~Modifier.TRANSIENT & ~Modifier.ABSTRACT);
    }

    private static String returnTypeRepr(Executable e) {
        return e instanceof Method ? ((Method) e).getReturnType().getTypeName() : "";
         
    }

    private static String parametersRepr(Executable e) {
        return Arrays.stream(e.getParameters())
        .map((Parameter p) -> p.getType().getCanonicalName() + " " + p.getName())
        .collect(Collectors.joining(", ", "(", ")"));
    }

    private static String bodyRepr(Executable e) {
        return "{" + EOL + TAB + TAB + (e instanceof Method 
            ? "return " + getDefaultVal(((Method) e).getReturnType())
            : "super" + Arrays.stream(e.getParameters()).map(Parameter::getName).collect(Collectors.joining(", ", "(", ")")))
            + ";" + EOL + TAB + "}" + EOL;
    }


    private static String exceptionsRepr(Executable e) {
        String repr = Arrays.stream(e.getExceptionTypes()).map(Class::getCanonicalName).collect(Collectors.joining(", "));
        return repr.isEmpty() ? "" : "throws " + repr;
    }

    private static String signatureRepr(Executable e) {
        return (e instanceof Method 
                ? e.getName() 
                : getImplName(e.getDeclaringClass())) 
            + parametersRepr(e) + exceptionsRepr(e);
    }

    private static String getExecutableCode(Executable e) {
        return TAB + String.join(" ", 
            modifiersRepr(e),
            returnTypeRepr(e), 
            signatureRepr(e),
            bodyRepr(e)
        );   

    }

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

    private static String getImplName(Class<?> token) {
        return token.getSimpleName() + "Impl";
    }
}