package org.abc.perftest;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.abc.perftest.model.auth.TokenResponse;
import org.abc.perftest.model.location.Location;
import org.abc.perftest.model.location.LocationResponse;
import org.abc.perftest.model.member.Member;
import org.abc.perftest.model.member.MemberResponse;

import java.io.File;
import java.net.URI;
import java.net.http.*;
import java.text.DecimalFormat;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

public class Main {
    public static void main(String[] args) throws Exception {
        Options options = Options.parseArgs(args);
        Instant start = Instant.now();
        try
        {
            // 1. Load all locations
            System.out.println("Loading all locations");
            initializeLocations(options);
            System.out.println("Done loading all locations");

            // 2. Setup dates for Subscriptions
            LocalDateTime ldt = LocalDateTime.now();
            // YYYY-MM-DD
            String startDate = DateTimeFormatter.ofPattern("yyyy-MM-dd", Locale.ENGLISH).format(ldt);
            // Add one year
            ldt.plusYears(1);
            String endDate = DateTimeFormatter.ofPattern("yyyy-MM-dd", Locale.ENGLISH).format(ldt);

            // 3. Output the CSV file
            File outputFile = new File(options.outputDirectory, "mass-modification.csv");
            outputMembersAsCsv(options, outputFile.getCanonicalPath(), startDate, endDate);
        }
        catch (Exception exception) {
            System.err.printf("Unhandled exception = %s%n", exception);
            System.exit(2);
        }
        finally {
            // 4. Output the duration taken to write the mass modification files
            Instant end = Instant.now();
            Duration interval = Duration.between(start, end);
            System.out.printf("Duration taken to output mass modification file = %s%n", interval);
        }

        System.exit(0);
    }

    private static HttpClient s_httpClient;

    public static HttpClient getHttpClient() throws Exception {
        if (s_httpClient == null) s_httpClient = HttpClient.newHttpClient();
        return s_httpClient;
    }

