package me.tatarka.fasax.internal;

import com.sun.codemodel.CodeWriter;
import com.sun.codemodel.JBlock;
import com.sun.codemodel.JClass;
import com.sun.codemodel.JClassAlreadyExistsException;
import com.sun.codemodel.JCodeModel;
import com.sun.codemodel.JConditionalFix;
import com.sun.codemodel.JDefinedClass;
import com.sun.codemodel.JExpression;
import com.sun.codemodel.JMethod;
import com.sun.codemodel.JPackage;
import com.sun.codemodel.JSwitch;
import com.sun.codemodel.JVar;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static com.sun.codemodel.JConditionalFix._if;
import static com.sun.codemodel.JExpr._new;
import static com.sun.codemodel.JExpr._null;
import static com.sun.codemodel.JExpr._super;
import static com.sun.codemodel.JExpr.invoke;
import static com.sun.codemodel.JExpr.lit;
import static com.sun.codemodel.JExpr.ref;
import static com.sun.codemodel.JMod.FINAL;
import static com.sun.codemodel.JMod.PRIVATE;
import static com.sun.codemodel.JMod.PUBLIC;
import static com.sun.codemodel.JMod.STATIC;

public final class FasaxReaderGenerator {
    public static final String SUFFIX = "$$SaxParser";
    private static final String TYPE_CONV_SUFFIX = "TypeConverter";

    private final String classPackage;
    private final String className;
    private final String rootName;
    private final String parserClassName;

    private final List<Node> elements = new ArrayList<Node>();
    private final List<Node> attributes = new ArrayList<Node>();
    private final List<ChildNode> children = new ArrayList<ChildNode>();
    private final List<ListNode> elementLists = new ArrayList<ListNode>();
    private final List<ListChildNode> listChildren = new ArrayList<ListChildNode>();
    private final List<TypeConverter> primitiveTypeConverters = new ArrayList<TypeConverter>();
    private final Map<String, CustomTypeConverter> customTypeConverters = new LinkedHashMap<String, CustomTypeConverter>();

    FasaxReaderGenerator(String classPackage, String className, String rootName) {
        this.classPackage = classPackage;
        this.className = className;
        this.rootName = rootName;

        parserClassName = getParserClassName(classPackage, className);

        primitiveTypeConverters.add(new StringTypeConverter());
        primitiveTypeConverters.add(new BooleanTypeConverter());
        primitiveTypeConverters.add(new IntTypeConverter());
        primitiveTypeConverters.add(new FloatTypeConverter());
        primitiveTypeConverters.add(new DoubleTypeConverter());
        primitiveTypeConverters.add(new CharTypeConverter());
    }

    void addElement(String field, String name, String type) {
        elements.add(new Node(field, name, type));
    }

    void addAttribute(String field, String name, String type) {
        attributes.add(new Node(field, name, type));
    }

    void addChild(String field, String name, String classPackage, String type) {
        children.add(new ChildNode(field, name, type, getParserClassName(classPackage, type)));
    }

    void addElementList(String field, String name, String entry, String type, String entryType) {
        elementLists.add(new ListNode(field, name, type, entry, entryType));
    }

    void addListChild(String field, String name, String entry, String classPackage, String listType, String entryType) {
        listChildren.add(new ListChildNode(field, name, listType, entry, entryType, getParserClassName(classPackage, entryType)));
    }

    void addTypeConverter(String field, String type) {
        customTypeConverters.put(field, new CustomTypeConverter(field + TYPE_CONV_SUFFIX, type));
    }

    boolean isPrimitive(String type) {
        for (TypeConverter typeConverter : primitiveTypeConverters) {
            if (typeConverter.matches(type)) return true;
        }
        return false;
    }

    void brewJava(CodeWriter codeWriter) throws IOException {
        try {
            JCodeModel m = new JCodeModel();
            JPackage pkg = m._package(classPackage);

            JClass className = m.ref(this.className);
            JDefinedClass clazz = pkg._class(PUBLIC, parserClassName)._extends(m.ref(FasaxHandler.class).narrow(className));
            emitStates(clazz);
            emitTypeConverters(clazz, m);
            emitChildren(clazz, m);
            emitStartDocument(clazz, className);
            emitStartElement(clazz, m);
            emitEndElement(clazz);
            emitCharacters(clazz);

            m.build(codeWriter);
        } catch (JClassAlreadyExistsException e) {
            throw new RuntimeException(e);
        }
    }

