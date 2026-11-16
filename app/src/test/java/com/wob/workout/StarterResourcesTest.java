package com.wob.workout;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.xml.parsers.DocumentBuilderFactory;

/**
 * The instrumented tests run against the debug variant, which shortens every starter exercise to
 * a few seconds - so nothing else can see what a release build actually ships. This parses both
 * resource files straight from disk (Gradle runs unit tests with the module directory as working
 * directory) and pins the shipped numbers, plus the one thing that would otherwise only fail at
 * first launch on a fresh install: titles and descriptions that don't line up.
 */
public class StarterResourcesTest {

    private static final File RELEASE = new File("src/main/res/values/starter.xml");
    private static final File DEBUG = new File("src/debug/res/values/starter.xml");
    private static final File RAW = new File("src/main/res/raw");

    @Test
    public void release_shipsTenExercisesOfSixtySeconds() throws Exception {
        Document release = parse(RELEASE);

        assertEquals(10, stringArray(release, "starter_exercise_titles").size());
        assertEquals(60, integer(release, "starter_exercise_seconds"));
    }

    @Test
    public void release_titlesAndDescriptionsLineUp() throws Exception {
        assertTitlesAndDescriptionsLineUp(parse(RELEASE));
    }

    @Test
    public void debug_shipsTheReleaseWorkoutWithFiveSecondExercises() throws Exception {
        // Any other resource in the debug file would make the debug app differ from the real one.
        Document debug = parse(DEBUG);

        assertEquals(Collections.singleton("starter_exercise_seconds"), resourceNames(debug));
        assertEquals(5, integer(debug, "starter_exercise_seconds"));
    }

    @Test
    public void release_shipsTheExercisesInWorkoutOrder() throws Exception {
        assertEquals(
                Arrays.asList("Hampelmänner", "Kniebeugen", "Ausfallschritte", "Wandsitzen",
                        "Glute Bridge", "Sit-Ups", "Liegestütze", "Mountain Climber", "Plank",
                        "Superman"),
                stringArray(parse(RELEASE), "starter_exercise_titles"));
    }

    @Test
    public void release_everyExerciseHasItsOwnPicture() throws Exception {
        Document release = parse(RELEASE);
        List<String> titles = stringArray(release, "starter_exercise_titles");
        List<String> images = array(release, "starter_exercise_images");

        assertEquals(titles.size(), images.size());
        assertEquals("one picture per exercise", images.size(), new HashSet<>(images).size());
        for (int i = 0; i < titles.size(); i++) {
            String image = images.get(i);
            assertTrue(titles.get(i) + ": " + image, image.startsWith("@raw/starter_"));
            String name = image.substring("@raw/".length());
            assertTrue(titles.get(i) + " has no file", new File(RAW, name + ".webp").isFile());
        }
    }

    private static void assertTitlesAndDescriptionsLineUp(Document document) {
        List<String> titles = stringArray(document, "starter_exercise_titles");
        List<String> descriptions = stringArray(document, "starter_exercise_descriptions");

        assertEquals(titles.size(), descriptions.size());
        for (String title : titles) {
            assertFalse("blank starter exercise title", title.trim().isEmpty());
        }
        for (String description : descriptions) {
            assertFalse("blank starter exercise description", description.trim().isEmpty());
        }
        assertTrue(titles.size() > 0);
    }

    private static Document parse(File file) throws Exception {
        assertTrue("missing " + file.getAbsolutePath(), file.isFile());
        return DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(file);
    }

    private static List<String> stringArray(Document document, String name) {
        return items(document, "string-array", name);
    }

    private static List<String> array(Document document, String name) {
        return items(document, "array", name);
    }

    private static List<String> items(Document document, String tag, String name) {
        NodeList arrays = document.getElementsByTagName(tag);
        for (int i = 0; i < arrays.getLength(); i++) {
            Element array = (Element) arrays.item(i);
            if (name.equals(array.getAttribute("name"))) {
                List<String> items = new ArrayList<>();
                NodeList itemNodes = array.getElementsByTagName("item");
                for (int j = 0; j < itemNodes.getLength(); j++) {
                    items.add(itemNodes.item(j).getTextContent());
                }
                return items;
            }
        }
        throw new AssertionError("no <" + tag + " name=\"" + name + "\">");
    }

    private static int integer(Document document, String name) {
        NodeList integers = document.getElementsByTagName("integer");
        for (int i = 0; i < integers.getLength(); i++) {
            Element element = (Element) integers.item(i);
            if (name.equals(element.getAttribute("name"))) {
                return Integer.parseInt(element.getTextContent().trim());
            }
        }
        throw new AssertionError("no <integer name=\"" + name + "\">");
    }

    private static Set<String> resourceNames(Document document) {
        Set<String> names = new HashSet<>();
        NodeList all = document.getDocumentElement().getChildNodes();
        for (int i = 0; i < all.getLength(); i++) {
            if (all.item(i) instanceof Element) {
                names.add(((Element) all.item(i)).getAttribute("name"));
            }
        }
        return names;
    }
}
