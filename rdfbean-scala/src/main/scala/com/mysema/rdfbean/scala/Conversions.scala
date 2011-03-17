package com.mysema.rdfbean.scala

import com.mysema.rdfbean.model.{ NODE, UID, BID, LIT, XSD };

/**
 * Implicit conversion from/and to NODEs
 * 
 * @author tiwe
 *
 */
object Conversions {

  implicit def toLIT(str: String) = new LIT(str);
    
  implicit def toLIT(i: Int) = new LIT(String.valueOf(i), XSD.intType);
    
  implicit def toLIT(l: Long) = new LIT(String.valueOf(l), XSD.longType);
  
  implicit def toLIT(d: Double) = new LIT(String.valueOf(d), XSD.doubleType);
  
  implicit def toLIT(f: Float) = new LIT(String.valueOf(f), XSD.floatType);
  
  implicit def toLIT(s: Short) = new LIT(String.valueOf(s), XSD.shortType);
  
  implicit def toLIT(b: Boolean) = new LIT(String.valueOf(b), XSD.booleanType);
  
  implicit def toLIT(b: Byte) = new LIT(String.valueOf(b), XSD.byteType);
  
  // TODO : function to RDFConnectionCallback
  
  // TODO : function to SessionCallback
  
}