## Introduction ##
This repo contains the [Gatling load testing scenarios](https://gatling.io/docs/gatling/tutorials/quickstart/) for GOV.UK Forms.
Gatling is a Java based load testing tool.

The following includes basic instruction on how to install and run the tests. For more detailed documentation on Gatling refer to
its [documentation](https://gatling.io/docs/gatling/tutorials/advanced/).

## Installation ##

Install Java:
`brew install openjdk`

Install Maven (Java build, package and task manager): `brew install maven`

Install the Java packages: `mvn clean install`

## Scenarios ##

### Form Submission ###
Intended to load test the form submission process. It should be able to complete any form comprised of the question types
configured in the FormSubmission class. The number of concurrent form submissions is gradually ramped up from 0 to the maximum
configured value (see below) and then held before ramping back down.

The following environment variables are available:

`FORM_ID` (default 71) the form id to complete.

`FORMS_RUNNER_BASE_URL` (default https://submit.dev.forms.service.gov.uk) the base URL for forms-runner.

`RAMP_DURATION_SECONDS` (default 1 second) how long in seconds the test ramps up from 0 to the maximum users and back down again.

`MAX_CONCURRENT_USERS` (default 1 user) how many users will be filling in a form at the same time.

`MAX_CONCURRENT_DURATION_SECONDS` (default 10 seconds) how long the test will run with the max concurrent users before ramping back down.

## Running the tests ##

### Execution Command ###
`mvn gatling:test` from the command line.

or

Execute the `Engine#main` function in Intellij.

### Results ###
The final output of the execution includes a link to an HTML based output.

