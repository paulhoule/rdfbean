package com.mysema.rdfbean.scala

import com.mysema.rdfbean.TEST
import com.mysema.rdfbean.`object`.Session
import com.mysema.rdfbean.`object`.SessionUtil
import com.mysema.rdfbean.model.IDType

import org.junit.{ Ignore, Test, Before, After };
import org.junit.Assert._;

class CaseClassPersistenceTest {

  @Test
  def Save {
    val session = SessionUtil.openSession(classOf[PersonCase])
    var person = new PersonCase(null, "John", "Smith")
    session.save(person);
    session.clear();

    person = session.getById(person.id, classOf[PersonCase])
    assertEquals("John", person.firstName)
    assertEquals("Smith", person.lastName)
  }

}

@ClassMapping
case class PersonCase(
  @Id(IDType.LOCAL) id: String,

  @Predicate firstName: String,

  @Predicate lastName: String) {

}

