package org.abc.perftest;

import com.opencsv.CSVWriter;
import org.abc.perftest.model.member.Member;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class MassModificationCsv {
    String filePath;
    CSVWriter csvWriter;

    public MassModificationCsv(String filePath) throws IOException {
        this.filePath = filePath;
        BufferedWriter writer = new BufferedWriter(new FileWriter(this.filePath));
        csvWriter = new CSVWriter(writer);
    }

    public void open() {
        // String[] headers = new String[] { "OrganizationId", "Id", "LocationId", "AgreementNumber" };
        // csvWriter.writeNext(headers);
    }

    public void writeRecords(List<String[]> records) {
        csvWriter.writeAll(records);
    }

    public void close() throws IOException {
        csvWriter.close();
    }
}