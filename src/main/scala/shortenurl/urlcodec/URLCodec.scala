/**
 * Copyright 2014-2015 Maxim Plevako
 **/
package shortenurl.urlcodec

object URLCodec {

  def encode(alphabet: String, num: Long): String = num match {
    case 0 => alphabet.head.toString
    case x if x > 0 =>
      val base   = alphabet.length
      //legal cast: alphabet.length is Int
      val digit  = alphabet.charAt((x % base).asInstanceOf[Int]).toString
      val n      = x / base
      val str = if (n > 0) encode(alphabet, n) else ""
      str ++ digit
    case _ => ""
  }

  //returns -1 if the "number" to decode is empty or null
  def decode(alphabet: String, num: String): Long = {
    if(null != num && num.nonEmpty)
      (0 /: num) { (x,c) => x * alphabet.length + alphabet.indexOf(c) }
    else -1
  }
}
