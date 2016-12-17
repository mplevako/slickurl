package slickurl.domain.model

import org.specs2.mutable.Specification

class AlphabetCodecSpec extends Specification {
  "AlphabetCodec" should {
    "encode correctly" in {
      AlphabetCodec.encode(alphabet, 0L) must_== "2"
      AlphabetCodec.encode(alphabet, alphabet.length) must_== encodedAlphabetLength
      AlphabetCodec.encode(alphabet, Int.MaxValue) must_== encodedIntMaxVal
      AlphabetCodec.encode(alphabet, Long.MaxValue) must_== encodedLongMaxVal
      AlphabetCodec.encode(alphabet, Int.MaxValue * 3) must_== encodedThreeTimesMaxInt
      AlphabetCodec.encode(alphabet, Long.MinValue) must_== encodedLongMinVal
    }

    "decode correctly" in {
      AlphabetCodec.decode(alphabet, "2") must_== 0L
      AlphabetCodec.decode(alphabet, encodedAlphabetLength) must_== alphabet.length
      AlphabetCodec.decode(alphabet, encodedIntMaxVal) must_== Int.MaxValue
      AlphabetCodec.decode(alphabet, encodedThreeTimesMaxInt) must_== Int.MaxValue * 3
      AlphabetCodec.decode(alphabet, encodedLongMinVal) must_== Long.MinValue
    }

    "check that arguments are valid" in {
      AlphabetCodec.decode(alphabet, null) must throwA[IllegalArgumentException]
      AlphabetCodec.decode(alphabet, "") must throwA[IllegalArgumentException]
    }
  }

  private val alphabet                = "23456789abcdefghijkmnpqrstuvwxyzABCDEFGHIJKLMNPQRSTUVWXYZ"
  private val encodedAlphabetLength   = alphabet.charAt(1).toString ++ alphabet.charAt(0).toString
  private val encodedIntMaxVal        = "5AsVAI"
  private val encodedLongMaxVal       = "tvbrKnb8c7Y"
  private val encodedLongMinVal       = "tvbrKnb8c7Z"
  private val encodedThreeTimesMaxInt = "5AsVAG"
}