    private void emitStates(JDefinedClass clazz) {
        for (int i = 0; i < children.size(); i++) {
            clazz.field(PRIVATE | STATIC | FINAL, int.class, children.get(i).staticName()).init(lit(i + 1));
        }

        for (int i = 0; i < elementLists.size(); i++) {
            clazz.field(PRIVATE | STATIC | FINAL, int.class, elementLists.get(i).staticName()).init(lit(i + children.size() + 1));
        }

        for (int i = 0; i < listChildren.size(); i++) {
            clazz.field(PRIVATE | STATIC | FINAL, int.class, listChildren.get(i).staticName()).init(lit(i + children.size() + elementLists.size() + 1));
        }
    }

    private void emitTypeConverters(JDefinedClass clazz, JCodeModel m) {
        for (CustomTypeConverter t : customTypeConverters.values()) {
            clazz.field(PRIVATE | FINAL, m.ref(t.type), t.name).init(_new(m.ref(t.type)));
        }
    }

    private void emitChildren(JDefinedClass clazz, JCodeModel m) {
        for (ChildNode c : children) {
            clazz.field(PRIVATE | FINAL, m.ref(c.childType), c.field).init(_new(m.ref(c.childType)));
        }

        for (ListChildNode el : listChildren) {
            clazz.field(PRIVATE | FINAL, m.ref(el.childType), el.field).init(_new(m.ref(el.childType)));
        }
    }

    private void emitStartDocument(JDefinedClass clazz, JClass className) {
        JMethod method = clazz.method(PUBLIC, void.class, "startDocument")._throws(SAXException.class);
        method.annotate(Override.class);
        JBlock body = method.body();
        body.invoke(_super(), "startDocument");

        if (rootName != null && !rootName.isEmpty()) {
            body.assign(ref("rootName"), lit(rootName));
        }

        body.assign(ref("result"), _new(className));

        for (Node c : children) {
            body.add(ref(c.field).invoke("startDocument"));
        }
    }

    private void emitStartElement(JDefinedClass clazz, JCodeModel m) {
        JMethod method = clazz.method(PUBLIC, void.class, "startElement")._throws(SAXException.class);
        method.annotate(Override.class);
        JVar uri = method.param(String.class, "uri");
        JVar localName = method.param(String.class, "localName");
        JVar qName = method.param(String.class, "qName");
        JVar attributes = method.param(Attributes.class, "attributes");
        JBlock body = method.body();
        body.invoke(_super(), "startElement").arg(uri).arg(localName).arg(qName).arg(attributes);

        if (children.isEmpty() && elementLists.isEmpty() && listChildren.isEmpty()) {
            emitAttributes(body, qName, attributes);
        } else {
            JConditionalFix _if = null;
            for (Node c : children) {
                JExpression test = lit(c.name).invoke("equals").arg(qName);
                JConditionalFix cond = _if == null ? _if = _if(test) : _if._elseif(test);
                cond._then().assign(ref("state"), ref(c.staticName()));
            }
            for (ListNode el : elementLists) {
                JExpression test = lit(el.name).invoke("equals").arg(qName);
                JConditionalFix cond = _if == null ? _if = _if(test) : _if._elseif(test);
                cond._then()
                        .assign(ref("state"), ref(el.staticName()))
                        .assign(ref("result").ref(el.field), _new(m.ref(el.listType).narrow(m.ref(el.type))));
            }
            for (ListNode el : listChildren) {
                JExpression test = lit(el.name).invoke("equals").arg(qName);
                JConditionalFix cond = _if == null ? _if = _if(test) : _if._elseif(test);
                cond._then()
                        .assign(ref("state"), ref(el.staticName()))
                        .assign(ref("result").ref(el.field), _new(m.ref(el.listType).narrow(m.ref(el.type))));
            }
            if (_if != null) body.add(_if);

            if (!(children.isEmpty() && listChildren.isEmpty())) {
                JSwitch _switch = body._switch(ref("state"));
                JBlock rootBody = _switch._case(ref("ROOT")).body();
                emitAttributes(rootBody, qName, attributes);
                rootBody._break();

                for (Node c : children) {
                    JBlock _case = _switch._case(ref(c.staticName())).body();
                    _case.invoke(ref(c.field), "startElement").arg(uri).arg(localName).arg(qName).arg(attributes);
                    _case._break();
                }

                for (ListNode el : listChildren) {
                    JBlock _case = _switch._case(ref(el.staticName())).body();
                    _case._if(lit(el.entry).invoke("equals").arg(qName))._then()
                            .invoke(ref(el.field), "startDocument");
                    _case.invoke(ref(el.field), "startElement").arg(uri).arg(localName).arg(qName).arg(attributes);
                    _case._break();
                }
            }
        }
    }

