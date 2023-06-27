package com.cloudurable.java2csv;

import com.opencsv.CSVReader;
import com.opencsv.CSVWriter;
import com.opencsv.exceptions.CsvValidationException;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;


/**
 * Parse Java files and turn them into CSV files.
 */
public class CodePrompts {


    public static void main(String[] args) throws IOException {
        System.out.println(new File(".").getCanonicalFile());
        try {
            final String outputDirectory = args.length > 0 ? args[0] : "prompts";
            final String inputFileName = args.length > 1 ? args[1] : "input.csv";

            final File dir = new File(outputDirectory).getCanonicalFile();
            if (!dir.exists()) {
                boolean success = dir.mkdirs();
                if (!success) {
                    System.out.println("Unable to make directories");
                    System.exit(-1);
                }
            }

            final File inputFile = new File(inputFileName);
            if (!inputFile.exists()) {
                throw new FileNotFoundException("Input file does not exist " + inputFileName);
            }

            final Context context = createContext(inputFile);
            writeClassPrompts(context, outputDirectory);
        } catch (Exception ex) {
            ex.printStackTrace();
            System.exit(-6);
        }
    }

    private static Context createContext(File inputFile) throws IOException, CsvValidationException {
        final List<Item> items = readCSVFile(inputFile);


        final List<Item> methods = items.stream().filter(item -> item.getType() == JavaItemType.METHOD).collect(Collectors.toList());
        final List<Item> fields = items.stream().filter(item -> item.getType() == JavaItemType.FIELD).collect(Collectors.toList());
        final List<Item> classes = items.stream().filter(item -> item.getType() == JavaItemType.CLASS)
                .filter(item -> item.getParent() == null || item.getParent().isBlank())
                .collect(Collectors.toList());
        final List<Item> enums = items.stream().filter(item -> item.getType() == JavaItemType.ENUM).collect(Collectors.toList());


        final Map<String, List<Item>> methodMap = methods.stream().collect(Collectors.groupingBy(Item::getParent, Collectors.toList()));
        final Map<String, List<Item>> fieldMap = fields.stream().collect(Collectors.groupingBy(Item::getParent, Collectors.toList()));

        final Map<String, List<Item>> innerClassesMap = classes.stream()
                .filter(item -> item.getParent() != null && !item.getParent().isBlank()).collect(Collectors.groupingBy(Item::getParent, Collectors.toList()));

        return Context.builder().classes(classes).methods(methods).fields(fields).enums(enums)
                .methodMap(methodMap).fieldMap(fieldMap).innerClassMap(innerClassesMap).build();
    }

    private static void writeClassPrompts(Context context, String outputDirectory) throws IOException {
        final File outputFile = new File(outputDirectory, "classPrompts.csv");
        try (CSVWriter writer = new CSVWriter(new FileWriter(outputFile))) {
            writer.writeNext(new String[]{"prompt", "completion"});
            context.getClasses().forEach(item -> {
                writeClassDefinitionPrompt(writer, context, item);
                writeClassBodyPrompt(writer, context, item);
                writeJavaDocPrompt(writer, context, item);
                writeInnerClassesPrompt(writer, context, item);
                writeImportsPrompt(writer, context, item);
                writeMethodsPrompt(writer, context, item);
                writeFieldsPrompt(writer, context, item);
            });
        }
    }

    private static void writeClassDefinitionPrompt(CSVWriter writer, Context context, Item item) {
        String prompt = String.format("%sHow is class %s (%s) defined?  ##-->", context.getPrefix(), item.getName(), item.getSimpleName());
        String completion = item.getDefinition();
        writer.writeNext(new String[]{prompt, completion + " ##END"});
    }

    private static void writeClassBodyPrompt(CSVWriter writer, Context context, Item item) {
        String prompt = String.format("%sWhat is the source code for class named %s with fully qualified name %s?  ##-->", context.getPrefix(), item.getName(), item.getSimpleName());
        String completion = item.getBody();
        writer.writeNext(new String[]{prompt, completion + " ##END"});
    }

    private static void writeJavaDocPrompt(CSVWriter writer, Context context, Item item) {
        if (!item.getJavadoc().isBlank()) {
            String prompt = String.format("%sWhat does class named %s with fully qualified name %s do according to the JavaDoc?  ##-->", context.getPrefix(), item.getName(), item.getSimpleName());
            String completion = item.getJavadoc();
            writer.writeNext(new String[]{prompt, completion + " ##END"});
        }
    }

    private static void writeInnerClassesPrompt(CSVWriter writer, Context context, Item item) {
        String prompt = String.format("%sWhat are the inner classes defined inside of class named %s with fully qualified name %s?  ##-->", context.getPrefix(), item.getName(), item.getSimpleName());
        Optional<List<Item>> innerClasses = Optional.ofNullable(context.getInnerClassesMap().get(item.getName()));
        String completion = innerClasses.map(cList -> cList.stream().map(Item::getDefinition).collect(Collectors.joining("\n"))).orElse("");
        if (!completion.isBlank()) {
            writer.writeNext(new String[]{prompt, completion + " ##END"});
        }
    }

