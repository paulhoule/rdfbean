package com.mysema.rdfbean.object;

import static org.junit.Assert.assertEquals;

import java.util.List;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;

import com.mysema.query.types.path.PathBuilder;
import com.mysema.rdfbean.TEST;
import com.mysema.rdfbean.annotations.ClassMapping;
import com.mysema.rdfbean.annotations.Id;
import com.mysema.rdfbean.annotations.Predicate;
import com.mysema.rdfbean.model.ID;
import com.mysema.rdfbean.model.IDType;

public class SessionInvLoadTest {

    @ClassMapping(ns = TEST.NS)
    public static class Note {
        @Id(IDType.RESOURCE)
        ID id;

        @Predicate(ln = "note", inv = true)
        Set<Comment> comments;

        @Predicate
        String basicForm, lemma;

        @Predicate
        Term term;
    }

    @ClassMapping(ns = TEST.NS)
    public static class Term {
        @Id(IDType.RESOURCE)
        ID id;

        @Predicate
        String meaning;
    }

    @ClassMapping(ns = TEST.NS)
    public static class Comment {
        @Id(IDType.RESOURCE)
        ID id;

        @Predicate
        Note note;
    }

    private Session session;

    @Before
    public void setUp(){
        session = SessionUtil.openSession(Note.class, Term.class, Comment.class);
        Term term = new Term();
        term.meaning = "X";

        Term term2 = new Term();
        term2.meaning = "X";

        Note note1 = new Note();
        note1.basicForm = "A";
        note1.lemma = "B";
        note1.term = term;

        Note note2 = new Note();
        note2.basicForm = "A";
        note2.lemma = "B";
        note2.term = term2;

        Comment comment1 = new Comment();
        comment1.note = note1;

        Comment comment2 = new Comment();
        comment2.note = note2;

        Comment comment3 = new Comment();
        comment3.note = note2;

        Comment comment4 = new Comment();
        comment4.note = note2;

        session.saveAll(term, term2, note1, note2, comment1, comment2, comment3, comment4);
        session.clear();
        System.out.println();
    }

    @Test
    public void FindInstances(){
        System.out.println("Get all notes");
        List<Note> notes = session.findInstances(Note.class);
        assertEquals(2, notes.size());
    }

    @Test
    public void QueryAll(){
        System.out.println("Query all notes");
        PathBuilder<Note> note = new PathBuilder<Note>(Note.class, "note");
        List<Note> notes = session.from(note).list(note);
        assertEquals(2, notes.size());
    }

}