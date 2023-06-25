# TreeLink
This project is meant to be a lightweight clone of a popular website where users can store a list of personal links.

It is built using HTTP4S for the server, Circe for encoding, and Doobie for database access. The database is a MySQL instance deployed on PlanetScale (heard good things about it, wanted to try it out), but if you want to use a different database, just change the driver.

If you have any recommendations or pointers, please submit a PR or an Issue.

## Endpoints

### POST `/users` Creates a user
#### Body
`{ "username": String }`
#### Response
`{
"id": Int,
"username": String,
"createdDate": String
}`

<br>

### GET `/users/{username}` Gets a user
#### Response
`{
"id": Int,
"username": String,
"createdDate": String 
}`

<br>

### POST `/tree` Creates a new link
#### Body
`{
"userId": Int, 
"link": String
}`
#### Response
`{
"id": Int,
"link": String,
"userId": Int,
"createdDate": String
}`

<br>

### GET `/tree/{username}` Gets user's links
#### Response
`
[
String, String, ...
]
`

<br>

### GET `/hello` - Test endpoint
#### Response
`{
"hi": "hello world!"
}`

## Notes and Learnings
- It can be difficult to extract optionals from the Doobie SQL transactions in `DatabaseService.scala` and do the error handling in `Main.scala`. For example, in the case of `/tree/{username}` we simply check for an empty list to determine the HTTP response code, instead of using pattern matching on an Optional, which would be ideal.
- Implicit vals are automatically passed as arguments to the demanding methods. I believe they match on the object's type.
- By default, Doobie absorbs `select *` columns into an object (such as a `User` in this project) the order they are received. You might need to query columns in a specific order, or reorder the case class definition to fit the database schema.