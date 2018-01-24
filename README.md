# LambdaWerk backend developer test

## Setup

### Tests

    lein test

### Development

Please make sure the [test data](http://exchange.lambdawerk.com.s3-website.eu-central-1.amazonaws.com/lambdawerk-backend-test.tar) is downloaded and extracted to `local-setup/`.

    # Starts the database
    # Also runs a prometheus setup to get some basic monitoring for the db
    ./local-setup.sh


Wait until `PostgreSQL init process complete; ready for start up.` appears in the logs.

    # Watch the logs
    docker-compose logs -f

To start the update process, you can either open `lambdawerk-backend-test.core` in the REPL and play around with the comment and settings.

    lein repl

Or build an uberjar and run the update process with the default settings.

    lein uberjar

To shutdown the stack.

    docker-compose kill && docker-compose rm -f

## Open questions and tasks for production use

 - Are there other clients running writes/reads on the database in production?
 - Is the production database already under load?
 - Are there requirements how the load has to be distributed over time?
 - Logging should be set up properly
 - DB params shouldn't be hardcoded in `lambdawerk-backend-test.core`
 - Update process parameters shouldn't be hardcoded in `lambdawerk-backend-test.core`
 - An automated end to end test is missing which checks some samples for correctness after running the update process

## Architecture decisions

### Use clojure's lazy sequences

To reduce the memory usage it should be avoided to load the whole updates file into memory.
The decision is to use clojure's lazy sequences to make sure that only parts of the updates are loaded into memory and can be freed again after processing.

### Minimize runtime of the update process

The update process runtime should be minimized and scalable.

To achieve this the db writes are batched and executed in parallel.
Both the batch size and the the number of executors can be configured.
A db connection pool is used to allow multiple database connections which can be adjusted as well.

The actual merge is done in one SQL statement which the author saw as the most efficient and simplest way to to the merge.

### Additional fields should be easy to add

A production system would have some additional fields which would have to be updated as well.

To make the addition of new fields easy the only place where they would have to be added are the `::person` spec in `lambdawerk-backend-test.service`.
If necessary some cleaning transformations could be added in `clean-person`.

## The task

There is a PostgreSQL table of persons (person), uniquely identified
by their first name (fname), last name (lname) and date of birth
(dob).  Every person has a telephone number (phone).

This table needs to be updated from an XML file containing elements of
the form

```
<member>
 <first-name>JOHN</first-name>
 <last-name>DOE</last-name>
 <date-of-birth>2002-02-01</date-of-birth>
 <phone>9548938821</phone>
</member>
```

If the phone number is already correct, nothing should be changed in
the database.  If a person record does not exist, it needs to be
created.

The person database table contains 10 million rows.

The update file contains 1.5 million entries.

## Objective

 - Write clean code that performs the operation correctly
 - Provide basic loading statistics at the end of the operation
 - Use proper mechanisms to process the input file
 - Find ways to minimize the overall run time of the merge process
 - Reason about performance and memory usage

The number of records in the sample database and the input file are
meant to reflect the number of records in a production system.  A
production system would have more individual fields per person,
consider that when choosing an implementation strategy.

## Files

The file person.sql.gz contains a database dump of the person table
which can be imported into PostgreSQL.

The file update-file.xml.gz contains the XML input file to be merged
into the database.
