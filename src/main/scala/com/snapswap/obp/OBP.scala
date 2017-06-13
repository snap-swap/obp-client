package com.snapswap.obp

import org.joda.time.format.ISODateTimeFormat
import org.joda.time.{DateTime, DateTimeZone}
import spray.json._

case class OBPAPIDirectLogin(username: String, password: String, consumerKey: String)

case class OBPAmount(currency: String, amount: BigDecimal)

case class FaceImage(url: String, date: DateTime)

case class DirectLoginToken(token: String)

case class AccountRouting(scheme: String, address: String)

case class CreateCustomer(userId: String,
                          customerNumber: String,
                          legalName: String,
                          mobilePhoneNumber: String,
                          email: String,
                          faceImage: FaceImage,
                          dateOfBirth: DateTime,
                          relationshipStatus: String,
                          dependants: Int,
                          dobOfDependants: Seq[DateTime],
                          highestEducationAttained: String,
                          employmentStatus: String,
                          kycStatus: Boolean,
                          lastOkDate: DateTime)

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
                       date: DateTime,
                       how: String,
                       staffUserId: String,
                       staffName: String,
                       satisfied: Boolean,
                       comments: String)

case class AddKycDocument(customerNumber: String,
                          `type`: String,
                          number: String,
                          issueDate: DateTime,
                          issuePlace: String,
                          expiryDate: DateTime)

case class AddKycMedia(customerNumber: String,
                       `type`: String,
                       url: String,
                       date: DateTime,
                       relatesToDocumentId: String,
                       relatesToCheckId: String)

case class OBPAccount(iban: String, swiftBic: String, balance: OBPAmount)

case class AddKycStatus(customerNumber: String, isOk: Boolean, date: DateTime)

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

  implicit object DateTimeFormat extends JsonFormat[DateTime] {
    def write(dt: DateTime): JsValue =
      JsString(ISODateTimeFormat.dateTimeNoMillis().withZoneUTC().print(dt))

    def read(value: JsValue): DateTime = value match {
      case JsString(s) => ISODateTimeFormat.dateOptionalTimeParser().withZoneUTC().parseDateTime(s.trim)
      case JsNumber(n) => new DateTime(n.toLong, DateTimeZone.UTC)
      case x => deserializationError("Expected DateTime as JsNumber or JsString in ISO8601 format, but got " + x)
    }
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
