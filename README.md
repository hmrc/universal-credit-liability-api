# universal-credit-liability-api

## Overview

This API provides the capability for DWP to send Universal Credit Liability notifications for a given individual on NPS (National Insurance & PAYE Service).


## Running Locally

Compile the project with:

```shell
sbt clean compile update
```

Run the project locally with:

```shell
sbt run
```

By default, the service runs on port **16107**.


## Running with Service Manager

Use [Service Manager](https://github.com/hmrc/sm2) to start all the services required to run and test Universal Credit
Liability service locally.

Start the **UNIVERSAL_CREDIT_LIABILITY_ALL** profile, responsible for starting up all the services required, with:

```shell
sm2 --start UNIVERSAL_CREDIT_LIABILITY_ALL
```

## Testing

Run unit tests with:

```shell
sbt test
```

Run integration tests with:

```shell
sbt it/test
```

Check code coverage with:

```shell
sbt clean coverage test it/test coverageReport
```

## Endpoints

### Insert/Terminate Universal Credit Liability Details

**Endpoint**: `POST /misc/universal-credit/liability/notification`

**Description**: Provides the capability to insert and terminate Universal Credit Liability details for a given individual.


## Previewing the OpenAPI Specification

To preview the OpenAPI Specification (OAS) locally, start the `DEVHUB_PREVIEW_OPENAPI` service with Service Manager:
```shell
sm2 --start DEVHUB_PREVIEW_OPENAPI
```

If the service is running on the Service Manager, stop it:
```shell
sm2 --stop UNIVERSAL_CREDIT_LIABILITY_API
```

and run the service locally:
```shell
sbt run
```

Go to the local preview page at http://localhost:9680/api-documentation/docs/openapi/preview

In the URL field enter the URL of the OpenAPI Specification (OAS) file:

http://localhost:16107/api/conf/1.0/application.yaml

and click "Submit".


## Scalafmt

Check all project files are formatted as expected as follows:

```bash
sbt scalafmtCheckAll scalafmtCheck
```

Format `*.sbt` and `project/*.scala` files as follows:

```bash
sbt scalafmtSbt
```

Format all project files as follows:

```bash
sbt scalafmtAll
```


### License

This code is open source software licensed under the [Apache 2.0 License]("http://www.apache.org/licenses/LICENSE-2.0.html").
