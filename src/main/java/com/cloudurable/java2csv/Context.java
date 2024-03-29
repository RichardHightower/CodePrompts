package com.cloudurable.java2csv;

import java.util.List;
import java.util.Map;

public class Context {

    private final String promptDelimiter;

    private final String completionEndDelimiter;

    private final  List<Item> methods;
    private final  List<Item> fields;
    private final  List<Item> classes ;
    private final  List<Item> enums;

    private final  Map<String, List<Item>>methodMap;
    private final  Map<String, List<Item>> fieldMap ;
    private final  Map<String, List<Item>>innerClassesMap;
    private final String promptPrefix;

    private final String completionPrefix;

    public String getPromptDelimiter() {
        return promptDelimiter;
    }

    public String getCompletionEndDelimiter() {
        return completionEndDelimiter;
    }

    public List<Item> getMethods() {
        return methods;
    }

    public List<Item> getFields() {
        return fields;
    }

    public List<Item> getClasses() {
        return classes;
    }

    public List<Item> getEnums() {
        return enums;
    }

    public Map<String, List<Item>> getMethodMap() {
        return methodMap;
    }

    public Map<String, List<Item>> getFieldMap() {
        return fieldMap;
    }

    public Map<String, List<Item>> getInnerClassesMap() {
        return innerClassesMap;
    }

    public Context(String promptDelimiter, String completionEndDelimiter, List<Item> methods, List<Item> fields, List<Item> classes, List<Item> enums,
                   Map<String, List<Item>> methodMap, Map<String, List<Item>> fieldMap, Map<String, List<Item>> innerClassesMap, String prefix, String completionPrefix) {
        this.promptDelimiter = promptDelimiter;
        this.completionEndDelimiter = completionEndDelimiter;
        this.methods = methods;
        this.fields = fields;
        this.classes = classes;
        this.enums = enums;
        this.methodMap = methodMap;
        this.fieldMap = fieldMap;
        this.innerClassesMap = innerClassesMap;
        this.promptPrefix = prefix;
        this.completionPrefix = completionPrefix;
    }

    public static Builder builder() {
        return new Builder();
    }

    public String getPromptPrefix() {
        return promptPrefix;
    }

    public String getCompletionPrefix() {
        return completionPrefix;
    }

    public static class Builder {

        private Builder(){}

        private   List<Item> methods;
        private   List<Item> fields;
        private   List<Item> classes ;
        private   List<Item> enums;

        private   Map<String, List<Item>> methodMap;
        private   Map<String, List<Item>> fieldMap ;
        private   Map<String, List<Item>> innerClassesMap;

        private String promptDelimiter = " ###-->";

        private String completionEndDelimiter = " ###END";

        private  String promptPrefix ="";

        private  String completionPrefix=" ";


        public Builder completionPrefix(String completionPrefix) {
            this.completionPrefix = completionPrefix;
            return this;
        }

        public Builder promptDelimiter(String promptDelimiter) {
            this.promptDelimiter = promptDelimiter;
            return this;
        }

        public Builder completionEndDelimiter(String completionEndDelimiter) {
            this.completionEndDelimiter = completionEndDelimiter;
            return this;
        }


        public Builder promptPrefix(String prefix) {
            this.promptPrefix = prefix;
            return this;
        }

        public Builder methods(List<Item> methods) {
            this.methods = methods;
            return this;
        }


        public Builder fields(List<Item> fields) {
            this.fields = fields;
            return this;
        }

        public Builder classes(List<Item> classes) {
            this.classes = classes;
            return this;
        }

        public Builder enums(List<Item> enums) {
            this.enums = enums;
            return this;
        }

        public Builder methodMap(Map<String, List<Item>> methodMap) {
            this.methodMap = methodMap;
            return this;
        }


        public Builder fieldMap(Map<String, List<Item>> fieldMap) {
            this.fieldMap = fieldMap;
            return this;
        }


        public Builder innerClassMap(Map<String, List<Item>> innerClassesMap) {
            this.innerClassesMap = innerClassesMap;
            return this;
        }

        public Context build() {
            return new Context(promptDelimiter, completionEndDelimiter, methods, fields, classes, enums, methodMap,
                    fieldMap, innerClassesMap, promptPrefix, completionPrefix);
        }
    }
}