    private void emitAttributes(JBlock body, JVar qName, JVar attributes) {
        if (!this.attributes.isEmpty()) {
            JBlock attrBody = body._if(ref("rootName").invoke("equals").arg(qName))._then();
            for (Node a : this.attributes) {
                attrBody.assign(
                        ref("result").ref(a.field),
                        emitConverter(a, attributes.invoke("getValue").arg(a.name))
                );
            }
        }
    }

    private void emitEndElement(JDefinedClass clazz) {
        JMethod method = clazz.method(PUBLIC, void.class, "endElement")._throws(SAXException.class);
        method.annotate(Override.class);
        JVar uri = method.param(String.class, "uri");
        JVar localName = method.param(String.class, "localName");
        JVar qName = method.param(String.class, "qName");
        JBlock body = method.body();
        body.invoke(_super(), "endElement").arg(uri).arg(localName).arg(qName);

        if (children.isEmpty() && elementLists.isEmpty() && listChildren.isEmpty()) {
            emitElements(body, qName);
            body.assign(ref("characters"), _null());
        } else {
            JConditionalFix _if = null;
            for (Node c : children) {
                JExpression test = lit(c.name).invoke("equals").arg(qName);
                JConditionalFix cond = _if == null ? _if = _if(test) : _if._elseif(test);
                cond._then()
                        .assign(ref("state"), ref("ROOT"))
                        .assign(ref("result").ref(c.field), ref(c.field).invoke("getResult"));
            }
            for (ListNode el : elementLists) {
                JExpression test = lit(el.name).invoke("equals").arg(qName);
                JConditionalFix cond = _if == null ? _if = _if(test) : _if._elseif(test);
                cond._then().assign(ref("state"), ref("ROOT"));
            }
            for (ListNode el : listChildren) {
                JExpression test = lit(el.name).invoke("equals").arg(qName);
                JConditionalFix cond = _if == null ? _if = _if(test) : _if._elseif(test);
                cond._then() .assign(ref("state"), ref("ROOT"));
            }
            if (_if != null) body.add(_if);

            JSwitch _switch = body._switch(ref("state"));
            JBlock rootBody = _switch._case(ref("ROOT")).body();
            emitElements(rootBody, qName);
            rootBody.assign(ref("characters"), _null());
            rootBody._break();

            for (Node c : children) {
                JBlock _case = _switch._case(ref(c.staticName())).body();
                _case.invoke(ref(c.field), "endElement").arg(uri).arg(localName).arg(qName);
                _case._break();
            }
            for (ListNode el : elementLists) {
                JBlock _case = _switch._case(ref(el.staticName())).body();
                _case._if(lit(el.entry).invoke("equals").arg(qName))
                        ._then().invoke(ref("result").ref(el.field), "add")
                        .arg(emitConverter(el, ref("characters").invoke("toString")));
                _case.assign(ref("characters"), _null());
                _case._break();
            }
            for (ListNode el : listChildren) {
                JBlock _case = _switch._case(ref(el.staticName())).body();
                _case.invoke(ref(el.field), "endElement").arg(uri).arg(localName).arg(qName);
                _case._if(lit(el.entry).invoke("equals").arg(qName))._then()
                        .invoke(ref("result").ref(el.field), "add").arg(ref(el.field).invoke("getResult"));
                _case._break();
            }

        }
    }

    private void emitElements(JBlock body, JVar qName) {
        if (!elements.isEmpty()) {
            JConditionalFix _if = null;
            for (Node e : elements) {
                JExpression test = lit(e.name).invoke("equals").arg(qName);
                JConditionalFix cond = _if == null ? _if = _if(test) : _if._elseif(test);
                cond._then().assign(
                            ref("result").ref(e.field),
                            emitConverter(e, ref("characters").invoke("toString"))
                    );
            }
            body.add(_if);
        }
    }

    private void emitCharacters(JDefinedClass clazz) {
        if (children.isEmpty() && listChildren.isEmpty()) return;

        JMethod method = clazz.method(PUBLIC, void.class, "characters")._throws(SAXException.class);
        method.annotate(Override.class);
        JVar ch = method.param(char[].class, "ch");
        JVar start = method.param(int.class, "start");
        JVar length = method.param(int.class, "length");
        JBlock body = method.body();

        JSwitch _switch = body._switch(ref("state"));

        JBlock rootCase = _switch._case(ref("ROOT")).body();
        rootCase.invoke(_super(), "characters").arg(ch).arg(start).arg(length);
        rootCase._break();

        for (Node c : children) {
            JBlock _case = _switch._case(ref(c.staticName())).body();
            _case.invoke(ref(c.field), "characters").arg(ch).arg(start).arg(length);
            _case._break();
        }
        for (ListNode el : listChildren) {
            JBlock _case = _switch._case(ref(el.staticName())).body();
            _case.invoke(ref(el.field), "characters").arg(ch).arg(start).arg(length);
            _case._break();
        }
    }

