package me.tatarka.fasax.internal;


import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Filer;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.MirroredTypeException;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.ElementFilter;
import javax.lang.model.util.Elements;
import javax.lang.model.util.SimpleTypeVisitor6;
import javax.tools.JavaFileObject;

import me.tatarka.fasax.Attribute;
import me.tatarka.fasax.ElementList;
import me.tatarka.fasax.Xml;

import static javax.tools.Diagnostic.Kind.ERROR;

@SupportedAnnotationTypes("me.tatarka.fasax.Xml")
public final class FasaxModelProcessor extends AbstractProcessor {
    private Elements elementUtils;
    private Filer filer;

    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
        elementUtils = processingEnv.getElementUtils();
        filer = processingEnv.getFiler();
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        Map<TypeElement, FasaxReaderGenerator> targetClassMap = findAndParseTargets(roundEnv);

        for (Map.Entry<TypeElement, FasaxReaderGenerator> entry : targetClassMap.entrySet()) {
            TypeElement typeElement = entry.getKey();
            FasaxReaderGenerator saxGenerator = entry.getValue();

            try {
                saxGenerator.brewJava(new AnnotationFilerCodeWriter(filer, typeElement));
            } catch (IOException e) {
                error(typeElement, "Unable to write injector for type %s: %s", typeElement, e.getMessage());
            }
        }

