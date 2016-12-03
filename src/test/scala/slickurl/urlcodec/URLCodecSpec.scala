package slickurl.urlcodec

import org.specs2.mutable.Specification

class URLCodecSpec extends Specification {
  "URL codec" should {
    "encode correctly" in {
      URLCodec.encode(alphabet, Long.MinValue) must beEmpty
      URLCodec.encode(alphabet, 0) must_== "a"
      URLCodec.encode(alphabet, alphabet.length) must_== encodedAlphabetLength
      URLCodec.encode(alphabet, Int.MaxValue) must_== encodedIntMaxVal
      URLCodec.encode(alphabet, Long.MaxValue) must_== encodedLongMaxVal
      URLCodec.encode(alphabet, Int.MaxValue * 3 ) must_== encodedThreeTimesMaxInt
    }

    "decode correctly" in {
      URLCodec.decode(alphabet, null) must_== -1
      URLCodec.decode(alphabet, "") must_== -1
      URLCodec.decode(alphabet, "a") must_== 0
      URLCodec.decode(alphabet, encodedAlphabetLength) must_== alphabet.length
      URLCodec.decode(alphabet, encodedIntMaxVal) must_== Int.MaxValue
      URLCodec.decode(alphabet, encodedThreeTimesMaxInt) must_== Int.MaxValue * 3
    }
  }

  private val alphabet                = "abcdefghijkmnpqrstuvwxyzABCDEFGHIJKLMNPQRSTUVWXYZ23456789"
  private val encodedAlphabetLength   = alphabet.charAt(1).toString ++ alphabet.charAt(0).toString
  private val encodedIntMaxVal        = "dIA5IR"
  private val encodedLongMaxVal       = "BDjzTwjgkf8"
  private val encodedThreeTimesMaxInt = "dIA5IP"
}
