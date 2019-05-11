package org.javarosa.core.model.utils.test;

import org.javarosa.core.services.locale.Localizer;
import org.javarosa.core.services.locale.TableLocaleSource;
import org.javarosa.core.util.UnregisteredLocaleException;
import org.javarosa.core.util.externalizable.PrototypeFactory;
import org.javarosa.core.util.test.ExternalizableTest;
import org.junit.Test;

import java.util.Hashtable;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

public class LocalizerTest {
    private void testSerialize(Localizer l, String msg) {
        PrototypeFactory pf = new PrototypeFactory();
        pf.addClass(TableLocaleSource.class);
        ExternalizableTest.testExternalizable(l, pf, "Localizer [" + msg + "]");
    }

    @Test
    public void testEmpty() {
        Localizer l = new Localizer();

        String[] locales = l.getAvailableLocales();
        if (locales == null || locales.length > 0) {
            fail("New localizer not empty");
        }
        String currentLocale = l.getLocale();
        if (currentLocale != null) {
            fail("New localizer has locale set");
        }
        String defaultLocale = l.getDefaultLocale();
        if (defaultLocale != null) {
            fail("New localizer has default locale set");
        }
    }

    @Test
    public void testAddLocale() {
        Localizer l = new Localizer();
        final String TEST_LOCALE = "test";

        if (l.hasLocale(TEST_LOCALE)) {
            fail("Localizer reports it contains non-existent locale");
        }
        boolean result = l.addAvailableLocale(TEST_LOCALE);
        if (!result) {
            fail("Localizer failed to add new locale");
        }
        if (!l.hasLocale(TEST_LOCALE)) {
            fail("Localizer reports it does not contain newly added locale");
        }
        Hashtable<String, String> localeData = l.getLocaleData(TEST_LOCALE);
        if (localeData == null || localeData.size() != 0) {
            fail("Newly created locale not empty (or undefined)");
        }
    }

    @Test
    public void testAddLocaleWithData() {
        Localizer l = new Localizer();
        final String TEST_LOCALE = "test";
        TableLocaleSource localeData = new TableLocaleSource();
        localeData.setLocaleMapping("textID", "text");


        if (l.hasLocale(TEST_LOCALE)) {
            fail("Localizer reports it contains non-existent locale");
        }

        l.addAvailableLocale(TEST_LOCALE);
        l.registerLocaleResource(TEST_LOCALE, localeData);

        if (!l.hasLocale(TEST_LOCALE)) {
            fail("Localizer reports it does not contain newly added locale");
        }
        if (!localeData.getLocalizedText().equals(l.getLocaleData(TEST_LOCALE))) {
            fail("Newly stored locale does not match source");
        }
    }

    @Test
    public void testAddExistingLocale() {
        Localizer l = new Localizer();
        final String TEST_LOCALE = "test";

        l.addAvailableLocale(TEST_LOCALE);
        TableLocaleSource table = new TableLocaleSource();
        table.setLocaleMapping("textID", "text");
        l.registerLocaleResource(TEST_LOCALE, table);

        Hashtable<String, String> localeData = l.getLocaleData(TEST_LOCALE);

        boolean result = l.addAvailableLocale(TEST_LOCALE);
        if (result) {
            fail("Localizer overwrote existing locale");
        }

        Hashtable newLocaleData = l.getLocaleData(TEST_LOCALE);
        if (!localeData.equals(newLocaleData)) {
            fail("Localizer overwrote existing locale");
        }
    }

    @Test
    public void testSetCurrentLocaleExists() {
        Localizer l = new Localizer();
        final String TEST_LOCALE = "test";
        l.addAvailableLocale(TEST_LOCALE);

        l.setLocale(TEST_LOCALE);
        if (!TEST_LOCALE.equals(l.getLocale())) {
            fail("Did not properly set current locale");
        }
    }

    @Test
    public void testSetCurrentLocaleNotExists() {
        Localizer l = new Localizer();
        final String TEST_LOCALE = "test";

        try {
            l.setLocale(TEST_LOCALE);

            fail("Set current locale to a non-existent locale");
        } catch (UnregisteredLocaleException nsee) {
            //expected
        }
    }

    @Test
    public void testUnsetCurrentLocale() {
        Localizer l = new Localizer();
        final String TEST_LOCALE = "test";
        l.addAvailableLocale(TEST_LOCALE);
        l.setLocale(TEST_LOCALE);

        try {
            l.setLocale(null);

            fail("Able to unset current locale");
        } catch (UnregisteredLocaleException nsee) {
            //expected
        }
    }

