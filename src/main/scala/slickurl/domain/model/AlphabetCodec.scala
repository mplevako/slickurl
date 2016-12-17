package slickurl.domain.model

object AlphabetCodec {

  def encode(alphabet: String, number: Long): String = {
    val base   = alphabet.length
    val encoding = StringBuilder.newBuilder
    var (quotient, reminder) = (number, 0L)

    import java.lang.Long._
    do {
      reminder = remainderUnsigned(quotient, base)
      quotient = divideUnsigned(quotient, base)
      encoding += alphabet.charAt(reminder.toInt)  //alphabet.length is Int
    } while(quotient != 0L)
    encoding.reverseContents().toString()
  }

  def decode(alphabet: String, encodedNumber: String): Long = {
    require(encodedNumber != null && alphabet != null)
    require(!encodedNumber.isEmpty && !alphabet.isEmpty)

    (BigInt(0) /: encodedNumber) { (x, c) => x * alphabet.length + alphabet.indexOf(c) }.toLong
  }
}
