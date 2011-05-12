package com.mysema.rdfbean.scala;

import java.util.Collection
import javax.annotation.Nullable
import com.mysema.query.types.{ Expression, ConstantImpl, Ops, Order, OrderSpecifier, ParamExpression, Visitor }
import com.mysema.query.scala._
import com.mysema.rdfbean.model.NODE
import com.mysema.query.scala.{ Operations, SimpleExpression }
import com.mysema.rdfbean.model.{ Blocks, NODE, ID, LIT, BID, UID, RDF }

import scala.collection.JavaConversions._

/**
 * @author tiwe
 *
 * @param <T>
 */
class QNODE[T <: NODE](val t: Class[T], val name: String) extends ParamExpression[T] with SimpleExpression[T] {

  def accept[R,C](v: Visitor[R,C], context: C): R = v.visit(this, context)
    
  def getName() = name
    
  def getType() = t
  
  def isAnon() = false
  
  def getNotSetMessage() = "A parameter of type " + getType.getName + " was not set"
    
  def is(p: AnyRef, s: AnyRef) =  Blocks.pattern(s, p, this)
  
  lazy val id = new QID(getName)
  
  lazy val lit = new QLIT(getName)
  
  lazy val asc = new OrderSpecifier[String](Order.ASC, this.asInstanceOf[Expression[String]])
  
  lazy val desc = new OrderSpecifier[String](Order.DESC, this.asInstanceOf[Expression[String]])
  
  override def equals(o: Any): Boolean = {
    o match {
      case n: QNODE[_] => n.getName == name
      case _ => false
    }
  }

  override def hashCode = getName.hashCode
  
}

object QNODE {

  val s = new QID("s")
    
  val p = new QID("p")

  val o = new QNODE[NODE](classOf[NODE],"o")

  val c = new QID("c")

  val t = new QID("type")

  val typeContext = new QID("typeContext")

  val first = new QNODE[NODE](classOf[NODE], "first")

  val rest = new QID("rest")

}

class QResource[T <: ID](c: Class[T], name: String) extends QNODE[T](c, name) {
    
  def a(t: AnyRef) = Blocks.pattern(this, RDF.`type`, t)
  
  def a(t: AnyRef, c: AnyRef) = Blocks.pattern(this, RDF.`type`, t, c)

  def has(p: AnyRef, o: AnyRef) = Blocks.pattern(this, p, o)
  
  def has(p: AnyRef, o: AnyRef, c: AnyRef) = Blocks.pattern(this, p, o, c) 
  
  
}

class QID(name: String) extends QResource[ID](classOf[ID], name) { }

class QUID(name: String) extends QResource[UID](classOf[UID], name) { }

class QBID(name: String) extends QResource[BID](classOf[BID], name) { }

class QLIT(name: String) extends QNODE[LIT](classOf[LIT], name) {

  private def literal(v: String) = new ConstantImpl[LIT](classOf[LIT], new LIT(v))

  private def literal(lit: LIT) = new ConstantImpl[LIT](classOf[LIT], lit)
  
  def <(lit: LIT) = Operations.boolean(Ops.LT, this, literal(lit));
  
  def >(lit: LIT) = Operations.boolean(Ops.GT, this, literal(lit));
  
  def <=(lit: LIT) = Operations.boolean(Ops.LOE, this, literal(lit));
  
  def >=(lit: LIT) = Operations.boolean(Ops.GOE, this, literal(lit));  
  
  def between(lit1: LIT, lit2 :LIT) = Operations.boolean(Ops.BETWEEN, this, literal(lit1), literal(lit2));  
  
  def like(v: String) = Operations.boolean(Ops.LIKE, this, literal(v))
  
  def matches(v: String) = Operations.boolean(Ops.MATCHES, this, literal(v))
  
  def eqIgnoreCase(v: String) = Operations.boolean(Ops.EQ_IGNORE_CASE, this, literal(v))
  
  lazy val empty = Operations.boolean(Ops.STRING_IS_EMPTY, this); 

}