    @Test
    public void testSetDefaultLocaleExists() {
        Localizer l = new Localizer();
        final String TEST_LOCALE = "test";
        l.addAvailableLocale(TEST_LOCALE);

        l.setDefaultLocale(TEST_LOCALE);
        if (!TEST_LOCALE.equals(l.getDefaultLocale())) {
            fail("Did not properly set default locale");
        }
    }

    @Test
    public void testSetDefaultLocaleNotExists() {
        Localizer l = new Localizer();
        final String TEST_LOCALE = "test";

        try {
            l.setDefaultLocale(TEST_LOCALE);

            fail("Set current locale to a non-existent locale");
        } catch (UnregisteredLocaleException nsee) {
            //expected
        }
    }

    @Test
    public void testUnsetDefaultLocale() {
        Localizer l = new Localizer();
        final String TEST_LOCALE = "test";
        l.addAvailableLocale(TEST_LOCALE);
        l.setDefaultLocale(TEST_LOCALE);

        try {
            l.setDefaultLocale(null);

            if (l.getDefaultLocale() != null) {
                fail("Could not unset default locale");
            }
        } catch (UnregisteredLocaleException nsee) {
            fail("Exception unsetting default locale");
        }
    }

    @Test
    public void testSetToDefault() {
        Localizer l = new Localizer();
        final String TEST_LOCALE = "test";
        l.addAvailableLocale(TEST_LOCALE);
        l.setDefaultLocale(TEST_LOCALE);

        l.setToDefault();
        if (!TEST_LOCALE.equals(l.getLocale())) {
            fail("Could not set current locale to default");
        }
    }

    @Test
    public void testSetToDefaultNoDefault() {
        Localizer l = new Localizer();
        final String TEST_LOCALE = "test";
        l.addAvailableLocale(TEST_LOCALE);

        try {
            l.setToDefault();

            fail("Set current locale to default when no default set");
        } catch (IllegalStateException ise) {
            //expected
        }
    }

    @Test
    public void testAvailableLocales() {
        Localizer l = new Localizer();
        String[] locales;

        l.addAvailableLocale("test1");
        locales = l.getAvailableLocales();
        if (locales.length != 1 || !locales[0].equals("test1")) {
            fail("Available locales not as expected");
        }

        l.addAvailableLocale("test2");
        locales = l.getAvailableLocales();
        if (locales.length != 2 || !locales[0].equals("test1") || !locales[1].equals("test2")) {
            fail("Available locales not as expected");
        }

        l.addAvailableLocale("test3");
        locales = l.getAvailableLocales();
        if (locales.length != 3 || !locales[0].equals("test1") || !locales[1].equals("test2") || !locales[2].equals("test3")) {
            fail("Available locales not as expected");
        }
    }

    @Test
    public void testGetLocaleMap() {
        Localizer l = new Localizer();
        final String TEST_LOCALE = "test";
        l.addAvailableLocale(TEST_LOCALE);

        if (!l.getLocaleMap(TEST_LOCALE).equals(l.getLocaleData(TEST_LOCALE))) {
            fail();
        }
    }

    @Test
    public void testGetLocaleMapNotExist() {
        Localizer l = new Localizer();
        final String TEST_LOCALE = "test";

        try {
            l.getLocaleMap(TEST_LOCALE);

            fail("Did not throw exception when getting locale mapping for non-existent locale");
        } catch (UnregisteredLocaleException nsee) {
            //expected
        }
    }

    @Test
    public void testTextMapping() {
        Localizer l = new Localizer();
        final String TEST_LOCALE = "test";
        l.addAvailableLocale(TEST_LOCALE);

        if (l.hasMapping(TEST_LOCALE, "textID")) {
            fail("Localizer contains text mapping that was not defined");
        }
        TableLocaleSource table = new TableLocaleSource();
        table.setLocaleMapping("textID", "text");
        l.registerLocaleResource(TEST_LOCALE, table);

        if (!l.hasMapping(TEST_LOCALE, "textID")) {
            fail("Localizer does not contain newly added text mapping");
        }
        if (!"text".equals(l.getLocaleData(TEST_LOCALE).get("textID"))) {
            fail("Newly added text mapping does not match source");
        }
    }


