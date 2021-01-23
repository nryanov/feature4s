package zootoggler.http.tapir.zio

import sttp.model.StatusCode
import zio.{Task, ZIO}
import zio.interop.catz._
import sttp.tapir.ztapir._
import zio.interop.catz.implicits._
import cats.syntax.semigroupk._
import sttp.tapir.server.http4s.ztapir.ZHttp4sServerInterpreter
import sttp.tapir.Codec.JsonCodec
import sttp.tapir.{Endpoint, anyJsonBody, endpoint, statusCode, stringBody}
import zootoggler.core.ZtClient
import zootoggler.http.tapir.{ErrorCode, Feature, FeatureUpdate}
import zootoggler.http.tapir.FeatureUpdate.{ByteArrayInput, StringInput}
import zootoggler.http.tapir.ResponseExamples._

final class ZootogglerZioRoutes(ztClient: ZtClient[Task])(implicit
  errorJsonCodec: JsonCodec[ErrorCode],
  featureViewCodec: JsonCodec[Feature],
  featureViewListCodec: JsonCodec[List[Feature]],
  featureUpdateStringInputCodec: JsonCodec[StringInput],
  featureUpdateByteArrayInputCodec: JsonCodec[ByteArrayInput]
) {

  private val baseEndpoint =
    endpoint.in("features").errorOut(statusCode.and(anyJsonBody[ErrorCode]))

  private val featureListEndpoint =
    baseEndpoint.get
      .out(anyJsonBody[List[Feature]].example(featureListExample))
      .description("Get registered feature list")

  private val updateFeatureStringEndpoint =
    baseEndpoint.put
      .in(anyJsonBody[StringInput].example(stringInputExample))
      .out(stringBody.example("true"))
      .description("Update feature using string as new value")

  private val updateFeatureByteArrayEndpoint
    : Endpoint[FeatureUpdate.ByteArrayInput, (StatusCode, ErrorCode), String, Any] =
    baseEndpoint.put
      .in("binary")
      .in(anyJsonBody[ByteArrayInput].example(byteArrayInputExample))
      .out(stringBody.example("true"))
      .description("Update feature using byte array as new value")

  private val featureListRoute = ZHttp4sServerInterpreter
    .from(featureListEndpoint) { _ =>
      ZIO
        .effect(ztClient.featureList().map(Feature(_)))
        .catchAll(err => ZIO.fail((StatusCode.BadRequest, ErrorCode(err.getLocalizedMessage))))
    }
    .toRoutes

  private val updateFeatureStringRoute =
    ZHttp4sServerInterpreter
      .from(updateFeatureStringEndpoint) { updateRequest =>
        ztClient
          .updateFromString(
            updateRequest.featureName,
            updateRequest.value,
            updateRequest.description
          )
          .map(_.toString)
          .catchAll(err => ZIO.fail((StatusCode.BadRequest, ErrorCode(err.getLocalizedMessage))))
      }
      .toRoutes

  private val updateFeatureByteArrayRoute =
    ZHttp4sServerInterpreter
      .from(updateFeatureByteArrayEndpoint) { updateRequest =>
        ztClient
          .updateFromByteArray(
            updateRequest.featureName,
            updateRequest.value,
            updateRequest.description
          )
          .map(_.toString)
          .catchAll(err => ZIO.fail((StatusCode.BadRequest, ErrorCode(err.getLocalizedMessage))))
      }
      .toRoutes

  val endpoints = List(
    featureListEndpoint,
    updateFeatureStringEndpoint,
    updateFeatureByteArrayEndpoint
  )

  val route = featureListRoute <+> updateFeatureStringRoute <+> updateFeatureByteArrayRoute
}

object ZootogglerZioRoutes {
  def apply(ztClient: ZtClient[Task])(implicit
    errorJsonCodec: JsonCodec[ErrorCode],
    featureViewCodec: JsonCodec[Feature],
    featureViewListCodec: JsonCodec[List[Feature]],
    featureUpdateStringInputCodec: JsonCodec[StringInput],
    featureUpdateByteArrayInputCodec: JsonCodec[ByteArrayInput]
  ): ZootogglerZioRoutes = new ZootogglerZioRoutes(ztClient)
}