        return true;
    }

    private Map<TypeElement, FasaxReaderGenerator> findAndParseTargets(RoundEnvironment env) {
        Map<TypeElement, FasaxReaderGenerator> targetClassMap = new LinkedHashMap<TypeElement, FasaxReaderGenerator>();

        for (TypeElement element : ElementFilter.typesIn(env.getElementsAnnotatedWith(Xml.class))) {
            try {
                parseTargets(element, targetClassMap);
            } catch (Exception e) {
                StringWriter stackTrace = new StringWriter();
                e.printStackTrace(new PrintWriter(stackTrace));

                error(element, "Unable to generate view SAX Parser for @Xml.\n\n%s", stackTrace.toString());
            }
        }

        return targetClassMap;
    }

    private void parseTargets(TypeElement element, Map<TypeElement, FasaxReaderGenerator> targetClassMap) {
        if (!hasPublicEmptyConstructor(element)) {
            error(element, "@Xml class must have a public empty constructor (%s).", element.getSimpleName());
            return;
        }

        getOrCreateTargetClass(targetClassMap, element);
    }

    private FasaxReaderGenerator getOrCreateTargetClass(Map<TypeElement, FasaxReaderGenerator> targetClassMap, TypeElement element) {
        Xml node = element.getAnnotation(Xml.class);

        FasaxReaderGenerator saxGenerator = targetClassMap.get(element);
        if (saxGenerator == null) {
            String classPackage = getPackageName(element);
            String className = getClassName(element);
            String rootName = node.name();

            saxGenerator = new FasaxReaderGenerator(classPackage, className, rootName);
            targetClassMap.put(element, saxGenerator);
        }

        List<VariableElement> fields = ElementFilter.fieldsIn(element.getEnclosedElements());
        processFields(saxGenerator, fields);

        return saxGenerator;
    }

    private void processFields(FasaxReaderGenerator saxGenerator, List<VariableElement> fields) {
        for (VariableElement field : fields) {
            String fieldName = field.getSimpleName().toString();
            String type = field.asType().toString();
            Attribute attribute = field.getAnnotation(Attribute.class);
            if (attribute != null) {
                String typeConverter = getType(attribute);
                if (typeConverter != null) {
                    saxGenerator.addTypeConverter(fieldName, typeConverter);
                }
                String name = getName(attribute.name(), fieldName);
                saxGenerator.addAttribute(fieldName, name, type);
                continue;
            }
            me.tatarka.fasax.Element element = field.getAnnotation(me.tatarka.fasax.Element.class);
            if (element != null) {
                String name = getName(element.name(), fieldName);
                String typeConverter = getType(element);
                if (typeConverter != null) {
                    saxGenerator.addTypeConverter(fieldName, typeConverter);
                    saxGenerator.addElement(fieldName, name, type);
                } else if (saxGenerator.isPrimitive(type)) {
                    saxGenerator.addElement(fieldName, name, type);
                } else {
                    saxGenerator.addChild(fieldName, name, getPackageName(field), type);
                }
                continue;
            }
            ElementList elementList = field.getAnnotation(ElementList.class);
            if (elementList != null) {
                String name = getName(elementList.name(), fieldName);
                String entry = elementList.entry();
                String typeConverter = getType(elementList);
                CollectionType listType = new CollectionType(field.asType());
                if (typeConverter != null) {
                    saxGenerator.addTypeConverter(fieldName, typeConverter);
                    saxGenerator.addElementList(fieldName, name, entry, listType.type, listType.entryType);
                } else if (saxGenerator.isPrimitive(listType.entryType)) {
                    saxGenerator.addElementList(fieldName, name, entry, listType.type, listType.entryType);
                } else {
                    saxGenerator.addListChild(fieldName, name, entry, getPackageName(field), listType.type, listType.entryType);
                }
            }
        }
    }

    private String getName(String name, String fieldName) {
        return (name == null || name.isEmpty()) ? fieldName : name;
    }

    private String getPackageName(Element element) {
        return elementUtils.getPackageOf(element).getQualifiedName().toString();
    }

    private String getClassName(TypeElement element) {
        return element.getQualifiedName().toString();
    }

    private boolean hasPublicEmptyConstructor(Element element) {
        boolean result = false;
        for (ExecutableElement e : ElementFilter.constructorsIn(element.getEnclosedElements())) {
            if (e.getModifiers().contains(Modifier.PUBLIC) && e.getParameters().isEmpty()) {
                result = true;
            }
        }
        return result;
    }

    private String getType(me.tatarka.fasax.Element elem) {
        try {
            elem.type();
        } catch (MirroredTypeException e) {
            String type = e.getTypeMirror().toString();
            if (type == null || type.isEmpty() || type.equals("void")) {
                return null; //No type set.
            }
            return type;
        }
        return null; // Won't happen
    }

    private String getType(Attribute attr) {
        try {
            attr.type();
        } catch (MirroredTypeException e) {
            String type = e.getTypeMirror().toString();
            if (type == null || type.isEmpty() || type.equals("void")) {
                return null; //No type set.
            }
            return type;
        }
        return null; // Won't happen
    }

    private String getType(ElementList elems) {
        try {
            elems.type();
        } catch (MirroredTypeException e) {
            String type = e.getTypeMirror().toString();
            if (type == null || type.isEmpty() || type.equals("void")) {
                return null; //No type set.
            }
            return type;
        }
        return null; // Won't happen
    }

    private void error(Element element, String message, Object... args) {
        processingEnv.getMessager().printMessage(ERROR, String.format(message, args), element);
    }

    private static class CollectionType {
        final String type;
        final String entryType;

        public CollectionType(TypeMirror type) {
            this.type = getContainerType(type);
            this.entryType = getGenericType(type);
        }

        private static String getContainerType(TypeMirror type) {
            String listType = getTypeName(type);
            // Default to ArrayList for List
            if (listType.equals("java.util.List")) {
                listType = "java.util.ArrayList";
            }
            return listType;
        }

        public static String getGenericType(final TypeMirror type) {
            final TypeMirror[] result = {null};

            type.accept(new SimpleTypeVisitor6<Void, Void>() {
                @Override
                public Void visitDeclared(DeclaredType declaredType, Void v) {
                    List<? extends TypeMirror> typeArguments = declaredType.getTypeArguments();
                    if (!typeArguments.isEmpty()) {
                        result[0] = typeArguments.get(0);
                    }
                    return null;
                }
            }, null);

            return getTypeName(result[0]);
        }

        private static String getTypeName(TypeMirror type) {
            return ((TypeElement) ((DeclaredType) type).asElement()).getQualifiedName().toString();
        }
    }
}
