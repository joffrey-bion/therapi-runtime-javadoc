package com.github.dnault.therapi.runtimejavadoc;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.smile.SmileFactory;
import com.fasterxml.jackson.datatype.guava.GuavaModule;
import com.google.common.collect.ImmutableList;
import com.sun.javadoc.ClassDoc;
import com.sun.javadoc.FieldDoc;
import com.sun.javadoc.MethodDoc;
import com.sun.javadoc.ParamTag;
import com.sun.javadoc.RootDoc;
import com.sun.javadoc.SeeTag;
import com.sun.javadoc.Tag;

import java.beans.XMLDecoder;
import java.beans.XMLEncoder;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

public class RuntimeJavadocWriter {
    private final File outputDir;
    private final ObjectMapper objectMapper = new ObjectMapper(new SmileFactory());
    private final ObjectMapper objectMapperReadable = new ObjectMapper();

    public RuntimeJavadocWriter(File outputDir) {
        this.outputDir = outputDir;
    }

    public boolean start(RootDoc root) throws IOException {
        for (ClassDoc c : root.classes()) {

            List<RuntimeFieldDoc> rtFields = new ArrayList<>();
            for (FieldDoc f : c.fields(false)) {
                rtFields.add(new RuntimeFieldDoc(f.qualifiedName(), f.commentText()));
            }

            List<RuntimeMethodDoc> rtMethods = new ArrayList<>();
            for (MethodDoc m : c.methods(false)) {
                rtMethods.add(new RuntimeMethodDoc(m.qualifiedName(), m.commentText(), m.signature(), newRuntimeTags(m.tags(), true)));
            }

            RuntimeClassDoc rtClassDoc = new RuntimeClassDoc(c.qualifiedName(), c.commentText(), rtFields, rtMethods);

            try (OutputStream os = new FileOutputStream(new File(outputDir, c.qualifiedName() + ".javadoc.sml"))) {
                objectMapper.writeValue(os, rtClassDoc);
            }
            try (OutputStream os = new FileOutputStream(new File(outputDir, c.qualifiedName() + ".javadoc.json"))) {
                objectMapperReadable.writerWithDefaultPrettyPrinter().writeValue(os, rtClassDoc);
            }
            try (ObjectOutputStream os = new ObjectOutputStream(new FileOutputStream(new File(outputDir, c.qualifiedName() + ".javadoc.ser")))) {
                os.writeObject(rtClassDoc);
            }

            try (ObjectInputStream is = new ObjectInputStream(new FileInputStream(new File(outputDir, c.qualifiedName() + ".javadoc.ser")))) {
                RuntimeClassDoc roundTrip = (RuntimeClassDoc) is.readObject();
                System.out.println(roundTrip);
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }

            String s = objectMapperReadable.writerWithDefaultPrettyPrinter().writeValueAsString(rtClassDoc);
            objectMapperReadable.registerModule(new GuavaModule());
            RuntimeClassDoc roundTrip = objectMapperReadable.readValue(s, RuntimeClassDoc.class);
            System.out.println(roundTrip);


/*
            print(c.qualifiedName(), c.commentText());
            for (FieldDoc f : c.fields(false)) {
                print(f.qualifiedName(), f.commentText());
            }
            for (MethodDoc m : c.methods(false)) {
                print(m.qualifiedName(), m.commentText());
                if (m.commentText() != null && m.commentText().length() > 0) {
                    for (ParamTag p : m.paramTags())
                        print(m.qualifiedName() + "@" + p.parameterName(), p.parameterComment());
                    for (Tag t : m.tags("return")) {
                        if (t.text() != null && t.text().length() > 0)
                            print(m.qualifiedName() + "@return", t.text());
                    }
                }
            }
 */
        }
        return true;
    }

    private void print(String name, String comment) throws IOException {
        System.out.println(name + ": " + comment);
        //if (comment != null && comment.length() > 0) {
        //  new FileWriter(new File(outputDir, name + ".txt")).append(comment).close();
        //}
    }


    protected RuntimeTag newRuntimeTag(Tag tag) {
       // System.out.println("newRuntimeTag : " + tag);

        if (tag instanceof SeeTag) {
            return newRuntimeSeeTag((SeeTag) tag);
        }
//        if (tag instanceof ParamTag) {
//            return newRuntimeParamTag((ParamTag) tag);
//        }

        return new RuntimeTag(tag.name(), tag.kind(), tag.text(), newInlineRuntimeTags(tag.inlineTags()));
    }

//    private RuntimeTag newRuntimeParamTag(ParamTag t) {
//        return new RuntimeParamTag(t.name(), t.kind(), t.text(), t.parameterName(), t.parameterComment(), t.isTypeParameter())
//
//    }

    protected ImmutableList<RuntimeTag> newInlineRuntimeTags(Tag[] tags) {
        ImmutableList.Builder<RuntimeTag> list = ImmutableList.builder();
        for (Tag t : tags) {
            if (t instanceof SeeTag) {
                list.add(newRuntimeSeeTag((SeeTag) t));
            } else {
                list.add(new RuntimeTag(t.name(), t.kind(), t.text(), ImmutableList.<RuntimeTag>of()));
            }
        }
        return list.build();
    }

    private RuntimeSeeTag newRuntimeSeeTag(SeeTag t) {
        return new RuntimeSeeTag(t.name(), t.kind(), t.text(), t.label(), t.referencedClassName(), t.referencedMemberName());
    }

    protected ImmutableList<RuntimeTag> newRuntimeTags(Tag[] tags, boolean recurse) {
        ImmutableList.Builder<RuntimeTag> list = ImmutableList.builder();
        for (Tag t : tags) {
           // System.out.println("processing: " + System.identityHashCode(t));
            list.add(newRuntimeTag(t));
        }
        return list.build();
    }
}
