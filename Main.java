import java.util.*;
import java.net.*;
import java.io.*;
import java.util.regex.*;

public class Main {

    public static void main(String[] args) throws Exception {

        String regNo = "2024CS101";

        File flag = new File("already_run.txt");
        if (flag.exists()) {
            System.out.println("Already executed once. Stop.");
            return;
        }

        Set<String> seen = new HashSet<>();
        Map<String, Integer> scores = new HashMap<>();

        for (int i = 0; i < 10; i++) {
            try {
                String urlStr = "https://devapigw.vidalhealthtpa.com/srm-quiz-task/quiz/messages?regNo="
                        + regNo + "&poll=" + i;

                URL url = new URL(urlStr);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");

                if (conn.getResponseCode() == 200) {
                    BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                    StringBuilder response = new StringBuilder();
                    String line;

                    while ((line = br.readLine()) != null) {
                        response.append(line);
                    }
                    br.close();

                    Pattern pattern = Pattern.compile("\\{\"roundId\":\"(.*?)\",\"participant\":\"(.*?)\",\"score\":(\\d+)\\}");
                    Matcher matcher = pattern.matcher(response.toString());

                    while (matcher.find()) {
                        String roundId = matcher.group(1);
                        String participant = matcher.group(2);
                        int score = Integer.parseInt(matcher.group(3));

                        String key = roundId + "_" + participant;

                        if (!seen.contains(key)) {
                            seen.add(key);
                            scores.put(participant, scores.getOrDefault(participant, 0) + score);
                        }
                    }
                }

                System.out.println("Poll " + i + " done");

            } catch (Exception e) {
                System.out.println("Error: " + e.getMessage());
            }

            Thread.sleep(5000);
        }

        List<Map.Entry<String, Integer>> list = new ArrayList<>(scores.entrySet());
        list.sort((a, b) -> b.getValue().compareTo(a.getValue()));

        System.out.println("\nLeaderboard:");
        for (Map.Entry<String, Integer> e : list) {
            System.out.println(e.getKey() + " → " + e.getValue());
        }

        int total = scores.values().stream().mapToInt(Integer::intValue).sum();
        System.out.println("\nTotal Score = " + total);

        String submitUrl = "https://devapigw.vidalhealthtpa.com/srm-quiz-task/quiz/submit";

        URL url = new URL(submitUrl);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();

        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setDoOutput(true);

        StringBuilder json = new StringBuilder();
        json.append("{\"regNo\":\"").append(regNo).append("\",\"leaderboard\":[");

        for (int i = 0; i < list.size(); i++) {
            Map.Entry<String, Integer> e = list.get(i);

            json.append("{\"participant\":\"")
                    .append(e.getKey())
                    .append("\",\"totalScore\":")
                    .append(e.getValue())
                    .append("}");

            if (i < list.size() - 1) json.append(",");
        }

        json.append("]}");

        OutputStream os = conn.getOutputStream();
        os.write(json.toString().getBytes());
        os.flush();

        BufferedReader br = new BufferedReader(
                new InputStreamReader(conn.getInputStream())
        );

        String output;
        while ((output = br.readLine()) != null) {
            System.out.println(output);
        }

        flag.createNewFile();
    }
}