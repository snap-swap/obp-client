package com.snapswap.obp

import java.time.{LocalDate, ZonedDateTime}

import akka.http.scaladsl.model.Uri

import scala.concurrent.Future

trait ObpApi2Client {

  def createUser(email: String,
                 username: String,
                 password: String,
                 firstName: String,
                 lastName: String): Future[String]

  def createCustomer(bankId: String,
                     userId: String,
                     customerNumber: String,
                     legalName: String,
                     mobilePhoneNumber: String,
                     email: String,
                     faceImageUrl: Uri,
                     faceImageDate: ZonedDateTime,
                     dateOfBirth: LocalDate,
                     relationshipStatus: String,
                     dependants: Int,
                     dobOfDependants: Seq[LocalDate],
                     creditRating: String,
                     creditRatingSource: String,
                     creditLimitAmount: BigDecimal,
                     creditLimitCurrency: String,
                     highestEducationAttained: String,
                     employmentStatus: String,
                     kycStatus: Boolean,
                     lastOkDate: ZonedDateTime): Future[String]

  def createAccount(accountId: String, bankId: String,
                    userId: String, label: String, `type`: String,
                    balanceAmount: BigDecimal, balanceCurrency: String, branchId: String,
                    accountRoutingScheme: String, accountRoutingAddress: String): Future[Unit]

  def addKycDocument(documentId: String,
                     customerId: String,
                     bankId: String,
                     customerNumber: String,
                     `type`: String,
                     number: String,
                     issueDate: LocalDate,
                     issuePlace: String,
                     expiryDate: LocalDate): Future[Unit]

  def addKycCheck(checkId: String,
                  customerId: String,
                  bankId: String,
                  customerNumber: String,
                  date: ZonedDateTime,
                  how: String,
                  staffUserId: String,
                  staffName: String,
                  satisfied: Boolean,
                  comments: String): Future[Unit]

  def addKycMedia(mediaId: String,
                  customerId: String,
                  bankId: String,
                  customerNumber: String,
                  `type`: String,
                  url: Uri,
                  date: ZonedDateTime,
                  relatesToDocumentId: String,
                  relatesToCheckId: String): Future[Unit]

  def addKycStatus(customerId: String,
                   bankId: String,
                   customerNumber: String,
                   isOk: Boolean,
                   date: ZonedDateTime): Future[Unit]

  def getAccount(accountId: String,
                 bankId: String): Future[OBPAccount]
}