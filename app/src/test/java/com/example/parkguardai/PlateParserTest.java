package com.example.parkguardai;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import com.example.parkguardai.ai.ocr.PlateParser;

import org.junit.Test;

public class PlateParserTest {

    @Test
    public void testMotorcycleTopCorrection_L8B1_becomes_48B1() {
        String top = PlateParser.correctMotorcycleTop("L8-B1");
        assertEquals("48-B1", top);

        String top2 = PlateParser.correctMotorcycleTop("48B1");
        assertEquals("48-B1", top2);

        String top3 = PlateParser.correctMotorcycleTop("59P1");
        assertEquals("59-P1", top3);
    }

    @Test
    public void testPlateDigitsCorrection_99999() {
        String digits = PlateParser.correctPlateDigits("999.99");
        assertEquals("99999", digits);

        String digits2 = PlateParser.correctPlateDigits("123.45");
        assertEquals("12345", digits2);

        String digits3 = PlateParser.correctPlateDigits("B888");
        assertEquals("8888", digits3);
    }
}
