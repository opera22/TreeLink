package com.TreeLink

import cats._
import cats.effect._
import cats.implicits._
//import cats.syntax.functor._
//import cats.syntax.flatMap._
import org.http4s.circe._
import org.http4s._
import io.circe.generic.auto._
import io.circe.syntax._
import org.http4s.dsl._
//import org.http4s.dsl.impl._
//import org.http4s.headers._
import org.http4s.implicits._
import org.http4s.server._
import org.http4s.server.blaze.BlazeServerBuilder

object Main extends IOApp {
  println("TreeLink app is running!")

  def helloRoutes[F[_] : Monad]: HttpRoutes[F] = {
    val dsl = Http4sDsl[F]
    import dsl._

    HttpRoutes.of[F] {
      case GET -> Root => Ok(Map("hi" -> "hello world!").asJson)
    }
  }

  def userRoutes[F[_] : Concurrent]: HttpRoutes[F] = {
    val dsl = Http4sDsl[F]
    import dsl._
    implicit val userDecoder: EntityDecoder[F, DatabaseService.User] = jsonOf[F, DatabaseService.User]
    implicit val userEncoder: EntityEncoder[F, DatabaseService.User] = jsonEncoderOf[F, DatabaseService.User]

    HttpRoutes.of[F] {
      case GET -> Root / username => DatabaseService.findUserByUsername(username) match {
        case Some(user) => Ok(user.asJson)
        case _ => NotFound(s"No user with name $username found in the database.")
      }
      case req @ POST -> Root =>
        for {
          // Decode a User request
          user <- req.as[DatabaseService.User]
          // Insert into database
          dbuser = DatabaseService.insertUser(user.username)
          // Encode a response
          resp <- Ok(dbuser.asJson)
        } yield resp
    }
  }

  def treeRoutes[F[_] : Concurrent]: HttpRoutes[F] = {
    val dsl = Http4sDsl[F]
    import dsl._
    implicit val linkDecoder: EntityDecoder[F, DatabaseService.Link] = jsonOf[F, DatabaseService.Link]
    implicit val linkEncoder: EntityEncoder[F, DatabaseService.Link] = jsonEncoderOf[F, DatabaseService.Link]

    HttpRoutes.of[F] {
      case GET -> Root / username =>
        val links = DatabaseService.findTreeByUsername(username)
        if (links.isEmpty) NotFound(s"User $username has no TreeLink.")
        else Ok(links.asJson)
      case req @ POST -> Root =>
        for {
          tree <- req.as[DatabaseService.Link]
          link = DatabaseService.insertLink(tree.userId, tree.link)
          resp <- Ok(link.asJson)
        } yield resp
    }
  }

  override def run(args: List[String]): IO[ExitCode] = {

    val apis = Router(
      "/hello" -> helloRoutes[IO],
      "/users" -> userRoutes[IO],
      "/tree"-> treeRoutes[IO]
    ).orNotFound

    BlazeServerBuilder[IO](runtime.compute)
      .bindHttp(8080, "localhost")
      .withHttpApp(apis)
      .resource
      .use(_ => IO.never)
      .as(ExitCode.Success)

  }
}
