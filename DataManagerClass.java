package _The_Last_Breath;

import ZSeven.MultiThreadArraySum;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@DataProcessor
class FilterProcessor{
    public List<String> filterData(List<String> data) {
        data = data.stream()
                .filter(word -> word.endsWith("."))//слово заканчивается на точку
                .toList();
        return data;


    }
}

@DataProcessor
class TransformProcessor {
    public List<String> transformData(List<String> data) {
        return data.stream()
                .map(String::toUpperCase)
                .collect(Collectors.toList());
    }
}

@SuppressWarnings({"ResultOfMethodCallIgnored", "CallToPrintStackTrace"})
@DataProcessor
class DataManager {
    public List<Object> processors = new ArrayList<>();
    public List<String> data = new ArrayList<>();

    public void registerDataProcessor(Object processor) {
        processors.add(processor);
    }

    public int loadData(String source){
        //наши данные
        try {
            File file = new File(source);
            String content = Files.readString(file.toPath(), StandardCharsets.UTF_8);
            String[] arr = content.split(" ");
            Collections.addAll(data, arr); //вместо цикла
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return data.size();
    }

    public void processData() {
        ExecutorService executor = Executors.newFixedThreadPool(processors.size());

        for (Object processor : processors) {
            executor.execute(() -> {
                if (processor instanceof FilterProcessor) {
                    data = ((FilterProcessor) processor).filterData(data);
                    //System.out.println("Процесс фильтрации...");
                }
                if (processor instanceof TransformProcessor) {
                    data = ((TransformProcessor) processor).transformData(data);
                    //System.out.println("Процесс трансформации...");
                }
            });
        }

        executor.shutdown();
        try {
            executor.awaitTermination(30, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void saveData(String destination) {
        System.out.println("Сохранить в: " + destination);
        // Сохранение в новый файл
        for (String s : data) {
            try(FileWriter writer = new FileWriter(destination, true))
            {
                // запись всей строки
                writer.write(s);
                writer.append('\n');

                writer.flush();
            }
            catch(IOException ex){
                System.out.println(ex.getMessage());
            }
        }
        // Вывод на экран
        data.forEach(System.out::println);
    }
}

public class DataManagerClass {
    public static void main(String[] args) throws IOException {
        final int NUM_THREADS = 4; // Количество потоков


        DataManager dataManager = new DataManager();
        dataManager.registerDataProcessor(new FilterProcessor());
        dataManager.registerDataProcessor(new TransformProcessor());

        final int ARRAY_SIZE = dataManager.loadData("C:\\Users\\weinm\\IdeaProjects\\lab_works\\src\\_The_Last_Breath\\replicant_test.txt");
        ExecutorService executor = Executors.newFixedThreadPool(NUM_THREADS);

        // Разделение массива на равные части и отправка каждой части в отдельный поток
        int chunkSize = ARRAY_SIZE / NUM_THREADS;
        AtomicInteger sum = new AtomicInteger();

        for (int i = 0; i < NUM_THREADS; i++) {
            final int threadIndex = i;

            int start = i * chunkSize;
            int end = (i == NUM_THREADS - 1) ? ARRAY_SIZE : start + chunkSize;

            executor.execute(() -> {
                int localSum = 0;
                for (int j = start; j < end; j++) {
                    localSum += 1;
                    dataManager.processData();
                }
                System.out.println("Поток: " + threadIndex + " Контрольное значение : " + localSum);
                synchronized (MultiThreadArraySum.class) {
                    sum.addAndGet(localSum);
                    dataManager.saveData("C:\\Users\\weinm\\IdeaProjects\\lab_works\\src\\_The_Last_Breath\\replicant_destination.txt");
                }

                executor.shutdown();
            });
        }
    }
}