    public static String getToken(Options options) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(String.format("https://%s/api/token?client_id=AUTOMATED_TESTING&grant_type=client_credentials&Content-Type=application/json", options.server)))
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .header("Authorization", String.format("Basic %s", options.basicAuthBase64String))
                .POST(HttpRequest.BodyPublishers.ofString(""))
                .build();

        CompletableFuture<HttpResponse<String>> response =
                getHttpClient().sendAsync(request, HttpResponse.BodyHandlers.ofString());
        String result = response.thenApply(HttpResponse::body).get(50, TimeUnit.SECONDS);
        ObjectMapper om = new ObjectMapper();
        TokenResponse root = om.readValue(result, TokenResponse.class);
        return root.access_token;
    }

    public static MemberResponse getMembers(
            Options options, String token, int page, int size) throws Exception {
        Instant start = Instant.now();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(String.format("https://%s/api/member?page=%d&size=%d", options.server, page, size)))
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .header("Authorization", "Bearer " + token)
                .GET()
                .build();

        CompletableFuture<HttpResponse<String>> response =
                getHttpClient().sendAsync(request, HttpResponse.BodyHandlers.ofString());
        String result = response.thenApply(HttpResponse::body).get(50, TimeUnit.SECONDS);
        ObjectMapper om = new ObjectMapper();
        MemberResponse memberResponse = om.readValue(result, MemberResponse.class);
        Instant end = Instant.now();
        Duration interval = Duration.between(start, end);
        System.out.printf("Page: %d, Size: %d, Number of returned members = %d (duration = %s)%n",
                page, size, memberResponse.numberOfElements, interval);
        return memberResponse;
    }

    public static LocationResponse getLocations(Options options, String token, int page, int size) throws Exception {
        Instant start = Instant.now();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(String.format("https://%s/api/location?page=%d&size=%d", options.server, page, size)))
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .header("Authorization", "Bearer " + token)
                .GET()
                .build();

        CompletableFuture<HttpResponse<String>> response =
                getHttpClient().sendAsync(request, HttpResponse.BodyHandlers.ofString());
        String result = response.thenApply(HttpResponse::body).get(60, TimeUnit.SECONDS);
        ObjectMapper om = new ObjectMapper();
        LocationResponse locationResponse = om.readValue(result, LocationResponse.class);
        Instant end = Instant.now();
        Duration interval = Duration.between(start, end);
        System.out.printf("Page: %d, Size: %d, Number of returned locations = %d (duration = %s)%n",
                page, size, locationResponse.numberOfElements, interval);
        return locationResponse;
    }

    public static List<Location> getAllLocations(Options options) throws Exception {
        List<Location> locations = new ArrayList<Location>();
        Instant start = Instant.now();
        String token = getToken(options);
        int page = 0;
        final int size = 2000;
        LocationResponse l = getLocations(options, token, page, size);
        while (page < l.totalPages) {
            locations.addAll(l.content);
            page++;
            Instant end = Instant.now();
            Duration interval = Duration.between(start, end);
            if (interval.getSeconds() > 13 * 60) {
                System.out.println("**Regenerating token ...**");
                start = Instant.now();
                token = getToken(options);
            }
            l = getLocations(options, token, page, size);
        }

        return locations;
    }

    public static void outputMembersAsCsv(
            Options options, String jsonFilePath, String startDate, String endDate) throws Exception {
        MassModificationCsv csv = new MassModificationCsv(jsonFilePath);
        try {
            csv.open();
            Instant start = Instant.now();
            String token = getToken(options);
            int page = 0;
            final int size = 2000;
            long totalMembers = 0;
            MemberResponse m = getMembers(options, token, page, size);
            while (++page < m.totalPages) {
                totalMembers += m.content.size();
                csv.writeRecords(formatCsvRecords(m.content, startDate, endDate));
                if (totalMembers >= 2000) break;
                Instant end = Instant.now();
                Duration interval = Duration.between(start, end);
                if (interval.getSeconds() > 13*60) {
                    System.out.println("**Regenerating token ...**");
                    start = Instant.now();
                    token = getToken(options);
                }
                m = getMembers(options, token, page, size);
            }
        }
        finally {
            csv.close();
        }
    }

    private static List<String[]> formatCsvRecords(ArrayList<Member> members, String startDate, String endDate) {
        final String empty = "";
        List<String[]> records = new ArrayList<String[]>();
        for (Member member : members) {
            records.add(
              new String[] {
                  // Club number Club, the field should contain from 1 to 6 chars of Club Number
                  clubFromLocationId(member.locationId),
                  // Member #. 1-9 char RCM-10818
                  member.number,
                  // # of Payments (optional)
                  empty,
                  // Recurring Schedule Flag, if INSTALLMENT, then true
                  "false",
                  // Frequency
                  "ANNUALLY", // WEEKLY, EVERY_OTHER_WEEK, MONTHLY, ANNUALLY, QUARTERLY, SEMI_ANNUALLY
                  // Profit Center = subscriptionItems[].catalogItemName
                  "DUES",
                  // Price (Draft Amount from file) = subscriptionItems[].price
                  NextValue(100, 1000), // Random value between 100 and 1000
                  // Start Date (Begin Date from file)
                  startDate,
                  // First Due Date (Day of Month from file)
                  startDate,
                  // expirationDate (optional)
                  endDate,
                  // type
                  "NEW",
                  // subType (Values to be processed are INSTALLMENT, OPEN_END)
                  "INSTALLMENT",
                  // signDate (optional)
                  empty,
                  // downPayment (optional)
                  empty,
                  // employeeID (optional)
                  empty,
                  // renewalOptions (optional)
                  empty,
                  // metadata (optional)
                  empty,
                  // clientEnum (DATA_TRAK or null)
                  "DATA_TRAK" });
        }

        return records;
    }

    private static ConcurrentHashMap<String, String> clubs = new ConcurrentHashMap<String, String>();

    private static void initializeLocations(Options options) throws Exception {
        List<Location> allLocations = getAllLocations(options);
        allLocations.parallelStream().forEach(location -> {
            clubs.put(location.id, location.number);
        });
    }

    private static String clubFromLocationId(String locationId) {
        String club = clubs.get(locationId);
        if (club == null) throw new NoSuchElementException("Location with locationId = " + locationId + " not found");
        return club;
    }

    private static final DecimalFormat df = new DecimalFormat("0.00");

    private static String NextValue(int lower, int upper) {
        return df.format((Math.random() * (upper - lower)) + lower);
    }
}
