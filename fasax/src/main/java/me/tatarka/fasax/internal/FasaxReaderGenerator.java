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
import java.util.Map;

import static com.sun.codemodel.JConditionalFix._if;
import static com.sun.codemodel.JExpr._new;
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
    private static final String TYPE_CONV_SUFFIX = "Converter";

    private final String classPackage;
    private final String className;
    private final String parserClassName;

    private final Nodes nodes = new Nodes();
    private final List<TypeConverter> primitiveTypeConverters = new ArrayList<TypeConverter>();
    private final Map<String, CustomTypeConverter> customTypeConverters = new LinkedHashMap<String, CustomTypeConverter>();

    FasaxReaderGenerator(String classPackage, String className) {
        this.classPackage = classPackage;
        this.className = className;

        parserClassName = getParserClassName(classPackage, className);

        primitiveTypeConverters.add(new StringTypeConverter());
        primitiveTypeConverters.add(new BooleanTypeConverter());
        primitiveTypeConverters.add(new IntTypeConverter());
        primitiveTypeConverters.add(new FloatTypeConverter());
        primitiveTypeConverters.add(new DoubleTypeConverter());
        primitiveTypeConverters.add(new CharTypeConverter());
    }

    void addElement(String field, String name, String type) {
        nodes.add(new Node(field, name, type));
    }

    void addAttribute(String field, String name, String type) {
        nodes.add(new Node.Attribute(field, name, type));
    }

    void addText(String field, String type) {
        nodes.add(new Node.Text(field, type));
    }

    void addNested(String field, String name, String classPackage, String type) {
        nodes.add(new Node.Nested(field, name, type, getParserClassName(classPackage, type)));
    }

    void addElementList(String field, String name, String entry, String type, String entryType) {
        nodes.add(new Node.List(field, name, type, entry, entryType));
    }

    void addInlineList(String field, String name, String entry, String type, String entryType) {
        nodes.add(new Node.InlineList(field, name, type, entry, entryType));
    }

    void addNestedList(String field, String name, String entry, String classPackage, String listType, String entryType) {
        nodes.add(new Node.NestedList(field, name, listType, entry, entryType, getParserClassName(classPackage, entryType)));
    }

    void addNestedInlineList(String field, String name, String entry, String classPackage, String listType, String entryType) {
        nodes.add(new Node.NestedInlineList(field, name, listType, entry, entryType, getParserClassName(classPackage, entryType)));
    }

    void addConverter(String field, String type) {
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
            emitNested(clazz, m);
            emitStartDocument(clazz, m);
            emitStartElement(clazz, m);
            emitEndElement(clazz);
            emitCharacters(clazz);

            m.build(codeWriter);
        } catch (JClassAlreadyExistsException e) {
            throw new RuntimeException(e);
        }
    }

    private void emitStates(JDefinedClass clazz) {
        Nodes requireState = nodes.requiresState();
        for (int i = 0; i < requireState.size(); i++) {
            clazz.field(PRIVATE | STATIC | FINAL, int.class, requireState.get(i).staticName()).init(lit(i + 1));
        }
    }

    private void emitTypeConverters(JDefinedClass clazz, JCodeModel m) {
        for (CustomTypeConverter t : customTypeConverters.values()) {
            JClass type = m.ref(t.type);
            clazz.field(PRIVATE | FINAL, type, t.name).init(_new(type));
        }
    }

    private void emitNested(JDefinedClass clazz, JCodeModel m) {
        for (Node n : nodes.nested()) {
            JClass type = m.ref(((Node.IsNested) n).parserClass());
            clazz.field(PRIVATE | FINAL, type, n.field()).init(_new(type));
        }
    }

    private void emitStartDocument(JDefinedClass clazz, JCodeModel m) {
        JMethod method = clazz.method(PUBLIC, void.class, "startDocument")._throws(SAXException.class);
        method.annotate(Override.class);
        JBlock body = method.body();
        body.invoke(_super(), "startDocument");

        body.assign(ref("result"), _new(m.ref(className)));

        for (Node n : nodes.inlineList()) {
            body.assign(ref("result").ref(n.field()), _new(n.typeRef(m)));
        }

        for (Node n : nodes.nested()) {
            body.invoke(ref(n.field()), "startDocument");
        }
    }

    private void emitStartElement(JDefinedClass clazz, JCodeModel m) {
        // @Override
        // public void startElement(String uri, String localName, String qName, String attributes) throws SAXException {
        JMethod method = clazz.method(PUBLIC, void.class, "startElement")._throws(SAXException.class);
        method.annotate(Override.class);
        JVar uri = method.param(String.class, "uri");
        JVar localName = method.param(String.class, "localName");
        JVar qName = method.param(String.class, "qName");
        JVar attributes = method.param(Attributes.class, "attributes");
        JBlock body = method.body();
        // super.startElement(uri, localName, qName, attributes);
        body.invoke(_super(), "startElement").arg(uri).arg(localName).arg(qName).arg(attributes);

        Nodes requireState = nodes.requiresState();
        if (requireState.isEmpty()) {
            emitAttributes(body, attributes);
            return;
        }

        emitStartTests(body, requireState, qName, m);

        Nodes nested = nodes.nested();
        if (nested.isEmpty()) return;

        emitStartSwitch(body, nested, uri, localName, qName, attributes);
    }

    private void emitAttributes(JBlock body, JVar attributes) {
        if (!nodes.attributes().isEmpty()) {
            for (Node a : nodes.attributes()) {
                body.assign(
                        ref("result").ref(a.field()),
                        emitConverter(a, attributes.invoke("getValue").arg(a.name()))
                );
            }
        }
    }

    private void emitStartTests(JBlock body, Nodes requireState, JVar qName, JCodeModel m) {
        JConditionalFix _if = null;
        for (Node n : requireState) {
            JExpression test = lit(n.testName()).invoke("equals").arg(qName);
            JConditionalFix cond = _if == null ? _if = _if(test) : _if._elseif(test);
            JBlock condBody = cond._then();
            condBody.assign(ref("state"), ref(n.staticName()));

            if (n.isList() && !((Node.IsList)n).isInline()) {
                condBody.assign(ref("result").ref(n.field()), _new(n.typeRef(m)));
            }
        }
        if (_if != null) body.add(_if);
    }

    private void emitStartSwitch(JBlock body, Nodes nested, JVar uri, JVar localName, JVar qName, JVar attributes) {
        JSwitch _switch = body._switch(ref("state"));
        JBlock rootBody = _switch._case(ref("ROOT")).body();
        emitAttributes(rootBody, attributes);
        rootBody._break();

        for (Node n : nested) {
            JBlock _case = _switch._case(ref(n.staticName())).body();
            if (n.isList()) {
                _case._if(lit(n.elemName()).invoke("equals").arg(qName))._then()
                        .invoke(ref(n.field()), "startDocument");
            }
            _case.invoke(ref(n.field()), "startElement").arg(uri).arg(localName).arg(qName).arg(attributes);
            _case._break();
        }
    }

    private void emitEndElement(JDefinedClass clazz) {
        // @Override
        // public void endElement(String uri, String localName, String qName) throws SAXException {
        JMethod method = clazz.method(PUBLIC, void.class, "endElement")._throws(SAXException.class);
        method.annotate(Override.class);
        JVar uri = method.param(String.class, "uri");
        JVar localName = method.param(String.class, "localName");
        JVar qName = method.param(String.class, "qName");
        JBlock body = method.body();
        // super.endElement(uri, localName, qName);
        body.invoke(_super(), "endElement").arg(uri).arg(localName).arg(qName);

        Nodes requiresState = nodes.requiresState();
        if (requiresState.isEmpty()) {
            emitElements(body, qName);
            emitClearCharacters(body);
            return;
        }

        emitEndSwitch(body, requiresState, uri, localName, qName);
        emitEndTests(body, requiresState, qName);
        emitClearCharacters(body);
    }

    private void emitElements(JBlock body, JVar qName) {
        JConditionalFix _if = null;
        for (Node n : nodes.onRoot()) {
            JExpression test = lit(n.elemName()).invoke("equals").arg(qName);
            JConditionalFix cond = _if == null ? _if = _if(test) : _if._elseif(test);
            JExpression arg = n.isNested() ? ref(n.field()).invoke("getResult")
                    : emitConverter(n, ref("characters").invoke("toString"));
            if (n.isList()) {
                cond._then().invoke(ref("result").ref(n.field()), "add").arg(arg);
            } else {
                cond._then().assign(ref("result").ref(n.field()), arg);
            }
        }
        Node text = nodes.text();
        if (text != null) {
            if (_if == null) {
                emitText(body, text);
            } else  {
                emitText(_if._else(), text);
            }
        }

        if (_if != null) body.add(_if);
    }

    private void emitText(JBlock body, Node text) {
        body.assign(ref("result").ref(text.field()), emitConverter(text, ref("characters").invoke("toString")));
    }

    private void emitEndSwitch(JBlock body, Nodes requireState, JVar uri, JVar localName, JVar qName) {
        JSwitch _switch = body._switch(ref("state"));
        JBlock rootBody = _switch._case(ref("ROOT")).body();
        emitElements(rootBody, qName);
        rootBody._break();

        for (Node n : requireState) {
            JBlock _case = _switch._case(ref(n.staticName())).body();
            if (n.isNested()) {
                _case.invoke(ref(n.field()), "endElement").arg(uri).arg(localName).arg(qName);
            }
            if (n.isList() && !((Node.IsList) n).isInline()) {
                JExpression result = n.isNested() ? ref(n.field()).invoke("getResult")
                                                  : emitConverter(n, ref("characters").invoke("toString"));
                _case._if(lit(n.elemName()).invoke("equals").arg(qName))
                        ._then().invoke(ref("result").ref(n.field()), "add").arg(result);

            }
            _case._break();
        }
    }

    private void emitEndTests(JBlock body, Nodes requireState, JVar qName) {
        JConditionalFix _if = null;
        for (Node n : requireState) {
            JExpression test = lit(n.testName()).invoke("equals").arg(qName);
            JConditionalFix cond = _if == null ? _if = _if(test) : _if._elseif(test);
            JBlock condBody = cond._then();
            condBody.assign(ref("state"), ref("ROOT"));

            if (n.isNested()) {
                JExpression result = ref(n.field()).invoke("getResult");
                if (n.isList()) {
                    if (((Node.IsList) n).isInline()) {
                        condBody.invoke(ref("result").ref(n.field()), "add").arg(result);
                    }
                } else {
                    condBody.assign(ref("result").ref(n.field()), result);
                }
            }
        }
        if (_if != null) body.add(_if);
    }

    private void emitClearCharacters(JBlock body) {
        body.invoke(ref("characters"), "setLength").arg(lit(0));
    }

    private void emitCharacters(JDefinedClass clazz) {
        Nodes nested = nodes.nested();
        if (nested.isEmpty()) return;

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

        for (Node n : nested) {
            JBlock _case = _switch._case(ref(n.staticName())).body();
            _case.invoke(ref(n.field()), "characters").arg(ch).arg(start).arg(length);
            _case._break();
        }
    }

    private JExpression emitConverter(Node e, JExpression expr) {
        TypeConverter typeConverter = findTypeConverter(e.field(), e.elemType());
        if (typeConverter == null)
            throw new RuntimeException("Don't know how to convert \"" + e.field() + "\" to type \"" + e.elemType() + "\"");
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
            return ref(name).invoke("read").arg(invoke("toString").arg(expr));
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
