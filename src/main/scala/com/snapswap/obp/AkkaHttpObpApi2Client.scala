package com.snapswap.obp

import java.util.UUID
import java.util.concurrent.TimeUnit

import akka.actor.ActorSystem
import akka.event.{Logging, LoggingAdapter}
import akka.http.scaladsl.Http
import akka.http.scaladsl.client.RequestBuilding.{Get, Post, Put}
import akka.http.scaladsl.model._
import akka.http.scaladsl.model.headers.RawHeader
import akka.http.scaladsl.settings.ConnectionPoolSettings
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.stream.Materializer
import akka.stream.scaladsl.{Sink, Source}
import com.google.common.cache.{CacheBuilder, CacheLoader, LoadingCache}
import com.snapswap.obp.OBP._
import org.joda.time.DateTime
import spray.json._

import scala.concurrent.duration.{Duration, SECONDS}
import scala.concurrent.{Await, ExecutionContext, Future}
import scala.util.{Failure, Success, Try}

class AkkaHttpObpApi2Client(directLogin: OBPAPIDirectLogin,
                            apiHost: Uri.Host,
                            apiVersion: String = "2.0.0")(
                             implicit system: ActorSystem,
                             ctx: ExecutionContext,
                             materializer: Materializer)
  extends ObpApi2Client {

  override def createUser(email: String,
                          username: String,
                          password: String,
                          firstName: String,
                          lastName: String): Future[String] = {
    withPossibleRelogin {
      apiPost(
        "/users",
        CreateUser(email, username, password, firstName, lastName).toJson)(
        _.convertTo[CreateUserResponse].userId
      )
    }
  }

  override def createCustomer(bankId: String,
                              userId: String,
                              customerNumber: String,
                              legalName: String,
                              mobilePhoneNumber: String,
                              email: String,
                              faceImageUrl: Uri,
                              faceImageDate: DateTime,
                              dateOfBirth: DateTime,
                              relationshipStatus: String,
                              dependants: Int,
                              dobOfDependants: Seq[DateTime],
                              creditRating: String,
                              creditRatingSource: String,
                              creditLimitAmount: BigDecimal,
                              creditLimitCurrency: String,
                              highestEducationAttained: String,
                              employmentStatus: String,
                              kycStatus: Boolean,
                              lastOkDate: DateTime): Future[String] = {
    withPossibleRelogin {
      apiPost(
        s"/banks/$bankId/customers",
        CreateCustomer(
          userId,
          customerNumber,
          legalName,
          mobilePhoneNumber,
          email,
          FaceImage(faceImageUrl.toString(), faceImageDate),
          dateOfBirth,
          relationshipStatus,
          dependants,
          dobOfDependants,
          highestEducationAttained,
          employmentStatus,
          kycStatus,
          lastOkDate
        ).toJson
      )(
        _.convertTo[CreateCustomerResponse].customerId
      )
    }
  }

  override def createAccount(accountId: String,
                             bankId: String,
                             userId: String,
                             label: String,
                             `type`: String,
                             balanceAmount: BigDecimal,
                             balanceCurrency: String,
                             branchId: String,
                             accountRoutingScheme: String,
                             accountRoutingAddress: String): Future[Unit] = {
    withPossibleRelogin {
      apiPut(s"/banks/$bankId/accounts/$accountId",
        CreateAccount(
          userId,
          label,
          `type`,
          OBPAmount(balanceCurrency, balanceAmount)
        ).toJson)(_ => ())
    }
  }

  override def addKycDocument(documentId: String,
                              customerId: String,
                              bankId: String,
                              customerNumber: String,
                              `type`: String,
                              number: String,
                              issueDate: DateTime,
                              issuePlace: String,
                              expiryDate: DateTime): Future[Unit] = {
    withPossibleRelogin {
      apiPut(s"/banks/$bankId/customers/$customerId/kyc_documents/$documentId",
        AddKycDocument(
          customerNumber,
          `type`,
          number,
          issueDate,
          issuePlace,
          expiryDate
        ).toJson)(_ => ())
    }
  }

  override def addKycCheck(checkId: String,
                           customerId: String,
                           bankId: String,
                           customerNumber: String,
                           date: DateTime,
                           how: String,
                           staffUserId: String,
                           staffName: String,
                           satisfied: Boolean,
                           comments: String): Future[Unit] = {
    withPossibleRelogin {
      apiPut(s"/banks/$bankId/customers/$customerId/kyc_check/$checkId",
        AddKycCheck(
          customerNumber,
          date,
          how,
          staffUserId,
          staffName,
          satisfied,
          comments
        ).toJson)(_ => ())
    }
  }

  override def addKycMedia(mediaId: String,
                           customerId: String,
                           bankId: String,
                           customerNumber: String,
                           `type`: String,
                           url: Uri,
                           date: DateTime,
                           relatesToDocumentId: String,
                           relatesToCheckId: String): Future[Unit] = {
    withPossibleRelogin {
      apiPut(
        s"/banks/$bankId/customers/$customerId/kyc_media/$mediaId",
        AddKycMedia(
          customerNumber,
          `type`,
          url.toString(),
          date,
          relatesToDocumentId,
          relatesToCheckId
        ).toJson
      )(_ => ())
    }
  }

  override def addKycStatus(customerId: String,
                            bankId: String,
                            customerNumber: String,
                            isOk: Boolean,
                            date: DateTime): Future[Unit] = {
    withPossibleRelogin {
      apiPut(s"/banks/$bankId/customers/$customerId/kyc_statuses",
        AddKycStatus(
          customerNumber,
          isOk,
          date
        ).toJson)(_ => ())
    }
  }

  override def getAccount(accountId: String,
                          bankId: String): Future[OBPAccount] = {
    withPossibleRelogin {
      apiGet(s"/banks/$bankId/accounts/$accountId/account")(
        _.convertTo[OBPAccount])
    }
  }

  private def apiPost[R: JsonReader](path: String, data: JsValue)(parser: JsValue => R)(token: String): Future[R] = {
    post(apiBase, authorizationHeader(token), path, data)(parser)
  }

  private def apiPut[R: JsonReader](path: String, data: JsValue)(parser: JsValue => R)(token: String): Future[R] = {
    put(apiBase, authorizationHeader(token), path, data)(parser)
  }

  private def apiGet[R: JsonReader](path: String)(parser: JsValue => R)(token: String): Future[R] = {
    get(apiBase, authorizationHeader(token), path)(parser)
  }

  private val log: LoggingAdapter = Logging(system, this.getClass)

  private val flow = Http()
    .cachedHostConnectionPoolHttps[UUID](apiHost.toString(),
    settings =
      ConnectionPoolSettings(system),
    log = log)
    .log("obp")

  private val apiBase = Uri.from(scheme = "https",
    host = apiHost.toString(),
    path = s"/obp/v$apiVersion")

  private val tokens: LoadingCache[OBPAPIDirectLogin, String] = CacheBuilder
    .newBuilder()
    .maximumSize(10)
    .expireAfterWrite(30, TimeUnit.MINUTES)
    .build(new CacheLoader[OBPAPIDirectLogin, String] {
      override def load(key: OBPAPIDirectLogin): String =
        Await.result(doDirectLogin(key), Duration(15, SECONDS))
    })

  private def doDirectLogin(directLogin: OBPAPIDirectLogin): Future[String] = {
    post(Uri.from(scheme = "https", host = apiHost.toString()),
      authorizationHeader(directLogin),
      "/my/logins/direct",
      JsNull) { response =>
      response.convertTo[DirectLoginToken].token
    }
  }

  private def authorizationHeader(directLogin: OBPAPIDirectLogin): HttpHeader = {
    RawHeader(
      "Authorization",
      s"""DirectLogin username="${directLogin.username}",password="${directLogin.password}",consumer_key="${directLogin.consumerKey}"""")
  }

  private def authorizationHeader(token: String): HttpHeader = {
    RawHeader("Authorization", s"DirectLogin token=$token")
  }

  private def withToken[T](f: String => Future[T]): Future[T] = {
    for {
      token <- Future(tokens.get(directLogin))
      result <- f(token)
    } yield result
  }

  private def withPossibleRelogin[T](f: String => Future[T]): Future[T] = {
    withToken(f).recoverWith {
      case OBPAPIUnauthorized =>
        tokens.invalidate(directLogin)
        withToken(f)
    }
  }

  private def send(request: HttpRequest): Future[JsValue] =
    Source
      .single(request -> UUID.randomUUID())
      .via(flow)
      .runWith(Sink.head)
      .recoverWith {
        case ex =>
          Future.failed(OBPAPIConnectionError(
            s"${request.method.value} ${request.uri} failed: ${ex.getMessage}",
            Some(ex)))
      }
      .flatMap {
        case (Success(response), _) =>
          if (response.status == StatusCodes.NotFound) {
            log.error(
              s"Response to ${request.method.value} ${request.uri} is ${response.status}")
            Future.failed(OBPAPIEntityNotFound)
          } else if (response.status == StatusCodes.Unauthorized) {
            log.error(
              s"Response to ${request.method.value} ${request.uri} is ${response.status}")
            Future.failed(OBPAPIUnauthorized)
          } else {
            Unmarshal(response.entity).to[String].flatMap { responseEntity =>
              Try(responseEntity.parseJson) match {
                case Success(json) =>
                  log.debug(
                    s"Response to ${request.method.value} ${request.uri} is ${response.status}: '${json.compactPrint}'")
                  json.asJsObject.fields.get("error") match {
                    case Some(JsString(error)) =>
                      Future.failed(OBPAPIError(error))
                    case _ => Future.successful(json)
                  }
                case Failure(ex) =>
                  log.error(
                    ex,
                    s"Response to ${request.method.value} ${request.uri} isn't JSON (${ex.getMessage}): $responseEntity")
                  Future.failed(
                    OBPAPIMalformedResponse(responseEntity, Some(ex)))
              }
            }
          }
        case (Failure(ex), _) =>
          log.error(
            ex,
            s"${request.method.value} ${request.uri} failed with ${ex.getMessage}")
          Future.failed(ex)
      }

  private def sendRequest[T](request: HttpRequest)(
    parser: JsValue => T): Future[T] =
    send(request).flatMap { json =>
      Try(parser(json)) match {
        case Success(result) =>
          Future.successful(result)
        case Failure(ex) =>
          Future.failed(OBPAPIMalformedResponse(json.compactPrint, Some(ex)))
      }
    }

  private def get[T](
                      base: Uri,
                      authorization: HttpHeader,
                      path: String,
                      query: Map[String, String] = Map())(parser: JsValue => T): Future[T] = {
    sendRequest(
      Get(base.withPath(base.path + path).withQuery(Uri.Query(query)))
        .withHeaders(authorization)
    )(parser)
  }

  private def post[T](base: Uri,
                      authorization: HttpHeader,
                      path: String,
                      data: JsValue)(parser: JsValue => T): Future[T] = {
    val body = data.compactPrint
    val url = base.withPath(base.path + path)

    log.debug(s"POST $url with: $body")

    sendRequest(
      Post(url)
        .withEntity(
          HttpEntity(ContentType(MediaTypes.`application/json`), body))
        .withHeaders(authorization)
    )(parser)
  }

  private def put[T](base: Uri,
                     authorization: HttpHeader,
                     path: String,
                     data: JsValue)(parser: JsValue => T): Future[T] = {
    val body = data.compactPrint
    val url = base.withPath(base.path + path)

    log.debug(s"PUT $url with: $body")

    sendRequest(
      Put(url)
        .withEntity(
          HttpEntity(ContentType(MediaTypes.`application/json`), body))
        .withHeaders(authorization)
    )(parser)
  }
}
