package slickurl.domain.model

import org.specs2.mutable.Specification
import slickurl.AppProps.encodingAlphabet

class AlphabetCodecSpec extends Specification {
  "AlphabetCodec" should {
    "encode correctly" in {
      AlphabetCodec.encode(0L) must_== "2"
      AlphabetCodec.encode(encodingAlphabet.length) must_== encodedAlphabetLength
      AlphabetCodec.encode(Int.MaxValue) must_== encodedIntMaxVal
      AlphabetCodec.encode(Long.MaxValue) must_== encodedLongMaxVal
      AlphabetCodec.encode(Int.MaxValue * 3) must_== encodedThreeTimesMaxInt
      AlphabetCodec.encode(Long.MinValue) must_== encodedLongMinVal
    }

    "decode correctly" in {
      AlphabetCodec.decode("2") must_== 0L
      AlphabetCodec.decode(encodedAlphabetLength) must_== encodingAlphabet.length
      AlphabetCodec.decode(encodedIntMaxVal) must_== Int.MaxValue
      AlphabetCodec.decode(encodedThreeTimesMaxInt) must_== Int.MaxValue * 3
      AlphabetCodec.decode(encodedLongMinVal) must_== Long.MinValue
    }

    "check that arguments are valid" in {
      AlphabetCodec.decode(null) must throwA[IllegalArgumentException]
      AlphabetCodec.decode("") must throwA[IllegalArgumentException]
    }
  }

  private val encodedAlphabetLength   = encodingAlphabet.charAt(1).toString ++ encodingAlphabet.charAt(0).toString
  private val encodedIntMaxVal        = "5AsVAI"
  private val encodedLongMaxVal       = "tvbrKnb8c7Y"
  private val encodedLongMinVal       = "tvbrKnb8c7Z"
  private val encodedThreeTimesMaxInt = "5AsVAG"
}
