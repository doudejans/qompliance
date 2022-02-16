# Qompliance

This repository contains the demo/reference source code for Qompliance, a system designed for my Master's Thesis research towards data-centric compliance for distributed data movements.
Qompliance checks user-defined SQL-based data transformations for compliance with policies.
These policies can be written using a policy model, and a corresponding YAML implementation which is specified in the [docs](docs/language.md).

## Build

This project is written in [Kotlin](https://kotlinlang.org) and has been written and tested using JDK version 17.
The project uses [Gradle](https://gradle.org) and this repository contains the Gradle wrapper.
Important dependencies (managed by Gradle) include [Spring](https://spring.io) and [Apache Calcite](https://calcite.apache.org).

To build the project, make sure that you have a JDK distribution of version 8 or higher installed.
Then, on macOS/Linux simply run:

```sh
./gradlew build
```

On Windows, use:

```pwsh
gradlew.bat build
```

This will build two different modules as two standalone jars: the `data-manager` and the `compliance-checker`.
The former is responsible for managing and gathering the (meta)data in the system such as policies, schema information for the governed data and additional metadata.
The latter is responsible for the actual compliance checking, by validating input using the (meta)data from the `data-manager`.
Both modules expose a REST API for interacting with the subsystems.

## Run

### Dependencies & Requirements

This project needs a PostgreSQL database for storing and managing the (meta)data in the system.
For a new development environment, please make sure that you have a PostgreSQL database running.
For example, use the following Docker command which starts a PostgreSQL database called `compliance` (with a weak password!) with a mounted volume to persist the data:

```sh
docker run -d -v ~/docker_volumes/qompliance/:/var/lib/postgresql/data -e POSTGRES_USER=qompliance -e POSTGRES_PASSWORD=password -p 5432:5432 postgres
```

Make sure to set the configuration properties for the database in the `data-manager`.
For more information on configuration properties, refer to [this section](#configure).

### Running the system

To run the standalone modules, simply execute the jars:

```sh
java -jar data-manager/build/libs/data-manager-0.1-SNAPSHOT.jar
java -jar compliance-checker/build/libs/compliance-checker-0.1-SNAPSHOT.jar
```

### Submitting a validation task

If everything goes right, we can now submit a task for validation.
With the default configuration, the `compliance-checker` API will now be exposed at `http://localhost:8081`.
To submit a task for validation, send a POST request to `http://localhost:8081/validation` with a JSON body containing the SQL and any additional attributes.
For example, try the following with curl (or use your preferred API tool):

```sh
curl --request POST \
  --url http://localhost:8081/validation \
  --header 'Content-Type: application/json' \
  --data '{
	"sql": "SELECT * FROM \"DB0\".\"school_finance\"",
	"attributes": {
		"purpose": ["Advertising"],
		"role": ["MarketingDept"]
	}
}'
```

### Adding a policy

For managing policies and metadata, we have to use the `data-manager`.
To submit a new policy in the YAML format, POST the YAML body to `http://localhost:8080/yaml/policies`:

```sh
curl --request POST \
  --url http://localhost:8080/yaml/policies \
  --header 'Content-Type: text/yaml' \
  --data 'name: Allow Marketing on PII in Europe
context:
  tag:
    - PII
  role:
    - MarketingDept
  purpose:
    - Marketing
  data-location:
    - Europe
decision: allow
require:
  data-location:
    - Europe'
```

## Configure

The application configuration uses properties and can be managed and modified as described in the [Spring Docs](https://docs.spring.io/spring-boot/docs/current/reference/html/features.html#features.external-config).
The default `application.properties` for the [`compliance-checker`](compliance-checker/src/main/resources/application.properties) and [`data-manager`](data-manager/src/main/resources/application.properties) are sensible defaults for a development environment.

### Generating data

The `data-manager` has a built-in data generator for loading some semi-random data.
To automatically create the database schema and to generate data in the database, use the following properties for the `data-manager`:

```properties
spring.jpa.hibernate.ddl-auto=create
datamanager.generatedata=true
```

Set the above properties to `none` and `false` respectively to prevent data from being overwritten.

### Default decision

A notable feature of the compliance checker is that the default decision is configurable.
The default decision is the decision that gets taken if no policy makes a decision or if an unresolvable conflict occurred.
For example, set the default decision to `deny` to ensure that all requests get denied unless a policy allows:

```properties
decision.default=deny
```

The alternative is `allow`.

## Experiment

The set of experiments used in the thesis can be automatically run using the [Python script](scripts/eval.py).
Make sure you have all of the requirements installed (`pip install -r scripts/requirements.txt`).
Simply run the following from the root project directory:

```sh
python scripts/eval.py
```

This will build the project and start and manage the jars for the modules.
It will start the `data-manager` with the right parameters for the different experiments.
Results and logs will be added to a `tmp/` folder in the project root.

For visualizing these results, the [Notebook](scripts/visualize.ipynb) contains the Python code for all plots that are used in the thesis.

## Docs

### Policy model & YAML language implementation

The policy model, its YAML implementation and all supported attributes are documented in [docs/language.md](docs/language.md).

### API Reference

Additional information about the endpoints are automatically generated when running the applications.
These can be found at [http://localhost:8080/docs.html](http://localhost:8080/docs.html) for the `data-manager` and [http://localhost:8081/docs.html](http://localhost:8081/docs.html) for the `compliance-checker`.
