import com.google.gson.Gson;

import static spark.Spark.*;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

public class ComparisonServer {

  private static Map<String, Double> countryToSalary;
  private static Map<String, Double> stateToCost;
  private static Map<String, List<Double>> stateCostDetails;

  private static Map<String, Double> loadCSV(String filePath) {
    Map<String, Double> data = new HashMap<>();
    try {
      List<String> lines = Files.readAllLines(Paths.get(filePath));
      boolean isFirstLine = true;

      for (String line : lines) {
        if(isFirstLine) {
          isFirstLine = false;
          continue;
        }

        String[] parts = line.split(","); // Ensure CSV is comma-separated
        if (parts.length == 2) {
          String key = parts[0].trim().replace("\"", ""); // Remove double quotes
          String valueStr = parts[1].trim().replace("\"", ""); // Remove double quotes
          double value = Double.parseDouble(valueStr); // Convert to double
          data.put(key, value);
        }
      }
    } catch (Exception e) {
      System.err.println("Error loading CSV: " + filePath);
      e.printStackTrace();
    }
    return data;
  }

  public static Map<String, List<Double>> loadDetailedCSV(String filePath) {
    Map<String, List<List<Double>>> tempStateData = new HashMap<>();

    try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
      String line;
      boolean isFirstLine = true;

      while ((line = br.readLine()) != null) {
        if (isFirstLine) {  // Skip the header
          isFirstLine = false;
          continue;
        }

        // Use regex-based split to correctly handle quotes and commas
        String[] parts = line.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)", -1);

        if (parts.length < 13) { // Ensure we have enough columns
          System.err.println("Skipping row due to insufficient data: " + line);
          continue;
        }

        String state = parts[1].trim(); // Extract the state abbreviation (column index 1)
        List<Double> selectedValues = new ArrayList<>();

        try {
          for (int i = 6; i <= 12; i++) { // Extract correct cost columns
            selectedValues.add(Double.parseDouble(parts[i].trim()));
          }
        } catch (NumberFormatException e) {
          System.err.println("Skipping row due to invalid number format: " + line);
          continue;
        }

        tempStateData.putIfAbsent(state, new ArrayList<>());
        tempStateData.get(state).add(selectedValues);
      }
    } catch (IOException e) {
      System.err.println("Error reading CSV file: " + e.getMessage());
    }

    // Compute the mean for each state
    Map<String, List<Double>> stateCostDetails = new HashMap<>();

    for (Map.Entry<String, List<List<Double>>> entry : tempStateData.entrySet()) {
      String state = entry.getKey();
      List<List<Double>> allEntries = entry.getValue();

      int numEntries = allEntries.size();
      int numColumns = allEntries.get(0).size();

      List<Double> meanValues = new ArrayList<>(Collections.nCopies(numColumns, 0.0));

      for (List<Double> values : allEntries) {
        for (int i = 0; i < numColumns; i++) {
          meanValues.set(i, meanValues.get(i) + values.get(i));
        }
      }

      for (int i = 0; i < numColumns; i++) {
        meanValues.set(i, meanValues.get(i) / numEntries);
      }

      stateCostDetails.put(state, meanValues);
    }

    return stateCostDetails;
  }


  public static void main(String[] args) {

    staticFiles.location("/static");

    // Load CSV data into memory
    countryToSalary = loadCSV("C:/Users/96248/IdeaProjects/MadData/src/clean_salary_data.csv");
    stateToCost = loadCSV("C:/Users/96248/IdeaProjects/MadData/src/clean_cost_of_living_in_the_us_updated.csv");
    stateCostDetails = loadDetailedCSV("C:/Users/96248/IdeaProjects/MadData/src/cost_of_living_in_the_us_updated.csv");

    // Start a simple web server
    port(8080);

    System.out.println("Static files are being served from: " + new File("src/main/resources").getAbsolutePath());

    get("/WebPage.html", (req, res) -> {
      res.redirect("/public/WebPage.html");
      return null;
    });


    get("/", (req, res) ->{
      res.redirect("WebPage.html");
      return null;
    });

    // Debugging route to list static files
    get("/list-files", (req, res) -> {
      File folder = new File("src/main/resources/static");
      File[] listOfFiles = folder.listFiles();

      if (listOfFiles == null) return "No files found!";

      StringBuilder sb = new StringBuilder("Files in /public:<br>");
      for (File file : listOfFiles) {
        sb.append(file.getName()).append("<br>");
      }
      return sb.toString();
    });

    // Handle GET request to serve WebPage.html
    get("/compare", (req, res) -> {
      String state = req.queryParams("state");
      String nationality = req.queryParams("nationality");

      if (state == null || nationality == null) {
        return "<html><body><h2>Error: Missing parameters!</h2><a href='/'>Go back</a></body></html>";
      }

      Double cost = stateToCost.get(state);
      Double salary = countryToSalary.get(nationality);
      List<Double> costDetails = stateCostDetails.get(state);

      if (cost == null || salary == null) {
        return "<html><body><h2>Error: Data not found!</h2>" +
                "<p><b>State:</b> " + state + " (Cost: " + (cost != null ? cost : "Not Found") + ")</p>" +
                "<p><b>Nationality:</b> " + nationality + " (Salary: " + (salary != null ? salary : "Not Found") + ")</p>" +
                "<a href='/'>Go back</a></body></html>";
      }

      Map<String, Object> response = new HashMap<>();
      response.put("cost", cost);
      response.put("costBreakdown", costDetails);

      double ratio = salary / cost;
      String resultMessage = (ratio >= 1.0)
              ? String.format("<h2>✅ Your salary (%.2f) covers the cost of living (%.2f) in %s! (Ratio: %.2f)</h2>", salary, cost, state, ratio)
              : String.format("<h2>❌ Your salary (%.2f) is not enough to cover cost (%.2f). You need %.2f more! (Ratio: %.2f)</h2>", salary, cost, (cost - salary), ratio);

      return "<html><head><title>Comparison Result</title></head><body>" +
              "<h1>Comparison Results</h1>" +
              "<p><b>State:</b> " + state + "</p>" +
              "<p><b>Nationality:</b> " + nationality + "</p>" +
              "<p><b>Cost of Living:</b> $" + cost + "</p>" +
              "<p><b>Average Salary:</b> $" + salary + "</p>" +
              resultMessage +
              "<br><br><a href='/'>Go Back</a>" +
              "</body></html>";
    });

    post("/compare", (req, res) -> {
      res.type("application/json");

      Map<String, Object> requestData = new Gson().fromJson(req.body(), Map.class);
      String state = (String)requestData.get("state");
      String nationality = (String)requestData.get("nationality");

      if (state == null || nationality == null) {
        return new Gson().toJson(Map.of("error", "Missing parameters!"));
      }

      state = state.trim().toUpperCase();
      System.out.println("Processed state: " + state);

      Double cost = stateToCost.get(state);
      Double salary = countryToSalary.get(nationality);

      if (cost == null || salary == null) {
        System.out.println(cost + "?????" + salary);
        return new Gson().toJson(Map.of("error", "Data not found for the selected state or nationality!"));
      }

      double ratio = salary / cost;

      Map<String, Object> response = new HashMap<>();
      response.put("state", state);
      response.put("nationality", nationality);
      response.put("cost", cost);
      response.put("salary", salary);
      response.put("costBreakdown", stateCostDetails.get(state));
      response.put("ratio", ratio);

      return new Gson().toJson(response);
    });


    // Serve the HTML form at GET "/"
    get("/", (req, res) -> {
      return """
            <html>
              <head><title>Cost vs. Salary</title></head>
              <body>
                <h1>Compare Salary vs. Cost of Living</h1>
                <form action="/compare" method="POST">
                  <label for="country">Select Your Country:</label>
                  <select name="country">
                      <option>Afghanistan</option>
                      <option>Aland Islands</option>
                      <option>Albania</option>
                      <option>Algeria</option>
                      <option>American Samoa</option>
                      <option>Andorra</option>
                      <option>Angola</option>
                      <option>Antigua and Barbuda</option>
                      <option>Argentina</option>
                      <option>Armenia</option>
                      <option>Aruba</option>
                      <option>Australia</option>
                      <option>Austria</option>
                      <option>Azerbaijan</option>
                      <option>Bahamas</option>
                      <option>Bahrain</option>
                      <option>Bangladesh</option>
                      <option>Barbados</option>
                      <option>Belarus</option>
                      <option>Belgium</option>
                      <option>Belize</option>
                      <option>Benin</option>
                      <option>Bermuda</option>
                      <option>Bhutan</option>
                      <option>Bolivia</option>
                      <option>Bosnia and Herzegovina</option>
                      <option>Botswana</option>
                      <option>Brazil</option>
                      <option>British Indian Ocean Territory</option>
                      <option>Brunei</option>
                      <option>Bulgaria</option>
                      <option>Burkina Faso</option>
                      <option>Burundi</option>
                      <option>Cambodia</option>
                      <option>Cameroon</option>
                      <option>Canada</option>
                      <option>Cape Verde</option>
                      <option>Cayman Islands</option>
                      <option>Central African Republic</option>
                      <option>Chad</option>
                      <option>Chile</option>
                      <option>China</option>
                      <option>Colombia</option>
                      <option>Comoros</option>
                      <option>Congo</option>
                      <option>Congo Democratic Republic</option>
                      <option>Cook Islands</option>
                      <option>Costa Rica</option>
                      <option>Cote D'Ivoire</option>
                      <option>Croatia</option>
                      <option>Cuba</option>
                      <option>Cyprus</option>
                      <option>Czech Republic</option>
                      <option>Denmark</option>
                      <option>Djibouti</option>
                      <option>Dominica</option>
                      <option>Dominican Republic</option>
                      <option>East Timor</option>
                      <option>Ecuador</option>
                      <option>Egypt</option>
                      <option>El Salvador</option>
                      <option>Equatorial Guinea</option>
                      <option>Eritrea</option>
                      <option>Estonia</option>
                      <option>Ethiopia</option>
                      <option>Faroe Islands</option>
                      <option>Fiji</option>
                      <option>Finland</option>
                      <option>France</option>
                      <option>French Guiana</option>
                      <option>French Polynesia</option>
                      <option>Gabon</option>
                      <option>Gambia</option>
                      <option>Georgia</option>
                      <option>Germany</option>
                      <option>Ghana</option>
                      <option>Gibraltar</option>
                      <option>Greece</option>
                      <option>Greenland</option>
                      <option>Grenada</option>
                      <option>Guadeloupe</option>
                      <option>Guam</option>
                      <option>Guatemala</option>
                      <option>Guernsey</option>
                      <option>Guinea</option>
                      <option>Guinea-Bissau</option>
                      <option>Guyana</option>
                      <option>Haiti</option>
                      <option>Honduras</option>
                      <option>Hong Kong</option>
                      <option>Hungary</option>
                      <option>Iceland</option>
                      <option>India</option>
                      <option>Indonesia</option>
                      <option>Iran</option>
                      <option>Iraq</option>
                      <option>Ireland</option>
                      <option>Italy</option>
                      <option>Jamaica</option>
                      <option>Japan</option>
                      <option>Jersey</option>
                      <option>Jordan</option>
                      <option>Kazakhstan</option>
                      <option>Kenya</option>
                      <option>Kiribati</option>
                      <option>Korea (North)</option>
                      <option>Korea (South)</option>
                      <option>Kyrgyzstan</option>
                      <option>Laos</option>
                      <option>Latvia</option>
                      <option>Lebanon</option>
                      <option>Lesotho</option>
                      <option>Liberia</option>
                      <option>Libya</option>
                      <option>Liechtenstein</option>
                      <option>Lithuania</option>
                      <option>Luxembourg</option>
                      <option>Macao</option>
                      <option>Macedonia</option>
                      <option>Madagascar</option>
                      <option>Malawi</option>
                      <option>Malaysia</option>
                      <option>Maldives</option>
                      <option>Mali</option>
                      <option>Malta</option>
                      <option>Marshall Islands</option>
                      <option>Martinique</option>
                      <option>Mauritania</option>
                      <option>Mauritius</option>
                      <option>Mayotte</option>
                      <option>Mexico</option>
                      <option>Micronesia</option>
                      <option>Moldova</option>
                      <option>Monaco</option>
                      <option>Mongolia</option>
                      <option>Montenegro</option>
                      <option>Montserrat</option>
                      <option>Morocco</option>
                      <option>Mozambique</option>
                      <option>Myanmar</option>
                      <option>Namibia</option>
                      <option>Nepal</option>
                      <option>Netherlands</option>
                      <option>New Caledonia</option>
                      <option>New Zealand</option>
                      <option>Nicaragua</option>
                      <option>Niger</option>
                      <option>Nigeria</option>
                      <option>Northern Mariana Islands</option>
                      <option>Norway</option>
                      <option>Oman</option>
                      <option>Pakistan</option>
                      <option>Palau</option>
                      <option>Palestine</option>
                      <option>Panama</option>
                      <option>Papua New Guinea</option>
                      <option>Paraguay</option>
                      <option>Peru</option>
                      <option>Philippines</option>
                      <option>Poland</option>
                      <option>Portugal</option>
                      <option>Puerto Rico</option>
                      <option>Qatar</option>
                      <option>Romania</option>
                      <option>Russia</option>
                      <option>Rwanda</option>
                      <option>Saint Kitts and Nevis</option>
                      <option>Saint Lucia</option>
                      <option>Saint Vincent and the Grenadines</option>
                      <option>Samoa</option>
                      <option>San Marino</option>
                      <option>Sao Tome and Principe</option>
                      <option>Saudi Arabia</option>
                      <option>Senegal</option>
                      <option>Serbia</option>
                      <option>Seychelles</option>
                      <option>Sierra Leone</option>
                      <option>Singapore</option>
                      <option>Slovakia</option>
                      <option>Slovenia</option>
                      <option>Solomon Islands</option>
                      <option>Somalia</option>
                      <option>South Africa</option>
                      <option>Spain</option>
                      <option>Sri Lanka</option>
                      <option>Sudan</option>
                      <option>Suriname</option>
                      <option>Swaziland</option>
                      <option>Sweden</option>
                      <option>Switzerland</option>
                      <option>Syria</option>
                      <option>Taiwan</option>
                      <option>Tajikistan</option>
                      <option>Tanzania</option>
                      <option>Thailand</option>
                      <option>Togo</option>
                      <option>Tonga</option>
                      <option>Trinidad and Tobago</option>
                      <option>Tunisia</option>
                      <option>Turkey</option>
                      <option>Ukraine</option>
                      <option>United Arab Emirates</option>
                      <option>United Kingdom</option>
                      <option>United States</option>
                      <option>Uruguay</option>
                      <option>Uzbekistan</option>
                      <option>Vanuatu</option>
                      <option>Venezuela</option>
                      <option>Vietnam</option>
                      <option>Yemen</option>
                      <option>Zambia</option>
                      <option>Zimbabwe</option>
                  </select>

                  <br><br>
                  <label for="state">Select US State:</label>
                  <select name="state">
                      <option value="AL">Alabama</option>
                      <option value="AK">Alaska</option>
                      <option value="AZ">Arizona</option>
                      <option value="AR">Arkansas</option>
                      <option value="CA">California</option>
                      <option value="CO">Colorado</option>
                      <option value="CT">Connecticut</option>
                      <option value="DE">Delaware</option>
                      <option value="FL">Florida</option>
                      <option value="GA">Georgia</option>
                      <option value="HI">Hawaii</option>
                      <option value="ID">Idaho</option>
                      <option value="IL">Illinois</option>
                      <option value="IN">Indiana</option>
                      <option value="IA">Iowa</option>
                      <option value="KS">Kansas</option>
                      <option value="KY">Kentucky</option>
                      <option value="LA">Louisiana</option>
                      <option value="ME">Maine</option>
                      <option value="MD">Maryland</option>
                      <option value="MA">Massachusetts</option>
                      <option value="MI">Michigan</option>
                      <option value="MN">Minnesota</option>
                      <option value="MS">Mississippi</option>
                      <option value="MO">Missouri</option>
                      <option value="MT">Montana</option>
                      <option value="NE">Nebraska</option>
                      <option value="NV">Nevada</option>
                      <option value="NH">New Hampshire</option>
                      <option value="NJ">New Jersey</option>
                      <option value="NM">New Mexico</option>
                      <option value="NY">New York</option>
                      <option value="NC">North Carolina</option>
                      <option value="ND">North Dakota</option>
                      <option value="OH">Ohio</option>
                      <option value="OK">Oklahoma</option>
                      <option value="OR">Oregon</option>
                      <option value="PA">Pennsylvania</option>
                      <option value="RI">Rhode Island</option>
                      <option value="SC">South Carolina</option>
                      <option value="SD">South Dakota</option>
                      <option value="TN">Tennessee</option>
                      <option value="TX">Texas</option>
                      <option value="UT">Utah</option>
                      <option value="VT">Vermont</option>
                      <option value="VA">Virginia</option>
                      <option value="WA">Washington</option>
                      <option value="WV">West Virginia</option>
                      <option value="WI">Wisconsin</option>
                      <option value="WY">Wyoming</option>
                  </select>
                  <br><br>
                  <button type="submit">Compare</button>
                </form>
              </body>
            </html>
            """;
    });

    // Handle form submission and process comparison
    post("/compare", (req, res) -> {
      res.type("application/json");

      // Parse request JSON
      Map<String, String> requestData = new Gson().fromJson(req.body(), Map.class);
      String state = requestData.get("state");
      String nationality = requestData.get("nationality"); // Get nationality from request

      if (!stateToCost.containsKey(state)) {
        return new Gson().toJson(Map.of("error", "State data not found"));
      }
      if (!countryToSalary.containsKey(nationality)) {
        return new Gson().toJson(Map.of("error", "Salary data for nationality not found"));
      }

      double cost = stateToCost.get(state);  // Get cost of living in state
      double salary = countryToSalary.get(nationality);  // Get salary based on nationality

      System.out.println("Responding: Cost=" + cost + ", Salary=" + salary); // Debugging log

      // Construct response JSON
      Map<String, Object> response = new HashMap<>();
      response.put("state", state);
      response.put("nationality", nationality);
      response.put("cost", cost);
      response.put("salary", salary);
      response.put("ratio", salary / cost);

      return new Gson().toJson(response);
    });

  }
}
