package me.tatarka.fasax.internal;

import com.sun.codemodel.JClass;
import com.sun.codemodel.JCodeModel;

import java.util.Locale;

class Node {
    private final String field;
    private final String name;
    private final String type;

    Node(String field, String name, String type) {
        this.field = field;
        this.name = name;
        this.type = type;
    }

    String field() {
        return field;
    }

    String name() {
        return name;
    }

    String type() {
        return type;
    }

    String elemName() {
        return name;
    }

    String elemType() {
        return type;
    }

    String staticName() {
        return name.toUpperCase(Locale.US);
    }

    String testName() {
        return name;
    }

    boolean isAttribute() {
        return false;
    }

    boolean isText() {
        return false;
    }

    final boolean isList() {
        return this instanceof IsList;
    }

    final boolean isNested() {
        return this instanceof IsNested;
    }

    JClass typeRef(JCodeModel m) {
       return m.ref(type);
    }

    static class Attribute extends Node {
        Attribute(String field, String name, String type) {
            super(field, name, type);
        }

        @Override
        boolean isAttribute() {
            return true;
        }
    }

    static class Text extends Node {
        Text(String field, String type) {
            super(field, null, type);
        }

        @Override
        boolean isText() {
            return true;
        }
    }

    static class Nested extends Node implements IsNested {
        private String parserClassName;

        Nested(String field, String name, String type, String parserClassName) {
            super(field, name, type);
            this.parserClassName = parserClassName;
        }

        @Override
        public String parserClass() {
            return parserClassName;
        }
    }

    static class List extends Node implements IsList {
        protected final String entry;
        protected final String entryType;

        List(String field, String name, String type, String entry, String entryType) {
            super(field, name, type);
            this.entry = entry;
            this.entryType = entryType;
        }

        @Override
        String elemName() {
            return entry;
        }

        @Override
        String elemType() {
            return entryType;
        }

        @Override
        JClass typeRef(JCodeModel m) {
            return m.ref(type()).narrow(m.ref(entryType));
        }

        @Override
        public boolean isInline() {
            return false;
        }
    }

    static class NestedList extends List implements IsNested {
        private String parserClassName;

        NestedList(String field, String name, String type, String entry, String entryType, String parserClassName) {
            super(field, name, type, entry, entryType);
            this.parserClassName = parserClassName;
        }

        @Override
        public String parserClass() {
            return parserClassName;
        }
    }

    static class InlineList extends List {
        InlineList(String field, String name, String type, String entry, String entryType) {
            super(field, name, type, entry, entryType);
        }

        @Override
        public boolean isInline() {
            return true;
        }

        @Override
        String testName() {
            return elemName();
        }
    }

    static class NestedInlineList extends InlineList implements IsNested {
        private String parserClassName;

        NestedInlineList(String field, String name, String type, String entry, String entryType, String parserClassName) {
            super(field, name, type, entry, entryType);
            this.parserClassName = parserClassName;
        }

        @Override
        public String parserClass() {
            return parserClassName;
        }
    }

    interface IsNested {
       String parserClass();
    }

    interface IsList {
        boolean isInline();
    }
}
