package feature4s.aerospike

import com.aerospike.client.{AerospikeClient, AerospikeException, Bin, ResultCode}
import com.aerospike.client.policy.{RecordExistsAction, WritePolicy}
import com.aerospike.client.query.Statement
import feature4s.compat.CollectionConverters._
import feature4s.{Feature, FeatureNotFound, FeatureRegistry, FeatureState}
import feature4s.monad.MonadError
import feature4s.monad.syntax._

abstract class AerospikeFeatureRegistry[F[_]](
  client: AerospikeClient,
  namespace: String,
  implicit val monad: MonadError[F]
) extends FeatureRegistry[F] {
  private val writePolicy = new WritePolicy(client.writePolicyDefault)
  writePolicy.expiration = -1 // never expire

  private val notOverride = new WritePolicy(client.writePolicyDefault)
  notOverride.recordExistsAction = RecordExistsAction.CREATE_ONLY

  override def register(name: String, enable: Boolean, description: Option[String]): F[Feature[F]] =
    monad
      .handleErrorWith(
        monad.eval(
          client.put(
            notOverride,
            key(name, namespace),
            new Bin(ValueFieldName, enable)
          )
        )
      ) {
        case err: AerospikeException if err.getResultCode == ResultCode.KEY_EXISTS_ERROR =>
          monad.pure()
      }
      .flatMap(_ => updateInfo(name, description.getOrElse("")))
      .map(_ => Feature(name, () => valueAccessor(name), description))

  private def valueAccessor(name: String): F[Boolean] =
    monad
      .eval(client.get(client.readPolicyDefault, key(name, namespace)))
      .flatMap(record =>
        monad.ifM(monad.pure(record != null))(
          ifTrue = monad.pure(record.getBoolean(ValueFieldName)),
          ifFalse = monad.raiseError(FeatureNotFound(name))
        )
      )

  private def updateInfo(name: String, description: String): F[Unit] =
    monad
      .eval(
        client.put(
          writePolicy,
          key(name, namespace),
          new Bin(FeatureNameFieldName, name),
          new Bin(DescriptionFieldName, description)
        )
      )
      .void

  override def recreate(name: String, enable: Boolean, description: Option[String]): F[Feature[F]] =
    monad
      .eval(
        client.put(
          writePolicy,
          key(name, namespace),
          new Bin(FeatureNameFieldName, name),
          new Bin(ValueFieldName, enable),
          new Bin(DescriptionFieldName, description.getOrElse(""))
        )
      )
      .map(_ => Feature(name, () => valueAccessor(name), description))

  override def update(name: String, enable: Boolean): F[Unit] =
    monad.ifM(isExist(name))(
      ifTrue =
        monad.eval(client.put(writePolicy, key(name, namespace), new Bin(ValueFieldName, enable))),
      ifFalse = monad.raiseError(FeatureNotFound(name))
    )

  override def featureList(): F[List[FeatureState]] = {
    val statement = new Statement()
    statement.setBinNames(FeatureNameFieldName, ValueFieldName, DescriptionFieldName)
    statement.setNamespace(namespace)
    statement.setSetName(DefaultSetName)

    monad
      .eval(client.query(client.queryPolicyDefault, statement))
      .map(set => set.iterator())
      .map(records =>
        records
          .map(r =>
            FeatureState(
              name = Option(r.record.getString(FeatureNameFieldName))
                .filter(_.nonEmpty)
                .getOrElse("empty_feature_name"),
              isEnable = r.record.getBoolean(ValueFieldName),
              description = Option(r.record.getString(DescriptionFieldName)).filter(_.nonEmpty)
            )
          )
          .toList
      )
  }

  override def isExist(name: String): F[Boolean] =
    monad.eval(client.exists(writePolicy, key(name, namespace)))

  override def remove(name: String): F[Boolean] =
    monad.eval(client.delete(writePolicy, key(name, namespace)))

  override def close(): F[Unit] = monad.unit

  override def monadError: MonadError[F] = monad
}
