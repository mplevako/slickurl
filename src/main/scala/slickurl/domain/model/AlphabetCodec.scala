package slickurl.domain.model

import slickurl.AppProps

object AlphabetCodec {

  import slickurl.AppProps.encodingAlphabet

  private[slickurl] def encode(number: Long): String = {
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

  private[slickurl] def decode(encodedNumber: String): Long = {
    require(encodedNumber != null && !encodedNumber.isEmpty)

    (BigInt(0) /: encodedNumber) { (x, c) => x * encodingAlphabet.length + encodingAlphabet.indexOf(c) }.toLong
  }

  private[slickurl] def packAndEncode(lo: Long)(hi: Long): String = {
    val packedNumber = (hi << AppProps.shardIdLenght) | lo
    AlphabetCodec.encode(packedNumber)
  }

  private[slickurl] def decodeLo(packedNumber: String): Long =
    AlphabetCodec.decode(packedNumber) & AppProps.shardIdMask
}