    private static void writeImportsPrompt(CSVWriter writer, Context context, Item item) {
        if (!item.getImportBody().isBlank()) {
            String prompt = String.format("%sWhat are the imports for class named %s with fully qualified name %s?  ##-->", context.getPrefix(), item.getName(), item.getSimpleName());
            String completion = item.getImportBody();
            writer.writeNext(new String[]{prompt, completion + " ##END"});
        }
    }

    private static void writeMethodsPrompt(CSVWriter writer, Context context, Item item) {
        String prompt = String.format("%sWhat methods does class named %s with fully qualified name %s have?  ##-->", context.getPrefix(), item.getName(), item.getSimpleName());
        Optional<List<Item>> methods = Optional.ofNullable(context.getMethodMap().get(item.getName()));
        String completion = methods.map(mList -> mList.stream().map(Item::getDefinition).collect(Collectors.joining("\n"))).orElse("");
        if (!completion.isBlank()) {
            writer.writeNext(new String[]{prompt, completion + " ##END"});
        }

        methods.ifPresent(methodList -> methodList.forEach(method -> writeEachMethodPrompt(writer, context, item, method)));
    }

    private static void writeEachMethodPrompt(CSVWriter writer, Context context, Item parent, Item item) {

        if (!item.getJavadoc().isBlank()) {
            String prompt = String.format("%sWhat does method %s (%s) do according to the JavaDoc?  ##-->",
                    context.getPrefix(), item.getName(), item.getSimpleName());
            String completion = item.getJavadoc();
            writer.writeNext(new String[]{prompt, completion + " ##END"});
        }

        String prompt = String.format("%sWhat is the source code for method %s (%s) from class %s?  ##-->", context.getPrefix(),
                item.getName(), item.getSimpleName(), parent.getName());
        String completion = item.getBody();
        writer.writeNext(new String[]{prompt, completion + " ##END"});

        String prompt1 = String.format("%sHow is the method %s (%s) from class %s defined?  ##-->", context.getPrefix(),
                item.getName(), item.getSimpleName(), parent.getName());
        String completion1 = item.getDefinition();
            writer.writeNext(new String[]{prompt1, completion1 + " ##END"});

    }

    private static void writeFieldsPrompt(CSVWriter writer, Context context, Item item) {
        String prompt = String.format("%sWhat fields does class named %s with fully qualified name %s have?  ##-->", context.getPrefix(), item.getName(), item.getSimpleName());
        Optional<List<Item>> fields = Optional.ofNullable(context.getFieldMap().get(item.getName()));
        String completion = fields.map(list -> list.stream().map(Item::getDefinition).collect(Collectors.joining("\n"))).orElse("");
        writer.writeNext(new String[]{prompt, completion + " ##END"});

        fields.ifPresent(fieldList -> fieldList.forEach(field -> writeEachFieldPrompt(writer, context, item, field)));
    }

    private static void writeEachFieldPrompt(CSVWriter writer, Context context, Item parent, Item item) {

        if (!item.getJavadoc().isBlank()) {
            String prompt = String.format("%sWhat is the field %s (%s) defined in class %s do according to the JavaDoc?  ##-->",
                    context.getPrefix(), item.getName(), item.getSimpleName(), parent.getName());
            String completion = item.getJavadoc();
            writer.writeNext(new String[]{prompt, completion + " ##END"});
        }

        String prompt1 = String.format("%sHow is the field %s (%s) which is in class %s defined?  ##-->", context.getPrefix(),
                item.getName(), item.getSimpleName(), parent.getName());
        String completion1 = item.getDefinition();
            writer.writeNext(new String[]{prompt1, completion1 + " ##END"});

    }


    private static List<Item> readCSVFile(File inputFile) throws IOException, CsvValidationException {

        final List<Item> items = new ArrayList<>(32);
        try (CSVReader reader = new CSVReader(new FileReader(inputFile))) {
            String[] nextLine;
            reader.readNext(); //skip first line
            while ((nextLine = reader.readNext()) != null) {

                if (nextLine.length != 8) {
                    throw new IllegalStateException("Must be 8 columns");
                }

                items.add(Item.builder()
                        .simpleName(nextLine[0])
                        .type(JavaItemType.valueOf(nextLine[1].toUpperCase()))
                        .name(nextLine[2])
                        .definition(nextLine[3])
                        .javadoc(nextLine[4])
                        .parent(nextLine[5])
                        .importBody(nextLine[6])
                        .body(nextLine[7])
                        .build());

            }
        }
        return items;
    }


}

