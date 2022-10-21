package com.dalveesrever.rclassgenerator;

import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.*;

public class Main {

    private final static List<String> subclassNames = new ArrayList<>();
    private static boolean addComments = false;
    private static String packageName;
    private static File publicXmlFile;
    private static File outRClassFile;

    public static void main(String[] args)
        throws IOException {

        for (int i = 0; i < args.length; i++) {
            String arg = args[i];
            if (arg.startsWith("-"))
                parseOption(args, i, arg);        
        }

        if (outRClassFile == null)
            outRClassFile = new File("R.java");

        Scanner scanner = new Scanner(publicXmlFile);
        DataOutputStream dos = new DataOutputStream(Files.newOutputStream(outRClassFile.toPath()));

        if (packageName != null && !packageName.isEmpty())
            dos.writeBytes(String.format("package %s;\n\n", packageName));

        dos.writeBytes("public final class R {\n\n");

        while(scanner.hasNextLine()) {
            String line = scanner.nextLine();
            if (line.matches(".*<public.+>"))
                parsePublic(line.trim(), dos);
        }

        dos.writeBytes("    }\n}");
    }

    private static void parseOption(String[] args, int i, String arg) {
        switch (arg) {
            case "-h":
                printHelpMessage();
                System.exit(0);
            case "-f":
            case "--file":  
                publicXmlFile = new File(args[++i]);
                break;
            case "-p":
            case "--package":
                packageName = args[++i];
                break;
            case "-o":
            case "--output":
                outRClassFile = new File(args[++i]);
                break;
            case "-c":
            case "--comments":
                addComments = true;
                break;   
        }
    }

    private static void parsePublic(String src, DataOutputStream dos) 
        throws IOException {
        Map<String, String> attrs = new HashMap<>();

        String[] splits = src.split("\\s+");
        for (int i = 1; i < splits.length - 1; i++) {
            String split = splits[i];
            String aname = split.substring(0, split.indexOf("="));
            String avalue = split.substring(split.indexOf("\"") + 1, split.lastIndexOf("\""));
        
            attrs.put(aname, avalue);
        }

        String type = attrs.get("type");
        if (!subclassNames.contains(type)) {
            if (subclassNames.size() > 0)
                dos.writeBytes("    }\n\n");
            dos.writeBytes(String.format("    public static final class %s {\n", type));
            subclassNames.add(type);
        }

        String name = attrs.get("name");
        String id = attrs.get("id");

        StringBuilder fieldBuilder = new StringBuilder("        public static final int ")
            .append(name.replace(".", "_"))
            .append(" = ")
            .append(id)
            .append(";");
        if (addComments) 
            fieldBuilder.append(" // ").append(Long.parseUnsignedLong(id.substring(2), 16));
            
        dos.writeBytes(fieldBuilder.append("\n").toString());    
    }

    private static void printHelpMessage() {
        System.out.println("RClassGenerator - R.java file generator with ids from public.xml");
        System.out.println();
        System.out.println("usage: rclass-generator [-h] -f <pathname> -p <packagename> [-c]");
        System.out.println(" -h, --help\t\t\tShow this help message and exit");
        System.out.println(" -f, --file <pathname>\t\tPathname of public.xml");
        System.out.println(" -p, --package <packagename>\tPackagename of generated class");
        System.out.println(" -o, --output\t\t\tOutput R.java file");
        System.out.println(" -c, --comments\t\t\tAdd comments with decimal ids");
    }
}