package forms;

import io.gatling.javaapi.core.ChainBuilder;
import io.gatling.javaapi.core.CheckBuilder;
import io.gatling.javaapi.core.Choice;
import io.gatling.javaapi.core.FeederBuilder;
import io.gatling.javaapi.core.ScenarioBuilder;
import io.gatling.javaapi.core.Simulation;
import io.gatling.javaapi.http.HttpProtocolBuilder;

import java.time.Duration;
import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

import static io.gatling.javaapi.core.CoreDsl.constantConcurrentUsers;
import static io.gatling.javaapi.core.CoreDsl.css;
import static io.gatling.javaapi.core.CoreDsl.doSwitch;
import static io.gatling.javaapi.core.CoreDsl.exec;
import static io.gatling.javaapi.core.CoreDsl.feed;
import static io.gatling.javaapi.core.CoreDsl.listFeeder;
import static io.gatling.javaapi.core.CoreDsl.rampConcurrentUsers;
import static io.gatling.javaapi.core.CoreDsl.scenario;
import static io.gatling.javaapi.http.HttpDsl.http;

public class FormSubmission extends Simulation {

    private final int RAMP_DURATION_SECONDS = Integer.parseInt(System.getenv().getOrDefault("RAMP_DURATION_SECONDS", "1"));
    private final int MAX_CONCURRENT_USERS = Integer.parseInt(System.getenv().getOrDefault("MAX_CONCURRENT_USERS", "4"));
    private final int MAX_CONCURRENT_DURATION_SECONDS = Integer.parseInt(System.getenv().getOrDefault("MAX_CONCURRENT_DURATION_SECONDS", "10"));
    private final String FORM_IDS = System.getenv().getOrDefault("FORM_IDS", "8921,71,33");
    private final String FORMS_RUNNER_BASE_URL = System.getenv().getOrDefault("FORMS_RUNNER_BASE_URL", "https://submit.dev.forms.service.gov.uk");

    private final FeederBuilder<Object> formFeeder = listFeeder(
            Arrays.stream(FORM_IDS.split(","))
                    .map( id -> Map.<String, Object>of("form_id", id.trim()))
                    .collect(Collectors.toList())
    ).circular();

    private final HttpProtocolBuilder HTTP_PROTOCOL = http.
            baseUrl(FORMS_RUNNER_BASE_URL)
            .inferHtmlResources()
            .acceptHeader("text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,*/*;q=0.8")
            .acceptEncodingHeader("gzip, deflate, br")
            .acceptLanguageHeader("en-GB,en;q=0.5")
            .upgradeInsecureRequestsHeader("1")
            .userAgentHeader("Gatling load tests");

    private final Map<CharSequence, String> headers = Map.of(
            "Origin", FORMS_RUNNER_BASE_URL,
            "Sec-Fetch-Dest", "document",
            "Sec-Fetch-Mode", "navigate",
            "Sec-Fetch-Site", "same-origin",
            "Sec-Fetch-User", "?1"
    );

    private final ScenarioBuilder scenario = scenario("RecordedSimulation")
            .exec(getStartPage())
            .asLongAs(session -> session.getString("input_name") != "false" &&
                                 !session.getString("input_name").startsWith("email_confirmation_input"),
                      "question_number")
            .on(
                    exec(answerQuestion())
                            .pause(3, 10)
            )
            .exec(submitAnswers());

    {
        setUp(scenario.injectClosed(
                rampConcurrentUsers(0).to(MAX_CONCURRENT_USERS).during(RAMP_DURATION_SECONDS),
                constantConcurrentUsers(MAX_CONCURRENT_USERS).during(MAX_CONCURRENT_DURATION_SECONDS),
                rampConcurrentUsers(MAX_CONCURRENT_USERS).to(0).during(RAMP_DURATION_SECONDS)
        ).protocols(HTTP_PROTOCOL));
    }