    @Test
    public void testTextMappingOverwrite() {
        Localizer l = new Localizer();
        final String TEST_LOCALE = "test";

        l.addAvailableLocale(TEST_LOCALE);
        TableLocaleSource table = new TableLocaleSource();

        table.setLocaleMapping("textID", "oldText");

        table.setLocaleMapping("textID", "newText");

        l.registerLocaleResource(TEST_LOCALE, table);

        if (!l.hasMapping(TEST_LOCALE, "textID")) {
            fail("Localizer does not contain overwritten text mapping");
        }
        if (!"newText".equals(l.getLocaleData(TEST_LOCALE).get("textID"))) {
            fail("Newly overwritten text mapping does not match source");
        }
    }

    @Test
    public void testGetText() {
        for (int localeCase = 1; localeCase <= 3; localeCase++) {
            for (int formCase = 1; formCase <= 2; formCase++) {
                testGetText(localeCase, formCase);
            }
        }
    }

    private static final int DEFAULT_LOCALE = 1;
    private static final int NON_DEFAULT_LOCALE = 2;
    private static final int NEUTRAL_LOCALE = 3;

    //private static final int BASE_FORM = 1;
    private static final int CUSTOM_FORM = 2;

    public void testGetText(int localeCase, int formCase) {
        String ourLocale = null;
        String otherLocale = null;

        switch (localeCase) {
            case DEFAULT_LOCALE:
                ourLocale = "default";
                otherLocale = null;
                break;
            case NON_DEFAULT_LOCALE:
                ourLocale = "other";
                otherLocale = "default";
                break;
            case NEUTRAL_LOCALE:
                ourLocale = "neutral";
                otherLocale = null;
                break;
        }

        String textID = "textID" + (formCase == CUSTOM_FORM ? ";form" : "");

        for (int i = 0; i < 4; i++) { //iterate through 4 possible fallback modes
            for (int j = 0; j < 4; j++) {
                if (otherLocale == null) {
                    testGetText(i, j, -1, ourLocale, otherLocale, textID, localeCase, formCase);
                } else {
                    for (int k = 0; k < 4; k++) {
                        testGetText(i, j, k, ourLocale, otherLocale, textID, localeCase, formCase);
                    }
                }
            }
        }
    }

    public void testGetText(int i, int j, int k, String ourLocale, String otherLocale, String textID, int localeCase, int formCase) {
        //System.out.println("testing getText: "+localeCase+","+formCase+","+i+","+j+","+k);

        Localizer l = buildLocalizer(i, j, k, ourLocale, otherLocale);
        String expected = expectedText(textID, l);
        String text, text2;

        text = l.getText(textID, ourLocale);
        if (expected == null ? text != null : !expected.equals(text)) {
            fail("Did not retrieve expected text from localizer [" + localeCase + "," + formCase + "," + i + "," + j + "," + k + "]");
        }

        text2 = l.getText(textID);

        if (expected == null && text2 != null) {
            fail("Localization shouldn't have returned a result");
        } else if (expected != null && !expected.equals(text2)) {
            fail("Did not retrieve expected text");
        }
    }

    private Localizer buildLocalizer(int i, int j, int k, String ourLocale, String otherLocale) {
        Localizer l = new Localizer(i / 2 == 0, i % 2 == 0);

        TableLocaleSource firstLocale = new TableLocaleSource();
        TableLocaleSource secondLocale = new TableLocaleSource();

        if (j / 2 == 0 || "default".equals(ourLocale))
            firstLocale.setLocaleMapping("textID", "text:" + ourLocale + ":base");
        if (j % 2 == 0 || "default".equals(ourLocale))
            firstLocale.setLocaleMapping("textID;form", "text:" + ourLocale + ":form");

        if (otherLocale != null) {
            if (k / 2 == 0 || "default".equals(otherLocale))
                secondLocale.setLocaleMapping("textID", "text:" + otherLocale + ":base");
            if (k % 2 == 0 || "default".equals(otherLocale))
                secondLocale.setLocaleMapping("textID;form", "text:" + otherLocale + ":form");
        }

        l.addAvailableLocale(ourLocale);
        l.registerLocaleResource(ourLocale, firstLocale);

        if (otherLocale != null) {
            l.addAvailableLocale(otherLocale);
            l.registerLocaleResource(otherLocale, secondLocale);
        }
        if (l.hasLocale("default")) {
            l.setDefaultLocale("default");
        }

        l.setLocale(ourLocale);

        return l;
    }

