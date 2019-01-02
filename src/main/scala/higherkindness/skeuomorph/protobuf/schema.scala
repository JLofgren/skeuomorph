/*
 * Copyright 2018-2019 47 Degrees, LLC. <http://www.47deg.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package higherkindness.skeuomorph.protobuf

import cats.Functor
import cats.implicits._
import cats.instances.int._
import com.google.protobuf.descriptor.{EnumOptions, FieldDescriptorProto, UninterpretedOption}
import qq.droste.Coalgebra
import scalapb.descriptors.{ScalaType, _}

sealed trait ProtobufF[A]

sealed trait FieldF[A] {
  val name: String
  val tpe: A
}

object ProtobufF {
  final case class Field[A](
      name: String,
      tpe: A,
      position: Int,
      options: List[Option],
      isRepeated: Boolean,
      isMapField: Boolean)
      extends FieldF[A]
// TODO: Check with Pepe, why do we need equality instances for these now?
//  object Field {
//    implicit def fieldEq[T: Eq]: Eq[Field[T]] = Eq.instance {
//      case (Field(n, t, p, o, r, m), Field(n2, t2, p2, o2, r2, m2)) =>
//        n === n2 && t === t2 && p === p2 && o === o2 && r === r2 && m === m2
//    }
//  }

  final case class OneOfField[A](name: String, tpe: A) extends FieldF[A]
//  object OneOfField {
//    implicit def oneOfFieldEq[T: Eq]: Eq[OneOfField[T]] = Eq.instance {
//      case (OneOfField(n, tpe), OneOfField(n2, tpe2)) =>
//        n === n2 && tpe === tpe2
//    }
//  }

  final case class Option(name: String, value: String)
//  object Option {
//    implicit val optionEq: Eq[Option] = Eq.instance {
//      case (Option(n, v), Option(n2, v2)) => n === n2 && v === v2
//    }
//  }

  final case class TDouble[A]()                                    extends ProtobufF[A]
  final case class TFloat[A]()                                     extends ProtobufF[A]
  final case class TInt32[A]()                                     extends ProtobufF[A]
  final case class TInt64[A]()                                     extends ProtobufF[A]
  final case class TUint32[A]()                                    extends ProtobufF[A]
  final case class TUint64[A]()                                    extends ProtobufF[A]
  final case class TSint32[A]()                                    extends ProtobufF[A]
  final case class TSint64[A]()                                    extends ProtobufF[A]
  final case class TFixed32[A]()                                   extends ProtobufF[A]
  final case class TFixed64[A]()                                   extends ProtobufF[A]
  final case class TSfixed32[A]()                                  extends ProtobufF[A]
  final case class TSfixed64[A]()                                  extends ProtobufF[A]
  final case class TBool[A]()                                      extends ProtobufF[A]
  final case class TString[A]()                                    extends ProtobufF[A]
  final case class TBytes[A]()                                     extends ProtobufF[A]
  final case class TNamedType[A](name: String)                     extends ProtobufF[A]
  final case class TRepeated[A](value: A)                          extends ProtobufF[A]
  final case class TOneOf[A](name: String, fields: List[Field[A]]) extends ProtobufF[A]
  final case class TMap[A](keyTpe: A, value: A)                    extends ProtobufF[A]

  final case class TEnum[A](
      name: String,
      symbols: List[(String, Int)],
      options: List[Option],
      aliases: List[(String, Int)])
      extends ProtobufF[A]
  final case class TMessage[A](name: String, fields: List[FieldF[A]], reserved: List[List[String]]) extends ProtobufF[A]

  final case class TFileDescriptor[A](values: List[A], name: String, `package`: String) extends ProtobufF[A]

  def double[A](): ProtobufF[A]                                    = TDouble()
  def float[A](): ProtobufF[A]                                     = TFloat()
  def int32[A](): ProtobufF[A]                                     = TInt32()
  def int64[A](): ProtobufF[A]                                     = TInt64()
  def uint32[A](): ProtobufF[A]                                    = TUint32()
  def uint64[A](): ProtobufF[A]                                    = TUint64()
  def sint32[A](): ProtobufF[A]                                    = TSint32()
  def sint64[A](): ProtobufF[A]                                    = TSint64()
  def fixed32[A](): ProtobufF[A]                                   = TFixed32()
  def fixed64[A](): ProtobufF[A]                                   = TFixed64()
  def sfixed32[A](): ProtobufF[A]                                  = TSfixed32()
  def sfixed64[A](): ProtobufF[A]                                  = TSfixed64()
  def bool[A](): ProtobufF[A]                                      = TBool()
  def string[A](): ProtobufF[A]                                    = TString()
  def bytes[A](): ProtobufF[A]                                     = TBytes()
  def namedType[A](name: String): ProtobufF[A]                     = TNamedType(name)
  def repeated[A](value: A): ProtobufF[A]                          = TRepeated(value)
  def oneOf[A](name: String, fields: List[Field[A]]): ProtobufF[A] = TOneOf(name, fields)
  def map[A](keyTpe: A, value: A): ProtobufF[A]                    = TMap(keyTpe, value)
  def enum[A](
      name: String,
      symbols: List[(String, Int)],
      options: List[Option],
      aliases: List[(String, Int)]): ProtobufF[A] = TEnum(name, symbols, options, aliases)
  def message[A](name: String, fields: List[Field[A]], reserved: List[List[String]]): ProtobufF[A] =
    TMessage(name, fields, reserved)

//  implicit def protobufEq[T: Eq]: Eq[ProtobufF[T]] = Eq.instance {
//    case (TDouble(), TDouble())          => true
//    case (TFloat(), TFloat())            => true
//    case (TInt32(), TInt32())            => true
//    case (TInt64(), TInt64())            => true
//    case (TUint32(), TUint32())          => true
//    case (TUint64(), TUint64())          => true
//    case (TSint32(), TSint32())          => true
//    case (TSint64(), TSint64())          => true
//    case (TFixed32(), TFixed32())        => true
//    case (TFixed64(), TFixed64())        => true
//    case (TSfixed32(), TSfixed32())      => true
//    case (TSfixed64(), TSfixed64())      => true
//    case (TBool(), TBool())              => true
//    case (TString(), TString())          => true
//    case (TBytes(), TBytes())            => true
//    case (TNamedType(n), TNamedType(n2)) => n === n2
//    case (TRepeated(v), TRepeated(v2))   => v === v2
//
//    case (TEnum(n, s, o, a), TEnum(n2, s2, o2, a2)) =>
//      n === n2 && s === s2 && o === o2 && a === a2
//    case (TMessage(n, f, r), TMessage(n2, f2, r2)) => n === n2 && f === f2 && r === r2
//
//    case _ => false
//  }

  def fromProtobuf: Coalgebra[ProtobufF, BaseDescriptor] = Coalgebra {
    case f: FileDescriptor                                                            => fileFromDescriptor(f)
    case e: EnumDescriptor                                                            => enumFromDescriptor(e)
    case o: OneofDescriptor                                                           => makeTOneOf(o)
    case d: Descriptor                                                                => messageFromDescriptor(d)
    case f: FieldDescriptor if f.protoType == FieldDescriptorProto.Type.TYPE_BOOL     => TBool()
    case f: FieldDescriptor if f.protoType == FieldDescriptorProto.Type.TYPE_BYTES    => TBytes()
    case f: FieldDescriptor if f.protoType == FieldDescriptorProto.Type.TYPE_DOUBLE   => TDouble()
    case f: FieldDescriptor if f.protoType == FieldDescriptorProto.Type.TYPE_FIXED32  => TFixed32()
    case f: FieldDescriptor if f.protoType == FieldDescriptorProto.Type.TYPE_FIXED64  => TFixed64()
    case f: FieldDescriptor if f.protoType == FieldDescriptorProto.Type.TYPE_FLOAT    => TFloat()
    case f: FieldDescriptor if f.protoType == FieldDescriptorProto.Type.TYPE_INT32    => TInt32()
    case f: FieldDescriptor if f.protoType == FieldDescriptorProto.Type.TYPE_INT64    => TInt64()
    case f: FieldDescriptor if f.protoType == FieldDescriptorProto.Type.TYPE_SFIXED32 => TFixed32()
    case f: FieldDescriptor if f.protoType == FieldDescriptorProto.Type.TYPE_SFIXED64 => TFixed64()
    case f: FieldDescriptor if f.protoType == FieldDescriptorProto.Type.TYPE_SINT32   => TSint32()
    case f: FieldDescriptor if f.protoType == FieldDescriptorProto.Type.TYPE_SINT64   => TSint64()
    case f: FieldDescriptor if f.protoType == FieldDescriptorProto.Type.TYPE_STRING   => TString()
    case f: FieldDescriptor if f.protoType == FieldDescriptorProto.Type.TYPE_UINT32   => TUint32()
    case f: FieldDescriptor if f.protoType == FieldDescriptorProto.Type.TYPE_UINT64   => TUint64()
    case f: FieldDescriptor if f.protoType == FieldDescriptorProto.Type.TYPE_MESSAGE  => getNestedType(f)
    case f: FieldDescriptor if f.protoType == FieldDescriptorProto.Type.TYPE_ENUM     => getNestedType(f)
  }

  implicit val protoFunctor: Functor[ProtobufF] = new Functor[ProtobufF] {
    def map[A, B](fa: ProtobufF[A])(f: A => B): ProtobufF[B] = fa match {
      case TDouble()                              => TDouble[B]()
      case TFloat()                               => TFloat[B]()
      case TInt32()                               => TInt32[B]()
      case TInt64()                               => TInt64[B]()
      case TUint32()                              => TUint32[B]()
      case TUint64()                              => TUint64[B]()
      case TSint32()                              => TSint32[B]()
      case TSint64()                              => TSint64[B]()
      case TFixed32()                             => TFixed32[B]()
      case TFixed64()                             => TFixed64[B]()
      case TSfixed32()                            => TSfixed32[B]()
      case TSfixed64()                            => TSfixed64[B]()
      case TBool()                                => TBool[B]()
      case TString()                              => TString[B]()
      case TBytes()                               => TBytes[B]()
      case TNamedType(name)                       => TNamedType[B](name)
      case TRepeated(value)                       => TRepeated[B](f(value))
      case TOneOf(name, fields)                   => TOneOf[B](name, fields.map(field => field.copy(tpe = f(field.tpe))))
      case TMap(keyTpe, value)                    => TMap[B](f(keyTpe), f(value))
      case TEnum(name, symbols, options, aliases) => TEnum(name, symbols, options, aliases)
      case TMessage(name, fields, reserved) =>
        TMessage[B](
          name,
          fields.map(
            field =>
              field match {
                case OneOfField(n, tpe)                         => OneOfField(n, f(tpe))
                case Field(n, tpe, pos, opt, isRepeated, isMap) => Field(n, f(tpe), pos, opt, isRepeated, isMap)
            }
          ),
          reserved
        )
      case TFileDescriptor(values, name, p) => TFileDescriptor(values.map(f), name, p)
    }
  }

  object Options {
    import com.google.protobuf.descriptor.UninterpretedOption.NamePart

    def options[A, B](a: A, defaultFlags: List[(String, B)], f: A => Seq[UninterpretedOption]): List[Option] =
      (defaultFlags.map(e => (e._1, s"${e._2}")) ++ uninterpretedOptions(a, f)).map {
        case (name, value) => Option(name, value)
      }

    private def uninterpretedOptions[A](a: A, f: A => Seq[UninterpretedOption]): Seq[(String, String)] =
      f(a).flatMap(o => o.identifierValue.map((toString(o.name), _)))

    private def toString(nameParts: Seq[NamePart]): String =
      nameParts.foldLeft("")((l, r) => if (r.isExtension) s"$l.($r)" else s"$l.$r")
  }

  def fileFromDescriptor(fileDescriptor: FileDescriptor): TFileDescriptor[BaseDescriptor] = {
    TFileDescriptor(
      fileDescriptor.messages.toList ++ fileDescriptor.enums.toList,
      fileDescriptor.fullName,
      fileDescriptor.packageName
    )
  }

  def enumFromDescriptor(e: EnumDescriptor): TEnum[BaseDescriptor] = {
    val defaultOptions                       = List(("allow_alias", e.getOptions.getAllowAlias), ("deprecated", e.getOptions.getDeprecated))
    val valuesAndAliases: Seq[(String, Int)] = e.values.map(value => (value.name, value.number))
    val (values, aliases)                    = partitionValuesAliases(valuesAndAliases)

    TEnum(
      e.name,
      values,
      Options
        .options(e.getOptions, defaultOptions, (enumDescriptor: EnumOptions) => enumDescriptor.uninterpretedOption),
      aliases
    )
  }

  def partitionValuesAliases(valuesAndAliases: Seq[(String, Int)]): (List[(String, Int)], List[(String, Int)]) = {
    val (hasAlias, noAlias) = valuesAndAliases
      .groupBy(_._2)
      .values
      .partition(_.lengthCompare(1) > 0)

    val separateValueFromAliases = hasAlias.map(list => (list.head, list.tail))
    val values                   = separateValueFromAliases.map(_._1) ++ noAlias.flatten
    val aliases                  = separateValueFromAliases.flatMap(_._2)
    (values.toList.sortBy(_._2), aliases.toList) // Sorted b/c Enums must always start with their 0 value field
  }

  def messageFromDescriptor(descriptor: Descriptor): TMessage[BaseDescriptor] = {
    val fields: List[FieldF[BaseDescriptor]] = fieldsFromDescriptor(descriptor)
    val reserved: List[List[String]] =
      descriptor.asProto.reservedRange.map(range => (range.getStart until range.getEnd).map(_.toString).toList).toList
    TMessage[BaseDescriptor](descriptor.name, fields, reserved)
  }

  def makeTOneOf(oneOf: OneofDescriptor): TOneOf[BaseDescriptor] = {
    val fields = oneOf.fields.map(
      f =>
        Field[BaseDescriptor](
          f.name,
          f,
          f.number,
          List(),
          f.isRepeated,
          f.isMapField
      )
    )

    TOneOf[BaseDescriptor](oneOf.name, fields.toList)
  }

  def fieldsFromDescriptor(descriptor: Descriptor): List[FieldF[BaseDescriptor]] = {
    val options        = descriptor.getOptions
    val defaultOptions = List(("deprecated", options.getDeprecated))

    val simpleFields: List[OneOfField[BaseDescriptor]] =
      descriptor.oneofs.map(oneOf => OneOfField[BaseDescriptor](oneOf.name, oneOf)).toList

    val fields = descriptor.fields
      .filterNot(
        fieldDesc => descriptor.oneofs.flatMap(_.fields.map(_.number)).contains(fieldDesc.number)
      )
      .map(
        fieldDesc =>
          Field[BaseDescriptor](
            fieldDesc.name,
            fieldDesc,
            fieldDesc.number,
            Options.options(descriptor, defaultOptions, (d: Descriptor) => d.getOptions.uninterpretedOption),
            fieldDesc.isRepeated,
            fieldDesc.isMapField
        )
      )
      .toList

    fields ++ simpleFields
  }

  def getNestedType(f: FieldDescriptor): ProtobufF[BaseDescriptor] = {
    f.scalaType match {
      case ScalaType.Message(descriptor) if f.isMapField => getMapTypes(descriptor)
      case ScalaType.Message(descriptor)                 => TNamedType(descriptor.name)
      case ScalaType.Enum(enumDesc)                      => TNamedType(enumDesc.name)
      case _: ScalaType                                  => TNamedType("Unknown") // TODO: what should be done here?
    }
  }

  /** Protobuf represents a .proto file's map<keyType, valueType>
   * as a Descriptor that it creates itself, which is not available at the top level
   * of a file. We need to parse that synthetic descriptor to get at its inner fields,
   * which represent the key and value of a scala map */
  def getMapTypes(syntheticDesc: Descriptor): TMap[BaseDescriptor] = {
    val keyValueFields = fieldsFromDescriptor(syntheticDesc)
    require(
      keyValueFields.length == 2,
      s"Message did not contain exactly two fields corresponding to the key and value of a Map"
    )
    val key :: value :: Nil = keyValueFields
    TMap(key.tpe, value.tpe)
  }
}
