package com.cloudurable.java2csv;

import java.util.List;

public class Item {
    private final String importBody;
    private final String body;
    private final String javadoc;
    private final String fullyQualifiedName;

    private final String simpleName;
    private final String definition;
    private final String parent;

    private final JavaItemType type;

    public Item(String importBody, String body, String javadoc, String fullyQualifiedName,
                String simpleName, String definition, String parent, JavaItemType type) {
        this.importBody = orEmptyString(importBody);
        this.body = orEmptyString(body);
        this.javadoc = orEmptyString(javadoc);
        this.fullyQualifiedName = orEmptyString(fullyQualifiedName);
        this.simpleName = orEmptyString(simpleName);
        this.definition = orEmptyString(definition);
        this.parent = orEmptyString(parent);
        this.type = type;
    }

    public static List<String> headers() {
        return List.of("Name", "Type", "FullName", "Definition", "JavaDoc", "Parent", "Imports", "Body");
    }

    public static Builder builder() {
        return new Builder();
    }

    private String orEmptyString(String part) {
        return part == null ? "" : part;
    }

    public List<String> row() {
        return List.of(simpleName, type.toString().toLowerCase(), fullyQualifiedName, definition, javadoc, parent, importBody, body);
    }

    public String getImportBody() {
        return importBody;
    }

    public String getSimpleName() {
        return simpleName;
    }

    public String getBody() {
        return body;
    }

    public String getJavadoc() {
        return javadoc;
    }

    public String getFullyQualifiedName() {
        return fullyQualifiedName;
    }

    public String getDefinition() {
        return definition;
    }

    public String getParent() {
        return parent;
    }

    public JavaItemType getType() {
        return type;
    }

    @Override
    public String toString() {
        return "Item{" +
                "simpleName='" + simpleName + '\'' +
                ", type=" + type +
                ", name='" + fullyQualifiedName + '\'' +
                ", definition='" + definition + '\'' +
                ", parent=" + parent +
                ", importBody='" + importBody + '\'' +
                ", javadoc='" + javadoc + '\'' +
                ", body='" + body + '\'' +
                '}';
    }

    public static class Builder {
        private String body;
        private String javadoc;
        private String name;
        private String definition;
        private String parent;
        private String importBody;
        private String simpleName;
        private JavaItemType type;

        private Builder() {
        }

        public Builder simpleName(String simpleName) {
            this.simpleName = simpleName;
            return this;
        }

        public Builder type(JavaItemType type) {
            this.type = type;
            return this;
        }

        public Builder importBody(String importBody) {
            this.importBody = importBody;
            return this;
        }

        public Builder body(String body) {
            this.body = body;
            return this;
        }

        public Builder javadoc(String javadoc) {
            this.javadoc = javadoc;
            return this;
        }

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder definition(String definition) {
            this.definition = definition;
            return this;
        }

        public Builder parent(String parent) {
            this.parent = parent;
            return this;
        }

        public Item build() {
            return new Item(importBody, body, javadoc, name, simpleName, definition, parent, type);
        }
    }

}
