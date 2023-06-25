package com.TreeLink

import doobie._
import doobie.implicits._
import cats.effect.IO
import cats.effect.unsafe.implicits.global

object DatabaseService {

  val dbUser = sys.env.getOrElse("db_user", throw new Exception("Missing DB User!"))
  val dbPassword = sys.env.getOrElse("db_password", throw new Exception("Missing DB Password!"))
  val dbHost = sys.env.getOrElse("db_host", throw new Exception("Missing DB Host!"))
  val dbName = sys.env.getOrElse("db_name", throw new Exception("Missing DB Name!"))

  val xa = Transactor.fromDriverManager[IO](
    "com.mysql.cj.jdbc.Driver",
    s"jdbc:mysql://$dbHost/$dbName?sslMode=VERIFY_IDENTITY",
    dbUser,
    dbPassword
  )

  case class User(id: Option[Int], username: String, createdDate: Option[String])
  case class Link(id: Option[Int], link: String, userId: Int, createdDate: Option[String])

  def findUserByUsername(username: String): Option[User] = {
    sql"select id, username, created_date from users where username = ${username}".query[User].option.transact(xa).unsafeRunSync()
  }

  def findUserByUserId(userId: Int): Option[User] = {
    sql"select id, username, created_date from users where id = ${userId}".query[User].option.transact(xa).unsafeRunSync()
  }

  def findTreeByUsername(username: String): List[String] = {
    sql"select link from links left join users on users.id = links.user_id where users.username = ${username}".query[String].to[List].transact(xa).unsafeRunSync()
  }

  def insertUser(username: String): User = {
    val retrievedUser = for {
      id <- sql"insert into users (username) values ($username)".update.withUniqueGeneratedKeys[Int]("id")
      user <- sql"select * from users where id = $id".query[User].unique
    } yield user
    retrievedUser.transact(xa).unsafeRunSync()
  }

  def insertLink(userId: Int, link: String): Link = {
    val retrievedLink = for {
      id <- sql"insert into links (user_id, link) values ($userId, $link)".update.withUniqueGeneratedKeys[Int]("id")
      link <- sql"select * from links where id = $id".query[Link].unique
    } yield link
    retrievedLink.transact(xa).unsafeRunSync()
  }
}