    private ChainBuilder answerQuestion() {
        return doSwitch("#{input_name}").on(
                Choice.withKey("question[date(3i)]", exec(enterDate())),
                Choice.withKey("question[address1]", exec(enterAddress())),
                Choice.withKey("question[selection]", exec(enterAnswerOf("Blue"))),
                Choice.withKey("question[selection][]", exec(enterAnswerOf("Blue"))),
                Choice.withKey("question[number]", exec(enterAnswerOf("42"))),
                Choice.withKey("question[email]", exec(enterAnswerOf("test@test.test"))),
                Choice.withKey("question[full_name]", exec(enterAnswerOf("Gatling Tester"))),
                Choice.withKey("question[text]", exec(enterAnswerOf("Just some text"))),
                Choice.withKey("question[phone_number]", exec(enterAnswerOf("01234567890"))),
                Choice.withKey("question[national_insurance_number]", exec(enterAnswerOf("AA123456D")))
        );
    }

    private ChainBuilder enterAnswerOf(String answer) {
        return exec(http("form #{form_id} question #{question_number}")
                .post("#{action_path}")
                .requestTimeout(Duration.ofMinutes(1))
                .headers(headers)
                .formParam("authenticity_token", "#{auth_token}")
                .formParam("#{input_name}", answer)
                .check(getAuthToken())
                .check(getActionPath())
                .check(getInputName()));
    }

    private ChainBuilder enterAddress() {
        return exec(http("form #{form_id} question #{question_number}")
                .post("#{action_path}")
                .requestTimeout(Duration.ofMinutes(1))
                .headers(headers)
                .formParam("question[address1]", "first line")
                .formParam("question[address2]", "second line")
                .formParam("question[town_or_city]", "the town")
                .formParam("question[county]", "the county")
                .formParam("question[postcode]", "SW1A 2AA")
                .formParam("authenticity_token", "#{auth_token}")
                .check(getAuthToken())
                .check(getActionPath())
                .check(getInputName()));
    }

    private ChainBuilder enterDate() {
        return exec(http("form #{form_id} question #{question_number}")
                .post("#{action_path}")
                .requestTimeout(Duration.ofMinutes(1))
                .headers(headers)
                .formParam("authenticity_token", "#{auth_token}")
                .formParam("question[date(3i)]", "1")
                .formParam("question[date(2i)]", "1")
                .formParam("question[date(1i)]", "2023")
                .check(getAuthToken())
                .check(getActionPath())
                .check(getInputName()));
    }

    private ChainBuilder getStartPage() {
        return feed(formFeeder)
                .exec(http("form #{form_id} start page")
                .get("/form/#{form_id}")
                .requestTimeout(Duration.ofMinutes(1))
                .headers(Map.of(
                        "Sec-Fetch-Dest", "document",
                        "Sec-Fetch-Mode", "navigate",
                        "Sec-Fetch-Site", "none",
                        "Sec-Fetch-User", "?1"
                ))
                .check(getAuthToken())
                .check(getActionPath())
                .check(getInputName()));
    }

    private CheckBuilder getAuthToken() {
        return css("#main-content form input[name=authenticity_token][type=hidden]", "value")
                .saveAs("auth_token");
    }

    private CheckBuilder getActionPath() {
        return css("#main-content form", "action")
                .saveAs("action_path");
    }

    private CheckBuilder getInputName() {
        return css(".govuk-input, .govuk-checkboxes__input, .govuk-radios__input, .govuk-textarea", "name").withDefault("false")
                .saveAs("input_name");
    }

    private ChainBuilder submitAnswers() {
        return exec(http("form #{form_id} submit answers")
                .post("#{action_path}")
                .requestTimeout(Duration.ofMinutes(1))
                .headers(headers)
                .formParam("email_confirmation_input[send_confirmation]", "skip_confirmation")
                .formParam("authenticity_token", "#{auth_token}")
                .formParam("notify_reference", "b2654330-37bc-4fa7-9e16-6341d9798d0b"));
    }

    private ChainBuilder debug(String prefix) {
        return exec(session -> {
            System.out.println(prefix + ": " +session.getString("input_name"));
            return session;
        });
    }
}

