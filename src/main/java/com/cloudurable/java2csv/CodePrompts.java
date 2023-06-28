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
                .filter(item -> item.getParent().isBlank())
                .collect(Collectors.toList());
        final List<Item> enums = items.stream().filter(item -> item.getType() == JavaItemType.ENUM).collect(Collectors.toList());


        final Map<String, List<Item>> methodMap = methods.stream().collect(Collectors.groupingBy(Item::getParent, Collectors.toList()));
        final Map<String, List<Item>> fieldMap = fields.stream().collect(Collectors.groupingBy(Item::getParent, Collectors.toList()));

        final Map<String, List<Item>> innerClassesMap = items.stream().filter(item -> item.getType() == JavaItemType.CLASS)
                .filter(item -> !item.getParent().isBlank()).collect(Collectors.groupingBy(Item::getParent, Collectors.toList()));

        return Context.builder().classes(classes).methods(methods).fields(fields).enums(enums)
                .methodMap(methodMap).fieldMap(fieldMap).innerClassMap(innerClassesMap).build();
    }

    private static void writeClassPrompts(Context context, String outputDirectory) throws IOException {
        final File outputFile = new File(outputDirectory, "classPrompts.csv");
        try (CSVWriter writer = new CSVWriter(new FileWriter(outputFile))) {
            writer.writeNext(new String[]{"prompt", "completion"});
            context.getClasses().forEach(item -> {
                writeClassPromptDetails(writer, context, item);
                writeInnerClassesPromptForClassItem(writer, context, item);
                writeImportsPrompt(writer, context, item);
                writeMethodsPromptForClassItem(writer, context, item);
                writeFieldsPromptForClassItem(writer, context, item);
            });
        }
    }

    private static void writeClassPromptDetails(CSVWriter writer, Context context, Item item) {
        writeClassDefinitionPrompt(writer, context, item);
        writeClassBodyPrompt(writer, context, item);
        writeJavaDocPromptForClassItem(writer, context, item);
    }


    private static String createClassNamePrompt(final Item item) {
        return String.format("class named %s whose fully qualified class name %s",
                item.getSimpleName(), item.getFullyQualifiedName());
    }

    private static void writePrompt(CSVWriter writer, Context context, final String promptText,
                                    final String completionText) {
        final String prompt = String.format("%s%s%s", context.getPromptPrefix(), promptText, context.getPromptDelimiter());
        final String completion = String.format("%s%s%s", context.getCompletionPrefix(), completionText,
                context.getCompletionEndDelimiter());
        writer.writeNext(new String[]{prompt,completion});
    }

    private static void writeClassDefinitionPrompt(CSVWriter writer, Context context, Item item) {
        writePrompt(writer, context, String.format("How is %s declared?",
                createClassNamePrompt(item)), item.getDefinition());
    }

    private static void writeClassBodyPrompt(CSVWriter writer, Context context, Item item) {
        writePrompt(writer, context, String.format("How is %s defined?",
                createClassNamePrompt(item)), item.getBody());
    }

    private static void writeJavaDocPromptForClassItem(CSVWriter writer, Context context, Item item) {
        if (!item.getJavadoc().isBlank()) {
            writePrompt(writer, context, String.format("What does %s do according to the JavaDoc?",
                    createClassNamePrompt(item)), item.getJavadoc());
        }
    }

    private static void writeInnerClassesPromptForClassItem(final CSVWriter writer, final Context context, Item classItem) {
        final Optional<List<Item>> innerClasses = Optional.ofNullable(context.getInnerClassesMap().get(classItem.getFullyQualifiedName()));
        final String completion = innerClasses.map(cList -> cList.stream().map(Item::getDefinition).collect(Collectors.joining("\n"))).orElse("");
        if (!completion.isBlank()) {
            writePrompt(writer, context, String.format("What are the inner classes defined inside of %s?",
                    createClassNamePrompt(classItem)), completion);

        }
        innerClasses.ifPresent(innerClassesList -> innerClassesList.forEach(innerClass -> writeClassPromptDetails(writer, context, innerClass)));
    }

    private static void writeImportsPrompt(CSVWriter writer, Context context, Item item) {
        if (!item.getImportBody().isBlank()) {
            writePrompt(writer, context, String.format("What are the imports for %s?",
                    createClassNamePrompt(item)), item.getImportBody());
        }
    }

    private static void writeMethodsPromptForClassItem(CSVWriter writer, Context context, Item classItem) {
        Optional<List<Item>> methods = Optional.ofNullable(context.getMethodMap()
                .get(classItem.getFullyQualifiedName()));
        String completion = methods.map(mList -> mList.stream().map(Item::getDefinition)
                .collect(Collectors.joining("\n"))).orElse("");
        if (!completion.isBlank()) {
            writePrompt(writer, context, String.format("What methods does %s have?",
                    createClassNamePrompt(classItem)), completion);
        }
        methods.ifPresent(methodList -> methodList.forEach(method ->
                writeEachMethodPrompt(writer, context, classItem, method)));
    }

    private static void writeEachMethodPrompt(CSVWriter writer, Context context, Item parent, Item item) {

        if (!item.getJavadoc().isBlank()) {
            writePrompt(writer, context, String.format("What does method %s do according to the JavaDoc from %s?",
                    item.getSimpleName(), createClassNamePrompt(parent)), item.getJavadoc());
        }
        writePrompt(writer, context, String.format("How is method %s defined from class %s?",
                item.getSimpleName(), createClassNamePrompt(parent)), item.getBody());
        writePrompt(writer, context, String.format("What is the method %s signature from %s?",
                item.getSimpleName(), createClassNamePrompt(parent)), item.getDefinition());

    }


    private static void writeFieldsPromptForClassItem(CSVWriter writer, Context context, Item item) {
        Optional<List<Item>> fields = Optional.ofNullable(context.getFieldMap().get(item.getFullyQualifiedName()));
        String completion = fields.map(list -> list.stream().map(Item::getDefinition).collect(Collectors.joining("\n"))).orElse("");
        if (!completion.isBlank()) {
            writePrompt(writer, context, String.format("What fields does %s have?",
                    createClassNamePrompt(item)), completion);
        }
        fields.ifPresent(fieldList -> fieldList.forEach(field -> writeEachFieldPrompt(writer, context, item, field)));
    }

    private static void writeEachFieldPrompt(CSVWriter writer, Context context, Item parent, Item item) {

        if (!item.getJavadoc().isBlank()) {
            writePrompt(writer, context, String.format("What is the field %s defined in %s according to the JavaDoc?",
                    item.getSimpleName(), createClassNamePrompt(parent)), item.getJavadoc());
        }

        writePrompt(writer, context, String.format("How is the field %s defined which is in %s?",
                item.getSimpleName(), createClassNamePrompt(parent)), item.getDefinition());

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
                final Item item = Item.builder()
                        .simpleName(nextLine[0])
                        .type(JavaItemType.valueOf(nextLine[1].toUpperCase()))
                        .name(nextLine[2])
                        .definition(nextLine[3])
                        .javadoc(nextLine[4])
                        .parent(nextLine[5])
                        .importBody(nextLine[6])
                        .body(nextLine[7])
                        .build();
                items.add(item);
            }
        }
        return items;
    }


}

