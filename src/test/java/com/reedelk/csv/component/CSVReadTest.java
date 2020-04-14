package com.reedelk.csv.component;

import com.reedelk.runtime.api.converter.ConverterService;
import com.reedelk.runtime.api.flow.FlowContext;
import com.reedelk.runtime.api.message.Message;
import com.reedelk.runtime.api.message.MessageBuilder;
import com.reedelk.runtime.api.message.content.DataRow;
import com.reedelk.runtime.api.message.content.MimeType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doAnswer;

@ExtendWith(MockitoExtension.class)
class CSVReadTest {

    private static final List<String> HEADERS =
            asList("Player Name", "Position", "Nicknames", "Years Active");

    @Mock
    private FlowContext context;
    @Mock
    private ConverterService converter;

    private CSVRead csvRead = new CSVRead();

    @BeforeEach
    void setUp() {
        csvRead = new CSVRead();
        csvRead.converter = converter;

        doAnswer(invocation -> invocation.getArgument(0))
                .when(converter)
                .convert(any(Object.class), any(Class.class));
    }

    @Test
    void shouldCorrectlyReadCSVFromPayloadWhenFirstRecordIsHeader() {
        // Given
        csvRead.setFirstRecordAsHeader(true);
        csvRead.initialize();

        String csvContent = CSVs.SAMPLE_WITH_HEADER.string();
        Message input = MessageBuilder.get()
                .withString(csvContent, MimeType.TEXT_PLAIN)
                .build();

        // When
        Message actual = csvRead.apply(context, input);

        // Then
        List<DataRow<String>> records = actual.payload();

        assertThat(records).hasSize(3);

        assertExistRecord(records, HEADERS,
                asList("Skippy Peterson","First Base","\"Blue Dog\", \"The Magician\"","1908-1913"));
        assertExistRecord(records, HEADERS,
                asList("Bud Grimsby","Center Field","\"The Reaper\", \"Longneck\"","1910-1917"));
        assertExistRecord(records, HEADERS,
                asList("Vic Crumb","Shortstop","\"Fat Vic\", \"Icy Hot\"","1911-1912"));
    }

    @Test
    void shouldCorrectlyReadCSVFromPayloadWhenFirstRecordIsNotHeader() {
        // Given
        csvRead.initialize();

        String csvContent = CSVs.SAMPLE_WITHOUT_HEADER.string();
        Message input = MessageBuilder.get()
                .withString(csvContent, MimeType.TEXT_PLAIN)
                .build();

        // When
        Message actual = csvRead.apply(context, input);

        // Then
        List<DataRow<String>> records = actual.payload();

        assertThat(records).hasSize(3);

        assertExistRecord(records,
                asList("Skippy Peterson","First Base","\"Blue Dog\", \"The Magician\"","1908-1913"));
        assertExistRecord(records,
                asList("Bud Grimsby","Center Field","\"The Reaper\", \"Longneck\"","1910-1917"));
        assertExistRecord(records,
                asList("Vic Crumb","Shortstop","\"Fat Vic\", \"Icy Hot\"","1911-1912"));
    }

    @Test
    void shouldCorrectlyIgnoreEmptyLines() {
        // Given
        csvRead.initialize();

        String csvContent = CSVs.SAMPLE_WITH_EMPTY_LINES.string();
        Message input = MessageBuilder.get()
                .withString(csvContent, MimeType.TEXT_PLAIN)
                .build();

        // When
        Message actual = csvRead.apply(context, input);

        // Then
        List<DataRow<String>> records = actual.payload();

        assertThat(records).hasSize(2);

        assertExistRecord(records,
                asList("Skippy Peterson","First Base","\"Blue Dog\", \"The Magician\"","1908-1913"));
        assertExistRecord(records,
                asList("Vic Crumb","Shortstop","\"Fat Vic\", \"Icy Hot\"","1911-1912"));
    }

    @Test
    void shouldCorrectlyTrimContent() {
        // Given
        csvRead.setTrim(true);
        csvRead.initialize();

        String csvContent = CSVs.SAMPLE_WITH_NOT_TRIMMED_CONTENT.string();
        Message input = MessageBuilder.get()
                .withString(csvContent, MimeType.TEXT_PLAIN)
                .build();

        // When
        Message actual = csvRead.apply(context, input);

        // Then
        List<DataRow<String>> records = actual.payload();

        assertThat(records).hasSize(2);

        assertExistRecord(records,
                asList("Skippy Peterson","First Base","\"Blue Dog\", \"The Magician\"","1908-1913"));
        assertExistRecord(records,
                asList("Vic Crumb","Shortstop","\"Fat Vic\", \"Icy Hot\"","1911-1912"));
    }

    @Test
    void shouldReturnEmptyWhenCsvEmpty() {
        // Given
        csvRead.initialize();

        String csvContent = CSVs.SAMPLE_WITH_NO_RECORDS.string();
        Message input = MessageBuilder.get()
                .withString(csvContent, MimeType.TEXT_PLAIN)
                .build();

        // When
        Message actual = csvRead.apply(context, input);

        // Then
        List<DataRow<String>> records = actual.payload();

        assertThat(records).isEmpty();
    }

    @Test
    void shouldReturnEmptyWhenCsvEmptyWithHeaders() {
        // Given
        csvRead.setFirstRecordAsHeader(true);
        csvRead.initialize();

        String csvContent = CSVs.SAMPLE_WITH_NO_RECORDS_AND_HEADERS.string();
        Message input = MessageBuilder.get()
                .withString(csvContent, MimeType.TEXT_PLAIN)
                .build();

        // When
        Message actual = csvRead.apply(context, input);

        // Then
        List<DataRow<String>> records = actual.payload();

        assertThat(records).isEmpty();
    }

    @Test
    void shouldReturnCorrectRecordsWhenCustomDelimiter() {
        // Given
        csvRead.setFirstRecordAsHeader(true);
        csvRead.setDelimiter(":");
        csvRead.initialize();

        String csvContent = CSVs.SAMPLE_WITH_CUSTOM_DELIMITER.string();
        Message input = MessageBuilder.get()
                .withString(csvContent, MimeType.TEXT_PLAIN)
                .build();

        // When
        Message actual = csvRead.apply(context, input);

        // Then
        List<DataRow<String>> records = actual.payload();

        assertThat(records).hasSize(3);

        assertExistRecord(records,
                asList("Skippy Peterson","First Base","\"Blue Dog\", \"The Magician\"","1908-1913"));
        assertExistRecord(records,
                asList("Bud Grimsby","Center Field","\"The Reaper\", \"Longneck\"","1910-1917"));
        assertExistRecord(records,
                asList("Vic Crumb","Shortstop","\"Fat Vic\", \"Icy Hot\"","1911-1912"));
    }

    private void assertExistRecord(List<DataRow<String>> records, List<String> headers, List<String> expected) {
        boolean found = records.stream().anyMatch(actual -> {
            List<String> strings = actual.columnNames();
            return headers.containsAll(strings) && areEquals(expected, actual);
        });
        assertThat(found).isTrue();
    }

    private void assertExistRecord(List<DataRow<String>> records, List<String> expected) {
        boolean found = records.stream().anyMatch(actual -> areEquals(expected, actual));
        assertThat(found).isTrue();
    }

    private boolean areEquals(List<String> expected, DataRow<String> actual) {
        List<String> row = actual.values();
        for (int i = 0; i < row.size(); i++) {
            String actualValue = row.get(i);
            String expectedValue = expected.get(i);
            if (!actualValue.equals(expectedValue)) return false;
        }
        return true;
    }
}