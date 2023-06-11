/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package net.clementlevallois.functions.similarity.tests;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import net.clementlevallois.functions.similarity.controller.InputOutput;
import net.clementlevallois.functions.similarity.controller.PairwiseComparisonsWithArrayOfIntegers;
import org.junit.Assert;
import org.junit.Test;

/**
 *
 * @author LEVALLOIS
 */
public class SimilarityTest {

    @Test
    public void doEntireChain() throws Exception {

        boolean insertGephiHeaders = true;
        InputStream inputStream = getClass().getResourceAsStream("/data-test.txt");
        File file = convertInputStreamToFile(inputStream);
        String rootFolderData = file.getParent();

        InputOutput io = new InputOutput();
        char delimiter = ',';
        io.loadEntitiesAndTheirElementsIntoAMap(file.getAbsolutePath(), delimiter);
        io.mapIdsToSequentialIds();

        io.fillingTheIntegerArray();

        PairwiseComparisonsWithArrayOfIntegers.setData(io.getData());
        PairwiseComparisonsWithArrayOfIntegers.setEntities(io.getEntities());
        PairwiseComparisonsWithArrayOfIntegers.setMapIntegersToEntityIds(io.getMapIntegersToEntityIds());
        PairwiseComparisonsWithArrayOfIntegers.computeSimilarities(Path.of(rootFolderData), insertGephiHeaders);

        Path resultFile = Path.of(rootFolderData, "similarities.csv");

        Assert.assertTrue(io.getMapOriginalStrings().size() == 5);
        Assert.assertTrue(io.getMapEntityIdsToIntegers().size() == 5);
        Assert.assertTrue(io.getMapElementIdsToIntegers().size() == 6);
        Assert.assertTrue(resultFile.toFile().exists());
        file.delete();
    }

    private static File convertInputStreamToFile(InputStream inputStream) {
        try {
            Path tempFile = Files.createTempFile("temp", ".tmp");
            Files.copy(inputStream, tempFile, StandardCopyOption.REPLACE_EXISTING);
            return tempFile.toFile();
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

}
