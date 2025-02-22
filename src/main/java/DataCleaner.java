import com.opencsv.CSVReader;
import com.opencsv.CSVWriter;
import com.opencsv.exceptions.CsvValidationException;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

import java.util.HashMap;
import java.util.Map;

public class DataCleaner {

  static class Accumulator {
    double sum = 0.0;
    int count = 0;

    void add(double cost) {
      sum += cost;
      count++;
    }

    double getAverage() {
      return (count == 0) ? 0.0 : (sum / count);
    }
  }

  public static void main(String[] args) {
    String inputfile1 = "C:/Users/96248/IdeaProjects/MadData/src/salary_data.csv";
    String outputfile1 = "C:/Users/96248/IdeaProjects/MadData/src/yearly_salary.csv";

    String inputfile2 = "C:/Users/96248/IdeaProjects/MadData/src/cost_of_living_in_the_us_updated.csv";
    String outputfile2 = "C:/Users/96248/IdeaProjects/MadData/src/clean_cost_of_living_in_the_us_updated.csv";

    try (
            CSVReader reader = new CSVReader(new FileReader(inputfile1));
            CSVWriter writer = new CSVWriter(new FileWriter(outputfile1))
    ) {
      String[] row;
      boolean isHeader = true;
      while ((row = reader.readNext()) != null) {

        // Print the raw row content before we do anything else
        System.out.println("Read Row (raw): " + String.join(",", row));

        if (isHeader) {
          // Rename the second column (index 1) to "YearlySalary"
          row[1] = "YearlySalary";
          writer.writeNext(row);

          // Also print a note about the header change
          System.out.println("Header updated: " + String.join(",", row));

          isHeader = false;
          continue;
        }

        // Parse monthly salary from the second column (index 1)
        double monthlySalary = Double.parseDouble(row[3]);
        double yearlySalary = monthlySalary * 12;

        // Convert back to string
        row[1] = String.valueOf(yearlySalary);

        // Write updated row to new CSV
        writer.writeNext(row);

        // Print info about the conversion
        System.out.println("Converted monthly " + monthlySalary + " to yearly " + yearlySalary);
      }

      System.out.println("Conversion complete! New CSV: " + outputfile1);

    } catch (IOException | CsvValidationException e) {
      e.printStackTrace();
    }

    // Map: stateName -> Accumulator
    Map<String, Accumulator> stateMap = new HashMap<>();

    try (CSVReader reader = new CSVReader(new FileReader(inputfile2))) {
      String[] row;
      boolean isHeader = true;

      while ((row = reader.readNext()) != null) {
        // Skip or process header
        if (isHeader) {
          isHeader = false;
          continue;
        }

        // row[0] = State, row[1] = RegionName, row[2] = Cost (adjust if your CSV layout differs)
        String state = row[1];  // e.g. "AL"
        double cost = Double.parseDouble(row[13]);

        // Accumulate the cost for this state
        stateMap.putIfAbsent(state, new Accumulator());
        stateMap.get(state).add(cost);
      }
    } catch (IOException | CsvValidationException e) {
      e.printStackTrace();
    }

    // Now compute average for each state and write to a new CSV
    try (CSVWriter writer = new CSVWriter(new FileWriter(outputfile2))) {
      // Write header
      writer.writeNext(new String[]{"State", "AverageCost"});

      for (Map.Entry<String, Accumulator> entry : stateMap.entrySet()) {
        String state = entry.getKey();
        double avgCost = entry.getValue().getAverage();

        // Convert to string row
        String[] outRow = { state, String.valueOf(avgCost) };
        writer.writeNext(outRow);
      }
    } catch (IOException e) {
      e.printStackTrace();
    }

    System.out.println("Aggregation complete! Check " + outputfile2 + " for results.");
  }

}

