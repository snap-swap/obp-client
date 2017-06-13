package com.snapswap.obp

import akka.http.scaladsl.model._
import org.joda.time.DateTime

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
                     lastOkDate: DateTime): Future[String]

  def createAccount(accountId: String, bankId: String,
                    userId: String, label: String, `type`: String,
                    balanceAmount: BigDecimal, balanceCurrency: String, branchId: String,
                    accountRoutingScheme: String, accountRoutingAddress: String): Future[Unit]

  def addKycDocument(documentId: String, customerId: String, bankId: String,
                     customerNumber: String,
                     `type`: String,
                     number: String,
                     issueDate: DateTime,
                     issuePlace: String,
                     expiryDate: DateTime): Future[Unit]

  def addKycCheck(checkId: String, customerId: String, bankId: String,
                  customerNumber: String,
                  date: DateTime,
                  how: String,
                  staffUserId: String,
                  staffName: String,
                  satisfied: Boolean,
                  comments: String): Future[Unit]

  def addKycMedia(mediaId: String, customerId: String, bankId: String,
                  customerNumber: String,
                  `type`: String,
                  url: Uri,
                  date: DateTime,
                  relatesToDocumentId: String,
                  relatesToCheckId: String): Future[Unit]

  def addKycStatus(customerId: String, bankId: String,
                   customerNumber: String,
                   isOk: Boolean,
                   date: DateTime): Future[Unit]

  def getAccount(accountId: String,
                 bankId: String): Future[OBPAccount]
}