    private String expectedText(String textID, Localizer l) {
        boolean[] searchOrder = new boolean[4];
        boolean fallbackLocale = l.getFallbackLocale();
        boolean fallbackForm = l.getFallbackForm();
        boolean hasForm = (textID.contains(";"));
        boolean hasDefault = (l.getDefaultLocale() != null && !l.getDefaultLocale().equals(l.getLocale()));
        String baseTextID = (hasForm ? textID.substring(0, textID.indexOf(";")) : textID);

        searchOrder[0] = hasForm;
        searchOrder[1] = !hasForm || fallbackForm;
        searchOrder[2] = hasForm && (hasDefault && fallbackLocale);
        searchOrder[3] = (!hasForm || fallbackForm) && (hasDefault && fallbackLocale);

        String text = null;
        for (int i = 0; text == null && i < 4; i++) {
            if (!searchOrder[i])
                continue;

            switch (i + 1) {
                case 1:
                    text = l.getRawText(l.getLocale(), textID);
                    break;
                case 2:
                    text = l.getRawText(l.getLocale(), baseTextID);
                    break;
                case 3:
                    text = l.getRawText(l.getDefaultLocale(), textID);
                    break;
                case 4:
                    text = l.getRawText(l.getDefaultLocale(), baseTextID);
                    break;
            }
        }

        return text;
    }

    @Test
    public void testGetTextNoCurrentLocale() {
        Localizer l = new Localizer();
        TableLocaleSource table = new TableLocaleSource();
        l.addAvailableLocale("test");
        l.setDefaultLocale("test");

        table.setLocaleMapping("textID", "text");
        l.registerLocaleResource("test", table);

        try {
            l.getText("textID");

            fail("Retrieved current locale text when current locale not set");
        } catch (UnregisteredLocaleException nsee) {
            //expected
        }
    }

    @Test
    public void testNullArgs() {
        Localizer l = new Localizer();
        l.addAvailableLocale("test");

        TableLocaleSource table = new TableLocaleSource();

        try {
            l.addAvailableLocale(null);

            fail("addAvailableLocale: Did not get expected null pointer exception");
        } catch (NullPointerException npe) {
            //expected
        }

        if (l.hasLocale(null)) {
            fail("Localizer reports it contains null locale");
        }

        try {
            l.registerLocaleResource(null, new TableLocaleSource());

            fail("setLocaleData: Did not get expected null pointer exception");
        } catch (NullPointerException npe) {
            //expected
        }

        try {
            l.registerLocaleResource("test", null);

            fail("setLocaleData: Did not get expected null pointer exception");
        } catch (NullPointerException npe) {
            //expected
        }

        if (l.getLocaleData(null) != null) {
            fail("getLocaleData: Localizer returns mappings for null locale");
        }

        try {
            l.getLocaleMap(null);

            fail("getLocaleMap: Did not get expected exception");
        } catch (UnregisteredLocaleException nsee) {
            //expected
        }

        try {
            table.setLocaleMapping(null, "text");

            fail("setLocaleMapping: Did not get expected null pointer exception");
        } catch (NullPointerException npe) {
            //expected
        }

        try {
            table.setLocaleMapping(null, null);

            fail("setLocaleMapping: Did not get expected null pointer exception");
        } catch (NullPointerException npe) {
            //expected
        }

        try {
            l.hasMapping(null, "textID");

            fail("hasMapping: Did not get expected exception");
        } catch (UnregisteredLocaleException nsee) {
            //expected
        }

        if (l.hasMapping("test", null)) {
            fail("Localization reports it contains null mapping");
        }

        try {
            l.getText("textID", (String)null);

            fail("getText: Did not get expected exception");
        } catch (UnregisteredLocaleException nsee) {
            //expected
        }

        try {
            l.getText(null, "test");

            fail("getText: Did not get expected null pointer exception");
        } catch (NullPointerException npe) {
            //expected
        }
    }

    @Test
    public void testSerialization() {
        Localizer l = new Localizer(true, true);
        TableLocaleSource firstLocale = new TableLocaleSource();
        TableLocaleSource secondLocale = new TableLocaleSource();
        TableLocaleSource finalLocale = new TableLocaleSource();

        testSerialize(l, "empty 1");
        testSerialize(new Localizer(false, false), "empty 2");
        testSerialize(new Localizer(true, false), "empty 3");
        testSerialize(new Localizer(false, true), "empty 4");

        l.addAvailableLocale("locale1");
        testSerialize(l, "one empty locale");

        l.addAvailableLocale("locale2");
        testSerialize(l, "two empty locales");

        l.setDefaultLocale("locale2");
        testSerialize(l, "two empty locales + default");

        l.setToDefault();
        testSerialize(l, "two empty locales + default/current");

        l.setLocale("locale1");
        testSerialize(l, "two empty locales + default/current 2");

        l.setDefaultLocale(null);
        testSerialize(l, "two empty locales + current");

        l.registerLocaleResource("locale1", firstLocale);
        l.registerLocaleResource("locale2", secondLocale);
        firstLocale.setLocaleMapping("id1", "text1");
        testSerialize(l, "locales with data 1");
        firstLocale.setLocaleMapping("id2", "text2");
        testSerialize(l, "locales with data 2");

        secondLocale.setLocaleMapping("id1", "text1");
        secondLocale.setLocaleMapping("id2", "text2");
        secondLocale.setLocaleMapping("id3", "text3");
        testSerialize(l, "locales with data 3");

        secondLocale.setLocaleMapping("id2", null);
        testSerialize(l, "locales with data 4");

        finalLocale.setLocaleMapping("id1", "text1");
        finalLocale.setLocaleMapping("id4", "text4");
        l.registerLocaleResource("locale3", finalLocale);
        testSerialize(l, "locales with data 5");

        testSerialize(l, "locales with data 6");
    }