    private JExpression emitConverter(Node e, JExpression expr) {
        TypeConverter typeConverter = findTypeConverter(e.field, e.type);
        if (typeConverter == null) throw new RuntimeException("Don't know how to convert \"" + e.field + "\" to type \"" + e.type + "\"");
        return typeConverter.emitConverter(expr);
    }

    private TypeConverter findTypeConverter(String field, String type) {
        // First try the custom ones
        TypeConverter typeConverter = customTypeConverters.get(field);
        if (typeConverter != null) return typeConverter;
        // Then try the built-in ones
        for (TypeConverter t : primitiveTypeConverters) {
            if (t.matches(type)) {
                return t;
            }
        }
        return null;
    }

    private static boolean typesEq(String typeName, Class<?> type) {
        return typeName.equals(type.getCanonicalName());
    }

    private static String getParserClassName(String classPackage, String className) {
        int packageLen = classPackage.length() + 1;
        return className.substring(packageLen).replace('.', '$') + SUFFIX;
    }

    private static class Node {
        final String field;
        final String name;
        final String type;

        Node(String field, String name, String type) {
            this.field = field;
            this.name = name;
            this.type = type;
        }

        String staticName() {
            return name.toUpperCase(Locale.US);
        }
    }

    private static class ChildNode extends Node {
        final String childType;

        ChildNode(String field, String name, String type, String childType) {
            super(field, name, type);
            this.childType = childType;
        }
    }

    private static class ListNode extends Node {
        final String entry;
        final String listType;

        ListNode(String field, String name, String listType, String entry, String entryType) {
            super(field, name, entryType);
            this.entry = entry;
            this.listType = listType;
        }
    }

    private static class ListChildNode extends ListNode {
        final String childType;
        ListChildNode(String field, String name, String listType, String entry, String entryType, String childType) {
            super(field, name, listType, entry, entryType);
            this.childType = childType;
        }
    }

    private interface TypeConverter {
        boolean matches(String type);
        JExpression emitConverter(JExpression expr);
    }

    private static class CustomTypeConverter implements TypeConverter {
        final String name;
        final String type;

        CustomTypeConverter(String name, String type) {
            this.name = name;
            this.type = type;
        }

        @Override
        public boolean matches(String type) {
            return this.type.equals(type);
        }

        @Override
        public JExpression emitConverter(JExpression expr) {
            return ref(name).invoke("convert").arg(expr);
        }
    }

    private static class StringTypeConverter implements TypeConverter {
        @Override
        public boolean matches(String type) {
            return typesEq(type, String.class);
        }

        @Override
        public JExpression emitConverter(JExpression expr) {
            return invoke("toString").arg(expr);
        }
    }

    private static class BooleanTypeConverter implements TypeConverter {
        @Override
        public boolean matches(String type) {
            return typesEq(type, boolean.class) || typesEq(type, Boolean.class);
        }

        @Override
        public JExpression emitConverter(JExpression expr) {
            return invoke("toBoolean").arg(expr);
        }
    }

    private static class IntTypeConverter implements TypeConverter {
        @Override
        public boolean matches(String type) {
            return typesEq(type, int.class) || typesEq(type, Integer.class);
        }

        @Override
        public JExpression emitConverter(JExpression expr) {
            return invoke("toInt").arg(expr);
        }
    }

    private static class FloatTypeConverter implements TypeConverter {
        @Override
        public boolean matches(String type) {
            return typesEq(type, float.class) || typesEq(type, Float.class);
        }

        @Override
        public JExpression emitConverter(JExpression expr) {
            return invoke("toFloat").arg(expr);
        }
    }

    private static class DoubleTypeConverter implements TypeConverter {
        @Override
        public boolean matches(String type) {
            return typesEq(type, double.class) || typesEq(type, Double.class);
        }

        @Override
        public JExpression emitConverter(JExpression expr) {
            return invoke("toDouble").arg(expr);
        }
    }

    private static class CharTypeConverter implements TypeConverter {
        @Override
        public boolean matches(String type) {
            return typesEq(type, char.class) || typesEq(type, Character.class);
        }

        @Override
        public JExpression emitConverter(JExpression expr) {
            return invoke("toChar").arg(expr);
        }
    }
}
