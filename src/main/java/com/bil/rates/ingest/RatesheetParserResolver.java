package com.bil.rates.ingest;

import org.apache.poi.ss.usermodel.Workbook;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class RatesheetParserResolver {

    private final List<RatesheetParser> parsers;

    public RatesheetParserResolver(List<RatesheetParser> parsers) {
        this.parsers = parsers;
    }

    public RatesheetParser resolve(String fileName, Workbook workbook) {
        return parsers.stream()
                .filter(p -> p.supports(fileName, workbook))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "No RatesheetParser supports file: " + fileName +
                        ". Registered: " + parsers.stream().map(RatesheetParser::name).toList()));
    }
}