    @Test
    public void testLinearSub() {
        final String F = "first";
        final String S = "second";

        final String C = "${0}";

        final String D = "${1}${0}";

        final String[] res = new String[]{"One", "Two"};


        assertEquals(Localizer.processArguments("${0}", new String[]{F}), F);
        assertEquals(Localizer.processArguments("${0},${1}", new String[]{F, S}), F + "," + S);
        assertEquals(Localizer.processArguments("testing ${0}", new String[]{F}), "testing " + F);

        assertEquals(Localizer.processArguments("1${arbitrary}2", new String[]{F}), "1" + F + "2");

        final String[] holder = new String[1];

        runAsync(() -> holder[0] = Localizer.processArguments("${0}", new String[]{C}), "Argument processing: " + C);

        assertEquals(holder[0], C);


        runAsync(() -> holder[0] = Localizer.processArguments("${0}", new String[]{D}), "Argument processing: " + D);

        assertEquals(holder[0], D);

        runAsync(() -> holder[0] = Localizer.processArguments(holder[0], res), "Argument processing: " + res[1] + res[0]);

        assertEquals(holder[0], res[1] + res[0]);

        runAsync(() -> holder[0] = Localizer.processArguments("$ {0} ${1}", res), "Argument processing: " + "$ {0} " + res[1]);

        assertEquals(holder[0], "$ {0} " + res[1]);

    }

    private void runAsync(Runnable test, String label) {
        Thread t = new Thread(test);
        t.start();
        int attempts = 4;

        for(int i = 0 ; i < attempts ; ++i) {
            try {
                t.join(50);
                break;
            } catch (InterruptedException e) {

            }
        }
        if (t.isAlive()) {
            t.stop();
            throw new RuntimeException("Failed to return from recursive argument processing: "+  label);
        }
    }

    @Test
    public void testHashSub() {
        final String F = "first";
        final String S = "second";
        Hashtable h = new Hashtable();
        h.put("fir", F);
        h.put("also first", F);
        h.put("sec", S);

        assertEquals(Localizer.processArguments("${fir}", h), F);
        assertEquals(Localizer.processArguments("${fir},${sec}", h), F + "," + S);
        assertEquals(Localizer.processArguments("${sec},${fir}", h), S + "," + F);
        assertEquals(Localizer.processArguments("${empty}", h), "${empty}");
        assertEquals(Localizer.processArguments("${fir},${fir},${also first}", h), F + "," + F + "," + F);
    }


    @Test
    public void testFallbacks() {
        Localizer localizer = new Localizer(true, true);

        localizer.addAvailableLocale("one");
        localizer.addAvailableLocale("two");

        TableLocaleSource firstLocale = new TableLocaleSource();
        firstLocale.setLocaleMapping("data", "val");
        firstLocale.setLocaleMapping("data2", "vald2");
        localizer.registerLocaleResource("one", firstLocale);

        TableLocaleSource secondLocale = new TableLocaleSource();
        firstLocale.setLocaleMapping("data", "val2");
        localizer.registerLocaleResource("two", secondLocale);
        localizer.setDefaultLocale("one");

        localizer.setLocale("two");

        String text = localizer.getText("data2");
        assertEquals("fallback", text, "vald2");
        String shouldBeNull = localizer.getText("noexist");
        assertNull("Localizer didn't return null value", shouldBeNull);

        localizer.setToDefault();

        shouldBeNull = localizer.getText("noexist");
        assertNull("Localizer didn't return null value", shouldBeNull);
        assertNull("Localizer didn't return null value", shouldBeNull);
    }
}