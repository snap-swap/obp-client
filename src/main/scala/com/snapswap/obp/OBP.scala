package com.snapswap.obp

import java.time.format.DateTimeFormatter
import java.time.{LocalDate, ZonedDateTime}

import spray.json._

import scala.util.{Failure, Success, Try}

case class OBPAPIDirectLogin(username: String, password: String, consumerKey: String)

case class OBPAmount(currency: String, amount: BigDecimal)

case class FaceImage(url: String, date: ZonedDateTime)

case class DirectLoginToken(token: String)

case class AccountRouting(scheme: String, address: String)

case class CreateCustomer(userId: String,
                          customerNumber: String,
                          legalName: String,
                          mobilePhoneNumber: String,
                          email: String,
                          faceImage: FaceImage,
                          dateOfBirth: LocalDate,
                          relationshipStatus: String,
                          dependants: Int,
                          dobOfDependants: Seq[LocalDate],
                          highestEducationAttained: String,
                          employmentStatus: String,
                          kycStatus: Boolean,
                          lastOkDate: ZonedDateTime)

case class CreateCustomerResponse(customerId: String)

case class CreateUser(email: String,
                      username: String,
                      password: String,
                      firstName: String,
                      lastName: String)

case class CreateUserResponse(userId: String)

case class CreateAccount(userId: String,
                         label: String,
                         `type`: String,
                         balance: OBPAmount)

case class AddKycCheck(customerNumber: String,
                       date: ZonedDateTime,
                       how: String,
                       staffUserId: String,
                       staffName: String,
                       satisfied: Boolean,
                       comments: String)

case class AddKycDocument(customerNumber: String,
                          `type`: String,
                          number: String,
                          issueDate: LocalDate,
                          issuePlace: String,
                          expiryDate: LocalDate)

case class AddKycMedia(customerNumber: String,
                       `type`: String,
                       url: String,
                       date: ZonedDateTime,
                       relatesToDocumentId: String,
                       relatesToCheckId: String)

case class OBPAccount(iban: String, swiftBic: String, balance: OBPAmount)

case class AddKycStatus(customerNumber: String, isOk: Boolean, date: ZonedDateTime)

trait OBPError extends RuntimeException {
  def details: String

  def cause: Option[Throwable] = None

  override def getMessage: String = details

  override def getCause: Throwable = cause.orNull
}

case class OBPAPIConnectionError(details: String, override val cause: Option[Throwable] = None) extends OBPError

case object OBPAPIEntityNotFound extends OBPError {
  override val details = "OBP API entity is not found"
}

case object OBPAPIUnauthorized extends OBPError {
  override val details = "OBP API authentication token is either invalid or expired"
}

case class OBPAPIMalformedResponse(responseEntity: String, override val cause: Option[Throwable] = None) extends OBPError {
  override val details = s"OBP API response is malformed: $responseEntity"
}

case class OBPAPIError(details: String) extends OBPError

object OBP extends DefaultJsonProtocol {

  implicit val bigDecimalJsonFormat = new RootJsonFormat[BigDecimal] {
    def write(d: BigDecimal): JsValue =
      JsString(d.toString())

    def read(value: JsValue): BigDecimal = value match {
      case JsString(s) => BigDecimal(s.trim)
      case JsNumber(n) => n
      case x => deserializationError("Expected BigDecimal as JsString or JsNumber, but got " + x)
    }
  }

  implicit object DateJsonFormat extends RootJsonFormat[LocalDate] {
    private val formatter = DateTimeFormatter.ISO_OFFSET_DATE

    override def read(json: JsValue): LocalDate = json match {
      case JsString(str) =>
        Try(ZonedDateTime.parse(str, formatter)) match {
          case Success(dt) =>
            dt.toLocalDate
          case Failure(ex) =>
            deserializationError(s"Expected Date as JsString in '${formatter.toString}' format, but got '$str'", ex)
        }
      case other =>
        deserializationError(s"Expected DateTime as JsString, but got $other")
    }

    override def write(obj: LocalDate) =
      JsString(obj.format(formatter))
  }


  implicit object DateTimeJsonFormat extends RootJsonFormat[ZonedDateTime] {
    private val formatter = DateTimeFormatter.ISO_OFFSET_DATE_TIME

    override def read(json: JsValue): ZonedDateTime = json match {
      case JsString(str) =>
        Try(ZonedDateTime.parse(str, formatter)) match {
          case Success(dt) =>
            dt
          case Failure(ex) =>
            deserializationError(s"Expected Date as JsString in '${formatter.toString}' format, but got '$str'", ex)
        }
      case other =>
        deserializationError(s"Expected DateTime as JsString, but got $other")
    }

    override def write(obj: ZonedDateTime) =
      JsString(obj.format(formatter))
  }

  implicit val DirectLoginTokenFormat = jsonFormat1(DirectLoginToken)
  implicit val CreditLimitFormat = jsonFormat2(OBPAmount)
  implicit val FaceImageFormat = jsonFormat2(FaceImage)
  implicit val AccountRoutingFormat = jsonFormat2(AccountRouting)
  implicit val OBPAccountFormat = jsonFormat(OBPAccount, "IBAN", "swift_bic", "balance")

  implicit val CreateCustomerFormat = jsonFormat(
    CreateCustomer,
    "user_id",
    "customer_number",
    "legal_name",
    "mobile_phone_number",
    "email",
    "face_image",
    "date_of_birth",
    "relationship_status",
    "dependants",
    "dob_of_dependants",
    "highest_education_attained",
    "employment_status",
    "kyc_status",
    "last_ok_date")

  implicit val CreateCustomerResponseFormat = jsonFormat(CreateCustomerResponse, "customer_id")

  implicit val CreateUserFormat = jsonFormat(CreateUser, "email", "username", "password", "first_name", "last_name")

  implicit val CreateUserResponseFormat = jsonFormat(CreateUserResponse, "user_id")

  implicit val CreateAccountFormat = jsonFormat(CreateAccount, "user_id", "label", "type", "balance")

  implicit val AddKycCheckFormat = jsonFormat(AddKycCheck, "customer_number", "date", "how", "staff_user_id", "staff_name", "satisfied", "comments")

  implicit val AddKycDocumentFormat = jsonFormat(AddKycDocument, "customer_number", "type", "number", "issue_date", "issue_place", "expiry_date")

  implicit val AddKycMediaFormat = jsonFormat(AddKycMedia, "customer_number", "type", "url", "date", "relates_to_kyc_document_id", "relates_to_kyc_check_id")

  implicit val AddKycStatusFormat = jsonFormat(AddKycStatus, "customer_number", "ok", "date")
}
