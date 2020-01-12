import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.*;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.*;

public class Main {

//    Домашнее задание по многопоточности (№1):
//    Подсчет топ 100 слов (по частоте встречаемости) параллельно.
//    Количество потоков = Runtime.getRuntime().availableProcessors()
//    Каждый поток собирает результат в свой map, затем добавляет в общий map (не забывайте про синхронизацию).
//    Поток, который создавал другие потоки должен ожидать их завершения и выводить результат (топ 100 из общей мапы) в консоль.

    public static void main(String[] args) throws IOException {

        File file = new File("resources/wp.txt");

//        int threadNumbers = Runtime.getRuntime().availableProcessors();

        ArrayList<String> wpListOfWords = Files.lines(file.toPath())
                .map(line -> line.replaceAll("\\p{Punct}", " ").toLowerCase().trim())
                .flatMap(line -> Arrays.stream(line.split(" ")))
                .collect(toCollection(ArrayList::new));

        ArrayList<String> findingRandomWord = wpListOfWords.stream().distinct().collect(toCollection(ArrayList::new));

        int topNumber = 10;   // Количество слов

        CommonMap commonMap = new CommonMap();
        System.out.printf("Топ %d самых часто встречающихся случайных слов и их количество в романе \"Война и мир\": \n", topNumber);

        for (int i = 0; i < topNumber; i++) {
            new Thread (new CountingThread(wpListOfWords, findingRandomWord, commonMap, topNumber)).start();
        }
    }

    static class CountingThread implements Runnable {

        private Map<String, Integer> commonMap = new HashMap<>();
        private CommonMap cMap;
        private Map<String, Integer> listOfRandomWordsFromFile = new HashMap<>();
        private ArrayList<String> findingRandomWord = new ArrayList<>();
        private ArrayList<String> wpListOfWords = new ArrayList<>();
        private Random rnd = new Random();
        private int topNumber;
//        private int threadNumbers = Runtime.getRuntime().availableProcessors();

        public CountingThread(ArrayList<String> basicList, ArrayList<String> uniqueWordsList, CommonMap cMap, int topNumber) {
            this.wpListOfWords.addAll(basicList);
            this.findingRandomWord.addAll(uniqueWordsList);
            this.topNumber = topNumber;
            this.cMap = cMap;
        }

        @Override
        public void run() {
            // сначала подсчет в своей мапе
            for (int i = 0; i < topNumber; i++) {
                String oneRandomWord = findingRandomWord.get(rnd.nextInt(findingRandomWord.size()));
                int oneRandomWordCounter = (int) wpListOfWords.stream().filter(w -> w.equals(oneRandomWord)).count();

                listOfRandomWordsFromFile.put(oneRandomWord, oneRandomWordCounter);
            }

            String commonWord = Collections.max(listOfRandomWordsFromFile.entrySet(), (entry1, entry2) -> entry1.getValue() - entry2.getValue()).getKey();
            commonMap.put(commonWord, listOfRandomWordsFromFile.get(commonWord));

            // потом работа с общей мапой
            synchronized (cMap) {
                try {

                    cMap.setCommonMap(commonMap);
                    cMap.getCommonMap(topNumber);

                } catch (NullPointerException e) {
                    System.out.println("Ошибка выполнения метода run() " + e);
                }
            }
        }
    }

    static class CommonMap {

        private Map<String, Integer> commonMap = new HashMap<>();

        public CommonMap() {}

        public synchronized void setCommonMap(Map<String, Integer> commonMap) {
            this.commonMap.putAll(commonMap);
        }

        public void getCommonMap(int topNumber) {
            Map<String, Integer> result = this.commonMap.entrySet()
                    .stream()
                    .sorted(Map.Entry.comparingByValue((o1, o2) -> o2 - o1))
                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue,
                            (oldValue, newValue) -> oldValue, LinkedHashMap::new));

            if (result.size() == topNumber) {
                for (Map.Entry<String, Integer> map : result.entrySet()) {
                    System.out.printf("%s - %d\n", map.getKey(), map.getValue());
                }
            }
        }
    }
}