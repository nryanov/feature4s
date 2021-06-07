package feature4s.effect.zio

trait ZioBaseSpec {
  val runtime: zio.Runtime[zio.ZEnv] = zio.Runtime.default
}
