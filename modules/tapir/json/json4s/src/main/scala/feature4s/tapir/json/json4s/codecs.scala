package feature4s.tapir.json.json4s

import sttp.tapir.generic.SchemaDerivation
import sttp.tapir.json.json4s.TapirJson4s

object codecs extends TapirJson4s with SchemaDerivation
