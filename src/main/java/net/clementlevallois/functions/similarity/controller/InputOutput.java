/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package net.clementlevallois.functions.similarity.controller;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Pattern;
import net.clementlevallois.utils.Clock;

/**
 *
 * @author LEVALLOIS
 */
public class InputOutput {

    private Map<String, Set<String>> mapOriginalStrings;
    private Set<String> setOriginalElementStrings;
    private Map<String, Integer> mapEntityIdsToIntegers;
    private Map<Integer, String> mapIntegersToEntityIds;
    private Map<String, Integer> mapElementIdsToIntegers;
    private Path rootPathForInputData;
    private int sizeBigArray = 0;
    private int[] data;
    private int[] entities;

    public void loadEntitiesAndTheirElementsIntoAMap(String filePath, char fieldDelimiter) throws Exception {

        String fieldDelimiterEscaped = Pattern.quote(String.valueOf(fieldDelimiter));
        Clock clock = new Clock("loading file to map");
        mapOriginalStrings = new HashMap();
        setOriginalElementStrings = new TreeSet();
        // Open the file using RandomAccessFile and FileChannel
        File file = new File(filePath);
        rootPathForInputData = file.getParentFile().toPath();
        RandomAccessFile randomAccessFile = new RandomAccessFile(file, "r");
        FileChannel fileChannel = randomAccessFile.getChannel();

        // Create a ByteBuffer to read the file in chunks
        int bufferSize = 8192; // Adjust this according to your needs
        ByteBuffer buffer = ByteBuffer.allocate(bufferSize * 10);

        StringBuilder line = new StringBuilder();
        int count = 0;
        while (fileChannel.read(buffer) != -1) {
            buffer.flip(); // Prepare the buffer for reading

            while (buffer.hasRemaining()) {
                char currentChar = (char) buffer.get();
                if (currentChar == '\n') {
                    // Process the line
                    processLine(line.toString(), fieldDelimiterEscaped);

                    // Clear the StringBuilder for the next line
                    line.setLength(0);
                } else {
                    line.append(currentChar);
                }
            }
            if (count++ % 3_000 == 0) {
                System.out.print("count buffers: " + count);
                System.out.print(", ");
                clock.printElapsedTime();
// Get the Java runtime
                Runtime runtime = Runtime.getRuntime();
                // Run the garbage collector
                runtime.gc();
                // Calculate the used memory
                long memory = runtime.totalMemory() - runtime.freeMemory();
                System.out.println("Used memory is megabytes: " + (double) memory / (1024 * 1024));
            }

            buffer.clear(); // Prepare the buffer for writing
        }

        // Process the last line if it doesn't end with a newline character
        if (line.length() > 0) {
            processLine(line.toString(), fieldDelimiterEscaped);
        }

        // Close the file channel and the random access file
        fileChannel.close();
        randomAccessFile.close();
        clock.closeAndPrintClock();
    }

    private void processLine(String line, String fieldDelimiter) {
        String fields[] = line.trim().split(fieldDelimiter);
        if (fields.length < 2) {
            return;
        }
        sizeBigArray += (fields.length + 1);
        String entityId = fields[0];
        Set<String> setEntities = new HashSet();
        for (int i = 1; i < fields.length; i++) {
            setEntities.add(fields[i]);
            setOriginalElementStrings.add(fields[i]);
        }
        mapOriginalStrings.put(entityId, setEntities);
    }

    public void mapIdsToSequentialIds() throws IOException {
        mapEntityIdsToIntegers = new HashMap();
        mapIntegersToEntityIds = new HashMap();
        mapElementIdsToIntegers = new HashMap();

        Clock clock = new Clock("mapping entity ids to ints");
        Integer index = 0;
        Set<String> setOriginalEntityStrings = mapOriginalStrings.keySet();
        for (String originalEntityId : setOriginalEntityStrings) {
            mapEntityIdsToIntegers.put(originalEntityId, index);
            mapIntegersToEntityIds.put(index, originalEntityId);
            index++;
        }
        clock.closeAndPrintClock();

        clock = new Clock("mapping element ids to ints");
        index = 0;
        for (String originalElementId : setOriginalElementStrings) {
            mapElementIdsToIntegers.put(originalElementId, index++);
        }
        clock.closeAndPrintClock();
    }

    public void fillingTheIntegerArray() {
        Clock clock = new Clock("initiating and filling the array");
        data = new int[sizeBigArray];

        // this "firsts" array is a convenience array which stores the indices of all journals in the data[] array.
        // useful later to iterate through journals in the outer loop
        entities = new int[mapEntityIdsToIntegers.size()];
        int i = 0;
        int countEntities = 0;
        Set<Map.Entry<String, Set<String>>> entrySet = mapOriginalStrings.entrySet();
        for (Map.Entry<String, Set<String>> entry : entrySet) {

            String entityIdAsString = entry.getKey();
            Set<String> elementsIdsAsString = entry.getValue();
            int entityId = mapEntityIdsToIntegers.get(entityIdAsString);

            // adding the index of the entity in the data array to a convenience array
            entities[countEntities++] = i;

            // adding the entity id to the array of ints
            data[i++] = entityId;

            // adding the number of elements to the array of ints
            // it is conveniently represented by the size of the array containing all element ids
            data[i++] = elementsIdsAsString.size();

            // now we need to insert the list of all elements ids in the int array, SORTED ASCENDING.
            // we need to sort the ints ascendingly before inserted them in the array!
            List<String> elementsAsStringsList = new ArrayList(elementsIdsAsString);
            List<Integer> elementsAsIntegers = new ArrayList(elementsAsStringsList.size());
            for (String elementIdAsString : elementsAsStringsList) {
                int elementIdAsInteger = mapElementIdsToIntegers.get(elementIdAsString);
                elementsAsIntegers.add(elementIdAsInteger);
            }
            // Sort the List using Collections.sort()
            Collections.sort(elementsAsIntegers);
            for (int elementIdAsInteger : elementsAsIntegers) {
                data[i++] = elementIdAsInteger;
            }
        }
        clock.closeAndPrintClock();

    }

    public Map<String, Set<String>> getMapOriginalStrings() {
        return mapOriginalStrings;
    }

    public int[] getData() {
        return data;
    }

    public int[] getEntities() {
        return entities;
    }

    public Map<String, Integer> getMapEntityIdsToIntegers() {
        return mapEntityIdsToIntegers;
    }

    public Map<String, Integer> getMapElementIdsToIntegers() {
        return mapElementIdsToIntegers;
    }

    public Map<Integer, String> getMapIntegersToEntityIds() {
        return mapIntegersToEntityIds;
    }
    
    

}
