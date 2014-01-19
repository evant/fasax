package me.tatarka.fasax.internal;

import com.sun.codemodel.CodeWriter;
import com.sun.codemodel.JPackage;

import java.io.IOException;
import java.io.OutputStream;

import javax.annotation.processing.Filer;
import javax.lang.model.element.Element;

public class AnnotationFilerCodeWriter extends CodeWriter {
    private Filer filer;
    private Element[] originatingElements;

    public AnnotationFilerCodeWriter(Filer filer, Element...originatingElements) {
        this.filer = filer;
        this.originatingElements = originatingElements;
    }

    @Override
    public OutputStream openBinary(JPackage pkg, String fileName) throws IOException {
        return filer.createSourceFile(pkg.name() + '.' + removeExtension(fileName), originatingElements).openOutputStream();
    }

    @Override
    public void close() throws IOException {
    }

    private static String removeExtension(String fileName) {
        return fileName.substring(0, fileName.lastIndexOf('.'));
    }
}
