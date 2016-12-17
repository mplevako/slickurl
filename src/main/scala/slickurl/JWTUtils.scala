package slickurl

import pdi.jwt.{JwtClaim, JwtJson4s}
import slickurl.AppProps.{apiPrivateKey, apiPublicKey, apiTokenAlgorithm}

import scala.util.Try

object JWTUtils {
  def subjectForToken(tokn: String): Try[JwtClaim] = JwtJson4s.decode(tokn, apiPublicKey, Seq(apiTokenAlgorithm))
  def tokenForSubject(subj: String) = JwtJson4s.encode(JwtClaim().about(subj).issuedNow, apiPrivateKey, apiTokenAlgorithm)
}
