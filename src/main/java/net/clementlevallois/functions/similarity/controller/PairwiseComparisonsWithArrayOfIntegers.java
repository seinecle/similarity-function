/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package net.clementlevallois.functions.similarity.controller;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import net.clementlevallois.utils.Clock;

/**
 *
 * @element LEVALLOIS
 */
public class PairwiseComparisonsWithArrayOfIntegers {

    private static int[] data;
    private static int[] entities;
    private static Map<Integer, String> mapIntegersToEntityIds;

    // method suggested by reddit user Ivory2Much:
    // https://www.reddit.com/r/java/comments/13rlb26/speeding_up_pairwise_comparisons_to_28_millionsec/

    public static void computeSimilarities(Path rootFolderOfInputData, boolean addGephiHeader) throws InterruptedException, ExecutionException, IOException {
        Clock clock = new Clock("computing similarities");
        ConcurrentLinkedQueue<int[]> queue = new ConcurrentLinkedQueue();
        int writeToDiskIntervalInSeconds = 1;
        
        Path outputFile = Path.of(rootFolderOfInputData.toString(),"similarities.csv");

        // this class is helpful to retrieve, and write to file the pairs of entities that do have a non zero similarity,
        // all while interrupting the least possible the computations on similartiies
        ArraysOfIntegerQueueProcessor queueProcessor = new ArraysOfIntegerQueueProcessor(mapIntegersToEntityIds, outputFile, queue, writeToDiskIntervalInSeconds, addGephiHeader);
        Thread queueProcessorThread = new Thread(queueProcessor);
        queueProcessorThread.start();

        ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();

        int indexOfFirstEntityInEntities = 0;
        Runnable runnableTask;
        // we iterate through all entities

        while (indexOfFirstEntityInEntities < entities.length) {
            int indexFirstEntityInDataArray = entities[indexOfFirstEntityInEntities];
            int indexOfFirstEntityInEntitiesAsLocalVariable = indexOfFirstEntityInEntities;
            runnableTask = () -> {
                /*
                
                how do we find the index of the second entitity to compare the first entity to?
                Remember that a entity's info, up to the next entity, is stored as:
                
                [entity id, number of elements associated with it, element a, element b, ..., next entity id]
                
                basically, when we encounter the index of the first entity, we must go:
                - past the index where the cardinality of the number of elements associated with it is stored
                - past each of these elements, which are listed after the first entity id
                - and one more index to get to the second entity.
               
                                
                 */
//                int second = first + 1 + data[first + 1] + 1;
                int indexOfSecondEntityInEntities = indexOfFirstEntityInEntitiesAsLocalVariable + 1;
                if (indexOfSecondEntityInEntities >= entities.length) {
                    return;
                }
                int indexSecondEntityInDataArray = entities[indexOfSecondEntityInEntities];
                int[] triplet;

                // as long as we don't hit the end of the array...
                while (indexSecondEntityInDataArray < data.length) {
                    // compute the similarities between the 2 entities
                    int similarity = pairwiseComparison(indexFirstEntityInDataArray, indexSecondEntityInDataArray);
                    if (similarity > 0) {
                        triplet = new int[3];
                        triplet[0] = data[indexFirstEntityInDataArray];
                        triplet[1] = data[indexSecondEntityInDataArray];
                        triplet[2] = similarity;
                        // this is the step where a similarity btw 2 entities has been found and it is offloaded to this queue.
                        queue.add(triplet);
                    }

                    /* and how do we move to the next entity to be compared to the first entity ?
                    
                    same logic as the logic we used to find this second entity, right above:
                    
                    - we take the index of the second entity, that we have just finished comparing to the first entity
                    - we move right by a number of indices which correspond to its number of associated elements. This number [the cardinality] is stored at [second +1]
                    - we add one indice to move past the cardinality as well, and one more to land on the next entity.

                     */
//                    second += data[second + 1] + 1 + 1;
                    indexOfSecondEntityInEntities++;
                    if (indexOfSecondEntityInEntities >= entities.length) {
                        return;
                    }
                    indexSecondEntityInDataArray = entities[indexOfSecondEntityInEntities];
                }

            };
            executor.execute(runnableTask);
            indexOfFirstEntityInEntities++;
        }
        executor.shutdown();
        //Awaits either 1 minute or if all tasks are completed. Whatever is first.
        executor.awaitTermination(1L, TimeUnit.MINUTES);

        queueProcessor.stop();
        clock.closeAndPrintClock();
    }

    public static int pairwiseComparison(int first, int second) {
        // indices of the last elements
        int firstEntityLastElementIndex = first + 1 + data[first + 1];
        int secondEntityLasrElementIndex = second + 1 + data[second + 1];

        // indices of the first elements
        // (potentially beyond last, when 0 elements) <-- I have added checks to make sure there is always one element in the data
        // so that 'first + 2' or 'second + 2' lands on an element, not on the next entity's id.
        int f = first + 2;
        int s = second + 2;

        int matches = 0;

        // elements
        int fa = -1;
        int sa = -1;

        // this part I understood thanks to the explanations of ChatGPT: it consists in moving from index on both arrays as a quick way to count similar elements.
        while (f <= firstEntityLastElementIndex && s <= secondEntityLasrElementIndex) {
            if (fa < 0) {
                fa = data[f];
            }
            if (sa < 0) {
                sa = data[s];
            }

            if (fa < sa) {
                f++;
                fa = -1;
            } else if (fa > sa) {
                s++;
                sa = -1;
            } else {
                matches++;
                f++;
                fa = -1;
                s++;
                sa = -1;
            }
        }
        return matches;
    }

    public static void setData(int[] data) {
        PairwiseComparisonsWithArrayOfIntegers.data = data;
    }

    public static void setEntities(int[] entities) {
        PairwiseComparisonsWithArrayOfIntegers.entities = entities;
    }

    public static void setMapIntegersToEntityIds(Map<Integer, String> mapIntegersToEntityIds) {
        PairwiseComparisonsWithArrayOfIntegers.mapIntegersToEntityIds = mapIntegersToEntityIds;
    }
    
    
    
}
