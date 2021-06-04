//package zootoggler.http.tapir.http4s
//
//import cats.effect.{ConcurrentEffect, ContextShift, Timer}
//import cats.syntax.functor._
//import cats.syntax.applicativeError._
//import cats.syntax.semigroupk._
//import cats.syntax.either._
//import org.http4s.HttpRoutes
//import sttp.model.StatusCode
//import sttp.tapir.Codec.JsonCodec
//import sttp.tapir.{Endpoint, anyJsonBody, endpoint, statusCode, stringBody}
//import sttp.tapir.server.http4s.Http4sServerInterpreter
//import zootoggler.core.ZtClient
//import zootoggler.http.tapir.FeatureUpdate.{ByteArrayInput, StringInput}
//import zootoggler.http.tapir.{ErrorCode, Feature, FeatureUpdate}
//import zootoggler.http.tapir.ResponseExamples._
//
//final class ZootogglerHttp4sRoutes[F[_]: ContextShift: Timer](
//  ztClient: ZtClient[F]
//)(implicit
//  F: ConcurrentEffect[F],
//  errorJsonCodec: JsonCodec[ErrorCode],
//  featureViewCodec: JsonCodec[Feature],
//  featureViewListCodec: JsonCodec[List[Feature]],
//  featureUpdateStringInputCodec: JsonCodec[StringInput],
//  featureUpdateByteArrayInputCodec: JsonCodec[ByteArrayInput]
//) {
//  private val baseEndpoint: Endpoint[Unit, (StatusCode, ErrorCode), Unit, Any] =
//    endpoint.in("features").errorOut(statusCode.and(anyJsonBody[ErrorCode]))
//
//  private val featureListEndpoint: Endpoint[Unit, (StatusCode, ErrorCode), List[Feature], Any] =
//    baseEndpoint.get
//      .out(anyJsonBody[List[Feature]].example(featureListExample))
//      .description("Get registered feature list")
//
//  private val updateFeatureStringEndpoint
//    : Endpoint[FeatureUpdate.StringInput, (StatusCode, ErrorCode), String, Any] =
//    baseEndpoint.put
//      .in(anyJsonBody[StringInput].example(stringInputExample))
//      .out(stringBody.example("true"))
//      .description("Update feature using string as new value")
//
//  private val updateFeatureByteArrayEndpoint
//    : Endpoint[FeatureUpdate.ByteArrayInput, (StatusCode, ErrorCode), String, Any] =
//    baseEndpoint.put
//      .in("binary")
//      .in(anyJsonBody[ByteArrayInput].example(byteArrayInputExample))
//      .out(stringBody.example("true"))
//      .description("Update feature using byte array as new value")
//
//  private val featureListRoute: HttpRoutes[F] =
//    Http4sServerInterpreter.toRoutes(featureListEndpoint)(_ =>
//      F.delay(Right(ztClient.featureList().map(Feature(_))))
//    )
//
//  private val updateFeatureStringRoute: HttpRoutes[F] =
//    Http4sServerInterpreter.toRoutes(updateFeatureStringEndpoint)(updateRequest =>
//      toRoute(
//        ztClient
//          .updateFromString(
//            updateRequest.featureName,
//            updateRequest.value,
//            updateRequest.description
//          )
//          .map(_.toString)
//      )
//    )
//
//  private val updateFeatureByteArrayRoute: HttpRoutes[F] =
//    Http4sServerInterpreter.toRoutes(updateFeatureByteArrayEndpoint)(updateRequest =>
//      toRoute(
//        ztClient
//          .updateFromByteArray(
//            updateRequest.featureName,
//            updateRequest.value,
//            updateRequest.description
//          )
//          .map(_.toString)
//      )
//    )
//
//  private def toRoute[A](fa: F[A]): F[Either[(StatusCode, ErrorCode), A]] =
//    fa.map(_.asRight[(StatusCode, ErrorCode)])
//      .handleError(err => (StatusCode.BadRequest, ErrorCode(err.getLocalizedMessage)).asLeft[A])
//
//  val endpoints = List(
//    featureListEndpoint,
//    updateFeatureStringEndpoint,
//    updateFeatureByteArrayEndpoint
//  )
//
//  val route =
//    featureListRoute.combineK(updateFeatureStringRoute).combineK(updateFeatureByteArrayRoute)
//}
//
//object ZootogglerHttp4sRoutes {
//  def apply[F[_]: ContextShift: Timer](ztClient: ZtClient[F])(implicit
//    F: ConcurrentEffect[F],
//    errorJsonCodec: JsonCodec[ErrorCode],
//    featureViewCodec: JsonCodec[Feature],
//    featureViewListCodec: JsonCodec[List[Feature]],
//    featureUpdateStringInputCodec: JsonCodec[StringInput],
//    featureUpdateByteArrayInputCodec: JsonCodec[ByteArrayInput]
//  ): ZootogglerHttp4sRoutes[F] = new ZootogglerHttp4sRoutes(ztClient)
//}
