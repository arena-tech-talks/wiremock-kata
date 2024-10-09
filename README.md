# wiremock-kata

Kata for using Wiremock in Unit Tests to test REST API clients independently.

- [Wiremock Homepage](https://wiremock.org/)
- [Wiremock Key features](https://wiremock.org/docs/overview/#key-features)
- [Wiremock Docs](https://wiremock.org/docs/)

## Repository Layout

| Item                         | description                                                                          |
|------------------------------|--------------------------------------------------------------------------------------|
| mvnw*, .mvn                  | Maven wrapper files                                                                  |
| [openapi.yaml](openapi.yaml) | simple sample API for practising                                                     |
| [book-server](book-server)   | simple server implementation of sample API                                           |
| [book-client](book-client)   | simple client implementation of sample API - for writing unit tests for in this kata |

## Sample API as test object

Simple REST API to understand easily: [openapi.yaml](openapi.yaml)

Features:
- simple CRUD app
- real backend uses file based database to store data (see [book-server](/book-server))
- GET, POST
- Json Format
- Path Parameters

## Commands

Run the unit tests using maven:

        ./mvnw -f book-client/pom.xml test

Start the sample server:

        ./mvnw -f book-server spring-boot:run

In Intellij you can also try out the API from the OpenAPI Spec against the sample server on localhost. 
 
