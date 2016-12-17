package slickurl.domain.model

object AlphabetCodec {

  import slickurl.AppProps.encodingAlphabet

  def encode(number: Long): String = {
    val base     = encodingAlphabet.length
    val encoding = StringBuilder.newBuilder
    var (quotient, reminder) = (number, 0L)

    import java.lang.Long._
    do {
      reminder = remainderUnsigned(quotient, base)
      quotient = divideUnsigned(quotient, base)
      encoding += encodingAlphabet.charAt(reminder.toInt)  //alphabet.length is Int
    } while(quotient != 0L)
    encoding.reverseContents().toString()
  }

  def decode(encodedNumber: String): Long = {
    require(encodedNumber != null && !encodedNumber.isEmpty)

    (BigInt(0) /: encodedNumber) { (x, c) => x * encodingAlphabet.length + encodingAlphabet.indexOf(c) }.toLong
  }
}
