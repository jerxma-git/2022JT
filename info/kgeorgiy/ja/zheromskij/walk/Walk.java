package info.kgeorgiy.ja.zheromskij.walk;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;


public class Walk {

    private final MessageDigest md;
    private static final String CORRUPTED_HASH = "0000000000000000000000000000000000000000";
    public Walk() throws NoSuchAlgorithmException {
        md = MessageDigest.getInstance("SHA-1");
    }

    public static void main(final String[] args) {
        if (args == null || args.length < 2) {
            System.out.println("Usage: java Walk <input_file> <output_file>");
            return;
        }
        if (args[0] == null) {
            System.err.println("First argument is null");
            return;
        } 
        if (args[1] == null) {
            System.err.println("Second argument is null");
            return;
        } 
        final String inputFileName = args[0];
        final String outputFileName = args[1];

        try {
            final Walk walk = new Walk();
            // :NOTE: Кодировки
            try (final BufferedReader reader = Files.newBufferedReader(Path.of(inputFileName))) {
                try (final BufferedWriter writer = Files.newBufferedWriter(Path.of(outputFileName))) {
                    try {
                        String fileName;
                        while ((fileName = reader.readLine()) != null) {
                            try {
                                String hashSum;
                                try {
                                    hashSum = walk.getFileHashSum(fileName);
                                } catch (WalkException e) {
                                    hashSum = CORRUPTED_HASH;
                                }
                                writer.write(String.format("%s %s%n", hashSum, fileName));
                            } catch (final IOException e) {
                                printError("Error while writing to file %s", outputFileName, e);
                            }
                        }
                    } catch (final IOException e) {
                        printError("Error while reading from file %s", inputFileName, e);
                    }
                    
                } catch (final InvalidPathException e) {
                    printError("Invalid file path to the output file: %s", outputFileName, e);
                } catch (final FileNotFoundException e) {
                    printError("Couldn't find output file %s", outputFileName, e);
                } catch (final IOException e) {
                    printError("Couldn't close output file %s", outputFileName, e);
                }
    
            } catch (final InvalidPathException e) {
                printError("Invalid path to the input file: %s", inputFileName, e);
            } catch (final FileNotFoundException e) {
                printError("Couldn't find input file %s", inputFileName, e);
            } catch (final IOException e) {
                printError("Couldn't close input file %s", inputFileName, e);
            }

        } catch (final NoSuchAlgorithmException e) {
            printError("SHA-1 hashing isn't supported%s", "", e);
        }   
    }

    
    private static void printError(String format, String arg, Exception e) {
        System.err.println(String.format(format + "%n%s", arg, e.getMessage()));
    }

    private String getFileHashSum(final String fileName) throws WalkException {
        return bytesToHexString(getDigest(fileName));
    }

    private byte[] getDigest(final String fileName) throws WalkException {
        try (final InputStream in = Files.newInputStream(Path.of(fileName))) {
            final byte[] bytes = new byte[512];
            try {
                // :NOTE: Переиспользовать
                md.reset();
                int read;
                while ((read = in.read(bytes)) >= 0) {
                    md.update(bytes, 0, read);
                }
                return md.digest();
            } catch (final IOException e) {
                printError("Error while reading from file %s", fileName, e);
                throw new WalkException();
            }
        } catch (final InvalidPathException e) {
            printError("Invalid path to the file%s", fileName, e);
            throw new WalkException();
        } catch (final FileNotFoundException e) {
            printError("File %s couldn't be found", fileName, e);
            throw new WalkException();
        } catch (final IOException e) {
            printError("Something went wrong with %s", fileName, e);
            throw new WalkException();
        }
        // :NOTE: Переиспользовать
        // return new byte[20];
    }


    private String bytesToHexString(final byte[] bytes) {
        final StringBuilder sb = new StringBuilder();
        for (final byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}
