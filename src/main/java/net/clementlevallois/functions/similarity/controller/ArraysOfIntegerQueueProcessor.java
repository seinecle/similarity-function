/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package net.clementlevallois.functions.similarity.controller;

/**
 *
 * @author ChatGPT and LEVALLOIS
 */
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ArraysOfIntegerQueueProcessor implements Runnable {

    private static Duration FLUSH_INTERVAL = Duration.ofMillis(500);

    private final Path outputFilePath;
    private final ConcurrentLinkedQueue<int[]> integerArrayQueue;
    private boolean arraysOfIntegerHarvestingRunning;
    private FileChannel outputChannel = null;
    private boolean addGephiHeader = false;
    private Map<Integer, String> mapIntegersToEntityIds;

    public ArraysOfIntegerQueueProcessor(Map<Integer, String> mapIntegersToEntityIds, Path outputFilePath, ConcurrentLinkedQueue<int[]> stringQueue, int flushIntervalInSeconds, boolean addGephiHeader) {
        this.outputFilePath = outputFilePath;
        this.integerArrayQueue = stringQueue;
        FLUSH_INTERVAL = Duration.ofSeconds(flushIntervalInSeconds);
        arraysOfIntegerHarvestingRunning = true;
        this.addGephiHeader = addGephiHeader;
        this.mapIntegersToEntityIds = mapIntegersToEntityIds;
    }

    public boolean stop() throws IOException {
        arraysOfIntegerHarvestingRunning = false;
        return true;
    }

    @Override
    public void run() {
        try {
            Files.deleteIfExists(outputFilePath);
            Files.createFile(outputFilePath);
            if (addGephiHeader) {
                Files.writeString(outputFilePath, "Source,Target,Weight\n");
            }
        } catch (IOException ex) {
            Logger.getLogger(ArraysOfIntegerQueueProcessor.class.getName()).log(Level.SEVERE, null, ex);
        }

        try {
            Instant lastFlushTime = Instant.now();
            outputChannel = FileChannel.open(outputFilePath,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.WRITE,
                    StandardOpenOption.APPEND);
            while (arraysOfIntegerHarvestingRunning | !integerArrayQueue.isEmpty()) {
                int[] arrayOfIntegers = integerArrayQueue.poll();
                if (arrayOfIntegers == null) {
                    // No more items in the queue, sleep for a bit before checking again
                    Thread.sleep(100);
                    continue;
                }
                ByteBuffer byteBuffer = ByteBuffer.allocate(arrayOfIntegers.length * Integer.BYTES);
                StringBuilder toWrite = new StringBuilder();
                toWrite.append(mapIntegersToEntityIds.get(arrayOfIntegers[0]));
                toWrite.append(",");
                toWrite.append(mapIntegersToEntityIds.get(arrayOfIntegers[1]));
                toWrite.append(",");
                toWrite.append(String.valueOf(arrayOfIntegers[2]));
                toWrite.append("\n");
                byte[] encodedValue = toWrite.toString().getBytes(StandardCharsets.UTF_8); // Encode the string using the desired encoding
                outputChannel.write(ByteBuffer.wrap(encodedValue)); // Write the encoded value to the file
                byteBuffer.flip();
                outputChannel.write(byteBuffer);

                Instant now = Instant.now();
                if (Duration.between(lastFlushTime, now).compareTo(FLUSH_INTERVAL) >= 0) {
                    outputChannel.force(true);
                    lastFlushTime = now;
                }
            }
            outputChannel.force(true);
            outputChannel.close();

            Thread.currentThread().interrupt();
        } catch (IOException | InterruptedException ex) {
            if (ex.getClass() == InterruptedException.class && arraysOfIntegerHarvestingRunning == false) {
                System.out.println("closing the processor of String queue");
            }
        }

    }
}
