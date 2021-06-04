//package zootoggler.http.tapir.akka
//
//import akka.http.scaladsl.server.Route
//import akka.http.scaladsl.server.RouteConcatenation._
//import sttp.model.StatusCode
//import sttp.tapir.Codec.JsonCodec
//import sttp.tapir.server.akkahttp.AkkaHttpServerInterpreter
//import sttp.tapir.{Endpoint, anyJsonBody, endpoint, statusCode, stringBody}
//import zootoggler.core.ZtClient
//import zootoggler.http.tapir.{ErrorCode, Feature, FeatureUpdate}
//import zootoggler.http.tapir.FeatureUpdate.{ByteArrayInput, StringInput}
//import zootoggler.http.tapir.ResponseExamples._
//
//import scala.concurrent.{ExecutionContext, Future}
//import scala.util.{Failure, Success}
//
//class ZootogglerAkkaRoutes(ztClient: ZtClient[Future])(implicit
//  ex: ExecutionContext,
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
//  private val featureListRoute: Route =
//    AkkaHttpServerInterpreter.toRoute(featureListEndpoint)(_ =>
//      Future(ztClient.featureList().map(Feature(_))).map(features => Right(features))
//    )
//
//  private val updateFeatureStringRoute: Route =
//    AkkaHttpServerInterpreter.toRoute(updateFeatureStringEndpoint)(updateRequest =>
//      ztClient
//        .updateFromString(
//          updateRequest.featureName,
//          updateRequest.value,
//          updateRequest.description
//        )
//        .map(_.toString)
//        .transformWith {
//          case Failure(exception) =>
//            Future
//              .successful(Left((StatusCode.BadRequest, ErrorCode(exception.getLocalizedMessage))))
//          case Success(value) => Future.successful(Right(value))
//        }
//    )
//
//  private val updateFeatureByteArrayRoute: Route =
//    AkkaHttpServerInterpreter.toRoute(updateFeatureByteArrayEndpoint)(updateRequest =>
//      ztClient
//        .updateFromByteArray(
//          updateRequest.featureName,
//          updateRequest.value,
//          updateRequest.description
//        )
//        .map(_.toString)
//        .transformWith {
//          case Failure(exception) =>
//            Future
//              .successful(Left((StatusCode.BadRequest, ErrorCode(exception.getLocalizedMessage))))
//          case Success(value) => Future.successful(Right(value))
//        }
//    )
//
//  val endpoints = List(
//    featureListEndpoint,
//    updateFeatureStringEndpoint,
//    updateFeatureByteArrayEndpoint
//  )
//
//  val route: Route = featureListRoute ~ updateFeatureStringRoute ~ updateFeatureByteArrayRoute
//}
//
//object ZootogglerAkkaRoutes {
//  def apply(ztClient: ZtClient[Future])(implicit
//    ex: ExecutionContext,
//    errorJsonCodec: JsonCodec[ErrorCode],
//    featureViewCodec: JsonCodec[Feature],
//    featureViewListCodec: JsonCodec[List[Feature]],
//    featureUpdateStringInputCodec: JsonCodec[StringInput],
//    featureUpdateByteArrayInputCodec: JsonCodec[ByteArrayInput]
//  ): ZootogglerAkkaRoutes = new ZootogglerAkkaRoutes(ztClient)
//}
